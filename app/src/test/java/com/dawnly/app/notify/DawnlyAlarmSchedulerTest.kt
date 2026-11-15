package com.dawnly.app.notify

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Dawnly alarms share the ring engine with reminders, timers, the test and the still-up check,
 * and AlarmManager with the watch: their ring ids and request codes must stay apart.
 */
class DawnlyAlarmSchedulerTest {
    private val ids = listOf(1L, 42L, 1_763_000_123_456L)

    @Test
    fun ringIdRoundTrips() {
        for (id in ids) assertEquals(id, DawnlyAlarmScheduler.alarmIdFromRing(DawnlyAlarmScheduler.ringId(id)))
    }

    @Test
    fun ringIdsAreDistinctPerAlarm() {
        val rings = ids.map { DawnlyAlarmScheduler.ringId(it) }
        assertEquals(rings.size, rings.toSet().size)
    }

    @Test
    fun alarmRingsAreNeverOtherRings() {
        for (id in ids) {
            val ring = DawnlyAlarmScheduler.ringId(id)
            // Reminders ring with their own positive id; timers, the test and still-up are negative.
            assertTrue(ring < 0)
            assertNotEquals(ReminderRingService.TEST_ID, ring)
            assertNotEquals(StillUpCheck.RING_ID, ring)
            assertNull(TimerController.timerIdFromRing(ring))
        }
    }

    @Test
    fun otherRingsAreNeverAlarms() {
        assertNull(DawnlyAlarmScheduler.alarmIdFromRing(42L)) // a reminder
        assertNull(DawnlyAlarmScheduler.alarmIdFromRing(ReminderRingService.TEST_ID))
        assertNull(DawnlyAlarmScheduler.alarmIdFromRing(StillUpCheck.RING_ID))
        assertNull(DawnlyAlarmScheduler.alarmIdFromRing(TimerController.ringId(1L)))
        assertNull(DawnlyAlarmScheduler.alarmIdFromRing(TimerController.ringId(500L)))
    }

    @Test
    fun requestCodesAreStableAndDistinct() {
        for (id in ids) {
            assertEquals(300_000 + id.toInt(), DawnlyAlarmScheduler.requestCode(id))
            assertEquals(DawnlyAlarmScheduler.requestCode(id), DawnlyAlarmScheduler.requestCode(id))
        }
        val codes = ids.map { DawnlyAlarmScheduler.requestCode(it) }
        assertEquals(codes.size, codes.toSet().size)
        // The watch's check alarm uses 0; timers start at 200_002 with small ids.
        assertNotEquals(0, DawnlyAlarmScheduler.requestCode(42L))
    }

    @Test
    fun notificationIdsStayApartFromRemindersAndTimers() {
        for (id in ids) {
            val notification = DawnlyAlarmScheduler.notificationId(id)
            assertTrue(notification in 2_000_000..2_999_999)
        }
    }
}