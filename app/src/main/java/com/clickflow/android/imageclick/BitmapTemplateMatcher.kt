package com.clickflow.android.imageclick

import android.graphics.Bitmap
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Locates an image template inside a screenshot.
 *
 * Matching uses zero-normalized cross-correlation (ZNCC) over a sampled grid,
 * computed PER COLOR CHANNEL (red, green, blue) and averaged. ZNCC measures how
 * well the STRUCTURE of the template matches the screen, independent of overall
 * brightness, and — crucially — it does not reward flat/empty regions. Using the
 * three color channels instead of luminance alone matters a lot for app icons and
 * buttons, which are strongly colored: a luminance-only score throws away most of
 * what makes an icon recognizable and reads low even on a real match.
 *
 * Confidence is the averaged correlation clamped to [0, 1]: a strong real match
 * reads about 0.85-0.98, an unrelated/empty area reads near 0. That keeps the saved
 * 0..1 thresholds intuitive (e.g. 0.8 means "80% correlation").
 *
 * A patch with no structure in ANY channel (uniform/flat area) is treated as "no
 * match" (-1), which stops blank screen areas from being reported as matches.
 *
 * Strategy: for each candidate scale a light "coarse" grid scan locates the most
 * promising position, then a dense stride-1 "refine" pass around that position
 * pinpoints the best match.
 *
 * Performance: both the screenshot and the template are read into plain int
 * arrays once via Bitmap.getPixels. Per-sample lookups then index those arrays
 * instead of calling Bitmap.getPixel (a JNI hop) millions of times per frame.
 */
object BitmapTemplateMatcher {
    private const val SCALE_STEP = 0.05f
    private const val COARSE_SAMPLES = 10

    data class Match(
        val x: Int,
        val y: Int,
        val width: Int,
        val height: Int,
        val scale: Float,
        val confidence: Float,
    )

    /** Best match at or above [threshold], or null. */
    fun findBest(screen: Bitmap, template: Bitmap, threshold: Float): Match? =
        findBest(screen, template, threshold, 0, 0, screen.width, screen.height, 1f, 1f)

    fun findBest(
        screen: Bitmap,
        template: Bitmap,
        threshold: Float,
        regionLeftPx: Int,
        regionTopPx: Int,
        regionRightPx: Int,
        regionBottomPx: Int,
        scaleMin: Float = 0.8f,
        scaleMax: Float = 1.2f,
    ): Match? = findBestMatch(
        screen, template, regionLeftPx, regionTopPx, regionRightPx, regionBottomPx, scaleMin, scaleMax,
    )?.takeIf { it.confidence >= threshold }

    /**
     * Returns the single best-scoring match regardless of threshold (or null only when the
     * inputs are unusable). Callers that want to show a confidence read-out — even for a
     * "no match" — use this and compare against the threshold themselves.
     */
    fun findBestMatch(
        screen: Bitmap,
        template: Bitmap,
        regionLeftPx: Int,
        regionTopPx: Int,
        regionRightPx: Int,
        regionBottomPx: Int,
        scaleMin: Float = 0.8f,
        scaleMax: Float = 1.2f,
    ): Match? {
        val screenW = screen.width
        val screenH = screen.height
        val templateW = template.width
        val templateH = template.height
        if (screenW <= 0 || screenH <= 0 || templateW <= 0 || templateH <= 0) return null

        val screenPixels = IntArray(screenW * screenH)
        screen.getPixels(screenPixels, 0, screenW, 0, 0, screenW, screenH)
        val templatePixels = IntArray(templateW * templateH)
        template.getPixels(templatePixels, 0, templateW, 0, 0, templateW, templateH)

        val left = regionLeftPx.coerceIn(0, screenW - 1)
        val top = regionTopPx.coerceIn(0, screenH - 1)
        val right = regionRightPx.coerceIn(left + 1, screenW)
        val bottom = regionBottomPx.coerceIn(top + 1, screenH)
        val minScale = scaleMin.coerceIn(0.5f, 2f)
        val maxScale = scaleMax.coerceIn(minScale, 2f)
        val scales = buildScales(minScale, maxScale)

        var best: Match? = null
        for (scale in scales) {
            val scaledWidth = (templateW * scale).roundToInt().coerceAtLeast(1)
            val scaledHeight = (templateH * scale).roundToInt().coerceAtLeast(1)
            if (scaledWidth > right - left || scaledHeight > bottom - top) continue

            val candidate = findBestAtScale(
                screenPixels, screenW, screenH,
                templatePixels, templateW, templateH,
                left, top, right, bottom,
                scaledWidth, scaledHeight, scale,
            )
            if (candidate != null && (best == null || candidate.confidence > best.confidence)) {
                best = candidate
            }
        }
        return best
    }

