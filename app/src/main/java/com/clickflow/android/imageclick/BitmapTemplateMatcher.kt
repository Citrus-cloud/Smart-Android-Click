package com.clickflow.android.imageclick

import android.graphics.Bitmap
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Locates an image template inside a screenshot.
 *
 * Strategy: for each candidate scale a light "coarse" grid scan locates the most
 * promising position, then a dense stride-1 "refine" pass around that position
 * pinpoints the best match. This is more accurate and more robust to small
 * offsets than a single coarse pass, while keeping per-frame cost reasonable.
 *
 * The public contract (Match and both findBest signatures) is unchanged.
 */
object BitmapTemplateMatcher {
    private const val SCALE_STEP = 0.05f
    private const val COARSE_SAMPLES = 10
    private const val MAX_COLOR_DISTANCE = 3f * 255f

    data class Match(
        val x: Int,
        val y: Int,
        val width: Int,
        val height: Int,
        val scale: Float,
        val confidence: Float,
    )

    fun findBest(screen: Bitmap, template: Bitmap, threshold: Float): Match? =
        findBest(screen, template, threshold, 0, 0, screen.width, screen.height, 0.8f, 1.2f)

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
    ): Match? {
        if (screen.width <= 0 || screen.height <= 0 || template.width <= 0 || template.height <= 0) return null

        val left = regionLeftPx.coerceIn(0, screen.width - 1)
        val top = regionTopPx.coerceIn(0, screen.height - 1)
        val right = regionRightPx.coerceIn(left + 1, screen.width)
        val bottom = regionBottomPx.coerceIn(top + 1, screen.height)
        val minScale = scaleMin.coerceIn(0.5f, 2f)
        val maxScale = scaleMax.coerceIn(minScale, 2f)
        val scales = buildScales(minScale, maxScale)

        var best: Match? = null
        for (scale in scales) {
            val scaledWidth = (template.width * scale).roundToInt().coerceAtLeast(1)
            val scaledHeight = (template.height * scale).roundToInt().coerceAtLeast(1)
            if (scaledWidth > right - left || scaledHeight > bottom - top) continue

            val candidate = findBestAtScale(
                screen = screen,
                template = template,
                left = left,
                top = top,
                right = right,
                bottom = bottom,
                scaledWidth = scaledWidth,
                scaledHeight = scaledHeight,
                scale = scale,
            )
            if (candidate != null && (best == null || candidate.confidence > best.confidence)) {
                best = candidate
            }
        }
        return best?.takeIf { it.confidence >= threshold }
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
        screen: Bitmap,
        template: Bitmap,
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
        var coarseScore = -1f
        var y = top
        while (y <= maxY) {
            var x = left
            while (x <= maxX) {
                val score = scoreAt(screen, template, x, y, scaledWidth, scaledHeight, COARSE_SAMPLES, COARSE_SAMPLES)
                if (score > coarseScore) {
                    coarseScore = score
                    coarseX = x
                    coarseY = y
                }
                x += coarseStride
            }
            y += coarseStride
        }
        if (coarseScore < 0f) return null

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
        var bestScore = scoreAt(screen, template, coarseX, coarseY, scaledWidth, scaledHeight, fineSamplesX, fineSamplesY)
        var ry = ryStart
        while (ry <= ryEnd) {
            var rx = rxStart
            while (rx <= rxEnd) {
                if (rx != coarseX || ry != coarseY) {
                    val score = scoreAt(screen, template, rx, ry, scaledWidth, scaledHeight, fineSamplesX, fineSamplesY)
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
        return Match(bestX, bestY, scaledWidth, scaledHeight, scale, bestScore)
    }

    /** Roughly one sample per 5px, clamped so small templates aren't oversampled. */
    private fun sampleCount(dimension: Int): Int = (dimension / 5).coerceIn(16, 28)

    private fun scoreAt(
        screen: Bitmap,
        template: Bitmap,
        originX: Int,
        originY: Int,
        scaledWidth: Int,
        scaledHeight: Int,
        sampleX: Int,
        sampleY: Int,
    ): Float {
        var total = 0f
        var count = 0
        for (sy in 0 until sampleY) {
            val scaledY = ((sy + 0.5f) / sampleY * scaledHeight).toInt().coerceIn(0, scaledHeight - 1)
            val ty = ((scaledY.toFloat() / scaledHeight) * template.height).toInt().coerceIn(0, template.height - 1)
            val py = (originY + scaledY).coerceIn(0, screen.height - 1)
            for (sx in 0 until sampleX) {
                val scaledX = ((sx + 0.5f) / sampleX * scaledWidth).toInt().coerceIn(0, scaledWidth - 1)
                val tx = ((scaledX.toFloat() / scaledWidth) * template.width).toInt().coerceIn(0, template.width - 1)
                val px = (originX + scaledX).coerceIn(0, screen.width - 1)
                total += pixelSimilarity(screen.getPixel(px, py), template.getPixel(tx, ty))
                count++
            }
        }
        return if (count == 0) 0f else total / count
    }

    private fun pixelSimilarity(a: Int, b: Int): Float {
        val ar = (a shr 16) and 0xFF
        val ag = (a shr 8) and 0xFF
        val ab = a and 0xFF
        val br = (b shr 16) and 0xFF
        val bg = (b shr 8) and 0xFF
        val bb = b and 0xFF
        val diff = abs(ar - br) + abs(ag - bg) + abs(ab - bb)
        return 1f - (diff / MAX_COLOR_DISTANCE)
    }
}
