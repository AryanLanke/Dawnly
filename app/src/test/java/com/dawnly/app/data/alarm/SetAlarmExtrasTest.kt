package com.dawnly.app.data.alarm

import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Test

/** The hour and minute sent to the Clock app. 24-hour, so noon and midnight don't swap. */
class SetAlarmExtrasTest {
    @Test
    fun midnightIsZero() {
        assertEquals(SetAlarmExtras(0, 0), SetAlarmExtras.of(LocalTime.MIDNIGHT))
    }

    @Test
    fun noonIsTwelve() {
        assertEquals(SetAlarmExtras(12, 0), SetAlarmExtras.of(LocalTime.NOON))
    }

    @Test
    fun lastMinuteOfTheDay() {
        assertEquals(SetAlarmExtras(23, 59), SetAlarmExtras.of(LocalTime.of(23, 59)))
    }

    @Test
    fun morningKeepsTheMinutes() {
        assertEquals(SetAlarmExtras(7, 5), SetAlarmExtras.of(LocalTime.of(7, 5)))
    }
}
