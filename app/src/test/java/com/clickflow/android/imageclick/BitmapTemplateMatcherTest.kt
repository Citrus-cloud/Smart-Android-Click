package com.clickflow.android.imageclick

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BitmapTemplateMatcherTest {

    // ---- sampleCount tests ----

    @Test
    fun sampleCount_smallDimension_returnsMin16() {
        assertEquals(16, sampleCount(50))
    }

    @Test
    fun sampleCount_mediumDimension() {
        assertEquals(20, sampleCount(100))
    }

    @Test
    fun sampleCount_largeDimension_returnsMax28() {
        assertEquals(28, sampleCount(200))
    }

    @Test
    fun sampleCount_verySmall_returnsMin16() {
        assertEquals(16, sampleCount(10))
    }

    @Test
    fun sampleCount_exactly80_returns16() {
        assertEquals(16, sampleCount(80))
    }

    @Test
    fun sampleCount_exactly140_returns28() {
        assertEquals(28, sampleCount(140))
    }

    // ---- buildScales tests ----

    @Test
    fun buildScales_singleScale() {
        val scales = buildScalesViaReflection(1.0f, 1.0f)
        assertEquals(1, scales.size)
        assertEquals(1.0f, scales[0], 0.001f)
    }

    @Test
    fun buildScales_tinyRange_returnsSingleScale() {
        val scales = buildScalesViaReflection(0.9f, 0.91f)
        assertEquals(1, scales.size)
    }

    @Test
    fun buildScales_normalRange() {
        val scales = buildScalesViaReflection(0.8f, 1.2f)
        assertTrue(scales.size >= 3)
        assertTrue(scales.first() >= 0.8f)
        assertTrue(scales.last() <= 1.2f + 0.01f)
    }

    @Test
    fun buildScales_includes1fIfInRange() {
        val scales = buildScalesViaReflection(0.8f, 1.2f)
        assertTrue(scales.any { kotlin.math.abs(it - 1.0f) < 0.03f })
    }

    @Test
    fun buildScales_sorted() {
        val scales = buildScalesViaReflection(0.7f, 1.3f)
        for (i in 1 until scales.size) {
            assertTrue(scales[i] >= scales[i - 1])
        }
    }

    @Test
    fun buildScales_noDuplicates() {
        val scales = buildScalesViaReflection(0.8f, 1.2f)
        val distinct = scales.distinctBy { (it * 100).toInt() }
        assertEquals(distinct.size, scales.size)
    }

    // ---- pixelSimilarity tests ----

    @Test
    fun pixelSimilarity_identicalPixels() {
        val result = pixelSimilarityViaReflection(0xFF000000.toInt(), 0xFF000000.toInt())
        assertEquals(1.0f, result, 0.001f)
    }

    @Test
    fun pixelSimilarity_whiteVsBlack() {
        val result = pixelSimilarityViaReflection(0xFFFFFFFF.toInt(), 0xFF000000.toInt())
        assertEquals(0.0f, result, 0.001f)
    }

    @Test
    fun pixelSimilarity_similarPixels() {
        val a = 0xFF808080.toInt()
        val b = 0xFF818181.toInt()
        val result = pixelSimilarityViaReflection(a, b)
        assertTrue(result > 0.9f)
    }

    // ---- Reflection helpers ----

    private fun sampleCount(dimension: Int): Int {
        val method = BitmapTemplateMatcher::class.java.getDeclaredMethod(
            "sampleCount", Int::class.javaPrimitiveType
        )
        method.isAccessible = true
        return method.invoke(BitmapTemplateMatcher, dimension) as Int
    }

    private fun buildScalesViaReflection(minScale: Float, maxScale: Float): List<Float> {
        val method = BitmapTemplateMatcher::class.java.getDeclaredMethod(
            "buildScales", Float::class.javaPrimitiveType, Float::class.javaPrimitiveType
        )
        method.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        return method.invoke(BitmapTemplateMatcher, minScale, maxScale) as List<Float>
    }

    private fun pixelSimilarityViaReflection(a: Int, b: Int): Float {
        val method = BitmapTemplateMatcher::class.java.getDeclaredMethod(
            "pixelSimilarity", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType
        )
        method.isAccessible = true
        return method.invoke(BitmapTemplateMatcher, a, b) as Float
    }
}
