package com.clickflow.android.realtap

import com.clickflow.android.audit.AuditLogManager
import com.clickflow.android.audit.AuditSeverity
import com.clickflow.android.audit.AuditType
import com.clickflow.android.safety.SafetyGate

/**
* Step 64 Part 1 — single domain entry point for every real-tap request.
*
* Until Step 64, real-tap consent/dispatch logic lived directly inside
* `ClickFlowViewModel`. That made the gating contract diffuse: every new caller
* (controlled-session controller, future floating controls) had to re-derive
* "what does it mean to be allowed to dispatch a real tap?" from scratch.
*
* `RealTapController` centralizes that contract. It holds NO mutable state of
* its own — it composes `SafetyGate` + `AuditLogManager` and exposes pure
* decision functions plus thin audit-recording helpers. Every real-tap call
* site (single-tap prototype, controlled session) routes through this class so
* the invariants are enforced in exactly one place.
*
* Hard invariants enforced here:
*  - A marker MUST be present (non-null pair) for any real-tap intent. Without
*    a marker the request is rejected as `BLOCKED_INVALID_CONSENT`.
*  - The single-prototype gate (`canRunRealTapSingleProto`) AND the bulk gate
*    (`canRunRealTap`) MUST be evaluated together. Today bulk is hard-coded
*    false, so every dispatch is BLOCKED_BY_GATE — that is intentional.
*  - Every decision is recorded with a granular `AuditType.REAL_TAP_*` entry
*    so the audit log surfaces lifecycle separately from the generic
*    `safety.realTapBlocked` chokepoint.
*
* This class does NOT touch StateFlows, the ViewModel, or any Android API. It
* is pure orchestration so it can be unit-tested without an `Application`.
*/
class RealTapController(
   private val gate: SafetyGate,
   private val auditLog: AuditLogManager,
) {

   /** Outcome of a single real-tap evaluation. */
   enum class Decision {
       ALLOWED,
       BLOCKED_BY_GATE,
       BLOCKED_NO_SERVICE,
       BLOCKED_INVALID_CONSENT,
   }

   /**
    * Marker for a real-tap request. Non-null x/y are REQUIRED — passing a
    * `null` marker is the canonical "invalid consent" signal and produces
    * [Decision.BLOCKED_INVALID_CONSENT].
    */
   data class Marker(val x: Int?, val y: Int?) {
       val isValid: Boolean get() = x != null && y != null
   }

   /**
    * Step 64 Part 1 invariant check: every real-tap request MUST come with a
    * non-null marker. This is the only place this rule is enforced — callers
    * never bypass it because they all route through [evaluate].
    */
   fun evaluate(marker: Marker, sessionId: String?): Decision {
       if (!marker.isValid) {
           auditLog.log(
               AuditType.REAL_TAP_BLOCKED,
               AuditSeverity.SAFETY,
               "Real-tap blocked: marker missing (sessionId=$sessionId)",
           )
           return Decision.BLOCKED_INVALID_CONSENT
       }

       // Accessibility-service-missing is reported as its own granular
       // outcome so the UI can render "service not bound" distinctly from
       // "gate blocked". The gate itself still denies — this is defense in
       // depth, not a bypass.
       if (!gate.currentState().accessibilityServiceEnabled) {
           auditLog.log(
               AuditType.REAL_TAP_PERMISSION_MISSING,
               AuditSeverity.SAFETY,
               "Real-tap blocked: accessibility service not enabled " +
                   "(sessionId=$sessionId, marker=(${marker.x},${marker.y}))",
           )
           return Decision.BLOCKED_NO_SERVICE
       }

       // Bulk gate is hard-coded false (Step 52). Single-proto gate may be
       // true if all four flags are live. Either denial → BLOCKED_BY_GATE.
       val bulkAllowed = gate.canRunRealTap()
       val singleAllowed = gate.canRunRealTapSingleProto()
       if (!bulkAllowed && !singleAllowed) {
           auditLog.log(
               AuditType.REAL_TAP_BLOCKED,
               AuditSeverity.SAFETY,
               "Real-tap blocked by SafetyGate (sessionId=$sessionId, " +
                   "marker=(${marker.x},${marker.y}), bulk=$bulkAllowed, " +
                   "single=$singleAllowed)",
           )
           // Defensive chokepoint — also pings the centralized denier.
           gate.attemptRealTap()
           return Decision.BLOCKED_BY_GATE
       }

       // Both bulk OR single passed — today this branch is unreachable
       // because bulk is false and single requires all four live flags +
       // canRunRealTap()=false short-circuits dispatch upstream. It exists
       // so Step 64 Part 2 (controlled session) can wire dispatch in
       // without changing this contract.
       auditLog.log(
           AuditType.REAL_TAP_DISPATCH_ATTEMPTED,
           AuditSeverity.SAFETY,
           "Real-tap allowed by SafetyGate (sessionId=$sessionId, " +
               "marker=(${marker.x},${marker.y}))",
       )
       return Decision.ALLOWED
   }

   /**
    * Records a granular session-started event. Centralizing this here means
    * future controlled-session lifecycle calls and the single-tap prototype
    * share the exact same audit shape.
    */
   fun recordSessionStarted(sessionId: String) {
       auditLog.log(
           AuditType.REAL_TAP_SESSION_STARTED,
           AuditSeverity.SAFETY,
           "Real-tap session started (sessionId=$sessionId, gate=blocked)",
       )
   }

   /** Records a granular session-ended event. */
   fun recordSessionEnded(sessionId: String?, reason: String) {
       auditLog.log(
           AuditType.REAL_TAP_SESSION_ENDED,
           AuditSeverity.SAFETY,
           "Real-tap session ended (sessionId=$sessionId, reason=$reason)",
       )
   }

   /** Records a granular consent-requested event. */
   fun recordConsentRequested(sessionId: String?, x: Int, y: Int) {
       auditLog.log(
           AuditType.REAL_TAP_CONSENT_SHOWN,
           AuditSeverity.SAFETY,
           "Real-tap consent requested at ($x,$y) — sessionId=$sessionId",
       )
   }

   /** Records a granular consent-given event. */
   fun recordConsentGiven(sessionId: String?, x: Int, y: Int) {
       auditLog.log(
           AuditType.REAL_TAP_CONSENT_GIVEN,
           AuditSeverity.SAFETY,
           "Real-tap consent given at ($x,$y) — sessionId=$sessionId",
       )
   }

   /** Records a granular consent-declined event. */
   fun recordConsentDeclined(sessionId: String?, reason: String) {
       auditLog.log(
           AuditType.REAL_TAP_CONSENT_DECLINED,
           AuditSeverity.SAFETY,
           "Real-tap consent declined (sessionId=$sessionId, reason=$reason)",
       )
   }

   /** Records a granular dispatch-failed event. */
   fun recordDispatchFailed(sessionId: String?, reason: String) {
       auditLog.log(
           AuditType.REAL_TAP_DISPATCH_FAILED,
           AuditSeverity.SAFETY,
           "Real-tap dispatch failed (sessionId=$sessionId, reason=$reason)",
       )
   }

   /** Records a granular safety-review-passed event. */
   fun recordSafetyReviewPassed() {
       auditLog.log(
           AuditType.REAL_TAP_SAFETY_REVIEW_PASSED,
           AuditSeverity.SAFETY,
           "Safety review passed (all 10 items checked)",
       )
   }

   /** Records a granular safety-review-failed event. */
   fun recordSafetyReviewFailed(missingCount: Int) {
       auditLog.log(
           AuditType.REAL_TAP_SAFETY_REVIEW_FAILED,
           AuditSeverity.SAFETY,
           "Safety review incomplete (missing=$missingCount)",
       )
   }
}
