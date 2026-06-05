package com.clickflow.android.capture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure-JVM tests for [TemplateMatcher] / [MatchResult] (no Android, no device, no tap). */
class TemplateMatcherTest {

	private val region = CaptureRegion(0.1f, 0.1f, 0.5f, 0.4f)

	private fun template(threshold: Float = 0.8f) = CaptureTemplate(
		id = "tpl-1",
		name = "Target",
		region = region,
		widthPx = 20,
		heightPx = 12,
		matchThreshold = threshold,
	)

	private fun matcher(now: Long = 7000L) = TemplateMatcher(nowProvider = { now })

	@Test
	fun matchAboveThreshold() {
		val r = matcher().evaluate(template(0.8f), MatchCandidate(region, 0.9f))
		assertTrue(r.matched)
		assertEquals(0.9f, r.confidence, 0.0001f)
		assertEquals(region.clampedToUnit(), r.location)
		assertEquals(region.clampedToUnit(), r.highlight)
	}

	@Test
	fun noMatchBelowThreshold() {
		val r = matcher().evaluate(template(0.8f), MatchCandidate(region, 0.5f))
		assertFalse(r.matched)
		assertNull(r.location)
		assertNull(r.highlight)
		assertEquals(0.5f, r.confidence, 0.0001f)
	}

	@Test
	fun matchExactlyAtThreshold() {
		val r = matcher().evaluate(template(0.8f), MatchCandidate(region, 0.8f))
		assertTrue(r.matched)
	}

	@Test
	fun confidenceIsClamped() {
		val high = matcher().evaluate(template(0.8f), MatchCandidate(region, 1.5f))
		assertEquals(1.0f, high.confidence, 0.0001f)
		assertTrue(high.matched)
		val low = matcher().evaluate(template(0.8f), MatchCandidate(region, -0.3f))
		assertEquals(0.0f, low.confidence, 0.0001f)
		assertFalse(low.matched)
	}

	@Test
	fun evaluateBestPicksHighestScore() {
		val a = MatchCandidate(CaptureRegion(0.0f, 0.0f, 0.2f, 0.2f), 0.55f)
		val b = MatchCandidate(CaptureRegion(0.5f, 0.5f, 0.7f, 0.7f), 0.92f)
		val r = matcher().evaluateBest(template(0.8f), listOf(a, b))
		assertTrue(r.matched)
		assertEquals(0.92f, r.confidence, 0.0001f)
		assertEquals(b.location.clampedToUnit(), r.location)
	}

	@Test
	fun evaluateBestEmptyIsNoMatch() {
		val r = matcher().evaluateBest(template(0.8f), emptyList())
		assertFalse(r.matched)
		assertEquals(0.0f, r.confidence, 0.0001f)
		assertNull(r.location)
		assertEquals("tpl-1", r.templateId)
	}

	@Test
	fun evaluatedAtMsComesFromProvider() {
		val r = matcher(now = 4242L).evaluate(template(), MatchCandidate(region, 0.9f))
		assertEquals(4242L, r.evaluatedAtMs)
	}

	@Test
	fun matchesOnlyFiltersAndSorts() {
		val m = matcher()
		val t1 = template(0.8f).copy(id = "tpl-1")
		val t2 = template(0.8f).copy(id = "tpl-2")
		val t3 = template(0.8f).copy(id = "tpl-3")
		val results = m.matchesOnly(
			listOf(
				t1 to listOf(MatchCandidate(region, 0.85f)),
				t2 to listOf(MatchCandidate(region, 0.5f)),
				t3 to listOf(MatchCandidate(region, 0.99f)),
			),
		)
		assertEquals(2, results.size)
		assertEquals("tpl-3", results[0].templateId)
		assertEquals("tpl-1", results[1].templateId)
	}
}
