package com.clickflow.android.safety

/**
 * Immutable snapshot of the app's safety posture.
 *
 * Step 52 invariant: this is a SIMULATION-ONLY foundation.
 * Every capability that could perform real input is disabled and is NOT
 * implemented anywhere in the codebase. These flags describe intent and
 * are the single source of truth consulted by [SafetyGate].
 */
data class SafetyState(
    val simulationOnly: Boolean = true,
    val realTapsEnabled: Boolean = false,
    val accessibilityServiceEnabled: Boolean = false,
    val mediaProjectionEnabled: Boolean = false,
    val overlayPermissionEnabled: Boolean = false,
    val emergencyStopReady: Boolean = true,
) {
    companion object {
        /** The only state shipped in Step 52. Real-input flags are hard-coded off. */
        val STEP_52_DEFAULT = SafetyState()
    }
}
