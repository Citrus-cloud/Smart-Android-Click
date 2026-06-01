package com.clickflow.android.scenarios

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Source of truth for scenarios at runtime. Backed by [ScenarioRepository] for
 * persistence and exposes the list as a [StateFlow] for Compose.
 */
class ScenarioManager(private val repository: ScenarioRepository) {

    private val _scenarios = MutableStateFlow<List<Scenario>>(emptyList())
    val scenarios: StateFlow<List<Scenario>> = _scenarios.asStateFlow()

    val storageReady: Boolean get() = repository.storageReady
    val corruptedStorageRecovered: Boolean get() = repository.corruptedStorageRecovered

    /** Loads persisted scenarios (called once at startup). */
    fun load() {
        _scenarios.value = repository.loadScenarios()
    }

    fun getScenarios(): List<Scenario> = _scenarios.value

    fun getActiveScenario(): Scenario? = repository.getActiveScenario(_scenarios.value)

    fun validateScenario(input: ScenarioInput): ScenarioValidationErrors =
        ScenarioValidator.validate(input)

    /** Creates a scenario if valid; returns errors otherwise (no state change). */
    fun createScenario(input: ScenarioInput): ScenarioValidationErrors {
        val errors = validateScenario(input)
        if (errors.hasErrors) return errors
        _scenarios.value = repository.createScenario(input, _scenarios.value)
        return errors
    }

    fun updateScenario(id: String, input: ScenarioInput): ScenarioValidationErrors {
        val errors = validateScenario(input)
        if (errors.hasErrors) return errors
        _scenarios.value = repository.updateScenario(id, input, _scenarios.value)
        return errors
    }

    fun deleteScenario(id: String) {
        _scenarios.value = repository.deleteScenario(id, _scenarios.value)
    }

    fun setActiveScenario(id: String) {
        _scenarios.value = repository.setActiveScenario(id, _scenarios.value)
    }

    fun resetToDefaults() {
        _scenarios.value = repository.resetScenarios()
    }

    fun byId(id: String): Scenario? = _scenarios.value.firstOrNull { it.id == id }
}
