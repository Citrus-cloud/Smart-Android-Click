package com.clickflow.android.capture

/**
 * In-memory registry of [CaptureTemplate]s.
 *
 * Pure Kotlin: no Android imports, no disk, no capture, no analysis. It only tracks
 * "which templates exist and what are their parameters" so later steps (matching
 * engine, scenarios, UI) can refer to them by a stable id. Thread-safe via
 * [Synchronized]; ids are sequential ("tpl-1", "tpl-2", ...).
 *
 * Phase 2 invariant: metadata/geometry only. Nothing here is persisted and no frame
 * pixels are ever read or stored.
 *
 * @param nowProvider injected clock so timestamps stay deterministic in tests.
 */
class TemplateManager(private val nowProvider: () -> Long = { 0L }) {

	private val templates = LinkedHashMap<String, CaptureTemplate>()
	private var sequence = 0

	@Synchronized
	fun list(): List<CaptureTemplate> = templates.values.toList()

	@Synchronized
	fun count(): Int = templates.size

	@Synchronized
	fun isEmpty(): Boolean = templates.isEmpty()

	@Synchronized
	fun get(id: String): CaptureTemplate? = templates[id]

	@Synchronized
	fun contains(id: String): Boolean = templates.containsKey(id)

	/**
	 * Create and register a new template from a selected [region] and reference size.
	 * Returns [Result.Ok] with the stored template, or [Result.Error] with a stable reason.
	 */
	@Synchronized
	fun add(
		name: String,
		region: CaptureRegion,
		widthPx: Int,
		heightPx: Int,
		matchThreshold: Float = CaptureTemplate.DEFAULT_THRESHOLD,
	): Result {
		val trimmed = name.trim()
		if (trimmed.isEmpty()) return Result.Error(ERROR_EMPTY_NAME)
		if (trimmed.length > CaptureTemplate.MAX_NAME_LENGTH) return Result.Error(ERROR_NAME_TOO_LONG)
		if (!region.isValid) return Result.Error(ERROR_INVALID_REGION)
		if (widthPx <= 0 || heightPx <= 0) return Result.Error(ERROR_INVALID_SIZE)
		if (hasName(trimmed, exceptId = null)) return Result.Error(ERROR_DUPLICATE_NAME)

		val template = CaptureTemplate(
			id = nextId(),
			name = trimmed,
			region = region.clampedToUnit(),
			widthPx = widthPx,
			heightPx = heightPx,
			matchThreshold = matchThreshold.coerceIn(
				CaptureTemplate.MIN_THRESHOLD,
				CaptureTemplate.MAX_THRESHOLD,
			),
			createdAtMs = nowProvider(),
		)
		templates[template.id] = template
		return Result.Ok(template)
	}

	@Synchronized
	fun rename(id: String, newName: String): Result {
		val existing = templates[id] ?: return Result.Error(ERROR_NOT_FOUND)
		val trimmed = newName.trim()
		if (trimmed.isEmpty()) return Result.Error(ERROR_EMPTY_NAME)
		if (trimmed.length > CaptureTemplate.MAX_NAME_LENGTH) return Result.Error(ERROR_NAME_TOO_LONG)
		if (hasName(trimmed, exceptId = id)) return Result.Error(ERROR_DUPLICATE_NAME)
		val updated = existing.withName(trimmed)
		templates[id] = updated
		return Result.Ok(updated)
	}

	@Synchronized
	fun setThreshold(id: String, threshold: Float): Result {
		val existing = templates[id] ?: return Result.Error(ERROR_NOT_FOUND)
		val updated = existing.withThreshold(threshold)
		templates[id] = updated
		return Result.Ok(updated)
	}

	@Synchronized
	fun setRegion(id: String, region: CaptureRegion): Result {
		val existing = templates[id] ?: return Result.Error(ERROR_NOT_FOUND)
		if (!region.isValid) return Result.Error(ERROR_INVALID_REGION)
		val updated = existing.withRegion(region.clampedToUnit())
		templates[id] = updated
		return Result.Ok(updated)
	}

	@Synchronized
	fun remove(id: String): Boolean = templates.remove(id) != null

	@Synchronized
	fun clear() {
		templates.clear()
	}

	private fun hasName(name: String, exceptId: String?): Boolean =
		templates.values.any { it.id != exceptId && it.name.equals(name, ignoreCase = true) }

	private fun nextId(): String {
		sequence += 1
		return "$ID_PREFIX$sequence"
	}

	/** Outcome of a mutating operation. */
	sealed class Result {
		data class Ok(val template: CaptureTemplate) : Result()
		data class Error(val reason: String) : Result()
	}

	companion object {
		const val ID_PREFIX = "tpl-"
		const val ERROR_EMPTY_NAME = "empty_name"
		const val ERROR_NAME_TOO_LONG = "name_too_long"
		const val ERROR_INVALID_REGION = "invalid_region"
		const val ERROR_INVALID_SIZE = "invalid_size"
		const val ERROR_DUPLICATE_NAME = "duplicate_name"
		const val ERROR_NOT_FOUND = "not_found"
	}
}
