package com.dawnly.app.data.alarm

import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Test

class DismissRequestTest {
    @Test
    fun `tries every form, next alarm first`() {
        assertEquals(
            listOf(DismissRequest.Next, DismissRequest.ByTime, DismissRequest.ByTimeShown),
            DismissRequest.order(preferred = null, haveTime = true),
        )
    }

    @Test
    fun `the form that worked last time goes first`() {
        assertEquals(
            listOf(DismissRequest.ByTimeShown, DismissRequest.Next, DismissRequest.ByTime),
            DismissRequest.order(preferred = DismissRequest.ByTimeShown, haveTime = true),
        )
    }

    @Test
    fun `without the alarm's time only the next-alarm form is left`() {
        assertEquals(listOf(DismissRequest.Next), DismissRequest.order(preferred = null, haveTime = false))
        assertEquals(listOf(DismissRequest.Next), DismissRequest.order(preferred = DismissRequest.ByTime, haveTime = false))
    }

    @Test
    fun `morning time is AM`() {
        assertEquals(TimeSearch(6, 30, isPm = false), TimeSearch.of(LocalTime.of(6, 30)))
    }

    @Test
    fun `afternoon time keeps the 24-hour hour and says PM`() {
        assertEquals(TimeSearch(14, 5, isPm = true), TimeSearch.of(LocalTime.of(14, 5)))
    }

    @Test
    fun `noon is PM and midnight is AM`() {
        assertEquals(TimeSearch(12, 0, isPm = true), TimeSearch.of(LocalTime.NOON))
        assertEquals(TimeSearch(0, 0, isPm = false), TimeSearch.of(LocalTime.MIDNIGHT))
    }
}
