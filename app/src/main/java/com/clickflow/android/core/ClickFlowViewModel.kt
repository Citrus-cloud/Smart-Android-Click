package com.clickflow.android.core

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.clickflow.android.R
import com.clickflow.android.audit.AuditEvent
import com.clickflow.android.audit.AuditLogManager
import com.clickflow.android.audit.AuditSeverity
import com.clickflow.android.audit.AuditType
import com.clickflow.android.diagnostics.DiagnosticsManager
import com.clickflow.android.diagnostics.DiagnosticsState
import com.clickflow.android.safety.SafetyCenter
import com.clickflow.android.safety.SafetyGate
import com.clickflow.android.scenarios.ActionInput
import com.clickflow.android.scenarios.ActionValidationErrors
import com.clickflow.android.scenarios.Scenario
import com.clickflow.android.scenarios.ScenarioAction
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

/** Screens reachable in the app. */
enum class Screen { HOME, SCENARIOS, SCENARIO_DETAIL, SCENARIO_FORM, ACTION_FORM, SAFETY, DIAGNOSTICS, AUDIT_LOG }

/** Editable metadata form state. */
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

/** Editable action form state. */
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

data class UiMessage(val key: String)

/**
 * Single app-wide state holder for ClickFlow Android (Step 54).
 *
 * Wires the multi-step scenario layer, simulation engine, audit log, safety gate, and diagnostics.
 * Holds NO real-input logic — it can only drive the simulation engine, which performs no real taps.
 */
class ClickFlowViewModel(app: Application) : AndroidViewModel(app) {

    private val gate = SafetyGate()
    private val repository = ScenarioRepository(File(app.filesDir, ScenarioRepository.FILE_NAME))
    private val scenarioManager = ScenarioManager(repository)
    private val auditLog = AuditLogManager()
    private val diagnosticsManager = DiagnosticsManager()

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

    private val _screen = MutableStateFlow(Screen.HOME)
    val screen: StateFlow<Screen> = _screen.asStateFlow()

    val scenarios: StateFlow<List<Scenario>> = scenarioManager.scenarios
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
    val editingActionId: StateFlow<String?> = _editingActionId.asStateFlow()

    private val _actionErrors = MutableStateFlow(ActionValidationErrors())
    val actionErrors: StateFlow<ActionValidationErrors> = _actionErrors.asStateFlow()

    private val _runHistory = MutableStateFlow<List<String>>(emptyList())
    val runHistory: StateFlow<List<String>> = _runHistory.asStateFlow()

    private val _message = MutableStateFlow<UiMessage?>(null)
    val message: StateFlow<UiMessage?> = _message.asStateFlow()

    val storageReady: Boolean get() = scenarioManager.storageReady
    val corruptedStorageRecovered: Boolean get() = scenarioManager.corruptedStorageRecovered
    val storageMigrated: Boolean get() = scenarioManager.storageMigrated

    init {
        scenarioManager.load()
        if (scenarioManager.corruptedStorageRecovered) {
            auditLog.log(AuditType.STORAGE_RECOVERED, AuditSeverity.WARNING,
                "Corrupted storage recovered to default scenario")
        }
        if (scenarioManager.storageMigrated) {
            auditLog.log(AuditType.STORAGE_MIGRATED, AuditSeverity.INFO,
                "Storage migrated to schema v2 (multi-step)")
        }
    }

    // ---- Navigation / messages --------------------------------------------

    fun navigateTo(screen: Screen) { _screen.value = screen }
    fun consumeMessage() { _message.value = null }

    fun selectedScenario(): Scenario? = _selectedScenarioId.value?.let { scenarioManager.byId(it) }
    fun activeScenario(): Scenario? = scenarioManager.getActiveScenario()

    // ---- Scenario list / selection ----------------------------------------

    fun openScenarioDetail(id: String) {
        _selectedScenarioId.value = id
        _screen.value = Screen.SCENARIO_DETAIL
    }

    fun selectScenario(id: String) {
        scenarioManager.setActiveScenario(id)
        _message.value = UiMessage("active_scenario")
    }

    fun deleteScenario(id: String) {
        scenarioManager.deleteScenario(id)
        _message.value = UiMessage("scenario_deleted")
    }

    fun resetScenarios() {
        scenarioManager.resetToDefaults()
        _message.value = UiMessage("scenario_saved")
    }

    // ---- Scenario metadata form -------------------------------------------

    fun openCreateScenario() {
        _scenarioForm.value = ScenarioFormState()
        _scenarioErrors.value = ScenarioValidationErrors()
        _screen.value = Screen.SCENARIO_FORM
    }

    fun openEditScenario(id: String) {
        val s = scenarioManager.byId(id) ?: return
        _scenarioForm.value = ScenarioFormState(
            editingId = s.id,
            name = s.name,
            repeatCount = s.settings.repeatCount.toString(),
            intervalMs = s.settings.intervalMs.toString(),
        )
        _scenarioErrors.value = ScenarioValidationErrors()
        _screen.value = Screen.SCENARIO_FORM
    }

