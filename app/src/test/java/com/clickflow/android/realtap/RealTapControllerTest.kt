package com.clickflow.android.realtap

import com.clickflow.android.audit.AuditLogManager
import com.clickflow.android.audit.AuditType
import com.clickflow.android.safety.SafetyGate
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Step 65 — pure-JVM unit tests for [RealTapController].
 *
 * The audit log is constructed with NO storage file, so the JSONL /
 * `org.json` persistence path is never exercised and the tests stay free of
 * the Android framework.
 */
class RealTapControllerTest {

    private fun newController(gate: SafetyGate): Pair<RealTapController, AuditLogManager> {
        val log = AuditLogManager() // no storage file -> in-memory only, JVM-safe
        return RealTapController(gate, log) to log
    }

    @Test
    fun invalidMarkerIsBlockedAsInvalidConsent() {
        val (controller, log) = newController(SafetyGate())
        val decision = controller.evaluate(RealTapController.Marker(null, 42), "s1")
        assertEquals(RealTapController.Decision.BLOCKED_INVALID_CONSENT, decision)
        assertEquals(AuditType.REAL_TAP_BLOCKED, log.lastType())
    }

    @Test
    fun missingAccessibilityServiceIsBlockedNoService() {
        val gate = SafetyGate() // accessibility disabled by default
        val (controller, log) = newController(gate)
        val decision = controller.evaluate(RealTapController.Marker(10, 20), "s2")
        assertEquals(RealTapController.Decision.BLOCKED_NO_SERVICE, decision)
        assertEquals(AuditType.REAL_TAP_PERMISSION_MISSING, log.lastType())
    }

    @Test
    fun gateClosedIsBlockedByGate() {
        val gate = SafetyGate()
        gate.updateAccessibility(true) // service bound, but session/review/consent missing
        val (controller, log) = newController(gate)
        val decision = controller.evaluate(RealTapController.Marker(10, 20), "s3")
        assertEquals(RealTapController.Decision.BLOCKED_BY_GATE, decision)
        assertEquals(AuditType.REAL_TAP_BLOCKED, log.lastType())
    }

    @Test
    fun allFlagsLiveYieldsAllowedDecision() {
        val gate = SafetyGate()
        gate.updateReviewPassed(true)
        gate.updateAccessibility(true)
        gate.updateSession(true)
        gate.updateConsentFresh(true)
        val (controller, log) = newController(gate)
        val decision = controller.evaluate(RealTapController.Marker(10, 20), "s4")
        assertEquals(RealTapController.Decision.ALLOWED, decision)
        assertEquals(AuditType.REAL_TAP_DISPATCH_ATTEMPTED, log.lastType())
        // The bulk gate is still categorically closed regardless of the decision.
        assertEquals(false, gate.canRunRealTap())
    }

    @Test
    fun recordHelpersEmitGranularAuditEvents() {
        val (controller, log) = newController(SafetyGate())
        controller.recordSessionStarted("s5")
        assertEquals(AuditType.REAL_TAP_SESSION_STARTED, log.lastType())
        controller.recordSafetyReviewPassed()
        assertEquals(AuditType.REAL_TAP_SAFETY_REVIEW_PASSED, log.lastType())
        controller.recordConsentDeclined("s5", "user_cancelled")
        assertEquals(AuditType.REAL_TAP_CONSENT_DECLINED, log.lastType())
    }
}
