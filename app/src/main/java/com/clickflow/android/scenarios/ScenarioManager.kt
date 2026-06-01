package com.clickflow.android.scenarios

/**
 * In-memory store of scenarios for Step 52.
 *
 * No persistence, no network. Ships with a couple of preset simulation
 * scenarios so the UI has something to run. Real persistence (Room/files)
 * is deferred to a later step.
 */
class ScenarioManager {

    private val scenarios = mutableListOf<Scenario>()
    private var seq = 0L

    init {
        // Deterministic seed presets (no wall-clock dependency at construction).
        addPreset("Demo tap (simulation)")
        addPreset("Repeat tap x5 (simulation)")
    }

    private fun nextId(): String = "scn_${++seq}"

    private fun addPreset(name: String) {
        val id = nextId()
        scenarios.add(
            Scenario(
                id = id,
                name = name,
                type = ScenarioType.SIMPLE_TAP_SIMULATION,
                settings = mapOf("mode" to "simulation"),
                createdAt = seq,
                updatedAt = seq,
            )
        )
    }

    fun all(): List<Scenario> = scenarios.toList()

    fun byId(id: String): Scenario? = scenarios.firstOrNull { it.id == id }

    fun create(name: String, nowMillis: Long): Scenario {
        val scenario = Scenario(
            id = nextId(),
            name = name,
            type = ScenarioType.SIMPLE_TAP_SIMULATION,
            settings = mapOf("mode" to "simulation"),
            createdAt = nowMillis,
            updatedAt = nowMillis,
        )
        scenarios.add(scenario)
        return scenario
    }
}
