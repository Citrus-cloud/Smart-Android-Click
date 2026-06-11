package com.clickflow.android.imageclick

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImageClickTemplateNormalizedTest {

    private fun baseTemplate() = ImageClickTemplate(
        id = "t1",
        name = "Test",
        filePath = "/data/test.png",
        width = 100,
        height = 100,
    )

    // ---- Region bounds ----

    @Test
    fun region_left_coercedToZero() {
        val t = baseTemplate().copy(regionLeft = -0.5f).normalized()
        assertEquals(0f, t.regionLeft, 0.001f)
    }

    @Test
    fun region_left_maxAt095() {
        val t = baseTemplate().copy(regionLeft = 1.0f).normalized()
        assertEquals(0.95f, t.regionLeft, 0.001f)
    }

    @Test
    fun region_top_coercedToZero() {
        val t = baseTemplate().copy(regionTop = -1.0f).normalized()
        assertEquals(0f, t.regionTop, 0.001f)
    }

    @Test
    fun region_top_maxAt095() {
        val t = baseTemplate().copy(regionTop = 1.0f).normalized()
        assertEquals(0.95f, t.regionTop, 0.001f)
    }

    @Test
    fun region_right_minIsLeftPlus005() {
        val t = baseTemplate().copy(regionLeft = 0.5f, regionRight = 0.3f).normalized()
        assertEquals(0.55f, t.regionRight, 0.001f)
    }

    @Test
    fun region_right_maxIs1() {
        val t = baseTemplate().copy(regionRight = 2.0f).normalized()
        assertEquals(1f, t.regionRight, 0.001f)
    }

    @Test
    fun region_bottom_minIsTopPlus005() {
        val t = baseTemplate().copy(regionTop = 0.5f, regionBottom = 0.3f).normalized()
        assertEquals(0.55f, t.regionBottom, 0.001f)
    }

    @Test
    fun region_bottom_maxIs1() {
        val t = baseTemplate().copy(regionBottom = 2.0f).normalized()
        assertEquals(1f, t.regionBottom, 0.001f)
    }

    // ---- Scale bounds ----

    @Test
    fun scaleMin_coercedTo05() {
        val t = baseTemplate().copy(scaleMin = 0.1f).normalized()
        assertEquals(0.5f, t.scaleMin, 0.001f)
    }

    @Test
    fun scaleMin_maxAt2() {
        val t = baseTemplate().copy(scaleMin = 3.0f).normalized()
        assertEquals(2f, t.scaleMin, 0.001f)
    }

    @Test
    fun scaleMax_atLeastScaleMin() {
        val t = baseTemplate().copy(scaleMin = 1.5f, scaleMax = 0.8f).normalized()
        assertEquals(1.5f, t.scaleMax, 0.001f)
    }

    @Test
    fun scaleMax_maxAt2() {
        val t = baseTemplate().copy(scaleMax = 5.0f).normalized()
        assertEquals(2f, t.scaleMax, 0.001f)
    }

    // ---- Threshold bounds ----

    @Test
    fun threshold_coercedTo05Min() {
        val t = baseTemplate().copy(threshold = 0.1f).normalized()
        assertEquals(0.5f, t.threshold, 0.001f)
    }

    @Test
    fun threshold_coercedTo099Max() {
        val t = baseTemplate().copy(threshold = 1.5f).normalized()
        assertEquals(0.99f, t.threshold, 0.001f)
    }

    // ---- Tap XY bounds ----

    @Test
    fun tapX_coercedTo01() {
        val t1 = baseTemplate().copy(tapX = -0.5f).normalized()
        assertEquals(0f, t1.tapX, 0.001f)
        val t2 = baseTemplate().copy(tapX = 2.0f).normalized()
        assertEquals(1f, t2.tapX, 0.001f)
    }

    @Test
    fun tapY_coercedTo01() {
        val t1 = baseTemplate().copy(tapY = -0.5f).normalized()
        assertEquals(0f, t1.tapY, 0.001f)
        val t2 = baseTemplate().copy(tapY = 2.0f).normalized()
        assertEquals(1f, t2.tapY, 0.001f)
    }

    // ---- Width/Height ----

    @Test
    fun width_height_atLeast1() {
        val t = baseTemplate().copy(width = 0, height = -5).normalized()
        assertEquals(1, t.width)
        assertEquals(1, t.height)
    }

    // ---- Interval/Repeat ----

    @Test
    fun intervalMs_coercedToMin300() {
        val t = baseTemplate().copy(intervalMs = 50L).normalized()
        assertEquals(300L, t.intervalMs)
    }

    @Test
    fun repeatCount_atLeast1() {
        val t = baseTemplate().copy(repeatCount = 0).normalized()
        assertEquals(1, t.repeatCount)
    }

    // ---- Name/ID ----

    @Test
    fun id_trimmed() {
        val t = baseTemplate().copy(id = "  id  ").normalized()
        assertEquals("id", t.id)
    }

    @Test
    fun name_trimmed() {
        val t = baseTemplate().copy(name = "  name  ").normalized()
        assertEquals("name", t.name)
    }

    @Test
    fun name_blankFallbackToDefault() {
        val t = baseTemplate().copy(name = "").normalized()
        assertEquals("Шаблон", t.name)
    }

    @Test
    fun name_truncatedTo60() {
        val t = baseTemplate().copy(name = "A".repeat(100)).normalized()
        assertEquals(60, t.name.length)
    }

    @Test
    fun filePath_trimmed() {
        val t = baseTemplate().copy(filePath = "  /data/file.png  ").normalized()
        assertEquals("/data/file.png", t.filePath)
    }

    // ---- Valid region after normalization ----

    @Test
    fun region_alwaysValid_afterNormalization() {
        val t = baseTemplate().copy(
            regionLeft = -1f,
            regionTop = -1f,
            regionRight = 0f,
            regionBottom = 0f,
        ).normalized()

        assertTrue(t.regionLeft >= 0f)
        assertTrue(t.regionTop >= 0f)
        assertTrue(t.regionRight > t.regionLeft)
        assertTrue(t.regionBottom > t.regionTop)
        assertTrue(t.regionRight <= 1f)
        assertTrue(t.regionBottom <= 1f)
    }
}
