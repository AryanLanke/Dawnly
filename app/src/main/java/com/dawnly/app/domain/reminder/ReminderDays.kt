package com.dawnly.app.domain.reminder

import java.time.LocalDate

/** Which days a reminder comes: the choices in the editor and how a saved reminder maps back to one. */
enum class ReminderDays {
    EveryDay,
    Today,
    Tomorrow,
    Both,
    OnDate,
    Range,
    ;

    /** The first and last day for this choice (both null for every day). [date] and [dateTo] are the picked dates. */
    fun span(today: LocalDate, date: LocalDate, dateTo: LocalDate): Pair<LocalDate?, LocalDate?> = when (this) {
        EveryDay -> null to null
        Today -> today to today
        Tomorrow -> today.plusDays(1) to today.plusDays(1)
        Both -> today to today.plusDays(1)
        OnDate -> date to date
        Range -> date to dateTo
    }

    companion object {
        /** Which choice a reminder with these days is, as seen on [today]. */
        fun of(first: LocalDate?, last: LocalDate?, today: LocalDate): ReminderDays = when {
            first == null || last == null -> EveryDay
            first == last && first == today -> Today
            first == last && first == today.plusDays(1) -> Tomorrow
            first == today && last == today.plusDays(1) -> Both
            first == last -> OnDate
            else -> Range
        }
    }
}
