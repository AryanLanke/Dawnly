package com.dawnly.app.domain.time

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimeStateTest {
    private fun at(ms: Long, boot: Int = 7) = TimeReading(ms, boot)
    private fun timer() = TimerState().start(5_000L, at(10_000L))

    @Test fun `five second timer is based on elapsed realtime not ticks`() {
        val timer = timer()
        assertEquals(5_000L, timer.remaining(at(10_000)))
        assertEquals(3_877L, timer.remaining(at(11_123)))
        assertEquals(0L, timer.remaining(at(100_000)))
    }

    @Test fun `pause freezes and resume uses only subsequent running time`() {
        val paused = timer().pause(at(12_000))
        assertEquals(TimerPhase.Paused, paused.phase)
        assertEquals(3_000L, paused.remaining(at(800_000)))
        val resumed = paused.resume(at(900_000))
        assertEquals(1_500L, resumed.remaining(at(901_500)))
        assertEquals(TimerPhase.Finished, resumed.checkpoint(at(903_000)).phase)
    }

    @Test fun `lifecycle checkpoints retain deadline and alarm generation`() {
        val initial = timer()
        val saved = initial.checkpoint(at(12_000))
        assertEquals(initial.generation, saved.generation)
        assertEquals(initial.anchorMs + initial.remainingMs, saved.anchorMs + saved.remainingMs)
        // A fresh object has the same behavior after process recreation; no UI ticks were needed.
        val restored = saved.copy()
        assertEquals(1_000L, restored.remaining(at(14_000)))
        assertTrue(restored.acceptsAlarm(initial.generation, at(15_000)))
    }

    @Test fun `finish creates one durable completion and remains finished`() {
        val timer = timer()
        val finished = timer.checkpoint(at(15_000))
        assertTrue(finished.completionPending)
        assertEquals(finished, finished.checkpoint(at(20_000)))
        assertFalse(finished.acceptsAlarm(timer.generation, at(20_000)))
        val delivered = finished.completionDelivered()
        assertFalse(delivered.completionPending)
        assertEquals(delivered, delivered.checkpoint(at(30_000)))
    }

    @Test fun `pausing at the deadline does not swallow completion`() {
        val paused = timer().pause(at(15_000))
        assertEquals(TimerPhase.Finished, paused.phase)
        assertTrue(paused.completionPending)
    }

    @Test fun `pause reset and replacement invalidate stale receivers`() {
        val first = timer()
        val paused = first.pause(at(12_000))
        assertFalse(paused.acceptsAlarm(first.generation, at(20_000)))
        val resumed = paused.resume(at(20_000))
        assertFalse(resumed.acceptsAlarm(first.generation, at(23_000)))
        assertTrue(resumed.acceptsAlarm(resumed.generation, at(23_000)))
        val reset = resumed.reset()
        assertFalse(reset.acceptsAlarm(resumed.generation, at(25_000)))
        assertEquals(5_000L, reset.remainingMs)
        val replacement = reset.start(1_000, at(25_000))
        assertFalse(replacement.acceptsAlarm(first.generation, at(26_000)))
        assertFalse(replacement.acceptsAlarm(resumed.generation, at(26_000)))
        assertTrue(replacement.acceptsAlarm(replacement.generation, at(26_000)))
    }

    @Test fun `early delivery never completes the timer`() {
        val timer = timer()
        assertFalse(timer.acceptsAlarm(timer.generation, at(14_999)))
        assertTrue(timer.acceptsAlarm(timer.generation, at(15_000)))
    }

    @Test fun `reset discards pending completion and keeps chosen duration`() {
        val reset = timer().checkpoint(at(15_000)).reset()
        assertEquals(TimerPhase.Ready, reset.phase)
        assertEquals(5_000L, reset.durationMs)
        assertFalse(reset.completionPending)
    }

    @Test fun `reboot pauses at last checkpoint even when new uptime is greater`() {
        val saved = timer().checkpoint(at(12_000))
        for (newUptime in listOf(100L, 999_999L)) {
            val rebooted = saved.checkpoint(at(newUptime, 8))
            assertEquals(TimerPhase.Paused, rebooted.phase)
            assertEquals(3_000L, rebooted.remainingMs)
            assertTrue(rebooted.pausedAfterReboot)
            assertFalse(rebooted.completionPending)
            assertFalse(rebooted.acceptsAlarm(saved.generation, at(newUptime, 8)))
            val resumed = rebooted.resume(at(newUptime, 8))
            assertFalse(resumed.pausedAfterReboot)
            assertEquals(2_000L, resumed.remaining(at(newUptime + 1000, 8)))
        }
    }

    @Test fun `unknown boot count or backwards monotonic reading fails closed`() {
        assertEquals(TimerPhase.Paused, timer().checkpoint(at(11_000, -1)).phase)
        assertEquals(TimerPhase.Paused, timer().checkpoint(at(9_999)).phase)
        val unknown = TimerState().start(5_000, at(10_000, -1))
        assertTrue(unknown.checkpoint(at(11_000, -1)).pausedAfterReboot)
    }

    @Test fun `paused timer stays paused across reboot without losing remaining time`() {
        val paused = timer().pause(at(12_000))
        assertEquals(paused, paused.checkpoint(at(1_000, 8)))
        assertEquals(2_000L, paused.resume(at(1_000, 8)).remaining(at(2_000, 8)))
    }

    @Test fun `repeated start pause resume do not restart an active timer`() {
        val running = timer()
        assertEquals(running, running.start(8_000, at(11_000)))
        assertEquals(running, running.resume(at(11_000)))
        val paused = running.pause(at(12_000))
        assertEquals(paused, paused.pause(at(13_000)))
        assertEquals(paused, paused.start(8_000, at(13_000)))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `zero timer duration rejected`() { TimerState().start(0, at(1)) }

    @Test(expected = IllegalArgumentException::class)
    fun `oversized timer duration rejected`() { TimerState().start(TimerState.MAX_DURATION_MS + 1, at(1)) }

    @Test fun `maximum timer duration works`() {
        assertEquals(TimerState.MAX_DURATION_MS, TimerState().start(TimerState.MAX_DURATION_MS, at(1)).remainingMs)
    }

    @Test fun `stopwatch counts background and process recreation without callbacks`() {
        val running = StopwatchState().start(at(10_000))
        assertEquals(1_234L, running.elapsed(at(11_234)))
        val saved = running.checkpoint(at(12_000)).copy()
        assertEquals(90_000L, saved.elapsed(at(100_000)))
    }

    @Test fun `stopwatch pause resume does not include paused interval`() {
        val paused = StopwatchState().start(at(1_000)).pause(at(2_500))
        assertEquals(1_500L, paused.elapsed(at(100_000)))
        val resumed = paused.start(at(100_000))
        assertEquals(2_000L, resumed.elapsed(at(100_500)))
        assertEquals(StopwatchState(), resumed.reset())
    }

    @Test fun `stopwatch laps are cumulative and exclude pauses`() {
        val first = StopwatchState().start(at(1_000)).lap(at(2_000))
        val second = first.lap(at(3_500))
        val paused = second.pause(at(4_000))
        assertEquals(paused, paused.lap(at(10_000)))
        val third = paused.start(at(10_000)).lap(at(11_000))
        assertEquals(listOf(1_000L, 2_500L, 4_000L), third.laps)
        assertEquals(1_500L, third.laps[2] - third.laps[1])
    }

    @Test fun `duplicate zero and excess laps are ignored`() {
        val start = StopwatchState().start(at(1_000))
        assertTrue(start.lap(at(1_000)).laps.isEmpty())
        val first = start.lap(at(2_000))
        assertEquals(first, first.lap(at(2_000)))
        var many = start
        repeat(150) { many = many.lap(at(2_000 + it * 1000L)) }
        assertEquals(StopwatchState.MAX_LAPS, many.laps.size)
        assertTrue(many.running)
    }

    @Test fun `stopwatch reboot pauses saved elapsed and keeps laps`() {
        val saved = StopwatchState().start(at(1_000)).lap(at(2_000)).checkpoint(at(4_000))
        val rebooted = saved.checkpoint(at(100_000, 8))
        assertFalse(rebooted.running)
        assertTrue(rebooted.pausedAfterReboot)
        assertEquals(3_000L, rebooted.elapsedMs)
        assertEquals(saved.laps, rebooted.laps)
        val resumed = rebooted.start(at(100_000, 8))
        assertEquals(4_000L, resumed.elapsed(at(101_000, 8)))
        assertFalse(resumed.pausedAfterReboot)
    }

    @Test fun `stopwatch unknown boot or backwards timestamp pauses safely`() {
        val saved = StopwatchState().start(at(5_000)).checkpoint(at(6_000))
        assertFalse(saved.checkpoint(at(4_000)).running)
        assertFalse(saved.checkpoint(at(7_000, -1)).running)
        assertEquals(1_000L, saved.checkpoint(at(4_000)).elapsedMs)
    }

    @Test fun `timer and stopwatch run and reset independently`() {
        val both = TimeToolsState(timer(), StopwatchState().start(at(10_000)))
        val finished = both.checkpoint(at(16_000))
        assertEquals(TimerPhase.Finished, finished.timer.phase)
        assertTrue(finished.stopwatch.running)
        assertEquals(6_000L, finished.stopwatch.elapsedMs)
        assertEquals(finished.stopwatch, finished.copy(timer = finished.timer.reset()).stopwatch)
    }
}
