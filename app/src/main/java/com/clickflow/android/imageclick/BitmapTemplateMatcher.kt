package com.clickflow.android.imageclick

import android.graphics.Bitmap
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

object BitmapTemplateMatcher {
    data class Match(
        val x: Int,
        val y: Int,
        val width: Int,
        val height: Int,
        val scale: Float,
        val confidence: Float,
    )

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
        scaleMin: Float = 0.85f,
        scaleMax: Float = 1.15f,
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
            s += 0.10f
        }
        if (values.none { abs(it - 1f) < 0.03f } && 1f in minScale..maxScale) values.add(1f)
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
        val stride = max(5, minOf(scaledWidth, scaledHeight) / 12)
        val sampleX = 8
        val sampleY = 8
        var best: Match? = null

        var y = top
        while (y <= bottom - scaledHeight) {
            var x = left
            while (x <= right - scaledWidth) {
                val score = scoreAt(screen, template, x, y, scaledWidth, scaledHeight, sampleX, sampleY)
                if (best == null || score > best.confidence) best = Match(x, y, scaledWidth, scaledHeight, scale, score)
                x += stride
            }
            y += stride
        }
        return best
    }

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
        return 1f - (diff / 765f)
    }
}
