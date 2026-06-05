package com.clickflow.android.scenario

/**
 * Step 73 — Domain-layer builder for assembling a visual scenario.
 *
 * Manages an ordered list of [PresetAction]s and exposes CRUD + preset-apply
 * operations. All mutations return a [BuilderResult] with stable reason codes.
 * No Android imports, no persistence, fully unit-testable on the JVM.
 */
class VisualScenarioBuilder {

    private val _actions: MutableList<PresetAction> = mutableListOf()

    /** Current (immutable) snapshot of the action list. */
    val actions: List<PresetAction> get() = _actions.toList()

    /** Number of actions currently in the builder. */
    val count: Int get() = _actions.size

    /** True when there are no actions. */
    val isEmpty: Boolean get() = _actions.isEmpty()

    // ---- Mutations --------------------------------------------------------

    /**
     * Append [action] to the end of the list.
     * Fails if the list would exceed 20 items or the action is invalid.
     * Reason codes: `invalid_action`, `too_many_actions`.
     */
    fun add(action: PresetAction): BuilderResult {
        if (!action.isValid) return BuilderResult.Error("invalid_action")
        if (_actions.size >= 20) return BuilderResult.Error("too_many_actions")
        _actions.add(action)
        return BuilderResult.Ok
    }

    /**
     * Replace the action at [index].
     * Reason codes: `invalid_index`, `invalid_action`.
     */
    fun update(index: Int, action: PresetAction): BuilderResult {
        if (index !in _actions.indices) return BuilderResult.Error("invalid_index")
        if (!action.isValid) return BuilderResult.Error("invalid_action")
        _actions[index] = action
        return BuilderResult.Ok
    }

    /**
     * Remove the action at [index].
     * Reason code: `invalid_index`.
     */
    fun remove(index: Int): BuilderResult {
        if (index !in _actions.indices) return BuilderResult.Error("invalid_index")
        _actions.removeAt(index)
        return BuilderResult.Ok
    }

    /**
     * Move the action at [from] to position [to].
     * Reason code: `invalid_index`.
     */
    fun move(from: Int, to: Int): BuilderResult {
        if (from !in _actions.indices || to !in _actions.indices) {
            return BuilderResult.Error("invalid_index")
        }
        val item = _actions.removeAt(from)
        _actions.add(to, item)
        return BuilderResult.Ok
    }

    /** Remove all actions. */
    fun clear() { _actions.clear() }

    // ---- Preset support ---------------------------------------------------

    /**
     * Replace all current actions with those from [preset].
     * Fails if [preset] is not valid.
     * Reason code: `invalid_preset`.
     */
    fun applyPreset(preset: ScenarioPreset): BuilderResult {
        if (!preset.isValid) return BuilderResult.Error("invalid_preset")
        _actions.clear()
        _actions.addAll(preset.actions)
        return BuilderResult.Ok
    }

    /**
     * Append all actions from [preset] to the current list.
     * Fails if the result would exceed 20 items or the preset is invalid.
     * Reason codes: `invalid_preset`, `too_many_actions`.
     */
    fun appendPreset(preset: ScenarioPreset): BuilderResult {
        if (!preset.isValid) return BuilderResult.Error("invalid_preset")
        if (_actions.size + preset.actions.size > 20) return BuilderResult.Error("too_many_actions")
        _actions.addAll(preset.actions)
        return BuilderResult.Ok
    }

    // ---- Sealed result type -----------------------------------------------

    sealed class BuilderResult {
        object Ok : BuilderResult()
        data class Error(val reason: String) : BuilderResult()
    }
}
