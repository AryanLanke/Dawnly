package com.dawnly.app.domain.wake

import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WakeTrackerTest {
    private val second = 1_000L
    private val minute = 60 * second

    @Test
    fun `starts with nothing`() {
        assertEquals(WakeSignals(), WakeTracker().signals(now = 0))
    }

    @Test
    fun `screen time runs from unlock until the screen goes off`() {
        val tracker = WakeTracker()
        tracker.onUnlocked(now = 10 * second)
        assertTrue(tracker.signals(now = 10 * second).unlocked)
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
        val signals = tracker.signals(now = 2 * minute)
        assertFalse(signals.unlocked)
        assertEquals(Duration.ZERO, signals.screenOnTime)
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
    fun `resumes from saved readings`() {
        val saved = WakeSignals(unlocked = true, screenOnTime = Duration.ofMinutes(3), steps = 12, stepsAvailable = false)
        val tracker = WakeTracker(resumeFrom = saved)
        assertEquals(saved, tracker.signals(now = 0))

        tracker.onUnlocked(now = 0)
        tracker.onStepCounter(1_000f)
        tracker.onStepCounter(1_008f)
        val signals = tracker.signals(now = minute)
        assertEquals(Duration.ofMinutes(4), signals.screenOnTime)
        assertEquals(20, signals.steps)
    }

    @Test
    fun `a quick time check at night doesn't count as awake`() {
        val tracker = WakeTracker()
        tracker.onUnlocked(now = 0)
        tracker.onScreenOff(now = 20 * second)
        assertFalse(WakeScore.of(tracker.signals(now = 30 * minute), WakeSignalSettings()).isAwake)

        tracker.onUnlocked(now = 30 * minute)
        assertTrue(WakeScore.of(tracker.signals(now = 35 * minute), WakeSignalSettings()).isAwake)
    }
}
