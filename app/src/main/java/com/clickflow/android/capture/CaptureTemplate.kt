package com.clickflow.android.capture

/**
 * Metadata describing a target reference ("template") that the matching engine
 * (Step 69+) will later look for inside a captured frame.
 *
 * IMPORTANT (Phase 2 invariant): this is geometry + metadata ONLY. It stores no
 * pixels, no image bytes, and is never persisted to disk in this step. The actual
 * reference bitmap and on-device matching arrive in later steps; here we only model
 * "what counts as a template" so the manager, scenarios, and UI can refer to one by id.
 *
 * @param id            stable id assigned by [TemplateManager] (never blank).
 * @param name          human-readable label (trimmed, unique within a manager).
 * @param region        normalized area on the frame the template was taken from.
 * @param widthPx        reference width in pixels (metadata only; no pixels stored).
 * @param heightPx       reference height in pixels (metadata only; no pixels stored).
 * @param matchThreshold confidence in [MIN_THRESHOLD, MAX_THRESHOLD] a later match must reach.
 * @param createdAtMs    creation timestamp (injected, so logic stays testable).
 */
data class CaptureTemplate(
	val id: String,
	val name: String,
	val region: CaptureRegion,
	val widthPx: Int,
	val heightPx: Int,
	val matchThreshold: Float = DEFAULT_THRESHOLD,
	val createdAtMs: Long = 0L,
) {
	/** A template is usable only when its identity, label, region and size are sane. */
	val isValid: Boolean
		get() = id.isNotBlank() &&
			name.isNotBlank() &&
			region.isValid &&
			widthPx > 0 &&
			heightPx > 0 &&
			matchThreshold in MIN_THRESHOLD..MAX_THRESHOLD

	/** Width / height of the reference, or 0 when height is unknown. */
	val aspectRatio: Float
		get() = if (heightPx == 0) 0f else widthPx.toFloat() / heightPx.toFloat()

	fun withName(newName: String): CaptureTemplate = copy(name = newName)

	fun withThreshold(newThreshold: Float): CaptureTemplate =
		copy(matchThreshold = newThreshold.coerceIn(MIN_THRESHOLD, MAX_THRESHOLD))

	fun withRegion(newRegion: CaptureRegion): CaptureTemplate = copy(region = newRegion)

	companion object {
		const val DEFAULT_THRESHOLD = 0.8f
		const val MIN_THRESHOLD = 0.1f
		const val MAX_THRESHOLD = 1.0f
		const val MAX_NAME_LENGTH = 60
	}
}
