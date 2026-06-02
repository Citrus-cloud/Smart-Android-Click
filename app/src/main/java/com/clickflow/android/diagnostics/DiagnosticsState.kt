package com.clickflow.android.diagnostics

/**
* Snapshot of diagnostic information shown on the Diagnostics screen.
*
* Step 55: real-input capabilities reported as disabled/planned.
* Step 62: adds single-real-tap prototype fields. These are read-only mirrors
* of in-memory state; they are NOT persisted.
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
   val profilesCount: Int = 0,
   val activeProfileName: String? = null,
   val auditEventsCount: Int = 0,
   val lastAuditEventType: String? = null,
   val lastRunStatus: String = "idle",
   val scenarioStorageReady: Boolean = false,
   val corruptedScenarioRecovered: Boolean = false,
   val storageMigrated: Boolean = false,
   val profileStorageReady: Boolean = false,
   val corruptedProfileStorageRecovered: Boolean = false,
   val auditStorageReady: Boolean = false,
   val corruptedAuditRecovered: Boolean = false,
   val backupAvailable: Boolean = true,
   val lastBackupExportAt: Long? = null,
   val lastBackupImportAt: Long? = null,
   val invalidImportItemsLast: Int = 0,
   val backupContainsAuditLog: Boolean = false,
   val externalStorageUsed: Boolean = false,
   val permissionsRequired: Boolean = false,
   val simpleClickerReady: Boolean = true,
   val markerX: Int = 500,
   val markerY: Int = 500,
   val quickIntervalMs: Long = 500,
   val quickRepeatCount: Int = 10,
   val overlayEnabled: Boolean = false,
   val accessibilityEnabled: Boolean = false,

   // ---- Step 62 — single-real-tap prototype (in-memory mirrors) ----
   /** Whether the device + API + service combination *could* dispatch a real tap. */
   val realTapPrototypeAvailable: Boolean = false,
   /** Whether the user has completed the 10-item safety review this session. */
   val realTapSafetyReviewPassed: Boolean = false,
   /** Whether a real-tap session is currently active. */
   val realTapSessionActive: Boolean = false,
   /** Number of real-tap dispatches accepted by the OS this session. */
   val realTapDispatchedCount: Int = 0,
   /** Number of real-tap dispatches blocked by gate this session. */
   val realTapBlockedCount: Int = 0,
   /** The outcome of the most recent attempt, or null if none yet. */
   val lastRealTapOutcome: String? = null,
)
