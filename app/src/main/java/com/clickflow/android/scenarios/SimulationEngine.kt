package com.clickflow.android.scenarios

import com.clickflow.android.safety.SafetyGate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Lifecycle status of the simulation engine, mirrored into the UI. */
enum class SimulationStatus {
    IDLE,
    RUNNING,
    COMPLETED,
    STOPPED,
    EMERGENCY_STOPPED,
    ERROR,
}

/** Progress of the current/last simulation run. */
data class SimulationProgress(
    val currentStep: Int = 0,
    val totalSteps: Int = 0,
    val lastLog: String? = null,
) {
    val percent: Int
        get() = if (totalSteps <= 0) 0 else ((currentStep * 100) / totalSteps).coerceIn(0, 100)
}

/**
 * Executes scenarios in SIMULATION ONLY mode.
 *
 * Step 53 guarantee: this engine performs NO real input. For each scenario it loops
 * `repeatCount` times, waiting `intervalMs` between steps, and emits a dry-run log line
 * ("Simulated tap at x,y") plus progress. Before running it consults [SafetyGate]; the gate
 * permits simulation and forbids real taps, and there is no real-tap code path to call.
 *
 * @param logFormatter renders the per-step log line; injected so strings can be localized.
 */
class SimulationEngine(
    private val gate: SafetyGate = SafetyGate(),
    private val logFormatter: (x: Int, y: Int, step: Int, total: Int) -> String =
        { x, y, step, total -> "Simulated tap at $x,$y (step $step/$total)" },
) {
    private val _status = MutableStateFlow(SimulationStatus.IDLE)
    val status: StateFlow<SimulationStatus> = _status.asStateFlow()

    private val _progress = MutableStateFlow(SimulationProgress())
    val progress: StateFlow<SimulationProgress> = _progress.asStateFlow()

    private var activeScenarioId: String? = null
    private var job: Job? = null

    /**
     * Starts a simulation run for [scenario] on [scope]. Returns the immediate status.
     * No real input is ever performed.
     */
    fun startSimulation(scenario: Scenario, scope: CoroutineScope): SimulationStatus {
        if (!gate.canRunSimulation()) {
            _status.value = SimulationStatus.ERROR
            return _status.value
        }
        // SAFETY: real taps are never executed; canRunRealTap() is false and unused here.
        job?.cancel()
        activeScenarioId = scenario.id

        val total = scenario.settings.repeatCount.coerceAtLeast(1)
        val interval = scenario.settings.intervalMs.coerceAtLeast(ScenarioValidator.MIN_INTERVAL_MS)
        val x = scenario.settings.x
        val y = scenario.settings.y

        _progress.value = SimulationProgress(currentStep = 0, totalSteps = total, lastLog = null)
        _status.value = SimulationStatus.RUNNING

        job = scope.launch {
            try {
                for (step in 1..total) {
                    if (!isActive) break
                    delay(interval)
                    if (!isActive) break
                    _progress.value = SimulationProgress(
                        currentStep = step,
                        totalSteps = total,
                        lastLog = logFormatter(x, y, step, total),
                    )
                }
                // Only mark completed if we were not stopped/emergency-stopped meanwhile.
                if (_status.value == SimulationStatus.RUNNING) {
                    _status.value = SimulationStatus.COMPLETED
                }
            } catch (_: Throwable) {
                if (_status.value == SimulationStatus.RUNNING) {
                    _status.value = SimulationStatus.ERROR
                }
            }
        }
        return _status.value
    }

    fun stopSimulation(): SimulationStatus {
        job?.cancel()
        job = null
        activeScenarioId = null
        _status.value = SimulationStatus.STOPPED
        return _status.value
    }

    /** Hard stop. Always available; cancels any in-flight run immediately. */
    fun emergencyStop(): SimulationStatus {
        job?.cancel()
        job = null
        activeScenarioId = null
        _status.value = SimulationStatus.EMERGENCY_STOPPED
        return _status.value
    }

    fun getStatus(): SimulationStatus = _status.value

    fun activeScenarioId(): String? = activeScenarioId
}
