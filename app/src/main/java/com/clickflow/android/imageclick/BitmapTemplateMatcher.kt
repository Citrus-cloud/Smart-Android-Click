package com.clickflow.android.imageclick

import android.graphics.Bitmap
import kotlin.math.abs
import kotlin.math.max

object BitmapTemplateMatcher {
    data class Match(val x: Int, val y: Int, val confidence: Float)

    fun findBest(screen: Bitmap, template: Bitmap, threshold: Float): Match? =
        findBest(screen, template, threshold, 0, 0, screen.width, screen.height)

    fun findBest(
        screen: Bitmap,
        template: Bitmap,
        threshold: Float,
        regionLeftPx: Int,
        regionTopPx: Int,
        regionRightPx: Int,
        regionBottomPx: Int,
    ): Match? {
        if (screen.width <= 0 || screen.height <= 0 || template.width <= 0 || template.height <= 0) return null
        if (template.width > screen.width || template.height > screen.height) return null

        val left = regionLeftPx.coerceIn(0, screen.width - 1)
        val top = regionTopPx.coerceIn(0, screen.height - 1)
        val right = regionRightPx.coerceIn(left + 1, screen.width)
        val bottom = regionBottomPx.coerceIn(top + 1, screen.height)
        if (right - left < template.width || bottom - top < template.height) return null

        val stride = max(5, minOf(template.width, template.height) / 12)
        val sampleX = 8
        val sampleY = 8
        var best: Match? = null

        var y = top
        while (y <= bottom - template.height) {
            var x = left
            while (x <= right - template.width) {
                val score = scoreAt(screen, template, x, y, sampleX, sampleY)
                if (best == null || score > best.confidence) best = Match(x, y, score)
                x += stride
            }
            y += stride
        }
        return best?.takeIf { it.confidence >= threshold }
    }

    private fun scoreAt(screen: Bitmap, template: Bitmap, originX: Int, originY: Int, sampleX: Int, sampleY: Int): Float {
        var total = 0f
        var count = 0
        for (sy in 0 until sampleY) {
            val ty = ((sy + 0.5f) / sampleY * template.height).toInt().coerceIn(0, template.height - 1)
            val py = (originY + ty).coerceIn(0, screen.height - 1)
            for (sx in 0 until sampleX) {
                val tx = ((sx + 0.5f) / sampleX * template.width).toInt().coerceIn(0, template.width - 1)
                val px = (originX + tx).coerceIn(0, screen.width - 1)
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
