package com.dawnly.app.domain.alarm

import java.time.LocalDateTime
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmTimesTest {
    private val now = LocalDateTime.of(2026, 11, 6, 21, 15, 30)

    @Test
    fun laterTodayIsToday() {
        assertEquals(LocalDateTime.of(2026, 11, 6, 22, 0), AlarmTimes.nextAt(LocalTime.of(22, 0), now))
    }

    @Test
    fun passedTimeIsTomorrow() {
        assertEquals(LocalDateTime.of(2026, 11, 7, 7, 0), AlarmTimes.nextAt(LocalTime.of(7, 0), now))
    }

    @Test
    fun thisVeryMinuteIsTomorrow() {
        assertEquals(LocalDateTime.of(2026, 11, 7, 21, 15), AlarmTimes.nextAt(LocalTime.of(21, 15), now))
    }

    @Test
    fun sameWithinAMinute() {
        assertTrue(AlarmTimes.same(1_000_000L, 1_030_000L))
        assertFalse(AlarmTimes.same(1_000_000L, 1_060_000L))
    }
}
