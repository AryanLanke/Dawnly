package com.dawnly.app.domain.reminder

import org.junit.Assert.assertEquals
import org.junit.Test

class SnoozeTimeTest {
    @Test
    fun tenMinutesLater() {
        assertEquals(1_000L + 600_000L, SnoozeTime.after(1_000L, 10))
    }

    @Test
    fun atLeastOneMinute() {
        assertEquals(60_000L, SnoozeTime.after(0L, 0))
    }
}
