package com.dawnly.app.domain.time

/** A monotonic clock reading is meaningful only within the same Android boot. */
data class TimeReading(val elapsedMs: Long, val bootCount: Int)

enum class TimerPhase { Ready, Running, Paused, Finished }

/** Pure, persisted timer state. UI refreshes never advance the clock themselves. */
data class TimerState(
    val durationMs: Long = 5 * 60_000L,
    val remainingMs: Long = durationMs,
    val phase: TimerPhase = TimerPhase.Ready,
    val anchorMs: Long = 0L,
    val bootCount: Int = -1,
    val generation: Long = 0L,
    val pausedAfterReboot: Boolean = false,
    val completionPending: Boolean = false,
) {
    fun remaining(now: TimeReading): Long = if (phase == TimerPhase.Running && sameBoot(now)) {
        (remainingMs - (now.elapsedMs - anchorMs)).coerceAtLeast(0L)
    } else remainingMs

    private fun sameBoot(now: TimeReading) = bootCount >= 0 && now.bootCount == bootCount && now.elapsedMs >= anchorMs

    /** Reboot policy: pause at the last durable checkpoint, never reuse an old uptime. */
    fun checkpoint(now: TimeReading): TimerState {
        if (phase != TimerPhase.Running) return this
        if (!sameBoot(now)) return copy(
            phase = TimerPhase.Paused,
            anchorMs = 0L,
            generation = generation + 1,
            pausedAfterReboot = true,
        )
        val left = remaining(now)
        return if (left == 0L) copy(
            remainingMs = 0L,
            phase = TimerPhase.Finished,
            anchorMs = 0L,
            completionPending = true,
        ) else copy(remainingMs = left, anchorMs = now.elapsedMs)
    }

    fun setDuration(duration: Long): TimerState {
        require(duration in 1..MAX_DURATION_MS)
        if (phase != TimerPhase.Ready) return this
        return copy(durationMs = duration, remainingMs = duration)
    }

    fun start(duration: Long, now: TimeReading): TimerState {
        require(duration in 1..MAX_DURATION_MS)
        if (phase == TimerPhase.Running || phase == TimerPhase.Paused) return this
        return TimerState(duration, duration, TimerPhase.Running, now.elapsedMs, now.bootCount, generation + 1)
    }

    fun pause(now: TimeReading): TimerState {
        val current = checkpoint(now)
        return if (current.phase == TimerPhase.Running) current.copy(
            phase = TimerPhase.Paused, anchorMs = 0L, generation = generation + 1,
        ) else current
    }

    fun resume(now: TimeReading): TimerState = if (phase == TimerPhase.Paused && remainingMs > 0) copy(
        phase = TimerPhase.Running,
        anchorMs = now.elapsedMs,
        bootCount = now.bootCount,
        generation = generation + 1,
        pausedAfterReboot = false,
    ) else this

    fun reset(): TimerState = TimerState(durationMs = durationMs, generation = generation + 1)

    /** A canceled, replaced, early, duplicate, or pre-reboot delivery cannot complete this timer. */
    fun acceptsAlarm(deliveryGeneration: Long, now: TimeReading): Boolean =
        deliveryGeneration == generation && phase == TimerPhase.Running && sameBoot(now) && remaining(now) == 0L

    fun completionDelivered(): TimerState = if (phase == TimerPhase.Finished) copy(completionPending = false) else this

    companion object {
        const val MAX_DURATION_MS = (99 * 3600L + 59 * 60L + 59) * 1000L
    }
}

data class StopwatchState(
    val elapsedMs: Long = 0L,
    val running: Boolean = false,
    val anchorMs: Long = 0L,
    val bootCount: Int = -1,
    /** Cumulative elapsed times; split durations are the difference between adjacent entries. */
    val laps: List<Long> = emptyList(),
    val pausedAfterReboot: Boolean = false,
) {
    private fun sameBoot(now: TimeReading) = bootCount >= 0 && now.bootCount == bootCount && now.elapsedMs >= anchorMs

    fun elapsed(now: TimeReading): Long = if (running && sameBoot(now)) {
        val delta = now.elapsedMs - anchorMs
        elapsedMs + delta.coerceAtMost(Long.MAX_VALUE - elapsedMs)
    } else elapsedMs

    fun checkpoint(now: TimeReading): StopwatchState = when {
        !running -> this
        !sameBoot(now) -> copy(running = false, anchorMs = 0L, pausedAfterReboot = true)
        else -> copy(elapsedMs = elapsed(now), anchorMs = now.elapsedMs)
    }

    fun start(now: TimeReading): StopwatchState {
        val current = checkpoint(now)
        return if (current.running) current else current.copy(
            running = true, anchorMs = now.elapsedMs, bootCount = now.bootCount, pausedAfterReboot = false,
        )
    }

    fun pause(now: TimeReading): StopwatchState = checkpoint(now).copy(running = false, anchorMs = 0L)

    fun lap(now: TimeReading): StopwatchState {
        val current = checkpoint(now)
        if (!current.running || current.laps.size >= MAX_LAPS) return current
        val total = current.elapsedMs
        if (total <= (current.laps.lastOrNull() ?: 0L)) return current
        return current.copy(laps = current.laps + total)
    }

    fun reset(): StopwatchState = StopwatchState()

    companion object { const val MAX_LAPS = 100 }
}

data class TimeToolsState(val timer: TimerState = TimerState(), val stopwatch: StopwatchState = StopwatchState()) {
    fun checkpoint(now: TimeReading) = copy(timer = timer.checkpoint(now), stopwatch = stopwatch.checkpoint(now))
}
