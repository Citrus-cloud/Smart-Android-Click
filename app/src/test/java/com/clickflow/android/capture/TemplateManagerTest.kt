package com.clickflow.android.capture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure-JVM tests for [TemplateManager] / [CaptureTemplate] (no Android, no device). */
class TemplateManagerTest {

	private val region = CaptureRegion(0.1f, 0.1f, 0.5f, 0.4f)

	private fun manager(now: Long = 1000L) = TemplateManager(nowProvider = { now })

	private fun ok(result: TemplateManager.Result): CaptureTemplate {
		assertTrue("expected Ok but was $result", result is TemplateManager.Result.Ok)
		return (result as TemplateManager.Result.Ok).template
	}

	@Test
	fun startsEmpty() {
		val m = manager()
		assertEquals(0, m.count())
		assertTrue(m.isEmpty())
		assertTrue(m.list().isEmpty())
	}

	@Test
	fun addRegistersValidTemplate() {
		val m = manager(now = 4242L)
		val tpl = ok(m.add("Login button", region, widthPx = 120, heightPx = 48))
		assertEquals("Login button", tpl.name)
		assertEquals(120, tpl.widthPx)
		assertEquals(48, tpl.heightPx)
		assertEquals(4242L, tpl.createdAtMs)
		assertTrue(tpl.isValid)
		assertEquals(1, m.count())
		assertNotNull(m.get(tpl.id))
		assertTrue(m.contains(tpl.id))
	}

	@Test
	fun addTrimsName() {
		val m = manager()
		val tpl = ok(m.add("  Spaced  ", region, 10, 10))
		assertEquals("Spaced", tpl.name)
	}

	@Test
	fun addRejectsEmptyName() {
		val m = manager()
		assertEquals(
			TemplateManager.Result.Error(TemplateManager.ERROR_EMPTY_NAME),
			m.add("   ", region, 10, 10),
		)
		assertEquals(0, m.count())
	}

	@Test
	fun addRejectsInvalidSize() {
		val m = manager()
		assertEquals(
			TemplateManager.Result.Error(TemplateManager.ERROR_INVALID_SIZE),
			m.add("X", region, 0, 10),
		)
		assertEquals(
			TemplateManager.Result.Error(TemplateManager.ERROR_INVALID_SIZE),
			m.add("X", region, 10, -1),
		)
	}

	@Test
	fun addRejectsInvalidRegion() {
		val m = manager()
		// Zero-area region (left == right) is not valid.
		val bad = CaptureRegion(0.3f, 0.3f, 0.3f, 0.6f)
		assertEquals(
			TemplateManager.Result.Error(TemplateManager.ERROR_INVALID_REGION),
			m.add("X", bad, 10, 10),
		)
	}

	@Test
	fun addRejectsDuplicateNameCaseInsensitive() {
		val m = manager()
		m.add("Target", region, 10, 10)
		assertEquals(
			TemplateManager.Result.Error(TemplateManager.ERROR_DUPLICATE_NAME),
			m.add("target", region, 10, 10),
		)
		assertEquals(1, m.count())
	}

	@Test
	fun addClampsThreshold() {
		val m = manager()
		val high = ok(m.add("High", region, 10, 10, matchThreshold = 5f))
		assertEquals(CaptureTemplate.MAX_THRESHOLD, high.matchThreshold, 0.0001f)
		val low = ok(m.add("Low", region, 10, 10, matchThreshold = -1f))
		assertEquals(CaptureTemplate.MIN_THRESHOLD, low.matchThreshold, 0.0001f)
	}

	@Test
	fun idsAreUniqueAndSequential() {
		val m = manager()
		val a = ok(m.add("A", region, 10, 10))
		val b = ok(m.add("B", region, 10, 10))
		assertTrue(a.id != b.id)
		assertEquals("tpl-1", a.id)
		assertEquals("tpl-2", b.id)
	}

	@Test
	fun renameUpdatesName() {
		val m = manager()
		val a = ok(m.add("A", region, 10, 10))
		ok(m.rename(a.id, "Renamed"))
		assertEquals("Renamed", m.get(a.id)?.name)
	}

	@Test
	fun renameRejectsDuplicate() {
		val m = manager()
		val a = ok(m.add("A", region, 10, 10))
		m.add("B", region, 10, 10)
		assertEquals(
			TemplateManager.Result.Error(TemplateManager.ERROR_DUPLICATE_NAME),
			m.rename(a.id, "B"),
		)
		assertEquals("A", m.get(a.id)?.name)
	}

	@Test
	fun renameToOwnNameIsAllowed() {
		val m = manager()
		val a = ok(m.add("A", region, 10, 10))
		ok(m.rename(a.id, "A"))
	}

	@Test
	fun renameMissingReturnsNotFound() {
		val m = manager()
		assertEquals(
			TemplateManager.Result.Error(TemplateManager.ERROR_NOT_FOUND),
			m.rename("nope", "X"),
		)
	}

	@Test
	fun setThresholdClampsAndUpdates() {
		val m = manager()
		val a = ok(m.add("A", region, 10, 10))
		ok(m.setThreshold(a.id, 0.05f))
		assertEquals(CaptureTemplate.MIN_THRESHOLD, m.get(a.id)!!.matchThreshold, 0.0001f)
	}

	@Test
	fun setRegionUpdates() {
		val m = manager()
		val a = ok(m.add("A", region, 10, 10))
		val newRegion = CaptureRegion(0.2f, 0.2f, 0.6f, 0.6f)
		ok(m.setRegion(a.id, newRegion))
		assertEquals(newRegion.clampedToUnit(), m.get(a.id)?.region)
	}

	@Test
	fun removeDeletesTemplate() {
		val m = manager()
		val a = ok(m.add("A", region, 10, 10))
		assertTrue(m.remove(a.id))
		assertNull(m.get(a.id))
		assertFalse(m.remove(a.id))
	}

	@Test
	fun clearEmptiesRegistry() {
		val m = manager()
		m.add("A", region, 10, 10)
		m.add("B", region, 10, 10)
		m.clear()
		assertEquals(0, m.count())
		assertTrue(m.isEmpty())
	}
}
