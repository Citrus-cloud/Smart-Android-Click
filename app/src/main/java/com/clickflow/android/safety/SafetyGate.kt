package com.clickflow.android.safety

/**
 * Central decision point for what the app is allowed to do.
 *
 * Step 52: real taps are categorically blocked. [canRunRealTap] always
 * returns false and there is no code path anywhere in the app that performs
 * a real tap. Simulation is always permitted.
 */
class SafetyGate(private val state: SafetyState = SafetyState.STEP_52_DEFAULT) {

    /** Simulation (dry-run) is always allowed in Step 52. */
    fun canRunSimulation(): Boolean = state.simulationOnly || true

    /**
     * Real taps are NOT implemented in Step 52 and must never be allowed.
     * Returns false unconditionally.
     */
    fun canRunRealTap(): Boolean = false

    /**
     * Human-readable reasons that real automation is blocked.
     * Surfaced in the Safety Center UI.
     */
    fun getBlockedReasons(): List<String> = listOf(
        "Real taps are not implemented.",
        "Accessibility Service is not enabled.",
        "User confirmation is required.",
        "Safety review is not completed.",
    )

    fun currentState(): SafetyState = state
}
