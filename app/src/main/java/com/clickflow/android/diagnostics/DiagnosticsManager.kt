package com.clickflow.android.diagnostics

import com.clickflow.android.core.AppInfo
import com.clickflow.android.scenarios.Scenario
import com.clickflow.android.scenarios.SimulationStatus

/**
 * Builds a [DiagnosticsState] from the current runtime values.
 * Pure/stateless assembler — no side effects, no I/O.
 */
class DiagnosticsManager {

    fun build(
        status: SimulationStatus,
        activeScenario: Scenario?,
    ): DiagnosticsState = DiagnosticsState(
        appVersion = AppInfo.VERSION_NAME,
        simulationOnly = true,
        realTapsEnabled = false,
        accessibilityServicePlanned = true,
        mediaProjectionPlanned = true,
        emergencyStopReady = true,
        activeScenario = activeScenario?.name,
        lastRunStatus = status.name.lowercase(),
    )
}