    private fun buildScales(minScale: Float, maxScale: Float): List<Float> {
        if (maxScale - minScale < 0.02f) return listOf(minScale)
        val values = mutableListOf<Float>()
        var s = minScale
        while (s <= maxScale + 0.001f) {
            values.add(s)
            s += SCALE_STEP
        }
        if (1f in minScale..maxScale && values.none { abs(it - 1f) < SCALE_STEP / 2f }) values.add(1f)
        return values.distinctBy { (it * 100).roundToInt() }.sorted()
    }

    private fun findBestAtScale(
        screenPixels: IntArray,
        screenW: Int,
        screenH: Int,
        templatePixels: IntArray,
        templateW: Int,
        templateH: Int,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
        scaledWidth: Int,
        scaledHeight: Int,
        scale: Float,
    ): Match? {
        val maxX = right - scaledWidth
        val maxY = bottom - scaledHeight
        if (maxX < left || maxY < top) return null

        val coarseStride = max(4, min(scaledWidth, scaledHeight) / 10)

        // Phase 1: coarse scan with a light sample grid to locate the best region.
        var coarseX = left
        var coarseY = top
        var coarseScore = -2f
        var y = top
        while (y <= maxY) {
            var x = left
            while (x <= maxX) {
                val score = scoreAt(screenPixels, screenW, screenH, templatePixels, templateW, templateH, x, y, scaledWidth, scaledHeight, COARSE_SAMPLES, COARSE_SAMPLES)
                if (score > coarseScore) {
                    coarseScore = score
                    coarseX = x
                    coarseY = y
                }
                x += coarseStride
            }
            y += coarseStride
        }
        if (coarseScore <= -2f) return null

        // Phase 2: dense stride-1 refine around the coarse winner.
        val refineRadius = coarseStride.coerceAtMost(8)
        val fineSamplesX = sampleCount(scaledWidth)
        val fineSamplesY = sampleCount(scaledHeight)
        val rxStart = (coarseX - refineRadius).coerceIn(left, maxX)
        val rxEnd = (coarseX + refineRadius).coerceIn(left, maxX)
        val ryStart = (coarseY - refineRadius).coerceIn(top, maxY)
        val ryEnd = (coarseY + refineRadius).coerceIn(top, maxY)

        var bestX = coarseX
        var bestY = coarseY
        var bestScore = scoreAt(screenPixels, screenW, screenH, templatePixels, templateW, templateH, coarseX, coarseY, scaledWidth, scaledHeight, fineSamplesX, fineSamplesY)
        var ry = ryStart
        while (ry <= ryEnd) {
            var rx = rxStart
            while (rx <= rxEnd) {
                if (rx != coarseX || ry != coarseY) {
                    val score = scoreAt(screenPixels, screenW, screenH, templatePixels, templateW, templateH, rx, ry, scaledWidth, scaledHeight, fineSamplesX, fineSamplesY)
                    if (score > bestScore) {
                        bestScore = score
                        bestX = rx
                        bestY = ry
                    }
                }
                rx++
            }
            ry++
        }
        // ZNCC is in [-1, 1]. Negative/zero correlation means "not a match", so clamp to [0, 1]
        // and use the correlation directly as the confidence. This keeps the numbers intuitive:
        // a strong match reads ~0.85-0.98, while unrelated/empty screen areas read near 0.
        val confidence = bestScore.coerceIn(0f, 1f)
        return Match(bestX, bestY, scaledWidth, scaledHeight, scale, confidence)
    }

    /** Roughly one sample per 5px, clamped so small templates aren't oversampled. */
    private fun sampleCount(dimension: Int): Int = (dimension / 5).coerceIn(16, 28)

