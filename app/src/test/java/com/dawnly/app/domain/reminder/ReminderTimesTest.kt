package com.dawnly.app.domain.reminder

import java.time.LocalDateTime
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderTimesTest {
    private fun at(day: Int, hour: Int, minute: Int = 0) = LocalDateTime.of(2026, 11, day, hour, minute)

    private val water = ReminderSchedule.Every(20, LocalTime.of(8, 0), LocalTime.of(22, 0))

    @Test
    fun `daily comes later today, or tomorrow once it's passed`() {
        val nine = ReminderSchedule.Daily(LocalTime.of(21, 0))
        assertEquals(at(3, 21), ReminderTimes.next(nine, at(3, 10)))
        assertEquals(at(4, 21), ReminderTimes.next(nine, at(3, 21)))
        assertEquals(at(4, 21), ReminderTimes.next(nine, at(3, 23)))
    }

    @Test
    fun `every 20 minutes keeps to its steps from the start of the day`() {
        assertEquals(at(3, 10, 20), ReminderTimes.next(water, at(3, 10, 5)))
        assertEquals(at(3, 10, 40), ReminderTimes.next(water, at(3, 10, 20)))
    }

    @Test
    fun `nothing at night, the next one is the next morning`() {
        assertEquals(at(4, 8), ReminderTimes.next(water, at(3, 22, 0)))
        assertEquals(at(4, 8), ReminderTimes.next(water, at(3, 23, 30)))
        assertEquals(at(3, 8), ReminderTimes.next(water, at(3, 3)))
    }

    @Test
    fun `the last one of the day can land right on the end time`() {
        assertEquals(at(3, 22), ReminderTimes.next(water, at(3, 21, 45)))
    }

    @Test
    fun `a window that runs past midnight keeps going after it`() {
        val nights = ReminderSchedule.Every(30, LocalTime.of(22, 0), LocalTime.of(2, 0))
        assertEquals(at(4, 0, 30), ReminderTimes.next(nights, at(4, 0, 10)))
        assertEquals(at(4, 22), ReminderTimes.next(nights, at(4, 2, 0)))
    }
}
