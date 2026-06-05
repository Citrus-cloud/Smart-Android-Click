package com.clickflow.android.realtap

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Step 65 — pure-JVM unit tests for the in-memory [RealTapSafetyReview]. */
class RealTapSafetyReviewTest {

    @Test
    fun emptyReviewIsNotPassed() {
        assertFalse(RealTapSafetyReview.EMPTY.passed)
    }

    @Test
    fun acknowledgingAllItemsThenMarkPassedPasses() {
        var review = RealTapSafetyReview()
        RealTapSafetyReview.REQUIRED_ITEMS.forEach { review = review.acknowledge(it) }
        assertFalse("Not passed until markPassed is called", review.passed)
        review = review.markPassed(123L)
        assertTrue(review.passed)
    }

    @Test(expected = IllegalArgumentException::class)
    fun acknowledgingUnknownItemThrows() {
        RealTapSafetyReview().acknowledge("not_a_real_item")
    }

    @Test
    fun revokingAnItemAfterPassingClearsPassed() {
        var review = RealTapSafetyReview()
        RealTapSafetyReview.REQUIRED_ITEMS.forEach { review = review.acknowledge(it) }
        review = review.markPassed(123L)
        assertTrue(review.passed)
        review = review.revoke(RealTapSafetyReview.REQUIRED_ITEMS.first())
        assertFalse(review.passed)
    }

    @Test
    fun resetReturnsEmptyReview() {
        var review = RealTapSafetyReview()
        RealTapSafetyReview.REQUIRED_ITEMS.forEach { review = review.acknowledge(it) }
        review = review.markPassed(123L).reset()
        assertEquals(RealTapSafetyReview.EMPTY, review)
        assertFalse(review.passed)
    }
}
