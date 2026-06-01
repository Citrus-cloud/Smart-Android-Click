package com.clickflow.android.diagnostics

import com.clickflow.android.core.AppInfo
import com.clickflow.android.scenarios.Scenario
import com.clickflow.android.scenarios.SimulationProgress
import com.clickflow.android.scenarios.SimulationStatus

/** Builds a [DiagnosticsState] from current runtime values. Pure/stateless assembler. */
class DiagnosticsManager {

    fun build(
        status: SimulationStatus,
        progress: SimulationProgress,
        scenariosCount: Int,
        activeScenario: Scenario?,
        profilesCount: Int,
        activeProfileName: String?,
        auditEventsCount: Int,
        lastAuditEventType: String?,
        scenarioStorageReady: Boolean,
        corruptedScenarioRecovered: Boolean,
        storageMigrated: Boolean,
        profileStorageReady: Boolean,
        corruptedProfileStorageRecovered: Boolean,
        auditStorageReady: Boolean,
        corruptedAuditRecovered: Boolean,
        lastBackupExportAt: Long?,
        lastBackupImportAt: Long?,
        invalidImportItemsLast: Int,
        markerX: Int,
        markerY: Int,
        quickIntervalMs: Long,
        quickRepeatCount: Int,
    ): DiagnosticsState = DiagnosticsState(
        appVersion = AppInfo.VERSION_NAME,
        scenariosCount = scenariosCount,
        activeScenarioName = activeScenario?.name,
        activeScenarioType = activeScenario?.type?.name?.lowercase(),
        actionsCount = activeScenario?.actions?.size ?: 0,
        currentActionIndex = progress.currentActionIndex,
        currentRepeatIndex = progress.currentRepeatIndex,
        profilesCount = profilesCount,
        activeProfileName = activeProfileName,
        auditEventsCount = auditEventsCount,
        lastAuditEventType = lastAuditEventType,
        lastRunStatus = status.name.lowercase(),
        scenarioStorageReady = scenarioStorageReady,
        corruptedScenarioRecovered = corruptedScenarioRecovered,
        storageMigrated = storageMigrated,
        profileStorageReady = profileStorageReady,
        corruptedProfileStorageRecovered = corruptedProfileStorageRecovered,
        auditStorageReady = auditStorageReady,
        corruptedAuditRecovered = corruptedAuditRecovered,
        backupAvailable = true,
        lastBackupExportAt = lastBackupExportAt,
        lastBackupImportAt = lastBackupImportAt,
        invalidImportItemsLast = invalidImportItemsLast,
        backupContainsAuditLog = false,
        externalStorageUsed = false,
        permissionsRequired = false,
        simpleClickerReady = true,
        markerX = markerX,
        markerY = markerY,
        quickIntervalMs = quickIntervalMs,
        quickRepeatCount = quickRepeatCount,
        overlayEnabled = false,
        accessibilityEnabled = false,
    )
}
