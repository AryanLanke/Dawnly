package com.dawnly.app.domain.wake

import com.dawnly.app.domain.learn.SignWeights
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
    private val anyOne = allOn.copy(signsNeeded = 1)

    private fun signals(
        screen: Duration = Duration.ZERO,
        inUseFor: Duration = Duration.ZERO,
        sleepMediaPlaying: Boolean = false,
        apps: Int = 0,
        steps: Int = 0,
        moving: Duration = Duration.ZERO,
        music: Duration = Duration.ZERO,
        chargerChanged: Boolean = false,
        // Still going and lasted long enough, unless a test says otherwise.
        stretch: Duration = WakeSignals.MIN_STRETCH,
        idle: Duration? = Duration.ZERO,
    ) = WakeSignals(
        screenOnTime = screen,
        inUseFor = inUseFor,
        sleepMediaPlaying = sleepMediaPlaying,
        appsOpened = apps,
        steps = steps,
        movingTime = moving,
        musicTime = music,
        chargerChanged = chargerChanged,
        stretch = stretch,
        idle = idle,
    )

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
    fun `one long uninterrupted phone session is enough on its own`() {
        // The morning that got missed: up early, in one app for minutes, no steps, no walking.
        val scrolling = signals(screen = minutes(5), inUseFor = minutes(5), stretch = minutes(5))
        assertEquals(1, WakeScore.of(scrolling, defaults).kindsShown)
        assertTrue(WakeScore.of(scrolling, defaults).isAwake)
        // Even when the user asked for three kinds of sign.
        assertTrue(WakeScore.of(scrolling, defaults.copy(signsNeeded = 3)).isAwake)
    }

    @Test
    fun `learning screen time for less doesn't stop a long session counting`() {
        // Answers that taught Dawnly to distrust screen use used to cap the total below 1, which
        // would have kept the very morning this rule is for from ever counting.
        val scrolling = signals(screen = minutes(5), inUseFor = minutes(5), stretch = minutes(5))
        val score = WakeScore.of(scrolling, defaults, SignWeights(screen = 0.7f))
        assertTrue(score.isAwake)
        assertEquals(1f, score.value, 0.001f)
        assertFalse(WakeScore.of(scrolling.copy(inUseFor = minutes(1)), defaults, SignWeights(screen = 0.7f)).isAwake)
    }

    @Test
    fun `short sessions on the phone, added up, still need another sign`() {
        // Six minutes in total, but the screen timed out between: the current session is a minute.
        val interrupted = signals(screen = minutes(6), inUseFor = minutes(1))
        assertEquals(1, WakeScore.of(interrupted, defaults).kindsShown)
        assertFalse(WakeScore.of(interrupted, defaults).isAwake)
    }

    @Test
    fun `sleep media keeps a long screen session from counting alone`() {
        // Asleep over a video: the screen stays on without being touched.
        val video = signals(screen = minutes(30), inUseFor = minutes(30), stretch = minutes(30), sleepMediaPlaying = true)
        assertFalse(WakeScore.of(video, defaults).isAwake)
        // But music started after waking is a sign of its own, so the pair still counts.
        assertTrue(WakeScore.of(video.copy(musicTime = minutes(1)), defaults).isAwake)
    }

    @Test
    fun `by default one kind of sign alone is not enough`() {
        assertFalse(WakeScore.of(signals(screen = minutes(30)), defaults).isAwake)
        assertFalse(WakeScore.of(signals(moving = minutes(30)), defaults).isAwake)
        assertFalse(WakeScore.of(signals(steps = 500), allOn).isAwake)
        // Screen time and apps opened are both phone use: one kind.
        assertFalse(WakeScore.of(signals(screen = minutes(5), apps = 9), allOn).isAwake)
    }

    @Test
    fun `two kinds of sign together are awake`() {
        val score = WakeScore.of(signals(screen = minutes(3), moving = minutes(1)), defaults)
        assertEquals(2, score.kindsShown)
        assertTrue(score.isAwake)
        assertTrue(WakeScore.of(signals(steps = 30, apps = 2), allOn).isAwake)
    }

    @Test
    fun `a sign barely showing doesn't count as a kind`() {
        // A full screen goal plus a few seconds of movement: only one kind clearly showed.
        val score = WakeScore.of(signals(screen = minutes(5), moving = seconds(20)), defaults)
        assertEquals(1, score.kindsShown)
        assertFalse(score.isAwake)
    }

    @Test
    fun `any one sign reaching its goal is awake when one kind is enough`() {
        assertFalse(WakeScore.of(signals(screen = seconds(299)), anyOne).isAwake)
        assertTrue(WakeScore.of(signals(screen = minutes(5)), anyOne).isAwake)
        assertTrue(WakeScore.of(signals(apps = 3), anyOne).isAwake)
        assertTrue(WakeScore.of(signals(steps = 50), anyOne).isAwake)
        assertTrue(WakeScore.of(signals(moving = minutes(2)), anyOne).isAwake)
    }

    @Test
    fun `three kinds can be asked for`() {
        val three = allOn.copy(signsNeeded = 3)
        assertFalse(WakeScore.of(signals(screen = minutes(3), steps = 30), three).isAwake)
        assertTrue(WakeScore.of(signals(screen = minutes(3), steps = 30, moving = minutes(1)), three).isAwake)
    }

    @Test
    fun `never asks for more kinds than can count`() {
        // Only screen time can count: one kind is all there is.
        val alone = screenOnly.copy(useCharger = false, signsNeeded = 3)
        assertEquals(1, WakeScore.of(signals(), alone).kindsNeeded)
        assertTrue(WakeScore.of(signals(screen = minutes(5)), alone).isAwake)
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
        val score = WakeScore.of(signals(music = minutes(40)), defaults.copy(signsNeeded = 1))
        assertEquals(1f, score.music)
        assertFalse(score.isAwake)
    }

    @Test
    fun `music started after waking counts as a kind`() {
        // Music on and half the movement goal: two kinds, and together they make 1.
        val score = WakeScore.of(signals(moving = minutes(1), music = minutes(1)), defaults)
        assertEquals(2, score.kindsShown)
        assertTrue(score.isAwake)
        assertFalse(WakeScore.of(signals(moving = minutes(1), music = minutes(1)), defaults.copy(useMusic = false)).isAwake)
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
        val stepsOnly = WakeSignalSettings(useScreenTime = false, useSteps = true, useMovement = false, useMusic = false, signsNeeded = 1)
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
    fun `a charger change adds to the other signs but never counts alone`() {
        val alone = WakeScore.of(signals(chargerChanged = true), defaults)
        assertEquals(WakeScore.CHARGER_SHARE, alone.charger)
        assertFalse(alone.isAwake)

        // Plugged in or unplugged, plus 3 of 5 minutes on screen: two kinds.
        assertTrue(WakeScore.of(signals(chargerChanged = true, screen = minutes(3)), screenOnly).isAwake)
        assertFalse(WakeScore.of(signals(screen = minutes(3)), screenOnly).isAwake)
    }

    @Test
    fun `the charger switched off doesn't count`() {
        val score = WakeScore.of(signals(chargerChanged = true, screen = minutes(3)), screenOnly.copy(useCharger = false))
        assertNull(score.charger)
        assertFalse(score.isAwake)
    }

    @Test
    fun `one burst that stopped is not awake`() {
        // Steps and screen reached, but nothing for 10 minutes since: back to bed.
        val stopped = WakeScore.of(signals(steps = 50, screen = minutes(2), stretch = minutes(1), idle = minutes(10)), allOn)
        assertEquals(1f, stopped.value)
        assertFalse(stopped.isAwake)
    }

    @Test
    fun `activity has to last a few minutes before it counts`() {
        val short = WakeScore.of(signals(steps = 50, screen = minutes(2), stretch = minutes(1), idle = Duration.ZERO), allOn)
        assertFalse(short.isAwake)
        val lasted = WakeScore.of(signals(steps = 50, screen = minutes(2), stretch = minutes(3), idle = seconds(20)), allOn)
        assertTrue(lasted.isAwake)
    }

    @Test
    fun `with no activity seen it is never awake`() {
        assertFalse(WakeScore.of(signals(screen = minutes(9), chargerChanged = true, idle = null), screenOnly).isAwake)
    }
}
