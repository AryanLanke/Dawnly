package com.dawnly.app.ui.home

import com.dawnly.app.domain.wake.WakeSignalSettings
import com.dawnly.app.domain.wake.WakeSignals
import com.dawnly.app.domain.watch.WatchState
import java.time.Duration
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeUiStateTest {
    private val alarm = Instant.parse("2026-11-02T05:30:00Z")
    private val windowStart = alarm.minus(Duration.ofHours(1))
    private val watching = WatchState.Watching(alarm, windowStart)
    private val allSigns = WakeSignalSettings(usePhoneUse = true, useSteps = true)

    // Half an hour before the alarm the goals are as set (see Closeness).
    private val halfHourBefore = alarm.minus(Duration.ofMinutes(30))

    /** Activity that's still going and has lasted long enough to count. */
    private fun WakeSignals.going() = copy(stretch = WakeSignals.MIN_STRETCH, idle = Duration.ZERO)

    @Test
    fun `no alarm shows no signals`() {
        val state = HomeUiState.from(WatchState.Idle, null, allSigns, alarm)
        assertEquals(HomeStatus.NoAlarm, state.status)
        assertTrue(state.signals.isEmpty())
    }

    @Test
    fun `waiting shows only the goals of the signs that are on`() {
        val state = HomeUiState.from(
            WatchState.Waiting(alarm, windowStart),
            null,
            WakeSignalSettings(useSteps = true, stepsGoal = 80, useMovement = false, useMusic = false, useCharger = false),
            windowStart.minusSeconds(60),
        )
        assertEquals(
            listOf(
                SignalRow(SignalKind.ScreenTime, current = null, goal = 5),
                SignalRow(SignalKind.Steps, current = null, goal = 80),
            ),
            state.signals,
        )
        assertFalse(state.signals.any { it.met })
    }

    @Test
    fun `watching shows progress through the window`() {
        val halfway = windowStart.plus(Duration.ofMinutes(30))
        val status = HomeUiState.from(watching, WakeSignals(), allSigns, halfway).status as HomeStatus.Watching
        assertEquals(0.5f, status.progress, 0.001f)
        assertEquals(windowStart, status.startedAt)
    }

    @Test
    fun `watching before the service publishes counts nothing as seen`() {
        val state = HomeUiState.from(watching, null, allSigns, windowStart)
        assertEquals(listOf(0, 0, 0, 0, 0, 0), state.signals.map { it.current })
        assertFalse((state.status as HomeStatus.Watching).awake)
    }

    @Test
    fun `rows count whole minutes and counts against the goal`() {
        val live = WakeSignals(
            screenOnTime = Duration.ofSeconds(4 * 60 + 59),
            appsOpened = 3,
            steps = 42,
            movingTime = Duration.ofSeconds(70),
            musicTime = Duration.ofSeconds(59),
            chargerChanged = true,
        )
        val rows = HomeUiState.from(watching, live, allSigns, halfHourBefore).signals
        assertEquals(
            listOf(
                SignalRow(SignalKind.ScreenTime, 4, 5),
                SignalRow(SignalKind.PhoneUse, 3, 3),
                SignalRow(SignalKind.Steps, 42, 50),
                SignalRow(SignalKind.Movement, 1, 2),
                SignalRow(SignalKind.Charger, 1, 1),
                SignalRow(SignalKind.Music, 0, 1),
            ),
            rows,
        )
        assertEquals(listOf(false, true, false, false, true, false), rows.map { it.met })
    }

    @Test
    fun `awake follows the wake score, so Home and the prompt agree`() {
        // No single sign has met its goal, but 3 of 5 minutes and 20 of 50 steps add up to awake.
        val settings = WakeSignalSettings(useSteps = true, useMovement = false, useMusic = false, useCharger = false)
        val live = WakeSignals(screenOnTime = Duration.ofMinutes(3), steps = 20).going()
        val state = HomeUiState.from(watching, live, settings, halfHourBefore)
        assertFalse(state.signals.any { it.met })
        assertTrue((state.status as HomeStatus.Watching).awake)

        val less = HomeUiState.from(watching, live.copy(steps = 10), settings, halfHourBefore)
        assertFalse((less.status as HomeStatus.Watching).awake)

        // Enough, but it stopped: back to bed.
        val stopped = HomeUiState.from(watching, live.copy(idle = Duration.ofMinutes(10)), settings, halfHourBefore)
        assertFalse((stopped.status as HomeStatus.Watching).awake)
    }

    @Test
    fun `the signs bar shows the wake score, full once awake`() {
        val settings = WakeSignalSettings(useSteps = true, useMovement = false, useMusic = false)
        val live = WakeSignals(screenOnTime = Duration.ofMinutes(3), steps = 10)
        // 3 of 5 minutes (0.6) plus 10 of 50 steps (0.2).
        val partly = HomeUiState.from(watching, live, settings, halfHourBefore).status as HomeStatus.Watching
        assertEquals(0.8f, partly.wakeProgress, 0.001f)

        // Past the goal the bar stays full rather than overflowing.
        val more = live.copy(screenOnTime = Duration.ofMinutes(9), steps = 80)
        val full = HomeUiState.from(watching, more, settings, halfHourBefore).status as HomeStatus.Watching
        assertEquals(1f, full.wakeProgress, 0.001f)
    }

    @Test
    fun `skipped alarms show no signals`() {
        val state = HomeUiState.from(WatchState.Skipped(alarm), WakeSignals(steps = 80), allSigns, windowStart)
        assertEquals(HomeStatus.Skipped(alarm), state.status)
        assertTrue(state.signals.isEmpty())
    }

    @Test
    fun `after Not yet in this window, Today knows, and an older one doesn't count`() {
        val now = windowStart.plus(Duration.ofMinutes(20))
        val up = WakeSignals(screenOnTime = Duration.ofMinutes(6), movingTime = Duration.ofMinutes(2)).going()
        val said = HomeUiState.from(watching, up, allSigns, now, notYetAt = now.minusSeconds(60)).status as HomeStatus.Watching
        assertTrue(said.awake)
        assertTrue(said.notYet)
        val yesterday = HomeUiState.from(watching, up, allSigns, now, notYetAt = windowStart.minus(Duration.ofDays(1)))
        assertFalse((yesterday.status as HomeStatus.Watching).notYet)
    }

    @Test
    fun `the same activity counts for more close to the alarm`() {
        val settings = WakeSignalSettings(useMovement = false, useMusic = false, useCharger = false)
        val live = WakeSignals(screenOnTime = Duration.ofMinutes(4)).going()
        // 4 of 5 minutes, with the goal at 0.8 of itself 10 minutes before: awake.
        val near = HomeUiState.from(watching, live, settings, alarm.minus(Duration.ofMinutes(10))).status as HomeStatus.Watching
        assertTrue(near.awake)
        // The same an hour before, when the goal is higher: not yet.
        val far = HomeUiState.from(watching, live, settings, windowStart).status as HomeStatus.Watching
        assertFalse(far.awake)
    }

    @Test
    fun `waiting shows the goals as set, however far the alarm`() {
        val state = HomeUiState.from(WatchState.Waiting(alarm, windowStart), null, allSigns, alarm.minus(Duration.ofHours(5)))
        assertEquals(5, state.signals.first { it.kind == SignalKind.ScreenTime }.goal)
    }
}
