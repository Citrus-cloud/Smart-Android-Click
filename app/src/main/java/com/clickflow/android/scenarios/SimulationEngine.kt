package com.clickflow.android.scenarios

import com.clickflow.android.safety.SafetyGate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Lifecycle status of the simulation engine, mirrored into the UI. */
enum class SimulationStatus {
    IDLE,
    RUNNING,
    COMPLETED,
    STOPPED,
    EMERGENCY_STOPPED,
}

/**
 * Executes scenarios in SIMULATION ONLY mode.
 *
 * Step 52 guarantee: this engine performs NO real input. It only transitions
 * a status value. Before "running" anything it consults [SafetyGate]; the gate
 * permits simulation and forbids real taps, and there is no real-tap code path
 * to call regardless.
 */
class SimulationEngine(
    private val gate: SafetyGate = SafetyGate(),
) {
    private val _status = MutableStateFlow(SimulationStatus.IDLE)
    val status: StateFlow<SimulationStatus> = _status.asStateFlow()

    private var activeScenarioId: String? = null

    fun startSimulation(scenario: Scenario): SimulationStatus {
        if (!gate.canRunSimulation()) {
            _status.value = SimulationStatus.STOPPED
            return _status.value
        }
        // SAFETY: real taps are never executed. We only mark the run as active.
        activeScenarioId = scenario.id
        _status.value = SimulationStatus.RUNNING
        return _status.value
    }

    /** Marks the current simulation as completed (dry-run finished). */
    fun completeSimulation(): SimulationStatus {
        if (_status.value == SimulationStatus.RUNNING) {
            _status.value = SimulationStatus.COMPLETED
        }
        return _status.value
    }

    fun stopSimulation(): SimulationStatus {
        activeScenarioId = null
        _status.value = SimulationStatus.STOPPED
        return _status.value
    }

    /** Hard stop. Always available; clears any active run immediately. */
    fun emergencyStop(): SimulationStatus {
        activeScenarioId = null
        _status.value = SimulationStatus.EMERGENCY_STOPPED
        return _status.value
    }

    fun getStatus(): SimulationStatus = _status.value

    fun activeScenarioId(): String? = activeScenarioId
}
