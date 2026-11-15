package com.dawnly.app.domain.alarm

import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DawnlyAlarmTimesTest {
    // A Friday evening, like the AlarmTimes tests' now.
    private val now = LocalDateTime.of(2026, 11, 6, 21, 15, 30)
    private val weekdays = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)

    private fun alarm(time: LocalTime, days: Set<DayOfWeek> = emptySet(), on: Boolean = true) =
        DawnlyAlarm(id = 1L, label = "Alarm", time = time, days = days, on = on)

    @Test
    fun laterTodayIsToday() {
        assertEquals(LocalDateTime.of(2026, 11, 6, 22, 0), DawnlyAlarmTimes.nextAfter(alarm(LocalTime.of(22, 0)), now))
    }

    @Test
    fun passedTimeIsTomorrow() {
        assertEquals(LocalDateTime.of(2026, 11, 7, 7, 0), DawnlyAlarmTimes.nextAfter(alarm(LocalTime.of(7, 0)), now))
    }

    @Test
    fun thisVeryMinuteIsTomorrow() {
        assertEquals(LocalDateTime.of(2026, 11, 7, 21, 15), DawnlyAlarmTimes.nextAfter(alarm(LocalTime.of(21, 15)), now))
    }

    @Test
    fun emptyDaysIsEveryDay() {
        val saturday = LocalDateTime.of(2026, 11, 7, 8, 0)
        assertEquals(LocalDateTime.of(2026, 11, 8, 7, 0), DawnlyAlarmTimes.nextAfter(alarm(LocalTime.of(7, 0)), saturday))
    }

    @Test
    fun allSevenDaysIsEveryDay() {
        assertEquals(
            LocalDateTime.of(2026, 11, 7, 7, 0),
            DawnlyAlarmTimes.nextAfter(alarm(LocalTime.of(7, 0), DayOfWeek.entries.toSet()), now),
        )
    }

    @Test
    fun weekdayAlarmSkipsTheWeekend() {
        assertEquals(LocalDateTime.of(2026, 11, 9, 7, 0), DawnlyAlarmTimes.nextAfter(alarm(LocalTime.of(7, 0), weekdays), now))
    }

    @Test
    fun weekdayAlarmLaterTodayStillCounts() {
        assertEquals(
            LocalDateTime.of(2026, 11, 6, 22, 0),
            DawnlyAlarmTimes.nextAfter(alarm(LocalTime.of(22, 0), weekdays), now),
        )
    }

    @Test
    fun oneWeekdayComesAround() {
        assertEquals(
            LocalDateTime.of(2026, 11, 11, 7, 0),
            DawnlyAlarmTimes.nextAfter(alarm(LocalTime.of(7, 0), setOf(DayOfWeek.WEDNESDAY)), now),
        )
    }

    @Test
    fun weekendAlarmLandsOnSaturday() {
        assertEquals(
            LocalDateTime.of(2026, 11, 7, 9, 0),
            DawnlyAlarmTimes.nextAfter(alarm(LocalTime.of(9, 0), setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)), now),
        )
    }

    @Test
    fun disabledHasNoNext() {
        assertNull(DawnlyAlarmTimes.nextAfter(alarm(LocalTime.of(7, 0), on = false), now))
        assertNull(DawnlyAlarmTimes.nextAfter(alarm(LocalTime.of(22, 0), weekdays, on = false), now))
    }
}