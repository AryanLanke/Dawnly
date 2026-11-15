package com.dawnly.app.notify

import com.dawnly.app.domain.time.TimeReading
import com.dawnly.app.domain.time.TimerPhase
import com.dawnly.app.domain.time.TimerState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The finished timer's notification offers Snooze, and snoozing rings the same timer again. */
class TimerSnoozeTest {
    private val boot = 3
    private val snoozeMs = TimerController.SNOOZE_MINUTES * 60_000L

    @Test
    fun snoozeLengthMatchesTheRings() {
        assertEquals(ReminderRingService.SNOOZE_MINUTES, TimerController.SNOOZE_MINUTES)
    }

    @Test
    fun snoozeIsALengthTimersAccept() {
        assertTrue(TimerController.SNOOZE_MINUTES > 0)
        assertTrue(snoozeMs in 1..TimerState.MAX_DURATION_MS)
    }

    @Test
    fun snoozeHasItsOwnActionSoItNeverLooksLikeAnExpiry() {
        assertNotEquals(TimerController.ACTION_EXPIRED, TimerController.ACTION_SNOOZE)
    }

    @Test
    fun everyTimerRingBelongsToItsOwnTimer() {
        for (id in 1L..2_000L) {
            assertEquals(id, TimerController.timerIdFromRing(TimerController.ringId(id)))
        }
    }

    @Test
    fun timerRingsNeverLookLikeDawnlyAlarmRings() {
        for (id in 1L..2_000L) {
            assertNull(DawnlyAlarmScheduler.alarmIdFromRing(TimerController.ringId(id)))
        }
    }

    @Test
    fun snoozingAFinishedTimerStartsAFreshCountdown() {
        val finished = TimerState(id = 7L)
            .start(60_000L, TimeReading(1_000L, boot))
            .checkpoint(TimeReading(61_000L, boot))
        assertEquals(TimerPhase.Finished, finished.phase)
        assertTrue(finished.completionPending)

        val snoozed = finished.start(snoozeMs, TimeReading(90_000L, boot))
        assertEquals(TimerPhase.Running, snoozed.phase)
        assertEquals(snoozeMs, snoozed.remainingMs)
        assertEquals(snoozeMs, snoozed.remaining(TimeReading(90_000L, boot)))
        assertFalse(snoozed.completionPending)
        assertEquals(finished.generation + 1, snoozed.generation)
    }

    @Test
    fun anOldDeliveryCannotFinishTheSnoozedTimer() {
        val finished = TimerState(id = 7L)
            .start(60_000L, TimeReading(1_000L, boot))
            .checkpoint(TimeReading(61_000L, boot))
        val snoozed = finished.start(snoozeMs, TimeReading(90_000L, boot))
        // The notice that was out for the finished run must not ring the new countdown.
        assertFalse(snoozed.acceptsAlarm(finished.generation, TimeReading(90_000L + snoozeMs, boot)))
        assertFalse(finished.acceptsAlarm(finished.generation, TimeReading(61_000L, boot)))
    }

    @Test
    fun theSnoozedTimerFinishesWhenItsNewCountdownIsUp() {
        val snoozed = TimerState(id = 7L)
            .start(60_000L, TimeReading(1_000L, boot))
            .checkpoint(TimeReading(61_000L, boot))
            .start(snoozeMs, TimeReading(90_000L, boot))
        val justBefore = TimeReading(90_000L + snoozeMs - 1L, boot)
        val due = TimeReading(90_000L + snoozeMs, boot)
        assertFalse(snoozed.acceptsAlarm(snoozed.generation, justBefore))
        assertTrue(snoozed.acceptsAlarm(snoozed.generation, due))
        assertEquals(TimerPhase.Finished, snoozed.checkpoint(due).phase)
    }

    @Test
    fun snoozingKeepsTheTimersNameToneAndFinishMode() {
        val finished = TimerState(id = 7L, label = "Tea", sound = "Flute", ringOnFinish = true)
            .start(60_000L, TimeReading(1_000L, boot))
            .checkpoint(TimeReading(61_000L, boot))
        val snoozed = finished.start(snoozeMs, TimeReading(90_000L, boot))
        assertEquals("Tea", snoozed.label)
        assertEquals("Flute", snoozed.sound)
        assertTrue(snoozed.ringOnFinish)
        assertEquals(7L, snoozed.id)
    }
}
