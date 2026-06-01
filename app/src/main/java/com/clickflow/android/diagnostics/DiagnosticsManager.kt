package com.clickflow.android.diagnostics

import com.clickflow.android.core.AppInfo
import com.clickflow.android.scenarios.Scenario
import com.clickflow.android.scenarios.SimulationProgress
import com.clickflow.android.scenarios.SimulationStatus

/**
 * Builds a [DiagnosticsState] from current runtime values. Pure/stateless assembler.
 */
class DiagnosticsManager {

    fun build(
        status: SimulationStatus,
        progress: SimulationProgress,
        scenariosCount: Int,
        activeScenario: Scenario?,
        auditEventsCount: Int,
        lastAuditEventType: String?,
        storageReady: Boolean,
        corruptedStorageRecovered: Boolean,
        storageMigrated: Boolean,
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
        actionsCount = activeScenario?.actions?.size ?: 0,
        currentActionIndex = progress.currentActionIndex,
        currentRepeatIndex = progress.currentRepeatIndex,
        auditEventsCount = auditEventsCount,
        lastAuditEventType = lastAuditEventType,
        lastRunStatus = status.name.lowercase(),
        storageReady = storageReady,
        corruptedStorageRecovered = corruptedStorageRecovered,
        storageMigrated = storageMigrated,
    )
}
