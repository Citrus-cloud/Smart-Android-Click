package com.clickflow.android.scenario

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScenarioLibraryStoreTest {

    // ---- ScenarioStep validation (already covered in ScenarioModelTest, but testing edge cases) ----

    @Test
    fun step_validMarker_withZeroCoords() {
        val step = ScenarioStep(id = "s1", type = StepType.MARKER, x = 0, y = 0)
        assertTrue(step.isValid)
    }

    @Test
    fun step_validPhoto_withTemplateId() {
        val step = ScenarioStep(id = "s1", type = StepType.PHOTO, photoTemplateId = "tpl-1")
        assertTrue(step.isValid)
    }

    @Test
    fun step_validPhoto_withPath() {
        val step = ScenarioStep(id = "s1", type = StepType.PHOTO, photoPath = "/data/img.png")
        assertTrue(step.isValid)
    }

    @Test
    fun step_photo_needsTemplateOrPath() {
        val step = ScenarioStep(id = "s1", type = StepType.PHOTO)
        assertFalse(step.isValid)
    }

    @Test
    fun step_text_maxLength200() {
        val step = ScenarioStep(id = "s1", type = StepType.TEXT, text = "A".repeat(200))
        assertTrue(step.isValid)
    }

    @Test
    fun step_text_tooLong() {
        val step = ScenarioStep(id = "s1", type = StepType.TEXT, text = "A".repeat(201))
        assertFalse(step.isValid)
    }

    @Test
    fun step_wait_validBounds() {
        assertTrue(ScenarioStep(id = "s1", type = StepType.WAIT, waitMs = 50L).isValid)
        assertTrue(ScenarioStep(id = "s1", type = StepType.WAIT, waitMs = 600000L).isValid)
        assertFalse(ScenarioStep(id = "s1", type = StepType.WAIT, waitMs = 49L).isValid)
        assertFalse(ScenarioStep(id = "s1", type = StepType.WAIT, waitMs = 600001L).isValid)
    }

    @Test
    fun step_notFoundRetries_validBounds() {
        assertTrue(ScenarioStep(id = "s1", type = StepType.MARKER, x = 0, y = 0, notFoundRetries = 0).isValid)
        assertTrue(ScenarioStep(id = "s1", type = StepType.MARKER, x = 0, y = 0, notFoundRetries = 100000).isValid)
    }

    @Test
    fun step_notFoundWaitMs_validBounds() {
        assertTrue(ScenarioStep(id = "s1", type = StepType.MARKER, x = 0, y = 0, notFoundWaitMs = 50L).isValid)
        assertTrue(ScenarioStep(id = "s1", type = StepType.MARKER, x = 0, y = 0, notFoundWaitMs = 600000L).isValid)
        assertFalse(ScenarioStep(id = "s1", type = StepType.MARKER, x = 0, y = 0, notFoundWaitMs = 49L).isValid)
    }

    // ---- Scenario validation ----

    @Test
    fun scenario_valid_withMinimalFields() {
        val scenario = Scenario(id = "sc1", name = "Test")
        assertTrue(scenario.isValid)
    }

    @Test
    fun scenario_valid_withSteps() {
        val scenario = Scenario(
            id = "sc1",
            name = "Test",
            steps = listOf(
                ScenarioStep(id = "s1", type = StepType.MARKER, x = 10, y = 20),
                ScenarioStep(id = "s2", type = StepType.TEXT, text = "OK"),
                ScenarioStep(id = "s3", type = StepType.WAIT, waitMs = 500L),
                ScenarioStep(id = "s4", type = StepType.PHOTO, photoTemplateId = "tpl"),
            ),
        )
        assertTrue(scenario.isValid)
    }

    @Test
    fun scenario_invalid_emptyId() {
        val scenario = Scenario(id = "", name = "Test")
        assertFalse(scenario.isValid)
    }

    @Test
    fun scenario_invalid_emptyName() {
        val scenario = Scenario(id = "sc1", name = "")
        assertFalse(scenario.isValid)
    }

    @Test
    fun scenario_invalid_nameTooLong() {
        val scenario = Scenario(id = "sc1", name = "A".repeat(61))
        assertFalse(scenario.isValid)
    }

    @Test
    fun scenario_invalid_loopCountZero() {
        val scenario = Scenario(id = "sc1", name = "Test", loopCount = 0)
        assertFalse(scenario.isValid)
    }

    @Test
    fun scenario_invalid_tooManySteps() {
        val steps = (1..51).map { ScenarioStep(id = "s$it", type = StepType.MARKER, x = 0, y = 0) }
        val scenario = Scenario(id = "sc1", name = "Test", steps = steps)
        assertFalse(scenario.isValid)
    }

    @Test
    fun scenario_invalid_stepWithInvalidStep() {
        val scenario = Scenario(
            id = "sc1",
            name = "Test",
            steps = listOf(
                ScenarioStep(id = "s1", type = StepType.MARKER, x = 0, y = 0),
                ScenarioStep(id = "", type = StepType.MARKER), // invalid step
            ),
        )
        assertFalse(scenario.isValid)
    }

    // ---- tapsPerPass ----

    @Test
    fun tapsPerPass_excludesWaits() {
        val scenario = Scenario(
            id = "sc1",
            name = "Test",
            steps = listOf(
                ScenarioStep(id = "s1", type = StepType.MARKER, x = 0, y = 0, repeat = 5),
                ScenarioStep(id = "s2", type = StepType.WAIT, waitMs = 500L, repeat = 3),
                ScenarioStep(id = "s3", type = StepType.TEXT, text = "OK", repeat = 2),
            ),
        )
        assertEquals(7, scenario.tapsPerPass)
    }

    @Test
    fun tapsPerPass_emptySteps() {
        val scenario = Scenario(id = "sc1", name = "Test", steps = emptyList())
        assertEquals(0, scenario.tapsPerPass)
    }

    @Test
    fun tapsPerPass_allWaits() {
        val scenario = Scenario(
            id = "sc1",
            name = "Test",
            steps = listOf(
                ScenarioStep(id = "s1", type = StepType.WAIT, waitMs = 500L),
                ScenarioStep(id = "s2", type = StepType.WAIT, waitMs = 1000L),
            ),
        )
        assertEquals(0, scenario.tapsPerPass)
    }

    @Test
    fun tapsPerPass_photoStep() {
        val scenario = Scenario(
            id = "sc1",
            name = "Test",
            steps = listOf(
                ScenarioStep(id = "s1", type = StepType.PHOTO, photoTemplateId = "tpl", repeat = 10),
            ),
        )
        assertEquals(10, scenario.tapsPerPass)
    }

    // ---- summary ----

    @Test
    fun summary_markerContainsCoords() {
        val step = ScenarioStep(id = "s1", type = StepType.MARKER, x = 123, y = 456)
        val summary = step.summary()
        assertTrue(summary.contains("123"))
        assertTrue(summary.contains("456"))
    }

    @Test
    fun summary_textContainsQuery() {
        val step = ScenarioStep(id = "s1", type = StepType.TEXT, text = "Submit")
        assertTrue(step.summary().contains("Submit"))
    }

    @Test
    fun summary_waitContainsDuration() {
        val step = ScenarioStep(id = "s1", type = StepType.WAIT, waitMs = 2500L)
        assertTrue(step.summary().contains("2500"))
    }

    @Test
    fun summary_photoContainsLabel() {
        val step = ScenarioStep(id = "s1", type = StepType.PHOTO, label = "Button")
        assertTrue(step.summary().contains("Button"))
    }

    // ---- StepType enum ----

    @Test
    fun stepType_hasAllExpectedValues() {
        val values = StepType.values()
        assertEquals(4, values.size)
        assertTrue(values.contains(StepType.MARKER))
        assertTrue(values.contains(StepType.PHOTO))
        assertTrue(values.contains(StepType.TEXT))
        assertTrue(values.contains(StepType.WAIT))
    }

    // ---- NotFoundPolicy enum ----

    @Test
    fun notFoundPolicy_hasAllExpectedValues() {
        val values = NotFoundPolicy.values()
        assertEquals(3, values.size)
        assertTrue(values.contains(NotFoundPolicy.SKIP))
        assertTrue(values.contains(NotFoundPolicy.WAIT_RETRY))
        assertTrue(values.contains(NotFoundPolicy.STOP))
    }

    // ---- data class copy ----

    @Test
    fun scenarioStep_copy_preservesOtherFields() {
        val original = ScenarioStep(
            id = "s1", type = StepType.MARKER, label = "Tap",
            x = 10, y = 20, repeat = 3, intervalMs = 1000L,
        )
        val modified = original.copy(x = 50, y = 60)
        assertEquals("s1", modified.id)
        assertEquals(StepType.MARKER, modified.type)
        assertEquals("Tap", modified.label)
        assertEquals(50, modified.x)
        assertEquals(60, modified.y)
        assertEquals(3, modified.repeat)
        assertEquals(1000L, modified.intervalMs)
    }

    @Test
    fun scenario_copy_preservesOtherFields() {
        val original = Scenario(
            id = "sc1", name = "Test", icon = "🎬",
            steps = listOf(ScenarioStep(id = "s1", type = StepType.MARKER, x = 0, y = 0)),
            loopInfinite = true, loopCount = 5,
        )
        val modified = original.copy(name = "Renamed")
        assertEquals("sc1", modified.id)
        assertEquals("Renamed", modified.name)
        assertEquals("🎬", modified.icon)
        assertEquals(1, modified.steps.size)
        assertTrue(modified.loopInfinite)
        assertEquals(5, modified.loopCount)
    }
}