    /**
     * Per-channel zero-normalized cross-correlation over a sampled grid, averaged across the
     * R/G/B channels that actually carry structure, in [-1, 1]. Returns -1 (no correlation)
     * when every channel of either the template patch or the screen patch is essentially flat,
     * which is what stops blank/uniform screen areas from being reported as matches.
     */
    private fun scoreAt(
        screenPixels: IntArray,
        screenW: Int,
        screenH: Int,
        templatePixels: IntArray,
        templateW: Int,
        templateH: Int,
        originX: Int,
        originY: Int,
        scaledWidth: Int,
        scaledHeight: Int,
        sampleX: Int,
        sampleY: Int,
    ): Float {
        var sumSr = 0f; var sumTr = 0f; var sumSSr = 0f; var sumTTr = 0f; var sumSTr = 0f
        var sumSg = 0f; var sumTg = 0f; var sumSSg = 0f; var sumTTg = 0f; var sumSTg = 0f
        var sumSb = 0f; var sumTb = 0f; var sumSSb = 0f; var sumTTb = 0f; var sumSTb = 0f
        var n = 0
        for (sy in 0 until sampleY) {
            val scaledY = ((sy + 0.5f) / sampleY * scaledHeight).toInt().coerceIn(0, scaledHeight - 1)
            val ty = ((scaledY.toFloat() / scaledHeight) * templateH).toInt().coerceIn(0, templateH - 1)
            val py = (originY + scaledY).coerceIn(0, screenH - 1)
            for (sx in 0 until sampleX) {
                val scaledX = ((sx + 0.5f) / sampleX * scaledWidth).toInt().coerceIn(0, scaledWidth - 1)
                val tx = ((scaledX.toFloat() / scaledWidth) * templateW).toInt().coerceIn(0, templateW - 1)
                val px = (originX + scaledX).coerceIn(0, screenW - 1)
                val sp = screenPixels[py * screenW + px]
                val tp = templatePixels[ty * templateW + tx]
                val sr = ((sp shr 16) and 0xFF).toFloat()
                val sg = ((sp shr 8) and 0xFF).toFloat()
                val sb = (sp and 0xFF).toFloat()
                val tr = ((tp shr 16) and 0xFF).toFloat()
                val tg = ((tp shr 8) and 0xFF).toFloat()
                val tb = (tp and 0xFF).toFloat()
                sumSr += sr; sumTr += tr; sumSSr += sr * sr; sumTTr += tr * tr; sumSTr += sr * tr
                sumSg += sg; sumTg += tg; sumSSg += sg * sg; sumTTg += tg * tg; sumSTg += sg * tg
                sumSb += sb; sumTb += tb; sumSSb += sb * sb; sumTTb += tb * tb; sumSTb += sb * tb
                n++
            }
        }
        if (n == 0) return -1f
        val nf = n.toFloat()
        var sum = 0f
        var channels = 0
        channelCorrelation(sumSr, sumTr, sumSSr, sumTTr, sumSTr, nf)?.let { sum += it; channels++ }
        channelCorrelation(sumSg, sumTg, sumSSg, sumTTg, sumSTg, nf)?.let { sum += it; channels++ }
        channelCorrelation(sumSb, sumTb, sumSSb, sumTTb, sumSTb, nf)?.let { sum += it; channels++ }
        // No channel had structure in both patches → flat/uniform area → not a match.
        if (channels == 0) return -1f
        return (sum / channels).coerceIn(-1f, 1f)
    }

    /**
     * ZNCC for a single channel, or null when either patch is essentially flat in this channel
     * (no structure to correlate). Variance threshold of 1f ignores tiny sensor/compression noise.
     */
    private fun channelCorrelation(
        sumS: Float,
        sumT: Float,
        sumSS: Float,
        sumTT: Float,
        sumST: Float,
        nf: Float,
    ): Float? {
        val varS = sumSS - sumS * sumS / nf
        val varT = sumTT - sumT * sumT / nf
        if (varS <= 1f || varT <= 1f) return null
        val denom = sqrt(varS * varT)
        if (denom <= 0f) return null
        return ((sumST - sumS * sumT / nf) / denom).coerceIn(-1f, 1f)
    }
}
