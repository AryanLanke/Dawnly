package com.dawnly.app.domain.worldclock

import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class WorldClockTest {
    private val utc = ZoneId.of("UTC")
    // Fixed instants in northern winter and summer, so the tests don't depend on the day they run.
    private val winter = Instant.parse("2026-01-15T12:00:00Z")
    private val summer = Instant.parse("2026-07-15T12:00:00Z")

    @Test
    fun `a city ahead shows plus hours and minutes`() {
        assertEquals("+5h 30m", WorldClock.offsetLabel(ZoneId.of("Asia/Kolkata"), winter, utc))
        assertEquals("+9h", WorldClock.offsetLabel(ZoneId.of("Asia/Tokyo"), winter, utc))
    }

    @Test
    fun `a city behind shows minus hours`() {
        assertEquals("-8h", WorldClock.offsetLabel(ZoneId.of("America/Los_Angeles"), winter, utc))
        assertEquals("-5h", WorldClock.offsetLabel(ZoneId.of("America/New_York"), winter, utc))
    }

    @Test
    fun `quarter-hour offsets keep their minutes`() {
        assertEquals("+5h 45m", WorldClock.offsetLabel(ZoneId.of("Asia/Kathmandu"), winter, utc))
        assertEquals("+15m", WorldClock.offsetLabel(ZoneId.of("Asia/Kathmandu"), winter, ZoneId.of("Asia/Kolkata")))
    }

    @Test
    fun `the same time is zero`() {
        assertEquals("0h", WorldClock.offsetLabel(ZoneId.of("Europe/London"), winter, utc))
        assertEquals("0h", WorldClock.offsetLabel(utc, winter, utc))
    }

    @Test
    fun `daylight saving moves the offset with the season`() {
        val losAngeles = ZoneId.of("America/Los_Angeles")
        assertEquals("-8h", WorldClock.offsetLabel(losAngeles, winter, utc))
        assertEquals("-7h", WorldClock.offsetLabel(losAngeles, summer, utc))
        val sydney = ZoneId.of("Australia/Sydney")
        assertEquals("+11h", WorldClock.offsetLabel(sydney, winter, utc))
        assertEquals("+10h", WorldClock.offsetLabel(sydney, summer, utc))
    }

    @Test
    fun `currentTime is the city's own time`() {
        assertEquals(LocalTime.of(17, 30), WorldClock.currentTime(ZoneId.of("Asia/Kolkata"), winter))
        assertEquals(LocalTime.of(4, 0), WorldClock.currentTime(ZoneId.of("America/Los_Angeles"), winter))
        assertEquals(LocalTime.of(21, 0), WorldClock.currentTime(ZoneId.of("Asia/Tokyo"), winter))
    }
}