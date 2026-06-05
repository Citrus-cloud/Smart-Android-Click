package com.clickflow.android.core

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.clickflow.android.R
import com.clickflow.android.audit.AuditEvent
import com.clickflow.android.audit.AuditLogManager
import com.clickflow.android.audit.AuditSeverity
import com.clickflow.android.audit.AuditSummary
import com.clickflow.android.audit.AuditType
import com.clickflow.android.backup.BackupImportResult
import com.clickflow.android.backup.BackupManager
import com.clickflow.android.backup.BackupPreview
import com.clickflow.android.backup.ImportStrategy
import com.clickflow.android.diagnostics.DiagnosticsManager
import com.clickflow.android.diagnostics.DiagnosticsState
import com.clickflow.android.permissions.PermissionStatus
import com.clickflow.android.permissions.PermissionsManager
import com.clickflow.android.profiles.DeleteResult
import com.clickflow.android.profiles.Profile
import com.clickflow.android.profiles.ProfileInput
import com.clickflow.android.profiles.ProfileManager
import com.clickflow.android.profiles.ProfileRepository
import com.clickflow.android.profiles.ProfileValidationErrors
import com.clickflow.android.realtap.RealTapController
import com.clickflow.android.safety.SafetyCenter
import com.clickflow.android.safety.SafetyGate
import com.clickflow.android.scenarios.ActionInput
import com.clickflow.android.scenarios.ActionValidationErrors
import com.clickflow.android.scenarios.Scenario
import com.clickflow.android.scenarios.ScenarioActionType
import com.clickflow.android.scenarios.ScenarioInput
import com.clickflow.android.scenarios.ScenarioManager
import com.clickflow.android.scenarios.ScenarioRepository
import com.clickflow.android.scenarios.ScenarioType
import com.clickflow.android.scenarios.ScenarioValidationErrors
import com.clickflow.android.scenarios.SimMessages
import com.clickflow.android.scenarios.SimulationEngine
import com.clickflow.android.scenarios.SimulationProgress
import com.clickflow.android.scenarios.SimulationStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.UUID
import kotlin.math.roundToInt

/** Screens reachable in the app. */
enum class Screen {
   HOME, ADVANCED, ABOUT, SCENARIOS, SCENARIO_DETAIL, SCENARIO_FORM, ACTION_FORM,
   PROFILES, PROFILE_FORM, BACKUP, SAFETY, DIAGNOSTICS, AUDIT_LOG, PERMISSIONS,
   REAL_TAP_PROTOTYPE,
}

data class ScenarioFormState(
   val editingId: String? = null,
   val name: String = "",
   val repeatCount: String = "1",
   val intervalMs: String = "500",
) {
   val isEditing: Boolean get() = editingId != null
   fun toInput(): ScenarioInput = ScenarioInput(
       name = name,
       repeatCount = repeatCount.trim().toIntOrNull() ?: 0,
       intervalMs = intervalMs.trim().toLongOrNull() ?: 0L,
       type = ScenarioType.MULTI_STEP_SIMULATION,
   )
}

data class ActionFormState(
   val type: ScenarioActionType = ScenarioActionType.SIMULATED_TAP,
   val x: String = "0",
   val y: String = "0",
   val label: String = "",
   val durationMs: String = "500",
   val message: String = "",
) {
   fun toInput(): ActionInput = ActionInput(
       type = type,
       x = x.trim().toIntOrNull() ?: -1,
       y = y.trim().toIntOrNull() ?: -1,
       durationMs = durationMs.trim().toLongOrNull() ?: -1L,
       message = message,
       label = label,
   )
}

data class ProfileFormState(
   val editingId: String? = null,
   val name: String = "",
   val description: String = "",
) {
   val isEditing: Boolean get() = editingId != null
   fun toInput(): ProfileInput = ProfileInput(name = name, description = description)
}

data class UiMessage(val key: String)

enum class RealTapSessionState { INACTIVE, ACTIVE }

data class RealTapConsent(
   val x: Int,
   val y: Int,
   val requestedAtMs: Long,
   val expiresAtMs: Long,
)

enum class RealTapDispatchResult {
   DISPATCHED,
   BLOCKED_BY_GATE,
   BLOCKED_NO_SERVICE,
   BLOCKED_INVALID_CONSENT,
   DISPATCH_CANCELLED,
   DISPATCH_FAILED,
}

