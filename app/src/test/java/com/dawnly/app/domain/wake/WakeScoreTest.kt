package com.dawnly.app.domain.wake

import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WakeScoreTest {
    private val defaults = WakeSignalSettings()
    private val allOn = WakeSignalSettings(usePhoneUse = true, useSteps = true)
    private val screenOnly = WakeSignalSettings(useMovement = false, useMusic = false)

    private fun signals(
        screen: Duration = Duration.ZERO,
        apps: Int = 0,
        steps: Int = 0,
        moving: Duration = Duration.ZERO,
        music: Duration = Duration.ZERO,
    ) = WakeSignals(screenOnTime = screen, appsOpened = apps, steps = steps, movingTime = moving, musicTime = music)

    private fun minutes(n: Long) = Duration.ofMinutes(n)
    private fun seconds(n: Long) = Duration.ofSeconds(n)

    @Test
    fun `nothing happened is not awake`() {
        val score = WakeScore.of(WakeSignals(), defaults)
        assertFalse(score.isAwake)
        assertEquals(0f, score.value)
    }

    @Test
    fun `a quick time check is not awake`() {
        val score = WakeScore.of(signals(screen = seconds(30), moving = seconds(3)), defaults)
        assertFalse(score.isAwake)
        assertEquals(0.125f, score.value, 0.001f)
    }

    @Test
    fun `any one sign reaching its goal is awake`() {
        assertFalse(WakeScore.of(signals(screen = seconds(299)), screenOnly).isAwake)
        assertTrue(WakeScore.of(signals(screen = minutes(5)), screenOnly).isAwake)
        assertTrue(WakeScore.of(signals(apps = 3), allOn).isAwake)
        assertTrue(WakeScore.of(signals(steps = 50), allOn).isAwake)
        assertTrue(WakeScore.of(signals(moving = minutes(2)), defaults).isAwake)
    }

    @Test
    fun `signs add up`() {
        // 3 of 5 minutes on screen and 20 of 50 steps.
        val score = WakeScore.of(signals(screen = minutes(3), steps = 20), allOn.copy(useMovement = false))
        assertEquals(1f, score.total, 0.001f)
        assertTrue(score.isAwake)
        assertFalse(WakeScore.of(signals(screen = minutes(2), steps = 20), allOn.copy(useMovement = false)).isAwake)
    }

    @Test
    fun `music alone never counts`() {
        val score = WakeScore.of(signals(music = minutes(40)), defaults)
        assertEquals(0f, score.musicShare)
        assertFalse(score.isAwake)

        val noMovement = defaults.copy(useMovement = false)
        assertFalse(WakeScore.of(signals(music = minutes(40), moving = minutes(10)), noMovement).isAwake)
    }

    @Test
    fun `music adds at most as much as movement`() {
        // Half the movement goal plus music makes 1.
        val half = WakeScore.of(signals(moving = minutes(1), music = minutes(5)), defaults)
        assertEquals(0.5f, half.musicShare)
        assertTrue(half.isAwake)

        val little = WakeScore.of(signals(moving = seconds(30), music = minutes(5)), defaults)
        assertEquals(0.25f, little.musicShare!!, 0.001f)
        assertFalse(little.isAwake)
    }

    @Test
    fun `progress is capped at 1 per sign`() {
        val score = WakeScore.of(signals(screen = minutes(50), steps = 25), allOn)
        assertEquals(1f, score.screenTime)
        assertEquals(0.5f, score.steps)
        assertEquals(1f, score.value)
    }

    @Test
    fun `signs switched off are ignored`() {
        val stepsOnly = WakeSignalSettings(useScreenTime = false, useSteps = true, useMovement = false, useMusic = false)
        val score = WakeScore.of(signals(screen = minutes(30), moving = minutes(30), steps = 10), stepsOnly)
        assertNull(score.screenTime)
        assertNull(score.movement)
        assertFalse(score.isAwake)
        assertTrue(WakeScore.of(signals(steps = 50), stepsOnly).isAwake)
    }

    @Test
    fun `signs that can't be read don't count`() {
        val unavailable = signals(steps = 500, apps = 9, moving = minutes(9))
            .copy(stepsAvailable = false, appsAvailable = false, movementAvailable = false)
        val score = WakeScore.of(unavailable, allOn)
        assertNull(score.steps)
        assertNull(score.phoneUse)
        assertNull(score.movement)
        assertFalse(score.isAwake)
    }

    @Test
    fun `with no sign counted it is never awake`() {
        val noneOn = WakeSignalSettings(useScreenTime = false, useMovement = false, useMusic = false)
        assertFalse(WakeScore.of(signals(screen = minutes(60), steps = 500), noneOn).isAwake)
        assertFalse(noneOn.canNotice)
    }

    @Test
    fun `music tipping it over is flagged, so Dawnly asks`() {
        // Half the movement goal plus music: awake only because of the music.
        val tipped = WakeScore.of(signals(moving = minutes(1), music = minutes(5)), defaults)
        assertTrue(tipped.isAwake)
        assertTrue(tipped.musicTippedIt)
    }

    @Test
    fun `music doesn't flag a wake the other signs reach alone`() {
        val screen = WakeScore.of(signals(screen = minutes(5), music = minutes(5)), defaults)
        assertTrue(screen.isAwake)
        assertFalse(screen.musicTippedIt)
        assertFalse(WakeScore.of(signals(moving = minutes(1), music = minutes(5)), defaults.copy(useMusic = false)).musicTippedIt)
    }
}
