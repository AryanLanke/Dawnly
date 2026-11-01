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
            WakeSignalSettings(useSteps = true, stepsGoal = 80, useMovement = false, useMusic = false),
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
        assertEquals(listOf(0, 0, 0, 0, 0), state.signals.map { it.current })
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
        )
        val rows = HomeUiState.from(watching, live, allSigns, windowStart).signals
        assertEquals(
            listOf(
                SignalRow(SignalKind.ScreenTime, 4, 5),
                SignalRow(SignalKind.PhoneUse, 3, 3),
                SignalRow(SignalKind.Steps, 42, 50),
                SignalRow(SignalKind.Movement, 1, 2),
                SignalRow(SignalKind.Music, 0, 1),
            ),
            rows,
        )
        assertEquals(listOf(false, true, false, false, false), rows.map { it.met })
    }

    @Test
    fun `awake follows the wake score, so Home and the prompt agree`() {
        // No single sign has met its goal, but 3 of 5 minutes and 20 of 50 steps add up to awake.
        val settings = WakeSignalSettings(useSteps = true, useMovement = false, useMusic = false)
        val live = WakeSignals(screenOnTime = Duration.ofMinutes(3), steps = 20)
        val state = HomeUiState.from(watching, live, settings, windowStart)
        assertFalse(state.signals.any { it.met })
        assertTrue((state.status as HomeStatus.Watching).awake)

        val less = HomeUiState.from(watching, live.copy(steps = 10), settings, windowStart)
        assertFalse((less.status as HomeStatus.Watching).awake)
    }

    @Test
    fun `skipped alarms show no signals`() {
        val state = HomeUiState.from(WatchState.Skipped(alarm), WakeSignals(steps = 80), allSigns, windowStart)
        assertEquals(HomeStatus.Skipped(alarm), state.status)
        assertTrue(state.signals.isEmpty())
    }
}
