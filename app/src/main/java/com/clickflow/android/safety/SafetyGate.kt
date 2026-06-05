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
* Step 63 makes the state live: callers (the ViewModel) update individual
* flags through [updateReviewPassed], [updateAccessibility], [updateSession],
* and [updateConsentFresh]. The classic [canRunRealTap] (bulk) stays false
* forever.
*
* Step 64 introduces [canRunControlledRealTapSession] as the controlled
* session's bespoke gate. It does NOT relax any rule — it is identical to
* [canRunRealTapSingleProto] in its base preconditions and additionally
* accepts a `sessionId` so audit reasoning can correlate a denial to a
* specific session. The bulk gate [canRunRealTap] still returns false
* unconditionally — DO NOT change that.
*
* The mutable [SafetyState] snapshot driven by these mutators is defined in
* `SafetyState.kt` (single source of truth). Step 64 removed a duplicate
* inline copy that previously lived in this file and broke compilation.
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
    * Step 64 — controlled real-tap session gate. Same hard preconditions as
    * the single-tap prototype: review passed, accessibility bound, session
    * active, and per-tap consent fresh. The `sessionId` parameter is opaque
    * to the gate but threaded into [getControlledSessionBlockedReasons] so
    * the diagnostic list can reflect WHICH session was denied.
    *
    * This is NOT a relaxation of any prior rule. Bulk taps remain blocked
    * via [canRunRealTap] returning false. The controlled session is a
    * UI-level construct that issues one consented single-tap at a time —
    * each dispatch still passes through [canRunRealTapSingleProto] freshness
    * checks. This method exists so the controlled-session controller has a
    * single API surface to query, instead of duplicating the four-flag
    * boolean expression at every call site.
    *
    * Returns true only when ALL of:
    *  - [SafetyState.realTapSafetyReviewPassed]
    *  - [SafetyState.accessibilityServiceEnabled]
    *  - [SafetyState.realTapSessionActive]
    *  - [SafetyState.realTapConsentFresh]
    *
    * @param sessionId opaque identifier for the controlled session — not
    *   validated here, used only as a debugging affordance.
    */
   @Suppress("UNUSED_PARAMETER")
   fun canRunControlledRealTapSession(sessionId: String): Boolean =
       canRunRealTapSingleProto()

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

   /**
    * Step 64 — diagnostic list of which controlled-session gating flags are
    * currently missing for the given session id. Returns an empty list when
    * the session is allowed to proceed.
    */
   fun getControlledSessionBlockedReasons(sessionId: String): List<String> {
       val reasons = mutableListOf<String>()
       if (!state.realTapSafetyReviewPassed) {
           reasons += "Safety review not completed (sessionId=$sessionId)."
       }
       if (!state.accessibilityServiceEnabled) {
           reasons += "Accessibility service not enabled (sessionId=$sessionId)."
       }
       if (!state.realTapSessionActive) {
           reasons += "Controlled session not started (sessionId=$sessionId)."
       }
       if (!state.realTapConsentFresh) {
           reasons += "Per-tap consent not confirmed (sessionId=$sessionId)."
       }
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
