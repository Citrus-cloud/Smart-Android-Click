package com.clickflow.android.capture

/**
 * Framework-free state machine for selecting a single [CaptureRegion] over a
 * captured frame (Step 67).
 *
 * Mirrors the proven Step 66 pattern: pure logic, fully unit-tested, no Android
 * imports. The UI (Compose) drives it from drag gestures; a future matching step
 * (Step 69+) reads [RegionSelectorState.region] to know where to look.
 *
 * SAFETY / PRIVACY: this controller only manipulates geometry. It captures
 * nothing, reads no pixels, persists nothing, and performs no analysis.
 */
enum class SelectionPhase { EMPTY, DRAGGING, SELECTED }

data class RegionSelectorState(
    val phase: SelectionPhase = SelectionPhase.EMPTY,
    val region: CaptureRegion? = null,
    val error: String? = null,
) {
    val hasRegion: Boolean get() = region != null && phase == SelectionPhase.SELECTED

    companion object {
        val EMPTY = RegionSelectorState()
    }
}

class RegionSelectorController(minDimension: Float = DEFAULT_MIN_DIMENSION) {

    private val minDim: Float = minDimension.coerceIn(0f, 1f)

    private var anchorX: Float = 0f
    private var anchorY: Float = 0f

    private var current = RegionSelectorState.EMPTY

    fun state(): RegionSelectorState = current

    /** Begins a drag selection at a normalized point. */
    @Synchronized
    fun beginSelection(x: Float, y: Float): RegionSelectorState {
        val px = x.coerceIn(0f, 1f)
        val py = y.coerceIn(0f, 1f)
        anchorX = px
        anchorY = py
        current = RegionSelectorState(
            phase = SelectionPhase.DRAGGING,
            region = CaptureRegion.fromCorners(px, py, px, py),
            error = null,
        )
        return current
    }

    /** Updates the in-progress drag to a new corner point. */
    @Synchronized
    fun updateSelection(x: Float, y: Float): RegionSelectorState {
        if (current.phase != SelectionPhase.DRAGGING) {
            current = current.copy(error = "no_active_selection")
            return current
        }
        current = current.copy(
            region = CaptureRegion.fromCorners(anchorX, anchorY, x, y),
            error = null,
        )
        return current
    }

    /**
     * Finalizes the current drag. Rejects a region whose width or height is below
     * [minDim], leaving the controller in DRAGGING so the user can keep dragging.
     */
    @Synchronized
    fun commitSelection(): RegionSelectorState {
        if (current.phase != SelectionPhase.DRAGGING) {
            current = current.copy(error = "no_active_selection")
            return current
        }
        val region = current.region
        if (region == null || !region.isValid || region.width < minDim || region.height < minDim) {
            current = current.copy(error = "region_too_small")
            return current
        }
        current = RegionSelectorState(phase = SelectionPhase.SELECTED, region = region, error = null)
        return current
    }

    /** Directly selects a region (e.g. a preset or the full frame). */
    @Synchronized
    fun setRegion(region: CaptureRegion): RegionSelectorState {
        val clamped = region.clampedToUnit()
        if (!clamped.isValid || clamped.width < minDim || clamped.height < minDim) {
            current = current.copy(error = "region_too_small")
            return current
        }
        current = RegionSelectorState(phase = SelectionPhase.SELECTED, region = clamped, error = null)
        return current
    }

    /** Clears any selection back to the empty state. */
    @Synchronized
    fun clear(): RegionSelectorState {
        anchorX = 0f
        anchorY = 0f
        current = RegionSelectorState.EMPTY
        return current
    }

    companion object {
        /** Minimum width/height (2% of the frame) for a committed region. */
        const val DEFAULT_MIN_DIMENSION = 0.02f
    }
}
