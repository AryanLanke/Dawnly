package com.dawnly.app.domain.reminder

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Reminders that come only on some days: [ReminderTimes.next] with a first and last day. */
class ReminderDateRangeTest {
    private fun at(day: Int, hour: Int, minute: Int = 0) = LocalDateTime.of(2026, 11, day, hour, minute)
    private fun day(d: Int) = LocalDate.of(2026, 11, d)

    private val nine = ReminderSchedule.Daily(LocalTime.of(21, 0))
    private val water = ReminderSchedule.Every(20, LocalTime.of(8, 0), LocalTime.of(22, 0))

    @Test
    fun `before its first day nothing comes until that day`() {
        assertEquals(at(10, 21), ReminderTimes.next(nine, at(3, 10), firstDay = day(10), lastDay = day(12)))
        assertEquals(at(10, 8), ReminderTimes.next(water, at(3, 10), firstDay = day(10), lastDay = day(12)))
    }

    @Test
    fun `on its first day it comes as usual`() {
        assertEquals(at(10, 21), ReminderTimes.next(nine, at(10, 6), firstDay = day(10), lastDay = day(12)))
        assertEquals(at(10, 9, 20), ReminderTimes.next(water, at(10, 9, 5), firstDay = day(10), lastDay = day(12)))
    }

    @Test
    fun `inside the range it comes every day`() {
        assertEquals(at(12, 21), ReminderTimes.next(nine, at(11, 21), firstDay = day(10), lastDay = day(12)))
    }

    @Test
    fun `after its last day nothing is left`() {
        assertNull(ReminderTimes.next(nine, at(12, 21), firstDay = day(10), lastDay = day(12)))
        assertNull(ReminderTimes.next(nine, at(15, 8), firstDay = day(10), lastDay = day(12)))
        assertNull(ReminderTimes.next(water, at(12, 22), firstDay = day(10), lastDay = day(12)))
    }

    @Test
    fun `the last one of the last day still comes`() {
        assertEquals(at(12, 22), ReminderTimes.next(water, at(12, 21, 45), firstDay = day(10), lastDay = day(12)))
        assertEquals(at(12, 21), ReminderTimes.next(nine, at(12, 20), firstDay = day(10), lastDay = day(12)))
    }

    @Test
    fun `just today comes once and then stops`() {
        assertEquals(at(6, 21), ReminderTimes.next(nine, at(6, 9), firstDay = day(6), lastDay = day(6)))
        assertNull(ReminderTimes.next(nine, at(6, 21), firstDay = day(6), lastDay = day(6)))
    }

    @Test
    fun `a window past midnight stops at midnight after the last day`() {
        val late = ReminderSchedule.Every(60, LocalTime.of(22, 0), LocalTime.of(2, 0))
        assertEquals(at(3, 23), ReminderTimes.next(late, at(3, 22), firstDay = day(1), lastDay = day(3)))
        assertNull(ReminderTimes.next(late, at(3, 23), firstDay = day(1), lastDay = day(3)))
    }

    @Test
    fun `no days at all behaves like before`() {
        assertEquals(at(4, 21), ReminderTimes.next(nine, at(3, 21)))
    }
}