data class SafetyReviewState(
   val checked: List<Boolean> = List(10) { false },
) {
   val allPassed: Boolean get() = checked.all { it }
   fun toggled(index: Int): SafetyReviewState {
       if (index !in checked.indices) return this
       val next = checked.toMutableList().also { it[index] = !it[index] }
       return copy(checked = next)
   }
   fun itemsLocalized(): List<ReviewLine> = LABELS.mapIndexed { i, label ->
       ReviewLine(label = label, checked = checked.getOrElse(i) { false })
   }
   data class ReviewLine(val label: String, val checked: Boolean)
   private companion object {
       val LABELS = listOf(
           "Я понимаю, что это прототип и реальный dispatch отключён",
           "Я не запускаю это на критичном устройстве",
           "У меня есть физический доступ к устройству",
           "Я готов нажать аварийную остановку в любой момент",
           "Я не использую сторонние оверлеи / accessibility services",
           "Я понимаю риск ложных тапов",
           "Я не оставлю сессию работать без присмотра",
           "Я согласен с тем, что каждый тап аудируется",
           "Я не использую это для обхода защит сторонних приложений",
           "Я принимаю всю ответственность за последствия",
       )
   }
}

class ClickFlowViewModel(app: Application) : AndroidViewModel(app) {

   private val appRef = app
   private val gate = SafetyGate()

   private val scenarioRepo = ScenarioRepository(File(app.filesDir, ScenarioRepository.FILE_NAME))
   private val scenarioManager = ScenarioManager(scenarioRepo)

   private val profileRepo = ProfileRepository(File(app.filesDir, ProfileRepository.FILE_NAME))
   private val profileManager = ProfileManager(profileRepo)

   private val auditLog = AuditLogManager(File(app.filesDir, "audit-log.jsonl"))
   private val backupManager = BackupManager()
   private val diagnosticsManager = DiagnosticsManager()
   private val realTapController = RealTapController(gate, auditLog)

   private val permissionsManager = PermissionsManager(app)
   private val _permissionStatus = MutableStateFlow(PermissionStatus.EMPTY)
   val permissionStatus: StateFlow<PermissionStatus> = _permissionStatus.asStateFlow()

   private val messages = SimMessages(
       started = { name -> app.getString(R.string.scenario_started, name) },
       completed = { name -> app.getString(R.string.scenario_completed, name) },
       stopped = { app.getString(R.string.scenario_stopped) },
       emergencyStopped = { app.getString(R.string.scenario_emergency_stopped) },
       simulatedTap = { x, y, label ->
           val base = app.getString(R.string.simulated_tap_at, x, y)
           if (label.isNullOrBlank()) base else "$base — $label"
       },
       waited = { ms -> app.getString(R.string.waited_ms, ms) },
   )
   private val engine = SimulationEngine(gate, auditLog, messages)

   val safetyCenter = SafetyCenter(gate)
   fun currentSafetyCenter(): SafetyCenter = SafetyCenter(gate, _permissionStatus.value)

   private val _screen = MutableStateFlow(Screen.HOME)
   val screen: StateFlow<Screen> = _screen.asStateFlow()

   val scenarios: StateFlow<List<Scenario>> = scenarioManager.scenarios
   val profiles: StateFlow<List<Profile>> = profileManager.profiles
   val status: StateFlow<SimulationStatus> = engine.status
   val progress: StateFlow<SimulationProgress> = engine.progress
   val auditEvents: StateFlow<List<AuditEvent>> = auditLog.events

   private val _selectedScenarioId = MutableStateFlow<String?>(null)
   val selectedScenarioId: StateFlow<String?> = _selectedScenarioId.asStateFlow()

   private val _scenarioForm = MutableStateFlow(ScenarioFormState())
   val scenarioForm: StateFlow<ScenarioFormState> = _scenarioForm.asStateFlow()
   private val _scenarioErrors = MutableStateFlow(ScenarioValidationErrors())
   val scenarioErrors: StateFlow<ScenarioValidationErrors> = _scenarioErrors.asStateFlow()

   private val _actionForm = MutableStateFlow(ActionFormState())
   val actionForm: StateFlow<ActionFormState> = _actionForm.asStateFlow()
   private val _editingActionId = MutableStateFlow<String?>(null)
   private val _actionErrors = MutableStateFlow(ActionValidationErrors())
   val actionErrors: StateFlow<ActionValidationErrors> = _actionErrors.asStateFlow()

   private val _profileForm = MutableStateFlow(ProfileFormState())
   val profileForm: StateFlow<ProfileFormState> = _profileForm.asStateFlow()
   private val _profileErrors = MutableStateFlow(ProfileValidationErrors())
   val profileErrors: StateFlow<ProfileValidationErrors> = _profileErrors.asStateFlow()

   private val _runHistory = MutableStateFlow<List<String>>(emptyList())
   val runHistory: StateFlow<List<String>> = _runHistory.asStateFlow()

   private val _message = MutableStateFlow<UiMessage?>(null)
   val message: StateFlow<UiMessage?> = _message.asStateFlow()

