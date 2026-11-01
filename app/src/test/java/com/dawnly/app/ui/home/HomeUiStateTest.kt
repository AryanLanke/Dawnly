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
    private val allSigns = WakeSignalSettings(useUnlock = true, useScreenTime = true, useSteps = true)

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
            WakeSignalSettings(useUnlock = false, useScreenTime = true, useSteps = true, stepsGoal = 80),
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
        assertEquals(listOf(0, 0, 0), state.signals.map { it.current })
        assertFalse((state.status as HomeStatus.Watching).allSignsSeen)
    }

    @Test
    fun `screen time counts whole minutes against the goal`() {
        val live = WakeSignals(unlocked = true, screenOnTime = Duration.ofSeconds(4 * 60 + 59), steps = 42)
        val rows = HomeUiState.from(watching, live, allSigns, windowStart).signals
        assertEquals(SignalRow(SignalKind.Unlock, 1, 1), rows[0])
        assertEquals(SignalRow(SignalKind.ScreenTime, 4, 5), rows[1])
        assertEquals(SignalRow(SignalKind.Steps, 42, 50), rows[2])
        assertEquals(listOf(true, false, false), rows.map { it.met })
    }

    @Test
    fun `all signs seen only when every sign that is on has met its goal`() {
        val live = WakeSignals(unlocked = true, screenOnTime = Duration.ofMinutes(5), steps = 10)
        val withSteps = HomeUiState.from(watching, live, allSigns, windowStart).status as HomeStatus.Watching
        assertFalse(withSteps.allSignsSeen)

        val withoutSteps = HomeUiState.from(watching, live, allSigns.copy(useSteps = false), windowStart)
            .status as HomeStatus.Watching
        assertTrue(withoutSteps.allSignsSeen)
    }

    @Test
    fun `skipped alarms show no signals`() {
        val state = HomeUiState.from(WatchState.Skipped(alarm), WakeSignals(unlocked = true), allSigns, windowStart)
        assertEquals(HomeStatus.Skipped(alarm), state.status)
        assertTrue(state.signals.isEmpty())
    }
}
