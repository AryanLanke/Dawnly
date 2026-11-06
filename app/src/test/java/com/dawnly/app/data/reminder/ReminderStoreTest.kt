package com.dawnly.app.data.reminder

import com.dawnly.app.domain.reminder.Reminder
import com.dawnly.app.domain.reminder.ReminderSchedule
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The reminders saved on the phone: they must survive an update, including the ones from before it. */
class ReminderStoreTest {
    private val daily = Reminder(
        id = 42L,
        name = "Stretch",
        why = "Back",
        schedule = ReminderSchedule.Daily(LocalTime.of(21, 30)),
        sound = "Bubble",
        firstDay = LocalDate.of(2026, 11, 7),
        lastDay = LocalDate.of(2026, 11, 7),
        ring = true,
    )
    private val every = Reminder(
        id = 43L,
        name = "Water",
        schedule = ReminderSchedule.Every(20, LocalTime.of(8, 0), LocalTime.of(22, 0)),
    )

    @Test
    fun roundTripKeepsEveryField() {
        val back = ReminderStore.decode(ReminderStore.encode(listOf(daily, every)))
        assertEquals(listOf(daily, every), back)
    }

    @Test
    fun ringIsSavedAndRead() {
        val back = ReminderStore.decode(ReminderStore.encode(listOf(daily, every)))
        assertTrue(back[0].ring)
        assertFalse(back[1].ring)
    }

    @Test
    fun reminderFromBeforeRingLoadsAsANotification() {
        // The JSON 1.6.x wrote: no "ring" key at all.
        val old = """[{"id":7,"name":"Drink","why":"","on":true,"kind":"every","minutes":30,
            "from":"08:00","until":"22:00"}]"""
        val loaded = ReminderStore.decode(old)
        assertEquals(1, loaded.size)
        assertEquals("Drink", loaded[0].name)
        assertFalse(loaded[0].ring)
    }

    @Test
    fun unreadableReminderIsSkippedNotTheRest() {
        val mixed = """[{"id":1,"name":"Broken","kind":"daily","at":"not a time"},
            {"id":2,"name":"Fine","kind":"daily","at":"09:00"}]"""
        val loaded = ReminderStore.decode(mixed)
        assertEquals(listOf("Fine"), loaded.map { it.name })
    }
}
