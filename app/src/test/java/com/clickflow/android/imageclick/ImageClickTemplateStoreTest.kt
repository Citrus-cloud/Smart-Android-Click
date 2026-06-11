package com.clickflow.android.imageclick

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ImageClickTemplateStoreTest {

    @Test
    fun template_defaultValues() {
        val t = ImageClickTemplate(
            id = "t1", name = "Test", filePath = "/data/test.png",
            width = 100, height = 50,
        )
        assertEquals(0.82f, t.threshold, 0.001f)
        assertEquals(0.5f, t.tapX, 0.001f)
        assertEquals(0.5f, t.tapY, 0.001f)
        assertEquals(0f, t.regionLeft, 0.001f)
        assertEquals(0f, t.regionTop, 0.001f)
        assertEquals(1f, t.regionRight, 0.001f)
        assertEquals(1f, t.regionBottom, 0.001f)
        assertFalse(t.continuous)
        assertEquals(0.8f, t.scaleMin, 0.001f)
        assertEquals(1.2f, t.scaleMax, 0.001f)
        assertEquals(1100L, t.intervalMs)
        assertEquals(50, t.repeatCount)
        assertFalse(t.infinite)
    }

    @Test
    fun template_copyPreservesFields() {
        val t = ImageClickTemplate(
            id = "t1", name = "Original", filePath = "/data/test.png",
            width = 100, height = 50, threshold = 0.9f,
        )
        val modified = t.copy(name = "Modified")
        assertEquals("t1", modified.id)
        assertEquals("Modified", modified.name)
        assertEquals(100, modified.width)
        assertEquals(50, modified.height)
        assertEquals(0.9f, modified.threshold, 0.001f)
    }

    @Test
    fun normalized_id_trimmed() {
        val t = ImageClickTemplate(
            id = "  id  ", name = "Test", filePath = "/data/test.png",
            width = 100, height = 100,
        ).normalized()
        assertEquals("id", t.id)
    }

    @Test
    fun normalized_name_trimmed() {
        val t = ImageClickTemplate(
            id = "t1", name = "  name  ", filePath = "/data/test.png",
            width = 100, height = 100,
        ).normalized()
        assertEquals("name", t.name)
    }

    @Test
    fun normalized_name_blankFallback() {
        val t = ImageClickTemplate(
            id = "t1", name = "", filePath = "/data/test.png",
            width = 100, height = 100,
        ).normalized()
        assertEquals("Шаблон", t.name)
    }

    @Test
    fun normalized_name_truncated() {
        val t = ImageClickTemplate(
            id = "t1", name = "A".repeat(100), filePath = "/data/test.png",
            width = 100, height = 100,
        ).normalized()
        assertEquals(60, t.name.length)
    }

    @Test
    fun normalized_filePath_trimmed() {
        val t = ImageClickTemplate(
            id = "t1", name = "Test", filePath = "  /data/test.png  ",
            width = 100, height = 100,
        ).normalized()
        assertEquals("/data/test.png", t.filePath)
    }

    @Test
    fun normalized_width_atLeast1() {
        val t = ImageClickTemplate(
            id = "t1", name = "Test", filePath = "/data/test.png",
            width = 0, height = -5,
        ).normalized()
        assertEquals(1, t.width)
        assertEquals(1, t.height)
    }

    @Test
    fun normalized_threshold_coercedToRange() {
        val low = ImageClickTemplate(
            id = "t1", name = "Test", filePath = "/data/test.png",
            width = 100, height = 100, threshold = 0.1f,
        ).normalized()
        assertEquals(0.5f, low.threshold, 0.001f)

        val high = ImageClickTemplate(
            id = "t1", name = "Test", filePath = "/data/test.png",
            width = 100, height = 100, threshold = 1.5f,
        ).normalized()
        assertEquals(0.99f, high.threshold, 0.001f)
    }

    @Test
    fun normalized_tapXY_coerced() {
        val t = ImageClickTemplate(
            id = "t1", name = "Test", filePath = "/data/test.png",
            width = 100, height = 100, tapX = -0.5f, tapY = 2.0f,
        ).normalized()
        assertEquals(0f, t.tapX, 0.001f)
        assertEquals(1f, t.tapY, 0.001f)
    }

    @Test
    fun normalized_region_coerced() {
        val t = ImageClickTemplate(
            id = "t1", name = "Test", filePath = "/data/test.png",
            width = 100, height = 100,
            regionLeft = -0.5f, regionTop = -0.5f,
            regionRight = 2.0f, regionBottom = 2.0f,
        ).normalized()
        assertEquals(0f, t.regionLeft, 0.001f)
        assertEquals(0f, t.regionTop, 0.001f)
        assertEquals(1f, t.regionRight, 0.001f)
        assertEquals(1f, t.regionBottom, 0.001f)
    }

    @Test
    fun normalized_scale_coerced() {
        val t = ImageClickTemplate(
            id = "t1", name = "Test", filePath = "/data/test.png",
            width = 100, height = 100,
            scaleMin = 0.1f, scaleMax = 5.0f,
        ).normalized()
        assertEquals(0.5f, t.scaleMin, 0.001f)
        assertEquals(2.0f, t.scaleMax, 0.001f)
    }

    @Test
    fun normalized_scaleMax_atLeastScaleMin() {
        val t = ImageClickTemplate(
            id = "t1", name = "Test", filePath = "/data/test.png",
            width = 100, height = 100,
            scaleMin = 1.5f, scaleMax = 0.8f,
        ).normalized()
        assertEquals(1.5f, t.scaleMax, 0.001f)
    }

    @Test
    fun normalized_intervalMs_coerced() {
        val t = ImageClickTemplate(
            id = "t1", name = "Test", filePath = "/data/test.png",
            width = 100, height = 100, intervalMs = 50L,
        ).normalized()
        assertEquals(300L, t.intervalMs)
    }

    @Test
    fun normalized_repeatCount_coerced() {
        val t = ImageClickTemplate(
            id = "t1", name = "Test", filePath = "/data/test.png",
            width = 100, height = 100, repeatCount = 0,
        ).normalized()
        assertEquals(1, t.repeatCount)
    }

    @Test
    fun normalized_regionValid_afterNormalization() {
        val t = ImageClickTemplate(
            id = "t1", name = "Test", filePath = "/data/test.png",
            width = 100, height = 100,
            regionLeft = -1f, regionTop = -1f,
            regionRight = 0f, regionBottom = 0f,
        ).normalized()
        assertTrue(t.regionLeft >= 0f)
        assertTrue(t.regionTop >= 0f)
        assertTrue(t.regionRight > t.regionLeft)
        assertTrue(t.regionBottom > t.regionTop)
    }
}
