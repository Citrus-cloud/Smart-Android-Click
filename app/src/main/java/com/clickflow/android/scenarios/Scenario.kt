package com.clickflow.android.scenarios

/** Scenario types supported by the engine. Step 52 supports simulation only. */
enum class ScenarioType {
    /** A dry-run "tap" that updates state/logs but performs NO real input. */
    SIMPLE_TAP_SIMULATION,
}

/**
 * A user-authored automation scenario.
 *
 * Step 52: only [ScenarioType.SIMPLE_TAP_SIMULATION] exists and it never
 * touches the real input system. `settings` is a free-form bag for future
 * parameters (coordinates, repeat count, etc.) kept intentionally simple.
 */
data class Scenario(
    val id: String,
    val name: String,
    val type: ScenarioType = ScenarioType.SIMPLE_TAP_SIMULATION,
    val settings: Map<String, String> = emptyMap(),
    val createdAt: Long,
    val updatedAt: Long,
)
