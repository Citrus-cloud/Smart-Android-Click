package com.clickflow.android.core

import androidx.lifecycle.ViewModel
import com.clickflow.android.diagnostics.DiagnosticsManager
import com.clickflow.android.diagnostics.DiagnosticsState
import com.clickflow.android.safety.SafetyCenter
import com.clickflow.android.safety.SafetyGate
import com.clickflow.android.scenarios.Scenario
import com.clickflow.android.scenarios.ScenarioManager
import com.clickflow.android.scenarios.SimulationEngine
import com.clickflow.android.scenarios.SimulationStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Screens reachable from the main screen. */
enum class Screen { HOME, SCENARIOS, SAFETY, DIAGNOSTICS }

/**
 * Single app-wide state holder.
 *
 * Wires the simulation engine, scenario store, safety gate, and diagnostics
 * together. Holds NO real-input logic — by construction it can only drive the
 * simulation engine, which itself performs no real taps.
 */
class ClickFlowViewModel(
    private val gate: SafetyGate = SafetyGate(),
    private val scenarioManager: ScenarioManager = ScenarioManager(),
    private val engine: SimulationEngine = SimulationEngine(gate),
    private val diagnosticsManager: DiagnosticsManager = DiagnosticsManager(),
) : ViewModel() {

    val safetyCenter = SafetyCenter(gate)

    private val _screen = MutableStateFlow(Screen.HOME)
    val screen: StateFlow<Screen> = _screen.asStateFlow()

    val status: StateFlow<SimulationStatus> = engine.status

    fun navigateTo(screen: Screen) { _screen.value = screen }

    fun scenarios(): List<Scenario> = scenarioManager.all()

    private fun activeScenario(): Scenario? =
        engine.activeScenarioId()?.let { scenarioManager.byId(it) }

    /** Starts the first preset (or a given scenario) in simulation mode. */
    fun startSimulation(scenario: Scenario? = scenarios().firstOrNull()) {
        scenario?.let { engine.startSimulation(it) }
    }

    fun stop() { engine.stopSimulation() }

    fun emergencyStop() { engine.emergencyStop() }

    fun diagnostics(): DiagnosticsState =
        diagnosticsManager.build(engine.getStatus(), activeScenario())

    fun blockedReasons(): List<String> = gate.getBlockedReasons()
}
