package com.dawnly.app.domain.wake

import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WakeScoreTest {
    private val defaults = WakeSignalSettings()
    private val allOn = WakeSignalSettings(useSteps = true)

    private fun signals(unlocked: Boolean = true, minutes: Long = 0, seconds: Long = 0, steps: Int = 0) =
        WakeSignals(unlocked, Duration.ofMinutes(minutes).plusSeconds(seconds), steps)

    @Test
    fun `nothing happened is not awake`() {
        val score = WakeScore.of(WakeSignals(), defaults)
        assertFalse(score.isAwake)
        assertEquals(0f, score.value)
    }

    @Test
    fun `a quick time check is not awake`() {
        val score = WakeScore.of(signals(seconds = 30), defaults)
        assertFalse(score.isAwake)
        assertEquals(0.55f, score.value, 0.001f)
    }

    @Test
    fun `unlocked and used for the goal is awake`() {
        assertFalse(WakeScore.of(signals(minutes = 4, seconds = 59), defaults).isAwake)
        assertTrue(WakeScore.of(signals(minutes = 5), defaults).isAwake)
        assertTrue(WakeScore.of(signals(minutes = 40), defaults).isAwake)
    }

    @Test
    fun `every counted signal has to reach its goal`() {
        assertFalse(WakeScore.of(signals(minutes = 10, steps = 49), allOn).isAwake)
        assertFalse(WakeScore.of(signals(unlocked = false, minutes = 10, steps = 80), allOn).isAwake)
        assertTrue(WakeScore.of(signals(minutes = 5, steps = 50), allOn).isAwake)
    }

    @Test
    fun `progress is capped at 1 per signal`() {
        val score = WakeScore.of(signals(minutes = 50, steps = 25), allOn)
        assertEquals(1f, score.screenTime)
        assertEquals(0.5f, score.steps)
        assertEquals((1f + 1f + 0.5f) / 3, score.value, 0.001f)
    }

    @Test
    fun `signals switched off are ignored`() {
        val unlockOnly = WakeSignalSettings(useUnlock = true, useScreenTime = false)
        val score = WakeScore.of(signals(minutes = 0, steps = 0), unlockOnly)
        assertNull(score.screenTime)
        assertNull(score.steps)
        assertTrue(score.isAwake)

        val stepsOnly = WakeSignalSettings(useUnlock = false, useScreenTime = false, useSteps = true)
        assertFalse(WakeScore.of(signals(minutes = 30, steps = 10), stepsOnly).isAwake)
        assertTrue(WakeScore.of(signals(unlocked = false, steps = 50), stepsOnly).isAwake)
    }

    @Test
    fun `steps don't count when they can't be read`() {
        val noSensor = signals(minutes = 5, steps = 0).copy(stepsAvailable = false)
        val score = WakeScore.of(noSensor, allOn)
        assertNull(score.steps)
        assertTrue(score.isAwake)
    }

    @Test
    fun `with no signal counted it is never awake`() {
        val noneOn = WakeSignalSettings(useUnlock = false, useScreenTime = false, useSteps = false)
        assertFalse(WakeScore.of(signals(minutes = 60, steps = 500), noneOn).isAwake)

        val onlyUnavailableSteps = noneOn.copy(useSteps = true)
        assertFalse(WakeScore.of(signals(steps = 500).copy(stepsAvailable = false), onlyUnavailableSteps).isAwake)
    }

    @Test
    fun `a goal of zero is met straight away`() {
        val zeroGoals = WakeSignalSettings(screenTimeGoal = Duration.ZERO, useSteps = true, stepsGoal = 0)
        assertTrue(WakeScore.of(signals(), zeroGoals).isAwake)
    }
}
