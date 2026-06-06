package com.clickflow.android.scenario

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScenarioModelTest {

    @Test
    fun markerStep_validWithNonNegativeCoords() {
        val step = ScenarioStep(id = "s1", type = StepType.MARKER, x = 100, y = 200)
        assertTrue(step.isValid)
    }

    @Test
    fun markerStep_invalidWithNegativeCoords() {
        val step = ScenarioStep(id = "s1", type = StepType.MARKER, x = -1, y = 0)
        assertFalse(step.isValid)
    }

    @Test
    fun photoStep_requiresTemplateOrPath() {
        val empty = ScenarioStep(id = "p1", type = StepType.PHOTO)
        assertFalse(empty.isValid)
        val withTemplate = empty.copy(photoTemplateId = "tpl-1")
        assertTrue(withTemplate.isValid)
        val withPath = empty.copy(photoPath = "/data/a.png")
        assertTrue(withPath.isValid)
    }

    @Test
    fun textStep_requiresNonBlankText() {
        val blank = ScenarioStep(id = "t1", type = StepType.TEXT, text = "")
        assertFalse(blank.isValid)
        val ok = blank.copy(text = "OK")
        assertTrue(ok.isValid)
        val tooLong = blank.copy(text = "x".repeat(201))
        assertFalse(tooLong.isValid)
    }

    @Test
    fun waitStep_validWithinBounds() {
        val step = ScenarioStep(id = "w1", type = StepType.WAIT, waitMs = 500L)
        assertTrue(step.isValid)
        assertFalse(step.copy(waitMs = 10L).isValid)
        assertFalse(step.copy(waitMs = 700000L).isValid)
    }

    @Test
    fun step_invalidWhenIdBlank() {
        val step = ScenarioStep(id = "", type = StepType.MARKER, x = 1, y = 1)
        assertFalse(step.isValid)
    }

    @Test
    fun step_invalidWhenRepeatOrIntervalOutOfRange() {
        val base = ScenarioStep(id = "s", type = StepType.MARKER, x = 1, y = 1)
        assertFalse(base.copy(repeat = 0).isValid)
        assertFalse(base.copy(intervalMs = 10L).isValid)
        assertTrue(base.copy(repeat = 5, intervalMs = 1000L).isValid)
    }

    @Test
    fun summary_reflectsStepType() {
        val marker = ScenarioStep(id = "m", type = StepType.MARKER, x = 5, y = 9)
        assertTrue(marker.summary().contains("5") && marker.summary().contains("9"))
        val text = ScenarioStep(id = "t", type = StepType.TEXT, text = "Hello")
        assertTrue(text.summary().contains("Hello"))
        val wait = ScenarioStep(id = "w", type = StepType.WAIT, waitMs = 750L)
        assertTrue(wait.summary().contains("750"))
    }

    @Test
    fun scenario_validWhenAllStepsValid() {
        val scenario = Scenario(
            id = "sc1",
            name = "Test",
            steps = listOf(
                ScenarioStep(id = "a", type = StepType.MARKER, x = 1, y = 1),
                ScenarioStep(id = "b", type = StepType.TEXT, text = "Go"),
            ),
        )
        assertTrue(scenario.isValid)
    }

    @Test
    fun scenario_invalidWhenNameBlankOrTooManySteps() {
        val ok = Scenario(id = "sc", name = "Name")
        assertTrue(ok.isValid)
        assertFalse(ok.copy(name = "").isValid)
        val tooMany = ok.copy(steps = (1..51).map { ScenarioStep(id = "s$it", type = StepType.MARKER, x = 0, y = 0) })
        assertFalse(tooMany.isValid)
    }

    @Test
    fun scenario_invalidWhenLoopCountOutOfRange() {
        val base = Scenario(id = "sc", name = "Loop")
        assertFalse(base.copy(loopCount = 0).isValid)
        assertTrue(base.copy(loopCount = 10).isValid)
    }

    @Test
    fun tapsPerPass_sumsRepeatsExcludingWaits() {
        val scenario = Scenario(
            id = "sc",
            name = "Taps",
            steps = listOf(
                ScenarioStep(id = "a", type = StepType.MARKER, x = 1, y = 1, repeat = 3),
                ScenarioStep(id = "b", type = StepType.TEXT, text = "Go", repeat = 2),
                ScenarioStep(id = "c", type = StepType.WAIT, waitMs = 500L, repeat = 5),
            ),
        )
        assertEquals(5, scenario.tapsPerPass)
    }
}
