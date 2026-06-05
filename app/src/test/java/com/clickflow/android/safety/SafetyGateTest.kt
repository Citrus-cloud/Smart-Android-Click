package com.clickflow.android.safety

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Step 65 — pure-JVM unit tests for [SafetyGate].
 *
 * No Android framework, no Robolectric: [SafetyGate] is plain Kotlin so these
 * run under `./gradlew testDebugUnitTest`.
 */
class SafetyGateTest {

    @Test
    fun bulkRealTapIsAlwaysBlocked() {
        val gate = SafetyGate()
        assertFalse(gate.canRunRealTap())
        // Even with every prototype flag live, the bulk gate stays closed.
        gate.updateReviewPassed(true)
        gate.updateAccessibility(true)
        gate.updateSession(true)
        gate.updateConsentFresh(true)
        assertFalse("Bulk real taps must never be allowed", gate.canRunRealTap())
        assertFalse(gate.attemptRealTap())
    }

    @Test
    fun singleProtoRequiresAllFourFlags() {
        val gate = SafetyGate()
        assertFalse(gate.canRunRealTapSingleProto())

        gate.updateReviewPassed(true)
        assertFalse(gate.canRunRealTapSingleProto())
        gate.updateAccessibility(true)
        assertFalse(gate.canRunRealTapSingleProto())
        gate.updateSession(true)
        assertFalse(gate.canRunRealTapSingleProto())
        gate.updateConsentFresh(true)
        assertTrue(gate.canRunRealTapSingleProto())
    }

    @Test
    fun resetPrototypeFlagsClearsReviewSessionConsentButKeepsAccessibility() {
        val gate = SafetyGate()
        gate.updateReviewPassed(true)
        gate.updateAccessibility(true)
        gate.updateSession(true)
        gate.updateConsentFresh(true)
        assertTrue(gate.canRunRealTapSingleProto())

        gate.resetPrototypeFlags()
        assertFalse(gate.canRunRealTapSingleProto())
        val state = gate.currentState()
        assertFalse(state.realTapSafetyReviewPassed)
        assertFalse(state.realTapSessionActive)
        assertFalse(state.realTapConsentFresh)
        // Accessibility reflects system settings and is intentionally preserved.
        assertTrue(state.accessibilityServiceEnabled)
    }

    @Test
    fun controlledSessionMatchesSingleProtoAndReportsReasons() {
        val gate = SafetyGate()
        val sid = "session-123"
        assertFalse(gate.canRunControlledRealTapSession(sid))
        val reasons = gate.getControlledSessionBlockedReasons(sid)
        assertEquals(4, reasons.size)
        assertTrue("every blocked reason should carry the session id", reasons.all { it.contains(sid) })

        gate.updateReviewPassed(true)
        gate.updateAccessibility(true)
        gate.updateSession(true)
        gate.updateConsentFresh(true)
        assertTrue(gate.canRunControlledRealTapSession(sid))
        assertTrue(gate.getControlledSessionBlockedReasons(sid).isEmpty())
    }

    @Test
    fun singleProtoBlockedReasonsShrinkAsFlagsAreSet() {
        val gate = SafetyGate()
        assertEquals(4, gate.getSingleProtoBlockedReasons().size)
        gate.updateReviewPassed(true)
        gate.updateAccessibility(true)
        gate.updateSession(true)
        gate.updateConsentFresh(true)
        assertTrue(gate.getSingleProtoBlockedReasons().isEmpty())
    }
}
