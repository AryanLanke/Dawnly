package com.dawnly.app.domain.reminder

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderDaysTest {
    private val today = LocalDate.of(2026, 11, 6)
    private val tomorrow = today.plusDays(1)
    private val later = LocalDate.of(2026, 11, 20)

    @Test
    fun `no days means every day`() {
        assertEquals(ReminderDays.EveryDay, ReminderDays.of(null, null, today))
    }

    @Test
    fun `a saved span maps back to its choice`() {
        assertEquals(ReminderDays.Today, ReminderDays.of(today, today, today))
        assertEquals(ReminderDays.Tomorrow, ReminderDays.of(tomorrow, tomorrow, today))
        assertEquals(ReminderDays.Both, ReminderDays.of(today, tomorrow, today))
        assertEquals(ReminderDays.OnDate, ReminderDays.of(later, later, today))
        assertEquals(ReminderDays.Range, ReminderDays.of(today, later, today))
    }

    @Test
    fun `a past one-day reminder is shown as a particular date`() {
        val past = today.minusDays(3)
        assertEquals(ReminderDays.OnDate, ReminderDays.of(past, past, today))
    }

    @Test
    fun `each choice gives its first and last day`() {
        assertEquals(null to null, ReminderDays.EveryDay.span(today, later, later))
        assertEquals(today to today, ReminderDays.Today.span(today, later, later))
        assertEquals(tomorrow to tomorrow, ReminderDays.Tomorrow.span(today, later, later))
        assertEquals(today to tomorrow, ReminderDays.Both.span(today, later, later))
        assertEquals(later to later, ReminderDays.OnDate.span(today, later, later.plusDays(3)))
        assertEquals(later to later.plusDays(3), ReminderDays.Range.span(today, later, later.plusDays(3)))
    }

    @Test
    fun `every choice survives a save and reopen`() {
        for (choice in ReminderDays.entries) {
            val (first, last) = choice.span(today, later, later.plusDays(3))
            assertEquals(choice, ReminderDays.of(first, last, today))
        }
    }
}
