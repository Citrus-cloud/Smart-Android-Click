package com.clickflow.android.scenarios

/** Scenario types. Step 54 adds multi-step; still simulation only. */
enum class ScenarioType {
    SIMPLE_TAP_SIMULATION,
    MULTI_STEP_SIMULATION,
}

/** Types of simulation actions. None of these perform real input. */
enum class ScenarioActionType {
    SIMULATED_TAP,
    WAIT,
    NOTE,
}

/**
 * A single step in a scenario. Modeled as a flat data class (not a sealed class) so it serializes
 * trivially to JSON via `org.json`. Only the fields relevant to [type] are populated.
 *
 * SAFETY: x/y are *simulation* coordinates only — never sent to any real input system.
 */
data class ScenarioAction(
    val id: String,
    val type: ScenarioActionType,
    val x: Int? = null,
    val y: Int? = null,
    val durationMs: Long? = null,
    val message: String? = null,
    val label: String? = null,
)

/** Run-level settings shared by all actions in a scenario. */
data class ScenarioSettings(
    val repeatCount: Int = 1,
    val intervalMs: Long = 500L,
)

/**
 * A user-authored automation scenario (schema version 2 = multi-step).
 *
 * Step 54: a scenario is an ordered list of simulation [actions] executed [settings.repeatCount]
 * times. It never touches the real input system.
 */
data class Scenario(
    val id: String,
    val name: String,
    val type: ScenarioType = ScenarioType.MULTI_STEP_SIMULATION,
    val settings: ScenarioSettings = ScenarioSettings(),
    val actions: List<ScenarioAction> = emptyList(),
    val createdAt: Long,
    val updatedAt: Long,
    val isActive: Boolean = false,
    val version: Int = 2,
)

// ---- Input + validation ----------------------------------------------------

/** Raw, unvalidated scenario metadata from the create/edit form. */
data class ScenarioInput(
    val name: String,
    val repeatCount: Int,
    val intervalMs: Long,
    val type: ScenarioType = ScenarioType.MULTI_STEP_SIMULATION,
)

/** Raw, unvalidated action input from the action form. */
data class ActionInput(
    val type: ScenarioActionType,
    val x: Int? = null,
    val y: Int? = null,
    val durationMs: Long? = null,
    val message: String? = null,
    val label: String? = null,
)

/** Field-level validation errors for scenario metadata. Null = valid. */
data class ScenarioValidationErrors(
    val name: String? = null,
    val repeatCount: String? = null,
    val intervalMs: String? = null,
    val actions: String? = null,
) {
    val hasErrors: Boolean
        get() = name != null || repeatCount != null || intervalMs != null || actions != null
}

/** Field-level validation errors for an action. Null = valid. */
data class ActionValidationErrors(
    val coordinates: String? = null,
    val duration: String? = null,
    val message: String? = null,
) {
    val hasErrors: Boolean
        get() = coordinates != null || duration != null || message != null
}

/**
 * Centralized validation rules:
 *  - scenario name not blank
 *  - actions not empty
 *  - repeatCount in 1..1000
 *  - intervalMs >= 100
 *  - SIMULATED_TAP: x/y >= 0
 *  - WAIT: durationMs >= 100
 *  - NOTE: message not blank, max 300 chars
 */
object ScenarioValidator {
    const val MIN_REPEAT = 1
    const val MAX_REPEAT = 1000
    const val MIN_INTERVAL_MS = 100L
    const val MIN_WAIT_MS = 100L
    const val MAX_NOTE_LEN = 300

    fun validateMeta(input: ScenarioInput, actionCount: Int): ScenarioValidationErrors =
        ScenarioValidationErrors(
            name = if (input.name.isBlank()) "name_required" else null,
            repeatCount = if (input.repeatCount !in MIN_REPEAT..MAX_REPEAT) "repeat_invalid" else null,
            intervalMs = if (input.intervalMs < MIN_INTERVAL_MS) "interval_invalid" else null,
            actions = if (actionCount <= 0) "actions_required" else null,
        )

    fun validateAction(input: ActionInput): ActionValidationErrors = when (input.type) {
        ScenarioActionType.SIMULATED_TAP -> ActionValidationErrors(
            coordinates = if ((input.x ?: -1) < 0 || (input.y ?: -1) < 0) "coordinate_invalid" else null,
        )
        ScenarioActionType.WAIT -> ActionValidationErrors(
            duration = if ((input.durationMs ?: -1L) < MIN_WAIT_MS) "duration_invalid" else null,
        )
        ScenarioActionType.NOTE -> ActionValidationErrors(
            message = run {
                val m = input.message?.trim().orEmpty()
                if (m.isBlank() || m.length > MAX_NOTE_LEN) "message_invalid" else null
            },
        )
    }

    fun isActionValid(input: ActionInput): Boolean = !validateAction(input).hasErrors
}