   private val _backupJsonText = MutableStateFlow("")
   val backupJsonText: StateFlow<String> = _backupJsonText.asStateFlow()
   private val _backupPreview = MutableStateFlow<BackupPreview?>(null)
   val backupPreview: StateFlow<BackupPreview?> = _backupPreview.asStateFlow()
   private val _backupImportResult = MutableStateFlow<BackupImportResult?>(null)
   val backupImportResult: StateFlow<BackupImportResult?> = _backupImportResult.asStateFlow()
   private val _replaceAllConfirmed = MutableStateFlow(false)
   val replaceAllConfirmed: StateFlow<Boolean> = _replaceAllConfirmed.asStateFlow()
   private var lastBackupExportAt: Long? = null
   private var lastBackupImportAt: Long? = null
   private var invalidImportItemsLast: Int = 0

   private val _markerX = MutableStateFlow(0.5f)
   val markerX: StateFlow<Float> = _markerX.asStateFlow()
   private val _markerY = MutableStateFlow(0.5f)
   val markerY: StateFlow<Float> = _markerY.asStateFlow()
   private val _quickIntervalMs = MutableStateFlow(500L)
   val quickIntervalMs: StateFlow<Long> = _quickIntervalMs.asStateFlow()
   private val _quickRepeatCount = MutableStateFlow(10)
   val quickRepeatCount: StateFlow<Int> = _quickRepeatCount.asStateFlow()
   private var quickScenarioId: String? = null

   private val _safetyReview = MutableStateFlow(SafetyReviewState())
   val safetyReview: StateFlow<SafetyReviewState> = _safetyReview.asStateFlow()
   private val _realTapSession = MutableStateFlow(RealTapSessionState.INACTIVE)
   val realTapSession: StateFlow<RealTapSessionState> = _realTapSession.asStateFlow()
   private val _realTapConsent = MutableStateFlow<RealTapConsent?>(null)
   val realTapConsent: StateFlow<RealTapConsent?> = _realTapConsent.asStateFlow()
   private var currentRealTapSessionId: String? = null

   private val _safetyGateReasons = MutableStateFlow<List<String>>(emptyList())
   val safetyGateReasons: StateFlow<List<String>> = _safetyGateReasons.asStateFlow()
   private val _lastDispatchResult = MutableStateFlow<RealTapDispatchResult?>(null)
   val lastDispatchResult: StateFlow<RealTapDispatchResult?> = _lastDispatchResult.asStateFlow()

   val scenarioStorageReady: Boolean get() = scenarioManager.storageReady
   val corruptedScenarioRecovered: Boolean get() = scenarioManager.corruptedStorageRecovered
   val storageMigrated: Boolean get() = scenarioManager.storageMigrated
   val profileStorageReady: Boolean get() = profileManager.storageReady
   val corruptedProfileStorageRecovered: Boolean get() = profileManager.corruptedStorageRecovered
   val auditStorageReady: Boolean get() = auditLog.storageReady
   val corruptedAuditRecovered: Boolean get() = auditLog.corruptedAuditRecovered

   companion object {
       const val QUICK_NAME = "Quick clicker"
   }

   init {
       profileManager.load()
       auditLog.loadAuditEvents()
       scenarioManager.load()
       if (scenarioManager.corruptedStorageRecovered)
           auditLog.log(AuditType.STORAGE_RECOVERED, AuditSeverity.WARNING, "Corrupted scenario storage recovered")
       if (scenarioManager.storageMigrated)
           auditLog.log(AuditType.STORAGE_MIGRATED, AuditSeverity.INFO, "Scenarios migrated (schema v2 / profileId)")
       if (profileManager.corruptedStorageRecovered)
           auditLog.log(AuditType.STORAGE_RECOVERED, AuditSeverity.WARNING, "Corrupted profile storage recovered")
       ensureQuickClicker()
       refreshPermissions()
       refreshSafetyGateReasons()
   }

   fun refreshPermissions() {
       val status = permissionsManager.refresh()
       _permissionStatus.value = status
       gate.updateAccessibility(status.accessibilityEnabled)
       refreshSafetyGateReasons()
   }

   fun openOverlaySettings() {
       runCatching {
           val intent = permissionsManager.overlaySettingsIntent().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
           appRef.startActivity(intent)
       }
   }

   fun openAccessibilitySettings() {
       runCatching {
           val intent = permissionsManager.accessibilitySettingsIntent().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
           appRef.startActivity(intent)
       }
   }

