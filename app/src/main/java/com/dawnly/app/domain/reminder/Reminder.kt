package com.dawnly.app.domain.reminder

import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * A reminder the user set on Today, like "Drink water, every 20 minutes": a [name], an optional
 * [why] shown under it in the notification, when it comes ([schedule]), and its sound by name
 * ([sound], one of Dawnly's sounds; null for the reminders' default). Kept on the phone.
 */
data class Reminder(
    val id: Long,
    val name: String,
    val why: String = "",
    val schedule: ReminderSchedule,
    val on: Boolean = true,
    val sound: String? = null,
    /** The first and last day it comes; both null is every day. */
    val firstDay: LocalDate? = null,
    val lastDay: LocalDate? = null,
    /** Rings like an alarm (full screen, its sound repeating until Stop or Snooze) instead of a notification. */
    val ring: Boolean = false,
)

sealed interface ReminderSchedule {
    /** Once a day at [at]. */
    data class Daily(val at: LocalTime) : ReminderSchedule

    /**
     * Every [minutes] from [from] to [until] each day, so a water reminder doesn't ring all night.
     * An [until] before [from] runs past midnight.
     */
    data class Every(val minutes: Int, val from: LocalTime, val until: LocalTime) : ReminderSchedule

    companion object {
        /** The repeats offered in the editor. */
        val REPEATS = listOf(15, 20, 30, 60)

        val DAY_START: LocalTime = LocalTime.of(8, 0)
        val DAY_END: LocalTime = LocalTime.of(22, 0)
    }
}

/** When reminders come next. Pure, so it's unit tested. */
object ReminderTimes {
    /** The first time [schedule] comes strictly after [after]. */
    fun next(
        schedule: ReminderSchedule,
        after: LocalDateTime,
        firstDay: LocalDate? = null,
        lastDay: LocalDate? = null,
    ): LocalDateTime? {
        val from = if (firstDay != null && after.toLocalDate().isBefore(firstDay)) {
            firstDay.atStartOfDay().minusNanos(1)
        } else {
            after
        }
        val time = nextEveryDay(schedule, from)
        return if (lastDay != null && time.toLocalDate().isAfter(lastDay)) null else time
    }

    /**
     * Whether [reminder] still has a time to come, from [now]. False once its last day has passed,
     * or for a one-day reminder whose time is already behind us: nothing would ever ring.
     */
    fun hasTimeLeft(reminder: Reminder, now: LocalDateTime): Boolean =
        next(reminder.schedule, now.plusSeconds(1), reminder.firstDay, reminder.lastDay) != null

    private fun nextEveryDay(schedule: ReminderSchedule, after: LocalDateTime): LocalDateTime = when (schedule) {
        is ReminderSchedule.Daily -> {
            val today = after.toLocalDate().atTime(schedule.at)
            if (today.isAfter(after)) today else today.plusDays(1)
        }
        is ReminderSchedule.Every -> nextEvery(schedule, after)
    }

    private fun nextEvery(schedule: ReminderSchedule.Every, after: LocalDateTime): LocalDateTime {
        val step = Duration.ofMinutes(schedule.minutes.toLong().coerceAtLeast(1))
        var span = Duration.between(schedule.from, schedule.until)
        if (span.isNegative || span.isZero) span = span.plusDays(1)
        // The window that started yesterday may still be open (past midnight); then today's, then tomorrow's.
        for (daysAhead in -1L..1L) {
            val start = after.toLocalDate().plusDays(daysAhead).atTime(schedule.from)
            val end = start.plus(span)
            if (!end.isAfter(after)) continue
            if (start.isAfter(after)) return start
            // The first step after `after` inside this window.
            val passed = Duration.between(start, after)
            val k = passed.toMillis() / step.toMillis() + 1
            val candidate = start.plus(step.multipliedBy(k))
            if (!candidate.isAfter(end)) return candidate
        }
        return after.toLocalDate().plusDays(1).atTime(schedule.from)
    }
}
