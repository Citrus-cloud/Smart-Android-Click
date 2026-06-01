package com.clickflow.android.scenarios

/** Scenario types supported by the engine. Step 53 still supports simulation only. */
enum class ScenarioType {
    /** A dry-run "tap" that updates state/logs/progress but performs NO real input. */
    SIMPLE_TAP_SIMULATION,
}

/**
 * Simulation parameters for a scenario.
 *
 * IMPORTANT: x/y are *simulation* coordinates only. They are never sent to any
 * real input system in Step 53 — they exist purely to drive the dry-run log
 * ("Simulated tap at x/y") and to model what a future, safety-gated real-tap
 * step would consume.
 */
data class ScenarioSettings(
    val x: Int = 0,
    val y: Int = 0,
    val repeatCount: Int = 1,
    val intervalMs: Long = 500L,
)

/**
 * A user-authored automation scenario.
 *
 * Step 53: only [ScenarioType.SIMPLE_TAP_SIMULATION] exists and it never touches
 * the real input system.
 */
data class Scenario(
    val id: String,
    val name: String,
    val type: ScenarioType = ScenarioType.SIMPLE_TAP_SIMULATION,
    val settings: ScenarioSettings = ScenarioSettings(),
    val createdAt: Long,
    val updatedAt: Long,
    val isActive: Boolean = false,
)

/** Raw, unvalidated user input from the create/edit form. */
data class ScenarioInput(
    val name: String,
    val x: Int,
    val y: Int,
    val repeatCount: Int,
    val intervalMs: Long,
    val type: ScenarioType = ScenarioType.SIMPLE_TAP_SIMULATION,
)

/** Field-level validation errors. Null value = field is valid. */
data class ScenarioValidationErrors(
    val name: String? = null,
    val coordinates: String? = null,
    val repeatCount: String? = null,
    val intervalMs: String? = null,
) {
    val hasErrors: Boolean
        get() = name != null || coordinates != null || repeatCount != null || intervalMs != null
}

/**
 * Pure validation for scenario input. Centralized so the form, manager, and
 * repository all agree on the rules.
 *
 * Rules:
 *  - name not blank
 *  - x >= 0, y >= 0
 *  - repeatCount in 1..1000
 *  - intervalMs >= 100
 */
object ScenarioValidator {
    const val MIN_REPEAT = 1
    const val MAX_REPEAT = 1000
    const val MIN_INTERVAL_MS = 100L

    fun validate(input: ScenarioInput): ScenarioValidationErrors = ScenarioValidationErrors(
        name = if (input.name.isBlank()) "name_required" else null,
        coordinates = if (input.x < 0 || input.y < 0) "coordinate_invalid" else null,
        repeatCount = if (input.repeatCount !in MIN_REPEAT..MAX_REPEAT) "repeat_invalid" else null,
        intervalMs = if (input.intervalMs < MIN_INTERVAL_MS) "interval_invalid" else null,
    )

    fun isValid(input: ScenarioInput): Boolean = !validate(input).hasErrors
}