   fun ensureQuickClicker() {
       val pid = activeProfileId()
       var q = scenarioManager.scenariosForProfile(pid).firstOrNull { it.name == QUICK_NAME }
       if (q == null) {
           scenarioManager.createScenario(ScenarioInput(QUICK_NAME, _quickRepeatCount.value, _quickIntervalMs.value), pid)
           q = scenarioManager.scenariosForProfile(pid).lastOrNull { it.name == QUICK_NAME }
           q?.let { sc ->
               sc.actions.firstOrNull()?.let { a ->
                   scenarioManager.updateAction(sc.id, a.id, ActionInput(ScenarioActionType.SIMULATED_TAP, x = 500, y = 500, label = "marker"))
               }
           }
           q = scenarioManager.scenariosForProfile(pid).firstOrNull { it.name == QUICK_NAME }
       }
       quickScenarioId = q?.id
       q?.let { sc ->
           _quickIntervalMs.value = sc.settings.intervalMs
           _quickRepeatCount.value = sc.settings.repeatCount
           val tap = sc.actions.firstOrNull { it.type == ScenarioActionType.SIMULATED_TAP }
           _markerX.value = ((tap?.x ?: 500).coerceIn(0, 1000)) / 1000f
           _markerY.value = ((tap?.y ?: 500).coerceIn(0, 1000)) / 1000f
       }
   }

   fun setMarker(fx: Float, fy: Float) {
       _markerX.value = fx.coerceIn(0f, 1f)
       _markerY.value = fy.coerceIn(0f, 1f)
   }

   fun centerMarker() { setMarker(0.5f, 0.5f); commitMarker() }

   fun commitMarker() {
       val id = quickScenarioId ?: return
       val tap = scenarioManager.byId(id)?.actions?.firstOrNull { it.type == ScenarioActionType.SIMULATED_TAP } ?: return
       scenarioManager.updateAction(
           id, tap.id,
           ActionInput(
               ScenarioActionType.SIMULATED_TAP,
               x = (_markerX.value * 1000).roundToInt(),
               y = (_markerY.value * 1000).roundToInt(),
               label = "marker",
           ),
       )
   }

   private fun persistQuickMeta() {
       val id = quickScenarioId ?: return
       scenarioManager.updateScenarioMeta(id, ScenarioInput(QUICK_NAME, _quickRepeatCount.value, _quickIntervalMs.value))
   }

   fun setQuickInterval(ms: Long) { _quickIntervalMs.value = ms.coerceAtLeast(100L); persistQuickMeta() }
   fun adjustQuickInterval(deltaMs: Long) = setQuickInterval(_quickIntervalMs.value + deltaMs)
   fun setQuickCount(n: Int) { _quickRepeatCount.value = n.coerceIn(1, 1000); persistQuickMeta() }
   fun adjustQuickCount(delta: Int) = setQuickCount(_quickRepeatCount.value + delta)

   fun startQuickSimulation() {
       ensureQuickClicker()
       commitMarker()
       persistQuickMeta()
       val id = quickScenarioId ?: return
       scenarioManager.setActiveScenario(id)
       scenarioManager.byId(id)?.let { startRun(it) }
   }

   fun navigateTo(screen: Screen) {
       _screen.value = screen
       if (screen == Screen.PERMISSIONS) refreshPermissions()
       if (screen == Screen.REAL_TAP_PROTOTYPE) refreshSafetyGateReasons()
   }
   fun consumeMessage() { _message.value = null }

   fun activeProfile(): Profile? = profileManager.getActiveProfile()
   fun activeProfileId(): String = profileManager.activeProfileId()
   fun selectedScenario(): Scenario? = _selectedScenarioId.value?.let { scenarioManager.byId(it) }
   fun scenariosForActiveProfile(): List<Scenario> = scenarioManager.scenariosForProfile(activeProfileId())
   fun activeScenario(): Scenario? = scenarioManager.getActiveScenarioForProfile(activeProfileId())

   fun openCreateProfile() {
       _profileForm.value = ProfileFormState()
       _profileErrors.value = ProfileValidationErrors()
       _screen.value = Screen.PROFILE_FORM
   }

   fun openEditProfile(id: String) {
       val p = profileManager.byId(id) ?: return
       _profileForm.value = ProfileFormState(editingId = p.id, name = p.name, description = p.description)
       _profileErrors.value = ProfileValidationErrors()
       _screen.value = Screen.PROFILE_FORM
   }

   fun updateProfileForm(transform: (ProfileFormState) -> ProfileFormState) { _profileForm.value = transform(_profileForm.value) }

   fun saveProfileForm() {
       val form = _profileForm.value
       val input = form.toInput()
       val errors = if (form.isEditing) profileManager.updateProfile(form.editingId!!, input) else profileManager.createProfile(input)
       _profileErrors.value = errors
       if (!errors.hasErrors) {
           _message.value = UiMessage("profile_saved")
           _screen.value = Screen.PROFILES
       }
   }

