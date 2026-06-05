package com.clickflow.android.capture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RegionSelectorControllerTest {

    @Test
    fun initialStateIsEmpty() {
        val c = RegionSelectorController()
        assertEquals(SelectionPhase.EMPTY, c.state().phase)
        assertNull(c.state().region)
        assertFalse(c.state().hasRegion)
    }

    @Test
    fun beginSelectionEntersDraggingWithPointRect() {
        val c = RegionSelectorController()
        val s = c.beginSelection(0.3f, 0.4f)
        assertEquals(SelectionPhase.DRAGGING, s.phase)
        val r = s.region!!
        assertEquals(0.3f, r.left, EPS)
        assertEquals(0.4f, r.top, EPS)
        assertEquals(0.3f, r.right, EPS)
        assertEquals(0.4f, r.bottom, EPS)
    }

    @Test
    fun updateNormalizesRegardlessOfDragDirection() {
        val c = RegionSelectorController()
        c.beginSelection(0.6f, 0.7f)
        // drag up-left so the new corner is above/left of the anchor
        val r = c.updateSelection(0.2f, 0.1f).region!!
        assertEquals(0.2f, r.left, EPS)
        assertEquals(0.1f, r.top, EPS)
        assertEquals(0.6f, r.right, EPS)
        assertEquals(0.7f, r.bottom, EPS)
    }

    @Test
    fun updateWithoutBeginIsRejected() {
        val c = RegionSelectorController()
        val s = c.updateSelection(0.5f, 0.5f)
        assertEquals(SelectionPhase.EMPTY, s.phase)
        assertEquals("no_active_selection", s.error)
    }

    @Test
    fun commitTooSmallStaysDragging() {
        val c = RegionSelectorController()
        c.beginSelection(0.5f, 0.5f)
        c.updateSelection(0.505f, 0.505f) // 0.5% < 2% min
        val s = c.commitSelection()
        assertEquals(SelectionPhase.DRAGGING, s.phase)
        assertEquals("region_too_small", s.error)
        assertFalse(s.hasRegion)
    }

    @Test
    fun commitValidRegionSelects() {
        val c = RegionSelectorController()
        c.beginSelection(0.1f, 0.1f)
        c.updateSelection(0.8f, 0.6f)
        val s = c.commitSelection()
        assertEquals(SelectionPhase.SELECTED, s.phase)
        assertTrue(s.hasRegion)
        assertNull(s.error)
        assertTrue(s.region!!.contains(0.4f, 0.3f))
        assertFalse(s.region!!.contains(0.9f, 0.9f))
    }

    @Test
    fun setRegionClampsAndSelects() {
        val c = RegionSelectorController()
        val s = c.setRegion(CaptureRegion(-0.5f, -0.5f, 1.5f, 1.5f))
        assertEquals(SelectionPhase.SELECTED, s.phase)
        val r = s.region!!
        assertEquals(0f, r.left, EPS)
        assertEquals(0f, r.top, EPS)
        assertEquals(1f, r.right, EPS)
        assertEquals(1f, r.bottom, EPS)
    }

    @Test
    fun setRegionTooSmallIsRejected() {
        val c = RegionSelectorController()
        val s = c.setRegion(CaptureRegion(0.5f, 0.5f, 0.505f, 0.505f))
        assertEquals(SelectionPhase.EMPTY, s.phase)
        assertEquals("region_too_small", s.error)
    }

    @Test
    fun clearResetsToEmpty() {
        val c = RegionSelectorController()
        c.beginSelection(0.1f, 0.1f)
        c.updateSelection(0.9f, 0.9f)
        c.commitSelection()
        val s = c.clear()
        assertEquals(SelectionPhase.EMPTY, s.phase)
        assertNull(s.region)
    }

    @Test
    fun captureRegionGeometry() {
        val r = CaptureRegion(0.2f, 0.1f, 0.6f, 0.5f)
        assertEquals(0.4f, r.width, EPS)
        assertEquals(0.4f, r.height, EPS)
        assertEquals(0.4f, r.centerX, EPS)
        assertEquals(0.3f, r.centerY, EPS)
        assertTrue(r.isValid)
        val px = r.toPixels(1000, 1000)
        assertEquals(200, px[0])
        assertEquals(100, px[1])
        assertEquals(600, px[2])
        assertEquals(500, px[3])
    }

    companion object {
        private const val EPS = 1e-4f
    }
}
