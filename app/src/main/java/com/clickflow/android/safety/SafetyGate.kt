package com.clickflow.android.safety

/**
* Mutable, per-process snapshot of the gating flags used by [SafetyGate].
*
* Step 62 introduced these as compile-time defaults; Step 63 makes them
* live, so the ViewModel can drive them as the safety review is ticked,
* the session is started/ended, and per-tap consent is requested.
*
* NONE OF THIS IS PERSISTED. Process death resets every flag.
*/
data class SafetyState(
   val simulationOnly: Boolean = true,
   val realTapSafetyReviewPassed: Boolean = false,
   val accessibilityServiceEnabled: Boolean = false,
   val realTapSessionActive: Boolean = false,
   val realTapConsentFresh: Boolean = false,
) {
   companion object {
       /** Default state when the prototype boots — everything false except simulation. */
       val STEP_62_DEFAULT = SafetyState()
   }
}

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
* Step 63 makes the state live: callers (the ViewModel) update individual
* flags through [updateReviewPassed], [updateAccessibility], [updateSession],
* and [updateConsentFresh]. The classic [canRunRealTap] (bulk) stays false
* forever.
*/
class SafetyGate(initial: SafetyState = SafetyState.STEP_62_DEFAULT) {

   @Volatile
   private var state: SafetyState = initial

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

   // ---- Step 63 — live mutators ---------------------------------------
   // All mutators are intentionally narrow: the caller can only set ONE
   // flag per call, and there is no bulk "set everything" entry point.
   // This makes audit-trail reasoning trivial — every flip is one call.

   /** Update whether the 10-item safety review has been ticked through. */
   @Synchronized
   fun updateReviewPassed(passed: Boolean) {
       state = state.copy(realTapSafetyReviewPassed = passed)
   }

   /** Update whether the AccessibilityService is currently bound. */
   @Synchronized
   fun updateAccessibility(enabled: Boolean) {
       state = state.copy(accessibilityServiceEnabled = enabled)
   }

   /** Update whether a real-tap session is currently active. */
   @Synchronized
   fun updateSession(active: Boolean) {
       state = state.copy(realTapSessionActive = active)
   }

   /** Update whether per-tap consent is currently fresh (single-use). */
   @Synchronized
   fun updateConsentFresh(fresh: Boolean) {
       state = state.copy(realTapConsentFresh = fresh)
   }

   /**
    * Step 63 — reset every prototype flag to a known-safe baseline. Called by
    * Emergency Stop and on session end. Simulation-only remains true.
    */
   @Synchronized
   fun resetPrototypeFlags() {
       state = state.copy(
           realTapSafetyReviewPassed = false,
           realTapSessionActive = false,
           realTapConsentFresh = false,
       )
   }
}