   fun cancelProfileForm() {
       _profileErrors.value = ProfileValidationErrors()
       _screen.value = Screen.PROFILES
   }

   fun selectProfile(id: String) {
       profileManager.setActiveProfile(id)
       val inProfile = scenarioManager.scenariosForProfile(id)
       if (inProfile.none { it.isActive } && inProfile.isNotEmpty()) scenarioManager.setActiveScenario(inProfile.first().id)
       ensureQuickClicker()
       _message.value = UiMessage("active_profile")
   }

   fun deleteProfile(id: String) {
       val count = scenarioManager.countForProfile(id)
       when (val r = profileManager.deleteProfile(id, count)) {
           is DeleteResult.Success -> _message.value = UiMessage("profile_deleted")
           is DeleteResult.Blocked -> _message.value = UiMessage(r.reasonKey)
       }
   }

   fun resetProfiles() { profileManager.resetProfiles(); _message.value = UiMessage("profile_saved") }
   fun scenarioCountForProfile(id: String): Int = scenarioManager.countForProfile(id)

   fun openScenarioDetail(id: String) { _selectedScenarioId.value = id; _screen.value = Screen.SCENARIO_DETAIL }
   fun selectScenario(id: String) { scenarioManager.setActiveScenario(id); _message.value = UiMessage("active_scenario") }
   fun deleteScenario(id: String) { scenarioManager.deleteScenario(id); _message.value = UiMessage("scenario_deleted") }
   fun resetScenarios() { scenarioManager.resetToDefaults(); _message.value = UiMessage("scenario_saved") }

   fun openCreateScenario() {
       _scenarioForm.value = ScenarioFormState()
       _scenarioErrors.value = ScenarioValidationErrors()
       _screen.value = Screen.SCENARIO_FORM
   }

   fun openEditScenario(id: String) {
       val s = scenarioManager.byId(id) ?: return
       _scenarioForm.value = ScenarioFormState(editingId = s.id, name = s.name, repeatCount = s.settings.repeatCount.toString(), intervalMs = s.settings.intervalMs.toString())
       _scenarioErrors.value = ScenarioValidationErrors()
       _screen.value = Screen.SCENARIO_FORM
   }

   fun updateScenarioForm(transform: (ScenarioFormState) -> ScenarioFormState) { _scenarioForm.value = transform(_scenarioForm.value) }

   fun saveScenarioForm() {
       val form = _scenarioForm.value
       val input = form.toInput()
       val errors = if (form.isEditing) scenarioManager.updateScenarioMeta(form.editingId!!, input) else scenarioManager.createScenario(input, activeProfileId())
       _scenarioErrors.value = errors
       if (!errors.hasErrors) {
           _message.value = UiMessage("scenario_saved")
           if (!form.isEditing) _selectedScenarioId.value = scenarioManager.getScenarios().lastOrNull()?.id
           _screen.value = Screen.SCENARIO_DETAIL
       }
   }

   fun cancelScenarioForm() {
       _scenarioErrors.value = ScenarioValidationErrors()
       _screen.value = if (_selectedScenarioId.value != null) Screen.SCENARIO_DETAIL else Screen.SCENARIOS
   }

   fun openAddAction(scenarioId: String, type: ScenarioActionType) {
       _selectedScenarioId.value = scenarioId
       _editingActionId.value = null
       _actionForm.value = ActionFormState(type = type)
       _actionErrors.value = ActionValidationErrors()
       _screen.value = Screen.ACTION_FORM
   }

   fun openEditAction(scenarioId: String, actionId: String) {
       _selectedScenarioId.value = scenarioId
       val a = scenarioManager.byId(scenarioId)?.actions?.firstOrNull { it.id == actionId } ?: return
       _editingActionId.value = actionId
       _actionForm.value = ActionFormState(type = a.type, x = (a.x ?: 0).toString(), y = (a.y ?: 0).toString(), label = a.label.orEmpty(), durationMs = (a.durationMs ?: 500L).toString(), message = a.message.orEmpty())
       _actionErrors.value = ActionValidationErrors()
       _screen.value = Screen.ACTION_FORM
   }

   fun updateActionForm(transform: (ActionFormState) -> ActionFormState) { _actionForm.value = transform(_actionForm.value) }

   fun saveActionForm() {
       val scenarioId = _selectedScenarioId.value ?: return
       val input = _actionForm.value.toInput()
       val errors = scenarioManager.validateAction(input)
       _actionErrors.value = errors
       if (errors.hasErrors) {
           auditLog.log(AuditType.VALIDATION_FAILED, AuditSeverity.WARNING, "Action validation failed", scenarioId = scenarioId)
           return
       }
       val editingId = _editingActionId.value
       val ok = if (editingId != null) scenarioManager.updateAction(scenarioId, editingId, input) else scenarioManager.addAction(scenarioId, input)
       if (ok) { _message.value = UiMessage("scenario_saved"); _screen.value = Screen.SCENARIO_DETAIL }
   }

