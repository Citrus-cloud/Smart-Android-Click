package com.clickflow.android.diagnostics

/**
 * Snapshot of diagnostic information shown on the Diagnostics screen.
 * All real-input capabilities are reported as disabled/planned in Step 54.
 */
data class DiagnosticsState(
    val appVersion: String,
    val simulationOnly: Boolean = true,
    val realTapsEnabled: Boolean = false,
    val accessibilityServicePlanned: Boolean = true,
    val mediaProjectionPlanned: Boolean = true,
    val emergencyStopReady: Boolean = true,
    val scenariosCount: Int = 0,
    val activeScenarioName: String? = null,
    val activeScenarioType: String? = null,
    val actionsCount: Int = 0,
    val currentActionIndex: Int = 0,
    val currentRepeatIndex: Int = 0,
    val auditEventsCount: Int = 0,
    val lastAuditEventType: String? = null,
    val lastRunStatus: String = "idle",
    val storageReady: Boolean = false,
    val corruptedStorageRecovered: Boolean = false,
    val storageMigrated: Boolean = false,
)
