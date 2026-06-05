package com.clickflow.android.capture

/**
 * Normalized rectangular region over a captured frame (Step 67).
 *
 * Coordinates are fractions in [0, 1] relative to the frame's width/height, so a
 * region is resolution-independent and can be reused across frames of different
 * pixel sizes. For a valid region, [left] < [right] and [top] < [bottom] hold.
 *
 * SAFETY / PRIVACY: a region is pure geometry. It references no pixels and is
 * never persisted by this step. It only describes WHERE a future matching step
 * (Step 69+) would look; it performs no capture and no analysis itself.
 */
data class CaptureRegion(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
    val area: Float get() = width * height
    val centerX: Float get() = (left + right) / 2f
    val centerY: Float get() = (top + bottom) / 2f

    /** True when the rectangle is well-formed and inside the unit square. */
    val isValid: Boolean
        get() = left in 0f..1f && top in 0f..1f &&
            right in 0f..1f && bottom in 0f..1f &&
            left < right && top < bottom

    /** True when the normalized point lies inside (inclusive) this region. */
    fun contains(x: Float, y: Float): Boolean =
        x in left..right && y in top..bottom

    /** Returns a copy with every edge clamped into [0, 1]. */
    fun clampedToUnit(): CaptureRegion = CaptureRegion(
        left = left.coerceIn(0f, 1f),
        top = top.coerceIn(0f, 1f),
        right = right.coerceIn(0f, 1f),
        bottom = bottom.coerceIn(0f, 1f),
    )

    /** Maps this normalized region onto a concrete pixel rectangle [l, t, r, b]. */
    fun toPixels(frameWidth: Int, frameHeight: Int): IntArray = intArrayOf(
        (left * frameWidth).toInt(),
        (top * frameHeight).toInt(),
        (right * frameWidth).toInt(),
        (bottom * frameHeight).toInt(),
    )

    companion object {
        /** The whole frame. */
        val FULL = CaptureRegion(0f, 0f, 1f, 1f)

        /**
         * Builds a normalized region from two arbitrary corner points, ordering
         * the edges (so drags in any direction work) and clamping into [0, 1].
         */
        fun fromCorners(ax: Float, ay: Float, bx: Float, by: Float): CaptureRegion =
            CaptureRegion(
                left = minOf(ax, bx),
                top = minOf(ay, by),
                right = maxOf(ax, bx),
                bottom = maxOf(ay, by),
            ).clampedToUnit()
    }
}