   fun cancelActionForm() { _actionErrors.value = ActionValidationErrors(); _screen.value = Screen.SCENARIO_DETAIL }
   fun deleteAction(scenarioId: String, actionId: String) = scenarioManager.deleteAction(scenarioId, actionId)
   fun moveActionUp(scenarioId: String, actionId: String) = scenarioManager.moveAction(scenarioId, actionId, up = true)
   fun moveActionDown(scenarioId: String, actionId: String) = scenarioManager.moveAction(scenarioId, actionId, up = false)
   fun duplicateAction(scenarioId: String, actionId: String) = scenarioManager.duplicateAction(scenarioId, actionId)

   fun runActiveScenarioSimulation(): Boolean {
       val active = activeScenario()
       if (active == null) { _message.value = UiMessage("no_active_scenario"); return false }
       startRun(active); return true
   }

   fun runScenarioSimulation(id: String) {
       scenarioManager.setActiveScenario(id)
       scenarioManager.byId(id)?.let { startRun(it) }
   }

   private fun startRun(scenario: Scenario) {
       val name = scenario.name
       engine.startSimulation(scenario, viewModelScope) { terminal ->
           _runHistory.value = (listOf("$name — ${terminal.name.lowercase()}") + _runHistory.value).take(50)
       }
   }

   fun stopSimulation() { engine.stopSimulation() }

   fun emergencyStop() {
       engine.emergencyStop()
       if (_realTapSession.value == RealTapSessionState.ACTIVE) {
           realTapController.recordSessionEnded(currentRealTapSessionId, "emergency_stop")
           _realTapSession.value = RealTapSessionState.INACTIVE
           currentRealTapSessionId = null
           _realTapConsent.value = null
       }
       gate.resetPrototypeFlags()
       refreshSafetyGateReasons()
   }

   fun clearAuditLog() { auditLog.clearEvents() }
   fun clearRunHistory() { _runHistory.value = emptyList() }
   fun auditSummary(): AuditSummary = auditLog.getAuditSummary()
   fun exportAuditLogText(): String = auditLog.exportAsText()

