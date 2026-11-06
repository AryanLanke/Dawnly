package com.dawnly.app.domain.alarm

import java.time.LocalDateTime
import java.time.LocalTime

/** When an alarm set for a time of day rings. Pure, so it's unit tested. */
object AlarmTimes {
    /** The next [time] after [now]: later today, or tomorrow if it has passed (like a Clock app). */
    fun nextAt(time: LocalTime, now: LocalDateTime): LocalDateTime {
        val today = now.toLocalDate().atTime(time.withSecond(0).withNano(0))
        return if (today.isAfter(now)) today else today.plusDays(1)
    }

    /** True if [a] and [b], in epoch millis, are the same alarm: within a minute of each other. */
    fun same(a: Long, b: Long): Boolean = kotlin.math.abs(a - b) < 60_000L
}
