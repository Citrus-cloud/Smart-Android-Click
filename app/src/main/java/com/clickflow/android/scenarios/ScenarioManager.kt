package com.clickflow.android.scenarios

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Runtime source of truth for scenarios (v2, multi-step). Backed by [ScenarioRepository] for
 * persistence; exposes the list as a [StateFlow] for Compose.
 */
class ScenarioManager(private val repository: ScenarioRepository) {

    private val _scenarios = MutableStateFlow<List<Scenario>>(emptyList())
    val scenarios: StateFlow<List<Scenario>> = _scenarios.asStateFlow()

    val storageReady: Boolean get() = repository.storageReady
    val corruptedStorageRecovered: Boolean get() = repository.corruptedStorageRecovered
    val storageMigrated: Boolean get() = repository.storageMigrated

    fun load() { _scenarios.value = repository.loadScenarios() }

    fun getScenarios(): List<Scenario> = _scenarios.value
    fun getActiveScenario(): Scenario? = repository.getActiveScenario(_scenarios.value)
    fun byId(id: String): Scenario? = _scenarios.value.firstOrNull { it.id == id }

    // ---- profile-aware views ----------------------------------------------

    fun scenariosForProfile(profileId: String): List<Scenario> =
        _scenarios.value.filter { it.profileId == profileId }

    fun countForProfile(profileId: String): Int = scenariosForProfile(profileId).size

    fun getActiveScenarioForProfile(profileId: String): Scenario? {
        val inProfile = scenariosForProfile(profileId)
        return inProfile.firstOrNull { it.isActive } ?: inProfile.firstOrNull()
    }

    // ---- validation --------------------------------------------------------

    fun validateScenarioMeta(input: ScenarioInput, actionCount: Int): ScenarioValidationErrors =
        ScenarioValidator.validateMeta(input, actionCount)

    fun validateAction(input: ActionInput): ActionValidationErrors =
        ScenarioValidator.validateAction(input)

    // ---- scenario-level ----------------------------------------------------

    fun createScenario(input: ScenarioInput, profileId: String): ScenarioValidationErrors {
        // A freshly created scenario is seeded with one starter action, so actionCount = 1.
        val errors = ScenarioValidator.validateMeta(input, actionCount = 1)
        if (errors.hasErrors) return errors
        _scenarios.value = repository.createScenario(input, _scenarios.value, profileId)
        return errors
    }

    fun updateScenarioMeta(id: String, input: ScenarioInput): ScenarioValidationErrors {
        val actionCount = byId(id)?.actions?.size ?: 0
        val errors = ScenarioValidator.validateMeta(input, actionCount)
        if (errors.hasErrors) return errors
        _scenarios.value = repository.updateScenarioMeta(id, input, _scenarios.value)
        return errors
    }

    fun deleteScenario(id: String) { _scenarios.value = repository.deleteScenario(id, _scenarios.value) }
    fun setActiveScenario(id: String) { _scenarios.value = repository.setActiveScenario(id, _scenarios.value) }
    fun resetToDefaults() { _scenarios.value = repository.resetScenarios() }

    // ---- action-level ------------------------------------------------------

    /** Builds a validated [ScenarioAction] from input, or null if invalid. */
    private fun buildAction(id: String, input: ActionInput): ScenarioAction? {
        if (validateAction(input).hasErrors) return null
        return when (input.type) {
            ScenarioActionType.SIMULATED_TAP -> ScenarioAction(
                id = id, type = input.type, x = input.x, y = input.y,
                label = input.label?.trim()?.ifBlank { null },
            )
            ScenarioActionType.WAIT -> ScenarioAction(id = id, type = input.type, durationMs = input.durationMs)
            ScenarioActionType.NOTE -> ScenarioAction(id = id, type = input.type, message = input.message?.trim())
        }
    }

    fun addAction(scenarioId: String, input: ActionInput): Boolean {
        val action = buildAction(repository.newActionId(), input) ?: return false
        val s = byId(scenarioId) ?: return false
        _scenarios.value = repository.replaceActions(scenarioId, s.actions + action, _scenarios.value)
        return true
    }

    fun updateAction(scenarioId: String, actionId: String, input: ActionInput): Boolean {
        val rebuilt = buildAction(actionId, input) ?: return false
        val s = byId(scenarioId) ?: return false
        val actions = s.actions.map { if (it.id == actionId) rebuilt else it }
        _scenarios.value = repository.replaceActions(scenarioId, actions, _scenarios.value)
        return true
    }

    fun deleteAction(scenarioId: String, actionId: String) {
        val s = byId(scenarioId) ?: return
        val actions = s.actions.filterNot { it.id == actionId }
        // Keep at least one action so the scenario stays runnable/valid.
        val safe = if (actions.isEmpty())
            listOf(ScenarioAction(id = repository.newActionId(), type = ScenarioActionType.NOTE, message = "Empty scenario"))
        else actions
        _scenarios.value = repository.replaceActions(scenarioId, safe, _scenarios.value)
    }

    /** Moves an action up (up=true) or down within its scenario. */
    fun moveAction(scenarioId: String, actionId: String, up: Boolean) {
        val s = byId(scenarioId) ?: return
        val list = s.actions.toMutableList()
        val i = list.indexOfFirst { it.id == actionId }
        if (i < 0) return
        val j = if (up) i - 1 else i + 1
        if (j < 0 || j >= list.size) return
        val tmp = list[i]; list[i] = list[j]; list[j] = tmp
        _scenarios.value = repository.replaceActions(scenarioId, list, _scenarios.value)
    }

    fun duplicateAction(scenarioId: String, actionId: String) {
        val s = byId(scenarioId) ?: return
        val i = s.actions.indexOfFirst { it.id == actionId }
        if (i < 0) return
        val copy = s.actions[i].copy(id = repository.newActionId())
        val list = s.actions.toMutableList().apply { add(i + 1, copy) }
        _scenarios.value = repository.replaceActions(scenarioId, list, _scenarios.value)
    }
}
