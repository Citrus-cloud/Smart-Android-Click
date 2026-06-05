package com.clickflow.android.realtap

import com.clickflow.android.realtap.RealTapSession.Companion.CONSENT_TTL_MS
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Step 65 — pure-JVM unit tests for the in-memory [RealTapSession]. */
class RealTapSessionTest {

    @Test
    fun freshSessionHasNoConsent() {
        val now = 1_000L
        val session = RealTapSession.start(now)
        assertTrue(session.id.startsWith("rts_"))
        assertFalse(session.hasFreshConsent(now))
    }

    @Test
    fun consentIsFreshWithinTtl() {
        val now = 1_000L
        val session = RealTapSession.start(now).grantConsent(now)
        assertNotNull(session.consentNonce)
        assertTrue(session.hasFreshConsent(now))
        assertTrue(session.hasFreshConsent(now + CONSENT_TTL_MS))
    }

    @Test
    fun consentExpiresAfterTtl() {
        val now = 1_000L
        val session = RealTapSession.start(now).grantConsent(now)
        assertFalse(session.hasFreshConsent(now + CONSENT_TTL_MS + 1))
    }

    @Test
    fun consentIsSingleUse() {
        val now = 1_000L
        val granted = RealTapSession.start(now).grantConsent(now)
        val consumed = granted.consumeConsent(now)
        assertNull(consumed.consentNonce)
        assertFalse(consumed.hasFreshConsent(now))
    }

    @Test
    fun consumingStaleConsentIsANoOp() {
        val now = 1_000L
        val granted = RealTapSession.start(now).grantConsent(now)
        // Past the TTL the consent is not fresh, so consume returns the same instance.
        val consumed = granted.consumeConsent(now + CONSENT_TTL_MS + 1)
        assertTrue(consumed === granted)
    }
}
