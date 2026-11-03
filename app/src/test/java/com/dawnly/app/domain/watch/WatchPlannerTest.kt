package com.dawnly.app.domain.watch

import java.time.Duration
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class WatchPlannerTest {
    private val alarm = Instant.parse("2026-11-02T05:30:00Z")
    private val hour = Duration.ofHours(1)

    private fun decide(now: String, alarmAt: Instant? = alarm, armedFor: Instant? = alarm) =
        WatchPlanner.decide(alarmAt, hour, Instant.parse(now), armedFor)

    @Test
    fun `no alarm is idle`() {
        assertEquals(WatchState.Idle, decide("2026-11-02T04:00:00Z", alarmAt = null))
    }

    @Test
    fun `alarm that has passed is idle`() {
        assertEquals(WatchState.Idle, decide("2026-11-02T05:30:00Z"))
        assertEquals(WatchState.Idle, decide("2026-11-02T05:31:00Z"))
    }

    @Test
    fun `before the window it waits for the window start`() {
        val expected = WatchState.Waiting(alarm, Instant.parse("2026-11-02T04:30:00Z"))
        assertEquals(expected, decide("2026-11-01T23:00:00Z", armedFor = null))
        assertEquals(expected, decide("2026-11-02T04:29:59Z"))
    }

    @Test
    fun `armed alarm is watched from the window start`() {
        val expected = WatchState.Watching(alarm, Instant.parse("2026-11-02T04:30:00Z"))
        assertEquals(expected, decide("2026-11-02T04:30:00Z"))
        assertEquals(expected, decide("2026-11-02T05:29:00Z"))
    }

    @Test
    fun `alarm first seen inside its window is skipped`() {
        assertEquals(WatchState.Skipped(alarm), decide("2026-11-02T05:00:00Z", armedFor = null))
    }

    @Test
    fun `snoozed alarm is skipped even though an earlier alarm was armed`() {
        val snoozed = alarm + Duration.ofMinutes(10)
        assertEquals(
            WatchState.Skipped(snoozed),
            decide("2026-11-02T05:31:00Z", alarmAt = snoozed, armedFor = alarm),
        )
    }

    @Test
    fun `progress runs from 0 to 1 across the window`() {
        val watching = WatchState.Watching(alarm, Instant.parse("2026-11-02T04:30:00Z"))
        assertEquals(0f, watching.progress(Instant.parse("2026-11-02T04:00:00Z")))
        assertEquals(0.5f, watching.progress(Instant.parse("2026-11-02T05:00:00Z")))
        assertEquals(1f, watching.progress(Instant.parse("2026-11-02T06:00:00Z")))
    }

    @Test
    fun `custom windows are kept within bounds`() {
        assertEquals(WatchWindow.MIN, WatchWindow.clamp(Duration.ZERO))
        assertEquals(WatchWindow.MAX, WatchWindow.clamp(Duration.ofHours(8)))
        assertEquals(Duration.ofMinutes(45), WatchWindow.clamp(Duration.ofMinutes(45)))
    }

    @Test
    fun `the custom slider stops every minute from 5 to half an hour, then every 5`() {
        val stops = WatchWindow.SLIDER_STOPS.map { it.toMinutes() }
        assertEquals((5L..30L).toList(), stops.take(26))
        assertEquals(listOf(35L, 40L), stops.subList(26, 28))
        assertEquals(180L, stops.last())
    }
}
