package com.dawnly.app.domain.watch

import java.time.Duration
import java.time.Instant

/** Where Dawnly stands with the next alarm. Decided by [WatchPlanner]. */
sealed interface WatchState {
    /** No upcoming alarm. */
    data object Idle : WatchState

    /** The watch window for the alarm at [alarmAt] opens at [startsAt]. */
    data class Waiting(val alarmAt: Instant, val startsAt: Instant) : WatchState

    /** Inside the window: watching for signs the user is awake until [alarmAt]. */
    data class Watching(val alarmAt: Instant, val startedAt: Instant) : WatchState {
        /** How much of the window has passed, from 0 to 1. Drives the progress ring. */
        fun progress(now: Instant): Float {
            val total = Duration.between(startedAt, alarmAt).toMillis()
            if (total <= 0) return 1f
            val passed = Duration.between(startedAt, now).toMillis()
            return (passed.toFloat() / total).coerceIn(0f, 1f)
        }
    }

    /**
     * The alarm at [alarmAt] appeared after its window had already begun, so Dawnly doesn't watch
     * for it. That covers snoozes and short naps: the user was clearly awake when they set it, and
     * watching would wrongly flag them as up early straight away.
     */
    data class Skipped(val alarmAt: Instant) : WatchState
}

object WatchPlanner {
    /**
     * Decides what to do about the next alarm.
     *
     * @param alarmAt the next alarm from the Clock app, or null if there is none.
     * @param armedFor the alarm whose window start Dawnly scheduled ahead of time. Only that alarm
     *   is watched once its window opens, which also lets watching resume after a reboot.
     */
    fun decide(alarmAt: Instant?, window: Duration, now: Instant, armedFor: Instant?): WatchState {
        if (alarmAt == null || !alarmAt.isAfter(now)) return WatchState.Idle
        val startsAt = alarmAt - window
        return when {
            now.isBefore(startsAt) -> WatchState.Waiting(alarmAt, startsAt)
            alarmAt == armedFor -> WatchState.Watching(alarmAt, startsAt)
            else -> WatchState.Skipped(alarmAt)
        }
    }
}
