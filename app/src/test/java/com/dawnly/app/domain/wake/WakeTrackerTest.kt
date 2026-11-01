package com.dawnly.app.domain.wake

import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WakeTrackerTest {
    private val second = 1_000L
    private val minute = 60 * second
    private val nanosPerSecond = 1_000_000_000L
    private val gravity = 9.81f

    @Test
    fun `starts with nothing`() {
        assertEquals(WakeSignals(), WakeTracker().signals(now = 0))
    }

    @Test
    fun `screen time runs from unlock until the screen goes off`() {
        val tracker = WakeTracker()
        tracker.onUnlocked(now = 10 * second)
        assertEquals(Duration.ofSeconds(50), tracker.signals(now = minute).screenOnTime)

        tracker.onScreenOff(now = 2 * minute)
        assertEquals(Duration.ofSeconds(110), tracker.signals(now = 10 * minute).screenOnTime)
    }

    @Test
    fun `sessions add up`() {
        val tracker = WakeTracker()
        tracker.onUnlocked(now = 0)
        tracker.onScreenOff(now = 2 * minute)
        tracker.onUnlocked(now = 10 * minute)
        tracker.onScreenOff(now = 13 * minute)
        assertEquals(Duration.ofMinutes(5), tracker.signals(now = 20 * minute).screenOnTime)
    }

    @Test
    fun `a second unlock event doesn't restart the session`() {
        // Screen on with no lock screen, then USER_PRESENT for the same session.
        val tracker = WakeTracker()
        tracker.onUnlocked(now = 0)
        tracker.onUnlocked(now = 30 * second)
        assertEquals(Duration.ofMinutes(1), tracker.signals(now = minute).screenOnTime)
    }

    @Test
    fun `screen going off without an unlock adds nothing`() {
        // Lighting up the lock screen to check the time.
        val tracker = WakeTracker()
        tracker.onScreenOff(now = minute)
        assertEquals(Duration.ZERO, tracker.signals(now = 2 * minute).screenOnTime)
    }

    @Test
    fun `a clock running backwards never subtracts time`() {
        val tracker = WakeTracker()
        tracker.onUnlocked(now = minute)
        assertEquals(Duration.ZERO, tracker.signals(now = 0).screenOnTime)
        tracker.onScreenOff(now = 0)
        assertEquals(Duration.ZERO, tracker.signals(now = minute).screenOnTime)
    }

    @Test
    fun `opening different apps counts each one`() {
        val tracker = WakeTracker(ignoredApps = setOf("launcher"))
        tracker.onAppResumed("mail")
        tracker.onAppResumed("mail") // another screen of the same app
        tracker.onAppResumed("launcher")
        tracker.onAppResumed("news")
        tracker.onAppResumed("launcher")
        tracker.onAppResumed("mail") // back to it from the home screen
        assertEquals(3, tracker.signals(now = 0).appsOpened)
    }

    @Test
    fun `steps count from the first reading`() {
        val tracker = WakeTracker()
        tracker.onStepCounter(10_000f)
        assertEquals(0, tracker.signals(now = 0).steps)
        tracker.onStepCounter(10_042f)
        assertEquals(42, tracker.signals(now = 0).steps)
    }

    @Test
    fun `steps survive the counter resetting on reboot`() {
        val tracker = WakeTracker()
        tracker.onStepCounter(500f)
        tracker.onStepCounter(530f)
        tracker.onStepCounter(5f)
        tracker.onStepCounter(25f)
        assertEquals(50, tracker.signals(now = 0).steps)
    }

    @Test
    fun `steps taken while not counting are left out`() {
        val tracker = WakeTracker()
        tracker.onStepCounter(100f)
        tracker.onStepCounter(120f)
        tracker.onStepCounterStopped()
        assertEquals(20, tracker.signals(now = 0).steps)

        tracker.onStepCounter(300f)
        tracker.onStepCounter(310f)
        assertEquals(30, tracker.signals(now = 0).steps)
    }

    @Test
    fun `a phone lying still doesn't move`() {
        val tracker = WakeTracker()
        // Five samples a second for a minute, with a little sensor noise.
        for (i in 0 until 300) {
            val noise = if (i % 2 == 0) 0.05f else -0.05f
            tracker.onAcceleration(0.1f, 0.2f, gravity + noise, i * nanosPerSecond / 5)
        }
        assertEquals(Duration.ZERO, tracker.signals(now = 0).movingTime)
    }

    @Test
    fun `each second with movement counts once`() {
        val tracker = WakeTracker()
        tracker.onAcceleration(0f, 0f, gravity, 0)
        // Swinging while carried: 10 seconds from second 100, 5 samples a second.
        for (i in 0 until 50) {
            val swing = if (i % 2 == 0) 3f else -3f
            tracker.onAcceleration(swing, 0f, gravity + swing, 100 * nanosPerSecond + i * nanosPerSecond / 5)
        }
        assertEquals(Duration.ofSeconds(10), tracker.signals(now = 0).movingTime)
    }

    @Test
    fun `music time adds up between checks, not across long gaps`() {
        val tracker = WakeTracker()
        tracker.onMusicCheck(playing = true, now = 0)
        tracker.onMusicCheck(playing = true, now = 5 * second)
        tracker.onMusicCheck(playing = true, now = 10 * second)
        tracker.onMusicCheck(playing = false, now = 15 * second)
        // The process slept for 10 minutes: only a short stretch counts.
        tracker.onMusicCheck(playing = true, now = 15 * second + 10 * minute)
        assertEquals(Duration.ofSeconds(25), tracker.signals(now = 0).musicTime)
    }

    @Test
    fun `resumes from saved readings`() {
        val saved = WakeSignals(
            screenOnTime = Duration.ofMinutes(3),
            appsOpened = 2,
            steps = 12,
            stepsAvailable = false,
            movingTime = Duration.ofSeconds(40),
            musicTime = Duration.ofSeconds(90),
        )
        val tracker = WakeTracker(resumeFrom = saved)
        assertEquals(saved, tracker.signals(now = 0))

        tracker.onUnlocked(now = 0)
        tracker.onAppResumed("mail")
        tracker.onStepCounter(1_000f)
        tracker.onStepCounter(1_008f)
        val signals = tracker.signals(now = minute)
        assertEquals(Duration.ofMinutes(4), signals.screenOnTime)
        assertEquals(3, signals.appsOpened)
        assertEquals(20, signals.steps)
    }

    @Test
    fun `a quick time check at night doesn't count as awake`() {
        val settings = WakeSignalSettings()
        val tracker = WakeTracker()
        tracker.onUnlocked(now = 0)
        tracker.onScreenOff(now = 20 * second)
        assertFalse(WakeScore.of(tracker.signals(now = 30 * minute), settings).isAwake)

        tracker.onUnlocked(now = 30 * minute)
        assertTrue(WakeScore.of(tracker.signals(now = 35 * minute), settings).isAwake)
    }
}
