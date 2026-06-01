package com.clickflow.android.core

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.clickflow.android.R
import com.clickflow.android.diagnostics.DiagnosticsManager
import com.clickflow.android.diagnostics.DiagnosticsState
import com.clickflow.android.safety.SafetyCenter
import com.clickflow.android.safety.SafetyGate
import com.clickflow.android.scenarios.Scenario
import com.clickflow.android.scenarios.ScenarioInput
import com.clickflow.android.scenarios.ScenarioManager
import com.clickflow.android.scenarios.ScenarioRepository
import com.clickflow.android.scenarios.ScenarioType
import com.clickflow.android.scenarios.ScenarioValidationErrors
import com.clickflow.android.scenarios.SimulationEngine
import com.clickflow.android.scenarios.SimulationProgress
import com.clickflow.android.scenarios.SimulationStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/** Screens reachable from the main screen. */
enum class Screen { HOME, SCENARIOS, SCENARIO_FORM, SAFETY, DIAGNOSTICS }

/** Editable form state for create/edit. Fields are strings to support inline validation. */
data class ScenarioFormState(
    val editingId: String? = null,
    val name: String = "",
    val x: String = "0",
    val y: String = "0",
    val repeatCount: String = "1",
    val intervalMs: String = "500",
) {
    val isEditing: Boolean get() = editingId != null

    /** Parses the string fields into a validated input model (lenient parse; validator decides). */
    fun toInput(): ScenarioInput = ScenarioInput(
        name = name,
        x = x.trim().toIntOrNull() ?: -1,
        y = y.trim().toIntOrNull() ?: -1,
        repeatCount = repeatCount.trim().toIntOrNull() ?: 0,
        intervalMs = intervalMs.trim().toLongOrNull() ?: 0L,
        type = ScenarioType.SIMPLE_TAP_SIMULATION,
    )
}

/** One-shot UI message (snackbar/toast-style), surfaced then cleared. */
data class UiMessage(val key: String)

/**
 * Single app-wide state holder for ClickFlow Android.
 *
 * Wires the scenario layer (manager + repository + persistence), the simulation engine,
 * the safety gate, and diagnostics. Holds NO real-input logic — it can only drive the
 * simulation engine, which performs no real taps.
 */
class ClickFlowViewModel(app: Application) : AndroidViewModel(app) {

    private val gate = SafetyGate()
    private val repository = ScenarioRepository(File(app.filesDir, ScenarioRepository.FILE_NAME))
    private val scenarioManager = ScenarioManager(repository)
    private val engine = SimulationEngine(gate) { x, y, step, total ->
        // Localized dry-run log line (RU/EN). No real input is performed.
        app.getString(R.string.simulated_tap_at, x, y, step, total)
    }
    private val diagnosticsManager = DiagnosticsManager()

    val safetyCenter = SafetyCenter(gate)

    private val _screen = MutableStateFlow(Screen.HOME)
    val screen: StateFlow<Screen> = _screen.asStateFlow()

    val scenarios: StateFlow<List<Scenario>> = scenarioManager.scenarios
    val status: StateFlow<SimulationStatus> = engine.status
    val progress: StateFlow<SimulationProgress> = engine.progress

    private val _formState = MutableStateFlow(ScenarioFormState())
    val formState: StateFlow<ScenarioFormState> = _formState.asStateFlow()

    private val _validationErrors = MutableStateFlow(ScenarioValidationErrors())
    val validationErrors: StateFlow<ScenarioValidationErrors> = _validationErrors.asStateFlow()

    private val _message = MutableStateFlow<UiMessage?>(null)
    val message: StateFlow<UiMessage?> = _message.asStateFlow()

    val storageReady: Boolean get() = scenarioManager.storageReady
    val corruptedStorageRecovered: Boolean get() = scenarioManager.corruptedStorageRecovered

    init {
        scenarioManager.load()
    }

    // ---- Navigation --------------------------------------------------------

    fun navigateTo(screen: Screen) { _screen.value = screen }

    fun consumeMessage() { _message.value = null }

    // ---- Scenario list / selection ----------------------------------------

    fun activeScenario(): Scenario? = scenarioManager.getActiveScenario()

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

    // ---- Form --------------------------------------------------------------

    fun openCreateScenario() {
        _formState.value = ScenarioFormState()
        _validationErrors.value = ScenarioValidationErrors()
        _screen.value = Screen.SCENARIO_FORM
    }

    fun openEditScenario(id: String) {
        val s = scenarioManager.byId(id) ?: return
        _formState.value = ScenarioFormState(
            editingId = s.id,
            name = s.name,
            x = s.settings.x.toString(),
            y = s.settings.y.toString(),
            repeatCount = s.settings.repeatCount.toString(),
            intervalMs = s.settings.intervalMs.toString(),
        )
        _validationErrors.value = ScenarioValidationErrors()
        _screen.value = Screen.SCENARIO_FORM
    }

    fun updateScenarioForm(transform: (ScenarioFormState) -> ScenarioFormState) {
        _formState.value = transform(_formState.value)
    }

    /** Validates and saves the form. On success returns to the Scenarios screen. */
    fun saveScenarioForm() {
        val form = _formState.value
        val input = form.toInput()
        val errors = if (form.isEditing) {
            scenarioManager.updateScenario(form.editingId!!, input)
        } else {
            scenarioManager.createScenario(input)
        }
        _validationErrors.value = errors
        if (!errors.hasErrors) {
            _message.value = UiMessage("scenario_saved")
            _screen.value = Screen.SCENARIOS
        }
    }

    fun cancelScenarioForm() {
        _validationErrors.value = ScenarioValidationErrors()
        _screen.value = Screen.SCENARIOS
    }

    // ---- Simulation --------------------------------------------------------

    /** Runs the active scenario in simulation. Returns false if there is no active scenario. */
    fun runActiveScenarioSimulation(): Boolean {
        val active = activeScenario()
        if (active == null) {
            _message.value = UiMessage("no_active_scenario")
            return false
        }
        engine.startSimulation(active, viewModelScope)
        return true
    }

    /** Runs a specific scenario (also makes it active first). */
    fun runScenarioSimulation(id: String) {
        scenarioManager.setActiveScenario(id)
        scenarioManager.byId(id)?.let { engine.startSimulation(it, viewModelScope) }
    }

    fun stopSimulation() { engine.stopSimulation() }

    fun emergencyStop() { engine.emergencyStop() }

    // ---- Diagnostics / safety ---------------------------------------------

    fun diagnostics(): DiagnosticsState = diagnosticsManager.build(
        status = engine.getStatus(),
        scenariosCount = scenarios.value.size,
        activeScenario = activeScenario(),
        storageReady = storageReady,
        corruptedStorageRecovered = corruptedStorageRecovered,
    )

    fun blockedReasons(): List<String> = gate.getBlockedReasons()
}
