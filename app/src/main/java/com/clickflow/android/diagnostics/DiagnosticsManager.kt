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
        scenariosCount: Int,
        activeScenario: Scenario?,
        storageReady: Boolean,
        corruptedStorageRecovered: Boolean,
    ): DiagnosticsState = DiagnosticsState(
        appVersion = AppInfo.VERSION_NAME,
        simulationOnly = true,
        realTapsEnabled = false,
        accessibilityServicePlanned = true,
        mediaProjectionPlanned = true,
        emergencyStopReady = true,
        scenariosCount = scenariosCount,
        activeScenarioName = activeScenario?.name,
        activeScenarioType = activeScenario?.type?.name?.lowercase(),
        lastRunStatus = status.name.lowercase(),
        storageReady = storageReady,
        corruptedStorageRecovered = corruptedStorageRecovered,
    )
}
