package com.dawnly.app.data.alarm

import com.dawnly.app.domain.alarm.DawnlyAlarm
import java.time.DayOfWeek
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The Dawnly alarms saved on the phone: they must survive an update, including the ones from before it. */
class DawnlyAlarmStoreTest {
    private val weekdays = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)
    private val work = DawnlyAlarm(
        id = 42L,
        label = "Work",
        time = LocalTime.of(7, 0),
        days = weekdays,
        sound = "Flute",
    )
    private val weekend = DawnlyAlarm(
        id = 43L,
        label = "Slow morning",
        time = LocalTime.of(9, 30),
        days = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY),
        on = false,
    )

    @Test
    fun roundTripKeepsEveryField() {
        val back = DawnlyAlarmStore.decode(DawnlyAlarmStore.encode(listOf(work, weekend)))
        assertEquals(listOf(work, weekend), back)
    }

    @Test
    fun nullSoundAndEmptyDaysSurvive() {
        val daily = DawnlyAlarm(id = 7L, label = "Alarm", time = LocalTime.of(6, 45))
        val back = DawnlyAlarmStore.decode(DawnlyAlarmStore.encode(listOf(daily))).single()
        assertNull(back.sound)
        assertTrue(back.days.isEmpty())
        assertTrue(back.on)
    }

    @Test
    fun alarmWithoutTheNewKeysLoadsWithDefaults() {
        // Only the first keys are guaranteed; days, sound and on may be missing.
        val old = """[{"id":7,"label":"Alarm","time":"06:45"}]"""
        val loaded = DawnlyAlarmStore.decode(old).single()
        assertTrue(loaded.days.isEmpty())
        assertNull(loaded.sound)
        assertTrue(loaded.on)
    }

    @Test
    fun unreadableAlarmIsSkippedNotTheRest() {
        val mixed = """[{"id":1,"label":"Broken","time":"not a time"},
            {"id":2,"label":"Fine","time":"09:00"}]"""
        val loaded = DawnlyAlarmStore.decode(mixed)
        assertEquals(listOf("Fine"), loaded.map { it.label })
    }

    @Test
    fun unknownDayNamesAreDroppedNotTheAlarm() {
        val json = """[{"id":1,"label":"Alarm","time":"09:00","days":["MONDAY","FUNDAY"]}]"""
        val loaded = DawnlyAlarmStore.decode(json).single()
        assertEquals(setOf(DayOfWeek.MONDAY), loaded.days)
    }

    @Test
    fun nothingSavedIsEmpty() {
        assertTrue(DawnlyAlarmStore.decode(null).isEmpty())
        assertTrue(DawnlyAlarmStore.decode("not json").isEmpty())
    }
}