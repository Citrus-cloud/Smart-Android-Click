package com.clickflow.android.scenario

/**
 * Premium "Scenario" mode — data model (Phase 1).
 *
 * A [Scenario] is an ordered list of typed [ScenarioStep]s that the user
 * assembles themselves (no built-in templates). Steps run one after another;
 * the whole scenario can loop a fixed number of times or forever.
 *
 * Pure Kotlin — no Android imports — so the model can be unit-tested on the JVM.
 * Persistence lives in [ScenarioLibraryStore]; execution will live in the runner.
 */

/** The kind of action a step performs. */
enum class StepType { MARKER, PHOTO, TEXT, WAIT }

/**
 * What a PHOTO/TEXT step does when it cannot find its target on screen.
 * Chosen per step so the user fully controls each step's behavior.
 */
enum class NotFoundPolicy { SKIP, WAIT_RETRY, STOP }

/**
 * A single step in a scenario.
 *
 * Only the fields relevant to [type] are used; the rest keep safe defaults so
 * the data class round-trips cleanly through JSON.
 */
data class ScenarioStep(
    val id: String,
    val type: StepType,
    val label: String = "",
    // MARKER — absolute screen coordinates of the tap point.
    val x: Int = 0,
    val y: Int = 0,
    // PHOTO — reference to a saved image template id and/or an inline file path.
    val photoTemplateId: String = "",
    val photoPath: String = "",
    // TEXT — query string + matching mode.
    val text: String = "",
    val textContains: Boolean = true,
    val textIgnoreCase: Boolean = true,
    // Common — repeat this step N times, with a delay after each tap.
    val repeat: Int = 1,
    val intervalMs: Long = 500L,
    // WAIT — pause duration.
    val waitMs: Long = 500L,
    // PHOTO/TEXT — behavior when the target is not found.
    val notFound: NotFoundPolicy = NotFoundPolicy.SKIP,
    val notFoundWaitMs: Long = 2000L,
    val notFoundRetries: Int = 5,
) {
    /** True when all type-specific constraints are satisfied. */
    val isValid: Boolean
        get() = id.isNotBlank() &&
            label.length <= 60 &&
            repeat in 1..100000 &&
            intervalMs in 50L..600000L &&
            notFoundWaitMs in 50L..600000L &&
            notFoundRetries in 0..100000 &&
            when (type) {
                StepType.MARKER -> x >= 0 && y >= 0
                StepType.PHOTO -> photoTemplateId.isNotBlank() || photoPath.isNotBlank()
                StepType.TEXT -> text.isNotBlank() && text.length <= 200
                StepType.WAIT -> waitMs in 50L..600000L
            }

    /** Short one-line human description used by the editor list. */
    fun summary(): String = when (type) {
        StepType.MARKER -> "\u0422\u0430\u043f \u043f\u043e \u043c\u0435\u0442\u043a\u0435 ($x, $y)"
        StepType.PHOTO -> "\u041f\u043e\u0438\u0441\u043a \u043f\u043e \u0444\u043e\u0442\u043e" + if (label.isNotBlank()) ": $label" else ""
        StepType.TEXT -> "\u041f\u043e\u0438\u0441\u043a \u043f\u043e \u0442\u0435\u043a\u0441\u0442\u0443: \"$text\""
        StepType.WAIT -> "\u041f\u0430\u0443\u0437\u0430 $waitMs \u043c\u0441"
    }
}

/**
 * A named, user-created scenario: an ordered list of steps plus loop settings.
 */
data class Scenario(
    val id: String,
    val name: String,
    val icon: String = "\uD83C\uDFAC",
    val steps: List<ScenarioStep> = emptyList(),
    val loopInfinite: Boolean = false,
    val loopCount: Int = 1,
) {
    /** True when the scenario satisfies all structural constraints. */
    val isValid: Boolean
        get() = id.isNotBlank() &&
            name.isNotBlank() && name.length <= 60 &&
            loopCount in 1..100000 &&
            steps.size <= 50 &&
            steps.all { it.isValid }

    /** Total number of taps one full pass performs (ignoring not-found skips). */
    val tapsPerPass: Int
        get() = steps.filter { it.type != StepType.WAIT }.sumOf { it.repeat }
}
