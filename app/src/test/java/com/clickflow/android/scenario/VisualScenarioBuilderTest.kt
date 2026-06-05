package com.clickflow.android.scenario

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class VisualScenarioBuilderTest {

    private lateinit var builder: VisualScenarioBuilder

    private val tapAction = PresetAction(PresetActionType.TAP, x = 0.5f, y = 0.5f)
    private val waitAction = PresetAction(PresetActionType.WAIT, durationMs = 300L)
    private val noteAction = PresetAction(PresetActionType.NOTE, note = "Hello")

    @Before
    fun setUp() { builder = VisualScenarioBuilder() }

    // 1. Starts empty
    @Test
    fun startsEmpty() {
        assertTrue(builder.isEmpty)
        assertEquals(0, builder.count)
    }

    // 2. Add valid action succeeds
    @Test
    fun addValidAction_succeeds() {
        val result = builder.add(tapAction)
        assertTrue(result is VisualScenarioBuilder.BuilderResult.Ok)
        assertEquals(1, builder.count)
    }

    // 3. Add invalid action returns error
    @Test
    fun addInvalidAction_returnsError() {
        val bad = PresetAction(PresetActionType.WAIT, durationMs = 50L) // < 100ms
        val result = builder.add(bad)
        assertTrue(result is VisualScenarioBuilder.BuilderResult.Error)
        assertEquals("invalid_action", (result as VisualScenarioBuilder.BuilderResult.Error).reason)
    }

    // 4. Cannot exceed 20 actions
    @Test
    fun add_tooManyActions_returnsError() {
        repeat(20) { builder.add(tapAction) }
        val result = builder.add(tapAction)
        assertTrue(result is VisualScenarioBuilder.BuilderResult.Error)
        assertEquals("too_many_actions", (result as VisualScenarioBuilder.BuilderResult.Error).reason)
    }

    // 5. Update replaces action at index
    @Test
    fun update_replacesAction() {
        builder.add(tapAction)
        builder.update(0, waitAction)
        assertEquals(waitAction, builder.actions[0])
    }

    // 6. Update out-of-bounds returns error
    @Test
    fun update_invalidIndex_returnsError() {
        val result = builder.update(5, tapAction)
        assertEquals("invalid_index", (result as VisualScenarioBuilder.BuilderResult.Error).reason)
    }

    // 7. Remove at valid index
    @Test
    fun remove_validIndex_removesAction() {
        builder.add(tapAction)
        builder.add(waitAction)
        builder.remove(0)
        assertEquals(1, builder.count)
        assertEquals(waitAction, builder.actions[0])
    }

    // 8. Move reorders correctly
    @Test
    fun move_reordersActions() {
        builder.add(tapAction)
        builder.add(waitAction)
        builder.add(noteAction)
        builder.move(0, 2) // move tap to end
        assertEquals(waitAction, builder.actions[0])
        assertEquals(tapAction, builder.actions[2])
    }

    // 9. Clear empties list
    @Test
    fun clear_emptiesList() {
        builder.add(tapAction)
        builder.clear()
        assertTrue(builder.isEmpty)
    }

    // 10. applyPreset replaces all actions
    @Test
    fun applyPreset_replacesActions() {
        builder.add(tapAction)
        builder.applyPreset(BuiltInPresets.TAP_AND_WAIT)
        assertEquals(2, builder.count)
        assertEquals(PresetActionType.TAP, builder.actions[0].type)
        assertEquals(PresetActionType.WAIT, builder.actions[1].type)
    }

    // 11. appendPreset appends without clearing
    @Test
    fun appendPreset_appendsToExisting() {
        builder.add(noteAction)
        builder.appendPreset(BuiltInPresets.TAP_CENTER)
        assertEquals(2, builder.count)
        assertEquals(PresetActionType.NOTE, builder.actions[0].type)
        assertEquals(PresetActionType.TAP, builder.actions[1].type)
    }

    // 12. appendPreset fails when limit exceeded
    @Test
    fun appendPreset_tooManyActions_returnsError() {
        repeat(20) { builder.add(tapAction) }
        val result = builder.appendPreset(BuiltInPresets.TAP_CENTER)
        assertEquals("too_many_actions", (result as VisualScenarioBuilder.BuilderResult.Error).reason)
    }

    // 13. BuiltInPresets are all valid
    @Test
    fun builtInPresets_allValid() {
        assertTrue(BuiltInPresets.ALL.all { it.isValid })
    }
}
