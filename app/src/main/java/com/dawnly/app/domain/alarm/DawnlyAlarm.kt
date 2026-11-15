package com.dawnly.app.domain.alarm

import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * An alarm Dawnly keeps and rings itself, not the Clock app's: a [label], a [time] of day, and the
 * [days] it repeats on (empty is every day). [sound] is one of Dawnly's sounds by name, or null for
 * the phone's own alarm sound. Kept on the phone, like the reminders.
 */
data class DawnlyAlarm(
    val id: Long,
    val label: String,
    val time: LocalTime,
    val days: Set<DayOfWeek> = emptySet(),
    val sound: String? = null,
    val on: Boolean = true,
)

/** When Dawnly's own alarms ring next. Pure, so it's unit tested. */
object DawnlyAlarmTimes {
    /**
     * The next occurrence of [alarm] strictly after [now], or null when it's off. Empty [DawnlyAlarm.days]
     * is every day; otherwise only the named weekdays count, so a Friday-evening look at a weekday
     * alarm lands on Monday.
     */
    fun nextAfter(alarm: DawnlyAlarm, now: LocalDateTime): LocalDateTime? {
        if (!alarm.on) return null
        val time = alarm.time.withSecond(0).withNano(0)
        // A week covers every choice of days; the eighth day guards against an empty match.
        for (daysAhead in 0L..7L) {
            val day = now.toLocalDate().plusDays(daysAhead)
            if (alarm.days.isNotEmpty() && day.dayOfWeek !in alarm.days) continue
            val at = day.atTime(time)
            if (at.isAfter(now)) return at
        }
        return null
    }
}