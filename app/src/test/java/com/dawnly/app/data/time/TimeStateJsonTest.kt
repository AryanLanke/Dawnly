package com.dawnly.app.data.time

import com.dawnly.app.domain.time.StopwatchState
import com.dawnly.app.domain.time.TimeToolsState
import com.dawnly.app.domain.time.TimerPhase
import com.dawnly.app.domain.time.TimerState
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The saved timers must survive an update, including the single timer from before multiple timers. */
class TimeStateJsonTest {

    @Test fun `version 1 single timer migrates to a one element list with id 1`() {
        val v1 = JSONObject().apply {
            put("version", 1)
            put("timer", JSONObject().apply {
                put("duration", 60_000L)
                put("remaining", 30_000L)
                put("phase", "Paused")
                put("anchor", 0L)
                put("boot", 7)
                put("generation", 3L)
                put("reboot", false)
                put("pending", false)
            })
            put("stopwatch", JSONObject().apply {
                put("elapsed", 5_000L)
                put("running", false)
                put("anchor", 0L)
                put("boot", 7)
                put("laps", JSONArray())
                put("reboot", false)
            })
        }.toString()
        val state = TimeStateJson.decode(v1)
        assertEquals(1, state.timers.size)
        val timer = state.timers.single()
        assertEquals(1L, timer.id)
        assertEquals(60_000L, timer.durationMs)
        assertEquals(30_000L, timer.remainingMs)
        assertEquals(TimerPhase.Paused, timer.phase)
        assertEquals(3L, timer.generation)
        assertEquals(7, timer.bootCount)
        assertEquals("Timer", timer.label)
        assertNull(timer.sound)
        assertFalse(timer.ringOnFinish)
        assertEquals(5_000L, state.stopwatch.elapsedMs)
    }

    @Test fun `version 1 running timer keeps its anchor boot and generation`() {
        val v1 = JSONObject().apply {
            put("version", 1)
            put("timer", JSONObject().apply {
                put("duration", 10_000L)
                put("remaining", 4_000L)
                put("phase", "Running")
                put("anchor", 6_000L)
                put("boot", 9)
                put("generation", 2L)
                put("reboot", false)
                put("pending", false)
            })
        }.toString()
        val timer = TimeStateJson.decode(v1).timers.single()
        assertEquals(1L, timer.id)
        assertEquals(TimerPhase.Running, timer.phase)
        assertEquals(6_000L, timer.anchorMs)
        assertEquals(9, timer.bootCount)
        assertEquals(2L, timer.generation)
        assertEquals(4_000L, timer.remainingMs)
    }

    @Test fun `multiple timers round trip with every field`() {
        val timers = listOf(
            TimerState(id = 1L, label = "Tea", durationMs = 60_000L, remainingMs = 60_000L, phase = TimerPhase.Ready, sound = "Bubble"),
            TimerState(
                id = 2L, label = "Run", durationMs = 30_000L, remainingMs = 15_000L, phase = TimerPhase.Running,
                anchorMs = 10_000L, bootCount = 7, generation = 2L, sound = null, ringOnFinish = true,
            ),
            TimerState(
                id = 3L, label = "Done", durationMs = 10_000L, remainingMs = 0L, phase = TimerPhase.Finished,
                generation = 5L, completionPending = true, sound = "Flute", ringOnFinish = true,
            ),
        )
        val stopwatch = StopwatchState(elapsedMs = 1_234L, running = false, anchorMs = 0L, bootCount = 7, laps = listOf(500L, 1_234L))
        val state = TimeToolsState(timers, stopwatch)
        assertEquals(state, TimeStateJson.decode(TimeStateJson.encode(state)))
    }

    @Test fun `an empty timer list round trips as empty`() {
        val state = TimeToolsState(timers = emptyList())
        assertTrue(TimeStateJson.decode(TimeStateJson.encode(state)).timers.isEmpty())
    }

    @Test fun `each timer is validated and a bad entry falls back to a default with its own id`() {
        val json = JSONObject().apply {
            put("version", 2)
            put("timers", JSONArray().apply {
                put(JSONObject().apply {
                    put("id", 5L); put("duration", 1_000L); put("remaining", 1_000L)
                    put("phase", "Ready"); put("anchor", 0L); put("generation", 0L)
                })
                put(JSONObject().apply { put("duration", -1L) })
            })
        }.toString()
        val state = TimeStateJson.decode(json)
        assertEquals(2, state.timers.size)
        assertEquals(5L, state.timers[0].id)
        assertEquals(2L, state.timers[1].id)
        assertEquals(TimerPhase.Ready, state.timers[1].phase)
        assertEquals(5 * 60_000L, state.timers[1].durationMs)
    }

    @Test fun `invalid documents fall back to the default state`() {
        assertEquals(TimeToolsState(), TimeStateJson.decode(null))
        assertEquals(TimeToolsState(), TimeStateJson.decode("not json"))
        assertEquals(TimeToolsState(), TimeStateJson.decode("""{"version":99}"""))
    }
}