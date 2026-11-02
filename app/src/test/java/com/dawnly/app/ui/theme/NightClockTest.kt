package com.dawnly.app.ui.theme

import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NightClockTest {
    @Test
    fun `night runs from 7 pm to 6 am`() {
        assertTrue(NightClock.isNight(LocalTime.of(2, 0)))
        assertTrue(NightClock.isNight(LocalTime.of(19, 0)))
        assertTrue(NightClock.isNight(LocalTime.of(5, 59)))
        assertFalse(NightClock.isNight(LocalTime.of(6, 0)))
        assertFalse(NightClock.isNight(LocalTime.of(12, 0)))
        assertFalse(NightClock.isNight(LocalTime.of(18, 59)))
    }

    @Test
    fun `at night the next change is the coming 6 am`() {
        assertEquals(Duration.ofHours(4), NightClock.untilChange(LocalDateTime.of(2026, 11, 3, 2, 0)))
        assertEquals(Duration.ofHours(11), NightClock.untilChange(LocalDateTime.of(2026, 11, 3, 19, 0)))
    }

    @Test
    fun `in the day the next change is 7 pm`() {
        assertEquals(Duration.ofHours(13), NightClock.untilChange(LocalDateTime.of(2026, 11, 3, 6, 0)))
        assertEquals(Duration.ofMinutes(1), NightClock.untilChange(LocalDateTime.of(2026, 11, 3, 18, 59)))
    }
}
