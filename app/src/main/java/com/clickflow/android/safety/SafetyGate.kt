package com.clickflow.android.safety

/**
* Central decision point for what the app is allowed to do.
*
* Step 52: real taps are categorically blocked. [canRunRealTap] always
* returns false and there is no code path anywhere in the app that performs
* a bulk/looped real tap. Simulation is always permitted.
*
* Step 62 adds a narrow, defensively-gated exception: the *single-real-tap
* prototype*. It is enabled only when EVERY one of the following is true:
*   - SafetyState.realTapSafetyReviewPassed
*   - SafetyState.accessibilityServiceEnabled
*   - SafetyState.realTapSessionActive
*   - SafetyState.realTapConsentFresh
*
* The classic [canRunRealTap] (bulk) stays false forever.
*/
class SafetyGate(private val state: SafetyState = SafetyState.STEP_62_DEFAULT) {

   /** Simulation (dry-run) is always allowed. */
   fun canRunSimulation(): Boolean = state.simulationOnly || true

   /**
    * Bulk / looped / scenario-driven real taps. NOT implemented and must never be allowed.
    * Returns false unconditionally — DO NOT change without a separate safety review.
    */
   fun canRunRealTap(): Boolean = false

   /**
    * Step 62 — single-real-tap prototype. True only when all four gating flags
    * are simultaneously satisfied. Any one missing -> false.
    *
    * This is consulted ONCE per tap, immediately before dispatch. Callers must
    * re-check freshness; consent is single-use.
    */
   fun canRunRealTapSingleProto(): Boolean =
       state.realTapSafetyReviewPassed &&
           state.accessibilityServiceEnabled &&
           state.realTapSessionActive &&
           state.realTapConsentFresh

   /**
    * Single defensive chokepoint for any hypothetical bulk real-tap attempt. Always denies.
    * Callers should record a `safety.realTapBlocked` audit event.
    */
   fun attemptRealTap(): Boolean = false

   /** Human-readable reasons the *bulk* real-tap surface is blocked. */
   fun getBlockedReasons(): List<String> = listOf(
       "Bulk real taps are not implemented.",
       "Accessibility Service is required.",
       "User confirmation is required per tap.",
       "Safety review must be completed in this session.",
   )

   /** Step 62 — diagnostic list of which single-tap gating flags are currently missing. */
   fun getSingleProtoBlockedReasons(): List<String> {
       val reasons = mutableListOf<String>()
       if (!state.realTapSafetyReviewPassed) reasons += "Safety review not completed."
       if (!state.accessibilityServiceEnabled) reasons += "Accessibility service not enabled."
       if (!state.realTapSessionActive) reasons += "Real-tap session not started."
       if (!state.realTapConsentFresh) reasons += "Per-tap consent not confirmed."
       return reasons
   }

   fun currentState(): SafetyState = state
}
