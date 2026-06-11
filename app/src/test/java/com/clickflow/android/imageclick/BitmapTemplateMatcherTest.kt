package com.clickflow.android.imageclick

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

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
        assertTrue(scales.any { abs(it - 1.0f) < 0.03f })
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

    // ---- channelCorrelation tests ----

    @Test
    fun channelCorrelation_identicalPatches_highCorrelation() {
        // S = [1,2,3,4,5,6,7,8,9,10], T = same
        // sumS=55, sumT=55, sumSS=385, sumTT=385, sumST=385, nf=10
        // varS = 385 - 55*55/10 = 82.5
        // correlation = (385 - 55*55/10) / sqrt(82.5*82.5) = 1.0
        val result = channelCorrelationViaReflection(
            sumS = 55f, sumT = 55f,
            sumSS = 385f, sumTT = 385f,
            sumST = 385f, nf = 10f
        )
        assertTrue(result != null)
        assertTrue(result!! > 0.99f)
    }

    @Test
    fun channelCorrelation_flatPatch_returnsNull() {
        // Zero variance = flat patch
        val result = channelCorrelationViaReflection(
            sumS = 0f, sumT = 0f,
            sumSS = 0f, sumTT = 0f,
            sumST = 0f, nf = 10f
        )
        assertNull(result)
    }

    @Test
    fun channelCorrelation_lowVariance_returnsNull() {
        // Variance <= 1 is treated as flat
        val result = channelCorrelationViaReflection(
            sumS = 10f, sumT = 10f,
            sumSS = 10.5f, sumTT = 10.5f,
            sumST = 10f, nf = 10f
        )
        assertNull(result)
    }

    @Test
    fun channelCorrelation_correlatedPatches_highScore() {
        // S = [10,20,30,40,50], T = [10,20,30,40,50]
        // sumS=150, sumT=150, sumSS=5500, sumTT=5500, sumST=5500, nf=5
        // varS = 5500 - 150*150/5 = 5500 - 4500 = 1000
        // correlation = 1000 / sqrt(1000*1000) = 1.0
        val result = channelCorrelationViaReflection(
            sumS = 150f, sumT = 150f,
            sumSS = 5500f, sumTT = 5500f,
            sumST = 5500f, nf = 5f
        )
        assertTrue(result != null)
        assertTrue(result!! > 0.99f)
    }

    @Test
    fun channelCorrelation_uncorrelatedPatches_lowScore() {
        // S = [1,2,3,4,5], T = [5,4,3,2,1] (anti-correlated)
        // sumS=15, sumT=15, sumSS=55, sumTT=55, sumST=35, nf=5
        // varS = 55 - 15*15/5 = 55 - 45 = 10
        // covariance = 35 - 15*15/5 = 35 - 45 = -10
        // correlation = -10 / sqrt(10*10) = -1.0
        val result = channelCorrelationViaReflection(
            sumS = 15f, sumT = 15f,
            sumSS = 55f, sumTT = 55f,
            sumST = 35f, nf = 5f
        )
        assertTrue(result != null)
        assertTrue(result!! < -0.9f)
    }

    @Test
    fun channelCorrelation_positiveCorrelation() {
        // S = [1,2,3,4,5], T = [2,4,6,8,10] (perfect positive, T = 2*S)
        // sumS=15, sumT=30, sumSS=55, sumTT=220, sumST=220, nf=5
        // varS = 55 - 15*15/5 = 10
        // varT = 220 - 30*30/5 = 40
        // covariance = 220 - 15*30/5 = 220 - 90 = 130
        // correlation = 130 / sqrt(10*40) = 130 / 20 = 0.645... wait let me recalculate
        // Actually: sqrt(10*40) = sqrt(400) = 20
        // correlation = 130 / 20 = 6.5? That can't be right...
        // Let me recalculate: sumST = 1*2 + 2*4 + 3*6 + 4*8 + 5*10 = 2+8+18+32+50 = 110
        // covariance = 110 - 15*30/5 = 110 - 90 = 20
        // correlation = 20 / sqrt(10*40) = 20 / 20 = 1.0
        val result = channelCorrelationViaReflection(
            sumS = 15f, sumT = 30f,
            sumSS = 55f, sumTT = 220f,
            sumST = 110f, nf = 5f
        )
        assertTrue(result != null)
        assertTrue(result!! > 0.99f)
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

    private fun channelCorrelationViaReflection(
        sumS: Float, sumT: Float,
        sumSS: Float, sumTT: Float,
        sumST: Float, nf: Float,
    ): Float? {
        val method = BitmapTemplateMatcher::class.java.getDeclaredMethod(
            "channelCorrelation",
            Float::class.javaPrimitiveType, Float::class.javaPrimitiveType,
            Float::class.javaPrimitiveType, Float::class.javaPrimitiveType,
            Float::class.javaPrimitiveType, Float::class.javaPrimitiveType,
        )
        method.isAccessible = true
        return method.invoke(BitmapTemplateMatcher, sumS, sumT, sumSS, sumTT, sumST, nf) as? Float
    }
}