   fun shareAuditLog(): Boolean = runCatching {
       val send = Intent(Intent.ACTION_SEND).apply {
           type = "text/plain"
           putExtra(Intent.EXTRA_SUBJECT, "ClickFlow Android Audit Log")
           putExtra(Intent.EXTRA_TEXT, auditLog.exportAsText())
       }
       val chooser = Intent.createChooser(send, appRef.getString(R.string.share_audit_log)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
       appRef.startActivity(chooser)
       _message.value = UiMessage("audit_log_exported")
       true
   }.getOrElse {
       _message.value = UiMessage("audit_log_export_failed")
       false
   }

   fun attemptRealTapBlocked(): Boolean {
       auditLog.log(AuditType.SAFETY_REAL_TAP_BLOCKED, AuditSeverity.SAFETY, "Real tap attempt blocked — not implemented")
       return gate.attemptRealTap()
   }

   fun toggleSafetyReviewItem(index: Int) {
       val next = _safetyReview.value.toggled(index)
       _safetyReview.value = next
       gate.updateReviewPassed(next.allPassed)
       if (next.allPassed) realTapController.recordSafetyReviewPassed() else realTapController.recordSafetyReviewFailed(next.checked.count { !it })
       refreshSafetyGateReasons()
   }

   fun startRealTapSession() {
       if (_realTapSession.value == RealTapSessionState.ACTIVE) return
       if (!_safetyReview.value.allPassed) {
           realTapController.recordSafetyReviewFailed(_safetyReview.value.checked.count { !it })
           _message.value = UiMessage("real_tap_audit_dispatch_blocked")
           return
       }
       val sid = UUID.randomUUID().toString()
       currentRealTapSessionId = sid
       _realTapSession.value = RealTapSessionState.ACTIVE
       _realTapConsent.value = null
       gate.updateSession(true)
       refreshSafetyGateReasons()
       realTapController.recordSessionStarted(sid)
       _message.value = UiMessage("real_tap_audit_session_started")
   }

   fun endRealTapSession() {
       if (_realTapSession.value == RealTapSessionState.INACTIVE) return
       realTapController.recordSessionEnded(currentRealTapSessionId, "user_ended")
       _realTapSession.value = RealTapSessionState.INACTIVE
       currentRealTapSessionId = null
       _realTapConsent.value = null
       gate.updateSession(false)
       gate.updateConsentFresh(false)
       refreshSafetyGateReasons()
       _message.value = UiMessage("real_tap_audit_session_ended")
   }

   fun requestRealTap() {
       if (_realTapSession.value != RealTapSessionState.ACTIVE) return
       if (_realTapConsent.value != null) return
       val now = System.currentTimeMillis()
       val x = (_markerX.value * 1000).roundToInt()
       val y = (_markerY.value * 1000).roundToInt()
       _realTapConsent.value = RealTapConsent(x = x, y = y, requestedAtMs = now, expiresAtMs = now + CONSENT_WINDOW_MS)
       gate.updateConsentFresh(true)
       refreshSafetyGateReasons()
       realTapController.recordConsentRequested(currentRealTapSessionId, x, y)
       _message.value = UiMessage("real_tap_audit_consent_requested")
   }

   fun confirmRealTap() {
       val consent = _realTapConsent.value ?: run {
           realTapController.recordConsentDeclined(currentRealTapSessionId, "no_pending_consent")
           _lastDispatchResult.value = RealTapDispatchResult.BLOCKED_INVALID_CONSENT
           return
       }
       val now = System.currentTimeMillis()
       if (now > consent.expiresAtMs) {
           realTapController.recordConsentDeclined(currentRealTapSessionId, "consent_expired")
           _realTapConsent.value = null
           gate.updateConsentFresh(false)
           refreshSafetyGateReasons()
           _lastDispatchResult.value = RealTapDispatchResult.BLOCKED_INVALID_CONSENT
           _message.value = UiMessage("real_tap_audit_consent_expired")
           return
       }
       val curX = (_markerX.value * 1000).roundToInt()
       val curY = (_markerY.value * 1000).roundToInt()
       if (consent.x != curX || consent.y != curY) {
           realTapController.recordConsentDeclined(currentRealTapSessionId, "marker_drift consent=(${consent.x},${consent.y}) current=($curX,$curY)")
           _realTapConsent.value = null
           gate.updateConsentFresh(false)
           refreshSafetyGateReasons()
           _lastDispatchResult.value = RealTapDispatchResult.BLOCKED_INVALID_CONSENT
           _message.value = UiMessage("real_tap_audit_dispatch_blocked")
           return
       }
       realTapController.recordConsentGiven(currentRealTapSessionId, consent.x, consent.y)
       val decision = realTapController.evaluate(RealTapController.Marker(consent.x, consent.y), currentRealTapSessionId)
       val result = when (decision) {
           RealTapController.Decision.ALLOWED -> RealTapDispatchResult.DISPATCHED
           RealTapController.Decision.BLOCKED_BY_GATE -> RealTapDispatchResult.BLOCKED_BY_GATE
           RealTapController.Decision.BLOCKED_NO_SERVICE -> RealTapDispatchResult.BLOCKED_NO_SERVICE
           RealTapController.Decision.BLOCKED_INVALID_CONSENT -> RealTapDispatchResult.BLOCKED_INVALID_CONSENT
       }
       _realTapConsent.value = null
       gate.updateConsentFresh(false)
       refreshSafetyGateReasons()
       _lastDispatchResult.value = result
       _message.value = UiMessage(if (result == RealTapDispatchResult.DISPATCHED) "real_tap_audit_consent_confirmed" else "real_tap_audit_dispatch_blocked")
   }

   fun cancelRealTap() {
       if (_realTapConsent.value == null) return
       realTapController.recordConsentDeclined(currentRealTapSessionId, "user_cancelled")
       _realTapConsent.value = null
       gate.updateConsentFresh(false)
       refreshSafetyGateReasons()
       _lastDispatchResult.value = RealTapDispatchResult.DISPATCH_CANCELLED
   }

   private fun refreshSafetyGateReasons() { _safetyGateReasons.value = gate.getSingleProtoBlockedReasons() }
   fun consumeLastDispatchResult() { _lastDispatchResult.value = null }

   fun openBackup() { _screen.value = Screen.BACKUP }
   fun updateBackupJsonText(text: String) { _backupJsonText.value = text }
   fun setReplaceAllConfirmed(value: Boolean) { _replaceAllConfirmed.value = value }

   fun clearBackupImportState() {
       _backupJsonText.value = ""
       _backupPreview.value = null
       _backupImportResult.value = null
       _replaceAllConfirmed.value = false
   }

   fun createBackupJson(): String = backupManager.createBackup(profiles.value, scenarios.value)

   fun shareBackupJson(): Boolean {
       auditLog.log(AuditType.BACKUP_EXPORT_REQUESTED, AuditSeverity.INFO, "Backup export requested (profiles=${profiles.value.size}, scenarios=${scenarios.value.size})")
       return runCatching {
           val json = createBackupJson()
           val send = Intent(Intent.ACTION_SEND).apply {
               type = "text/plain"
               putExtra(Intent.EXTRA_SUBJECT, "ClickFlow Android Backup")
               putExtra(Intent.EXTRA_TEXT, json)
           }
           val chooser = Intent.createChooser(send, appRef.getString(R.string.export_backup)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
           appRef.startActivity(chooser)
           lastBackupExportAt = System.currentTimeMillis()
           auditLog.log(AuditType.BACKUP_EXPORT_SHARED, AuditSeverity.INFO, "Backup shared as text")
           _message.value = UiMessage("backup_exported")
           true
       }.getOrElse {
           auditLog.log(AuditType.BACKUP_EXPORT_FAILED, AuditSeverity.WARNING, "Backup export failed")
           _message.value = UiMessage("backup_export_failed")
           false
       }
   }

   fun validateBackupJson() {
       auditLog.log(AuditType.BACKUP_IMPORT_VALIDATION_STARTED, AuditSeverity.INFO, "Backup validation started")
       val preview = backupManager.previewBackup(_backupJsonText.value)
       _backupPreview.value = preview
       invalidImportItemsLast = preview.invalidItemsCount
       if (!preview.valid) auditLog.log(AuditType.BACKUP_IMPORT_VALIDATION_FAILED, AuditSeverity.WARNING, "Backup validation failed (errors=${preview.errors.size})")
   }

   fun importBackup(strategy: ImportStrategy) {
       if (strategy == ImportStrategy.REPLACE_ALL_REQUIRE_CONFIRMATION && !_replaceAllConfirmed.value) {
           auditLog.log(AuditType.BACKUP_IMPORT_REPLACE_ALL_REQUESTED, AuditSeverity.WARNING, "Replace-all requested without confirmation")
           _message.value = UiMessage("replace_all_requires_confirmation")
           return
       }
       if (strategy == ImportStrategy.REPLACE_ALL_REQUIRE_CONFIRMATION) auditLog.log(AuditType.BACKUP_IMPORT_REPLACE_ALL_CONFIRMED, AuditSeverity.SAFETY, "Replace-all confirmed")
       val result = backupManager.importBackup(_backupJsonText.value, strategy, profiles.value, scenarios.value, _replaceAllConfirmed.value)
       _backupImportResult.value = result
       if (!result.success) {
           _message.value = UiMessage("backup_import_failed")
           return
       }
       profileManager.applyImported(result.mergedProfiles)
       scenarioManager.applyImported(result.mergedScenarios)
       if (result.skippedItems > 0) auditLog.log(AuditType.BACKUP_IMPORT_SKIPPED_INVALID_ITEM, AuditSeverity.WARNING, "Skipped ${result.skippedItems} invalid/conflicting item(s)")
       invalidImportItemsLast = result.skippedItems
       lastBackupImportAt = System.currentTimeMillis()
       auditLog.log(AuditType.BACKUP_IMPORT_COMPLETED, AuditSeverity.INFO, "Backup import completed (profiles=${result.importedProfiles}, scenarios=${result.importedScenarios}, skipped=${result.skippedItems})")
       _message.value = UiMessage("backup_import_completed")
   }

   fun diagnostics(): DiagnosticsState = diagnosticsManager.build(
       status = engine.getStatus(),
       progress = progress.value,
       scenariosCount = scenarios.value.size,
       activeScenario = activeScenario(),
       profilesCount = profiles.value.size,
       activeProfileName = activeProfile()?.name,
       auditEventsCount = auditLog.count(),
       lastAuditEventType = auditLog.lastType(),
       scenarioStorageReady = scenarioStorageReady,
       corruptedScenarioRecovered = corruptedScenarioRecovered,
       storageMigrated = storageMigrated,
       profileStorageReady = profileStorageReady,
       corruptedProfileStorageRecovered = corruptedProfileStorageRecovered,
       auditStorageReady = auditStorageReady,
       corruptedAuditRecovered = corruptedAuditRecovered,
       lastBackupExportAt = lastBackupExportAt,
       lastBackupImportAt = lastBackupImportAt,
       invalidImportItemsLast = invalidImportItemsLast,
       markerX = (_markerX.value * 1000).roundToInt(),
       markerY = (_markerY.value * 1000).roundToInt(),
       quickIntervalMs = _quickIntervalMs.value,
       quickRepeatCount = _quickRepeatCount.value,
       overlayEnabled = _permissionStatus.value.overlayGranted,
       accessibilityEnabled = _permissionStatus.value.accessibilityEnabled,
   )

   fun blockedReasons(): List<String> = gate.getBlockedReasons()
}
