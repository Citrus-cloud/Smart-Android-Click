package com.clickflow.android.diagnostics

/**
 * Snapshot of diagnostic information shown on the Diagnostics screen.
 * All real-input capabilities are reported as disabled/planned in Step 52.
 */
data class DiagnosticsState(
    val appVersion: String,
    val simulationOnly: Boolean = true,
    val realTapsEnabled: Boolean = false,
    val accessibilityServicePlanned: Boolean = true,
    val mediaProjectionPlanned: Boolean = true,
    val emergencyStopReady: Boolean = true,
    val activeScenario: String? = null,
    val lastRunStatus: String = "idle",
)
