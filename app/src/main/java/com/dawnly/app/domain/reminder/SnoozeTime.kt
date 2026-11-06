package com.dawnly.app.domain.reminder

/** When a snoozed reminder rings again. Pure, so it's unit tested. */
object SnoozeTime {
    /** [minutes] after [nowMillis], at least one minute. */
    fun after(nowMillis: Long, minutes: Int): Long = nowMillis + minutes.coerceAtLeast(1) * 60_000L
}
