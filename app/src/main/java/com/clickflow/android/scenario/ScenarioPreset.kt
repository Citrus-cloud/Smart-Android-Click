package com.clickflow.android.scenario

/**
 * Step 73 — A named preset that bundles a ready-made list of [PresetAction]s.
 *
 * Presets are immutable templates; users apply them to create a new scenario
 * or to replace an existing one's action list. No Android imports.
 *
 * @param id          Stable identifier (e.g. "preset-tap-center").
 * @param name        Human-readable name (≤ 60 chars).
 * @param description Short description (≤ 200 chars, may be empty).
 * @param actions     Ordered list of preset actions (1–20 items).
 */
data class ScenarioPreset(
    val id: String,
    val name: String,
    val description: String = "",
    val actions: List<PresetAction>
) {
    /** True when the preset satisfies all structural constraints. */
    val isValid: Boolean
        get() = id.isNotBlank() &&
            name.isNotBlank() && name.length <= 60 &&
            description.length <= 200 &&
            actions.isNotEmpty() && actions.size <= 20 &&
            actions.all { it.isValid }
}

/**
 * One action inside a [ScenarioPreset].
 *
 * @param type      What the action does.
 * @param label     Optional human-readable label (≤ 60 chars).
 * @param x         Normalized [0, 1] tap X (only for [PresetActionType.TAP]).
 * @param y         Normalized [0, 1] tap Y (only for [PresetActionType.TAP]).
 * @param durationMs Delay in ms ≥ 100 (only for [PresetActionType.WAIT]).
 * @param note      Note text ≤ 300 chars (only for [PresetActionType.NOTE]).
 */
data class PresetAction(
    val type: PresetActionType,
    val label: String = "",
    val x: Float = 0f,
    val y: Float = 0f,
    val durationMs: Long = 0L,
    val note: String = ""
) {
    /** True when all type-specific constraints are satisfied. */
    val isValid: Boolean
        get() = label.length <= 60 && when (type) {
            PresetActionType.TAP -> x in 0f..1f && y in 0f..1f
            PresetActionType.WAIT -> durationMs >= 100L
            PresetActionType.NOTE -> note.isNotBlank() && note.length <= 300
        }
}

enum class PresetActionType { TAP, WAIT, NOTE }

/** Built-in preset catalogue. Add more as the product grows. */
object BuiltInPresets {
    val TAP_CENTER = ScenarioPreset(
        id = "builtin-tap-center",
        name = "Tap center",
        description = "Single tap at the centre of the screen.",
        actions = listOf(PresetAction(PresetActionType.TAP, x = 0.5f, y = 0.5f))
    )

    val TAP_AND_WAIT = ScenarioPreset(
        id = "builtin-tap-and-wait",
        name = "Tap and wait",
        description = "Tap at centre, then wait 500 ms.",
        actions = listOf(
            PresetAction(PresetActionType.TAP, x = 0.5f, y = 0.5f),
            PresetAction(PresetActionType.WAIT, durationMs = 500L)
        )
    )

    val ALL: List<ScenarioPreset> = listOf(TAP_CENTER, TAP_AND_WAIT)
}
