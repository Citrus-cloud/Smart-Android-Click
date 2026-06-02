package com.clickflow.android.safety

/**
* Immutable snapshot of the app's safety posture.
*
* Step 52 invariant: this is a SIMULATION-ONLY foundation.
* Step 62 adds the *single-real-tap prototype* flags. The classic real-tap
* surface (loops, batches, repeats, scenarios with real input) remains
* categorically OFF — [realTapsEnabled] stays false. The new prototype flags
* gate a tightly-scoped, single-shot, user-confirmed tap via AccessibilityService.
*
* Every capability that could perform real bulk input is disabled and is NOT
* implemented anywhere in the codebase. These flags describe intent and are
* the single source of truth consulted by [SafetyGate].
*/
data class SafetyState(
   /** Whole-app simulation-only banner. Step 52: true. */
   val simulationOnly: Boolean = true,

   /**
    * Whether bulk/looped/scenario real taps are enabled.
    * Step 52..62: HARD-CODED false. Do not flip without removing the
    * SafetyGate block AND completing a separate safety review for bulk input.
    */
   val realTapsEnabled: Boolean = false,

   /** Whether the system thinks our AccessibilityService is enabled and connected. */
   val accessibilityServiceEnabled: Boolean = false,

   /** Step 62: MediaProjection has not been requested. Must stay false. */
   val mediaProjectionEnabled: Boolean = false,

   /** Whether SYSTEM_ALERT_WINDOW (overlay) permission has been granted. */
   val overlayPermissionEnabled: Boolean = false,

   /** UI must always offer an emergency stop. Step 52..62: true. */
   val emergencyStopReady: Boolean = true,

   // ---- Step 62: single-real-tap prototype ----

   /**
    * The user has read and acknowledged every item in the realtap safety review
    * during the current process lifetime.
    */
   val realTapSafetyReviewPassed: Boolean = false,

   /** A single-real-tap session is currently open. In-memory only — never persisted. */
   val realTapSessionActive: Boolean = false,

   /** Fresh per-tap consent has been confirmed within [RealTapSession.CONSENT_TTL_MS]. */
   val realTapConsentFresh: Boolean = false,
) {
   companion object {
       /** The default state shipped in Step 52, unchanged. */
       val STEP_52_DEFAULT = SafetyState()

       /** Alias for the Step 62 default — identical to STEP_52_DEFAULT. */
       val STEP_62_DEFAULT = SafetyState()
   }
}
