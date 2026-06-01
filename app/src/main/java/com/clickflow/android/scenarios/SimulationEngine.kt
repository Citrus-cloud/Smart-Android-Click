package com.clickflow.android.scenarios

import com.clickflow.android.audit.AuditLogManager
import com.clickflow.android.audit.AuditSeverity
import com.clickflow.android.audit.AuditType
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
    IDLE, RUNNING, COMPLETED, STOPPED, EMERGENCY_STOPPED, ERROR,
}

/** Progress of the current/last simulation run. */
data class SimulationProgress(
    val currentStep: Int = 0,
    val totalSteps: Int = 0,
    val currentActionIndex: Int = 0,
    val currentRepeatIndex: Int = 0,
    val lastLog: String? = null,
) {
    val percent: Int
        get() = if (totalSteps <= 0) 0 else ((currentStep * 100) / totalSteps).coerceIn(0, 100)
}

/** Localized message builders injected by the ViewModel (resources need a Context). */
data class SimMessages(
    val started: (name: String) -> String,
    val completed: (name: String) -> String,
    val stopped: () -> String,
    val emergencyStopped: () -> String,
    val simulatedTap: (x: Int, y: Int, label: String?) -> String,
    val waited: (ms: Long) -> String,
)

/**
 * Executes multi-step scenarios in SIMULATION ONLY mode.
 *
 * Step 54 guarantee: NO real input. For each of `repeatCount` cycles it walks the scenario's
 * [ScenarioAction] list in order, pacing each step by `intervalMs`; SIMULATED_TAP only logs,
 * WAIT additionally `delay`s its duration, NOTE logs its message. Every step and lifecycle
 * transition emits an audit event. There is no real-tap code path.
 */
class SimulationEngine(
    private val gate: SafetyGate,
    private val audit: AuditLogManager,
    private val messages: SimMessages,
) {
    private val _status = MutableStateFlow(SimulationStatus.IDLE)
    val status: StateFlow<SimulationStatus> = _status.asStateFlow()

    private val _progress = MutableStateFlow(SimulationProgress())
    val progress: StateFlow<SimulationProgress> = _progress.asStateFlow()

    private var activeScenarioId: String? = null
    private var job: Job? = null

    fun startSimulation(
        scenario: Scenario,
        scope: CoroutineScope,
        onTerminal: (SimulationStatus) -> Unit = {},
    ): SimulationStatus {
        if (!gate.canRunSimulation() || scenario.actions.isEmpty()) {
            _status.value = SimulationStatus.ERROR
            audit.log(AuditType.VALIDATION_FAILED, AuditSeverity.ERROR,
                "Scenario has no actions to simulate", scenarioId = scenario.id)
            onTerminal(_status.value)
            return _status.value
        }
        job?.cancel()
        activeScenarioId = scenario.id

        val repeat = scenario.settings.repeatCount.coerceAtLeast(1)
        val interval = scenario.settings.intervalMs.coerceAtLeast(ScenarioValidator.MIN_INTERVAL_MS)
        val actions = scenario.actions
        val total = repeat * actions.size

        _progress.value = SimulationProgress(0, total, 0, 0, null)
        _status.value = SimulationStatus.RUNNING
        audit.log(AuditType.SCENARIO_STARTED, AuditSeverity.INFO,
            messages.started(scenario.name), scenarioId = scenario.id,
            metadata = mapOf("repeatCount" to repeat.toString(), "actions" to actions.size.toString()))

        job = scope.launch {
            var step = 0
            try {
                for (r in 1..repeat) {
                    if (!isActive) break
                    for ((ai, action) in actions.withIndex()) {
                        if (!isActive) break
                        delay(interval)
                        if (!isActive) break
                        // SAFETY: no real input. We only log + (for WAIT) delay.
                        val log = executeSimulated(action, scenario.id)
                        if (action.type == ScenarioActionType.WAIT) {
                            val d = (action.durationMs ?: ScenarioValidator.MIN_WAIT_MS)
                                .coerceAtLeast(ScenarioValidator.MIN_WAIT_MS)
                            delay(d)
                        }
                        step++
                        _progress.value = SimulationProgress(
                            currentStep = step,
                            totalSteps = total,
                            currentActionIndex = ai,
                            currentRepeatIndex = r,
                            lastLog = log,
                        )
                    }
                }
                if (_status.value == SimulationStatus.RUNNING) {
                    _status.value = SimulationStatus.COMPLETED
                    audit.log(AuditType.SCENARIO_COMPLETED, AuditSeverity.INFO,
                        messages.completed(scenario.name), scenarioId = scenario.id)
                }
            } catch (_: Throwable) {
                if (_status.value == SimulationStatus.RUNNING) {
                    _status.value = SimulationStatus.ERROR
                }
            } finally {
                onTerminal(_status.value)
            }
        }
        return _status.value
    }

    /** Logs an audit event for a simulated action and returns the log line. No real input. */
    private fun executeSimulated(action: ScenarioAction, scenarioId: String): String = when (action.type) {
        ScenarioActionType.SIMULATED_TAP -> {
            val msg = messages.simulatedTap(action.x ?: 0, action.y ?: 0, action.label)
            audit.log(AuditType.ACTION_SIMULATED_TAP, AuditSeverity.INFO, msg, scenarioId, action.id)
            msg
        }
        ScenarioActionType.WAIT -> {
            val msg = messages.waited(action.durationMs ?: 0L)
            audit.log(AuditType.ACTION_WAIT, AuditSeverity.INFO, msg, scenarioId, action.id)
            msg
        }
        ScenarioActionType.NOTE -> {
            val msg = action.message.orEmpty()
            audit.log(AuditType.ACTION_NOTE, AuditSeverity.INFO, msg, scenarioId, action.id)
            msg
        }
    }

    fun stopSimulation(): SimulationStatus {
        job?.cancel(); job = null
        val sid = activeScenarioId
        activeScenarioId = null
        _status.value = SimulationStatus.STOPPED
        audit.log(AuditType.SCENARIO_STOPPED, AuditSeverity.WARNING, messages.stopped(), scenarioId = sid)
        return _status.value
    }

    fun emergencyStop(): SimulationStatus {
        job?.cancel(); job = null
        val sid = activeScenarioId
        activeScenarioId = null
        _status.value = SimulationStatus.EMERGENCY_STOPPED
        audit.log(AuditType.SCENARIO_EMERGENCY_STOPPED, AuditSeverity.SAFETY,
            messages.emergencyStopped(), scenarioId = sid)
        return _status.value
    }

    fun getStatus(): SimulationStatus = _status.value
    fun activeScenarioId(): String? = activeScenarioId
}
