package com.dawnly.app.data.time

import com.dawnly.app.domain.time.StopwatchState
import com.dawnly.app.domain.time.TimeToolsState
import com.dawnly.app.domain.time.TimerPhase
import com.dawnly.app.domain.time.TimerState
import org.json.JSONArray
import org.json.JSONObject

/** A single preferences value makes transitions (including completion claims) atomic. */
internal object TimeStateJson {
    fun encode(state: TimeToolsState): String = JSONObject().apply {
        put("version", 2)
        put("timers", JSONArray().apply { state.timers.forEach { put(timerJson(it)) } })
        put("stopwatch", JSONObject().apply {
            with(state.stopwatch) {
                put("elapsed", elapsedMs)
                put("running", running)
                put("anchor", anchorMs)
                put("boot", bootCount)
                put("laps", JSONArray(laps))
                put("reboot", pausedAfterReboot)
            }
        })
    }.toString()

    fun decode(json: String?): TimeToolsState {
        val root = try { JSONObject(json ?: return TimeToolsState()) } catch (_: Exception) { return TimeToolsState() }
        val stopwatch = stopwatch(root.optJSONObject("stopwatch"))
        return when (root.optInt("version")) {
            2 -> {
                val array = root.optJSONArray("timers") ?: return TimeToolsState(stopwatch = stopwatch)
                val timers = List(array.length()) { timer(array.optJSONObject(it), (it + 1).toLong()) }
                TimeToolsState(timers, stopwatch)
            }
            // Before multiple timers: the one saved timer becomes the first of the list, state intact.
            1 -> TimeToolsState(listOf(timer(root.optJSONObject("timer"), 1L)), stopwatch)
            else -> TimeToolsState()
        }
    }

    private fun timerJson(timer: TimerState): JSONObject = JSONObject().apply {
        with(timer) {
            put("id", id)
            put("label", label)
            if (sound != null) put("sound", sound)
            put("ring", ringOnFinish)
            put("duration", durationMs)
            put("remaining", remainingMs)
            put("phase", phase.name)
            put("anchor", anchorMs)
            put("boot", bootCount)
            put("generation", generation)
            put("reboot", pausedAfterReboot)
            put("pending", completionPending)
        }
    }

    private fun timer(json: JSONObject?, fallbackId: Long): TimerState = try {
        requireNotNull(json)
        val id = json.optLong("id", fallbackId)
        require(id >= 1)
        val duration = json.getLong("duration")
        val remaining = json.getLong("remaining")
        val phase = TimerPhase.valueOf(json.getString("phase"))
        val anchor = json.getLong("anchor")
        val generation = json.getLong("generation")
        val pending = json.optBoolean("pending")
        require(duration in 1..TimerState.MAX_DURATION_MS && remaining in 0..duration)
        require(anchor >= 0 && generation >= 0)
        require(phase != TimerPhase.Finished || remaining == 0L)
        require(phase != TimerPhase.Ready || remaining == duration)
        require(phase != TimerPhase.Paused || remaining > 0L)
        require(!pending || phase == TimerPhase.Finished)
        val label = if (json.has("label")) json.getString("label") else "Timer"
        val sound = if (json.has("sound") && !json.isNull("sound")) json.getString("sound") else null
        TimerState(
            duration, remaining, phase, anchor, json.optInt("boot", -1), generation,
            json.optBoolean("reboot"), pending, id, label, sound, json.optBoolean("ring"),
        )
    } catch (_: Exception) { TimerState(id = fallbackId) }

    private fun stopwatch(json: JSONObject?): StopwatchState = try {
        requireNotNull(json)
        val elapsed = json.getLong("elapsed")
        val anchor = json.getLong("anchor")
        require(elapsed >= 0 && anchor >= 0)
        val array = json.optJSONArray("laps") ?: JSONArray()
        require(array.length() <= StopwatchState.MAX_LAPS)
        val laps = List(array.length()) { array.getLong(it) }
        require(laps.all { it in 1..elapsed } && laps.zipWithNext().all { (a, b) -> a < b })
        StopwatchState(elapsed, json.getBoolean("running"), anchor, json.optInt("boot", -1), laps, json.optBoolean("reboot"))
    } catch (_: Exception) { StopwatchState() }
}