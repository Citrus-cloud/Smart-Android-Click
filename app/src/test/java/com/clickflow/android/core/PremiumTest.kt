package com.clickflow.android.core

import org.junit.Assert.assertEquals
import org.junit.Test

class PremiumTest {

    @Test
    fun freeTargetLimit_is2() {
        assertEquals(2, Premium.FREE_TARGET_LIMIT)
    }

    @Test
    fun premiumTargetLimit_is10() {
        assertEquals(10, Premium.PREMIUM_TARGET_LIMIT)
    }

    @Test
    fun markerLimit_is50() {
        assertEquals(50, Premium.MARKER_LIMIT)
    }

    @Test
    fun premiumIsGreaterThanFree() {
        assertTrue(Premium.PREMIUM_TARGET_LIMIT > Premium.FREE_TARGET_LIMIT)
    }

    @Test
    fun freeTargetLimit_positive() {
        assertTrue(Premium.FREE_TARGET_LIMIT > 0)
    }

    @Test
    fun premiumTargetLimit_positive() {
        assertTrue(Premium.PREMIUM_TARGET_LIMIT > 0)
    }

    @Test
    fun markerLimit_positive() {
        assertTrue(Premium.MARKER_LIMIT > 0)
    }

    @Test
    fun markerLimit_greaterThanPremiumTargetLimit() {
        assertTrue(Premium.MARKER_LIMIT >= Premium.PREMIUM_TARGET_LIMIT)
    }

    // Note: isPremiumUnlocked, setPremiumUnlocked, and targetLimit require Android Context
    // and SharedPreferences, so they cannot be unit-tested on JVM without mocking.
    // These are integration tests that should run on device.

    private fun assertTrue(condition: Boolean) {
        org.junit.Assert.assertTrue(condition)
    }
}