    fun updateScenarioForm(transform: (ScenarioFormState) -> ScenarioFormState) {
        _scenarioForm.value = transform(_scenarioForm.value)
    }

    fun saveScenarioForm() {
        val form = _scenarioForm.value
        val input = form.toInput()
        val errors = if (form.isEditing) scenarioManager.updateScenarioMeta(form.editingId!!, input)
        else scenarioManager.createScenario(input)
        _scenarioErrors.value = errors
        if (!errors.hasErrors) {
            _message.value = UiMessage("scenario_saved")
            if (form.isEditing) {
                _screen.value = Screen.SCENARIO_DETAIL
            } else {
                // Open the new scenario's detail so the user can add actions.
                _selectedScenarioId.value = scenarioManager.getScenarios().lastOrNull()?.id
                _screen.value = Screen.SCENARIO_DETAIL
            }
        }
    }

    fun cancelScenarioForm() {
        _scenarioErrors.value = ScenarioValidationErrors()
        _screen.value = if (_selectedScenarioId.value != null) Screen.SCENARIO_DETAIL else Screen.SCENARIOS
    }

    // ---- Action form -------------------------------------------------------

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
        _actionForm.value = ActionFormState(
            type = a.type,
            x = (a.x ?: 0).toString(),
            y = (a.y ?: 0).toString(),
            label = a.label.orEmpty(),
            durationMs = (a.durationMs ?: 500L).toString(),
            message = a.message.orEmpty(),
        )
        _actionErrors.value = ActionValidationErrors()
        _screen.value = Screen.ACTION_FORM
    }

    fun updateActionForm(transform: (ActionFormState) -> ActionFormState) {
        _actionForm.value = transform(_actionForm.value)
    }

    fun saveActionForm() {
        val scenarioId = _selectedScenarioId.value ?: return
        val input = _actionForm.value.toInput()
        val errors = scenarioManager.validateAction(input)
        _actionErrors.value = errors
        if (errors.hasErrors) {
            auditLog.log(AuditType.VALIDATION_FAILED, AuditSeverity.WARNING,
                "Action validation failed", scenarioId = scenarioId)
            return
        }
        val editingId = _editingActionId.value
        val ok = if (editingId != null) scenarioManager.updateAction(scenarioId, editingId, input)
        else scenarioManager.addAction(scenarioId, input)
        if (ok) {
            _message.value = UiMessage("scenario_saved")
            _screen.value = Screen.SCENARIO_DETAIL
        }
    }

    fun cancelActionForm() {
        _actionErrors.value = ActionValidationErrors()
        _screen.value = Screen.SCENARIO_DETAIL
    }

    fun deleteAction(scenarioId: String, actionId: String) = scenarioManager.deleteAction(scenarioId, actionId)
    fun moveActionUp(scenarioId: String, actionId: String) = scenarioManager.moveAction(scenarioId, actionId, up = true)
    fun moveActionDown(scenarioId: String, actionId: String) = scenarioManager.moveAction(scenarioId, actionId, up = false)
    fun duplicateAction(scenarioId: String, actionId: String) = scenarioManager.duplicateAction(scenarioId, actionId)

    // ---- Simulation --------------------------------------------------------

    fun runActiveScenarioSimulation(): Boolean {
        val active = activeScenario()
        if (active == null) { _message.value = UiMessage("no_active_scenario"); return false }
        startRun(active)
        return true
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
    fun emergencyStop() { engine.emergencyStop() }

    // ---- Audit -------------------------------------------------------------

    fun clearAuditLog() { auditLog.clear() }
    fun clearRunHistory() { _runHistory.value = emptyList() }
    fun exportAuditLogText(): String = auditLog.exportText()

    /**
     * Defensive demonstration chokepoint: any hypothetical real-tap attempt is blocked and audited.
     * Not wired to any UI control — there is no way to enable real taps.
     */
    fun attemptRealTapBlocked(): Boolean {
        auditLog.log(AuditType.SAFETY_REAL_TAP_BLOCKED, AuditSeverity.SAFETY,
            "Real tap attempt blocked — not implemented")
        return gate.attemptRealTap()
    }

    // ---- Diagnostics -------------------------------------------------------

    fun diagnostics(): DiagnosticsState = diagnosticsManager.build(
        status = engine.getStatus(),
        progress = progress.value,
        scenariosCount = scenarios.value.size,
        activeScenario = activeScenario(),
        auditEventsCount = auditLog.count(),
        lastAuditEventType = auditLog.lastType(),
        storageReady = storageReady,
        corruptedStorageRecovered = corruptedStorageRecovered,
        storageMigrated = storageMigrated,
    )

    fun blockedReasons(): List<String> = gate.getBlockedReasons()
}
