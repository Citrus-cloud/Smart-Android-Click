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
    overlayEnabled: Boolean = false,
    accessibilityEnabled: Boolean = false,
    // ---- Step 62 — single-real-tap prototype ----
    realTapPrototypeAvailable: Boolean = false,
    realTapSafetyReviewPassed: Boolean = false,
    realTapSessionActive: Boolean = false,
    realTapDispatchedCount: Int = 0,
    realTapBlockedCount: Int = 0,
    lastRealTapOutcome: String? = null,
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
    // Step 61: permissions remain OPT-IN, never required.
    permissionsRequired = false,
    simpleClickerReady = true,
    markerX = markerX,
    markerY = markerY,
    quickIntervalMs = quickIntervalMs,
    quickRepeatCount = quickRepeatCount,
    overlayEnabled = overlayEnabled,
    accessibilityEnabled = accessibilityEnabled,
    // Step 62 — mirrors of in-memory real-tap state.
    realTapPrototypeAvailable = realTapPrototypeAvailable,
    realTapSafetyReviewPassed = realTapSafetyReviewPassed,
    realTapSessionActive = realTapSessionActive,
    realTapDispatchedCount = realTapDispatchedCount,
    realTapBlockedCount = realTapBlockedCount,
    lastRealTapOutcome = lastRealTapOutcome,
)
}
