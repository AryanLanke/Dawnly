package com.dawnly.app.notify

import com.dawnly.app.data.settings.DismissMode
import java.time.Duration
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AutoDismissPlanTest {
    private val now = Instant.parse("2026-11-02T05:00:00Z")

    @Test
    fun `dismisses one minute after the warning`() {
        val alarmAt = now.plus(Duration.ofMinutes(30))
        assertEquals(now.plusSeconds(60), AutoDismissPlan.dismissAt(now, alarmAt))
    }

    @Test
    fun `still dismisses when the minute ends just in time`() {
        val alarmAt = now.plusSeconds(70)
        assertEquals(now.plusSeconds(60), AutoDismissPlan.dismissAt(now, alarmAt))
    }

    @Test
    fun `asks instead when the alarm is too close for the warning`() {
        assertNull(AutoDismissPlan.dismissAt(now, now.plusSeconds(69)))
        assertNull(AutoDismissPlan.dismissAt(now, now.plusSeconds(30)))
    }

    @Test
    fun `asks instead when the alarm has passed`() {
        assertNull(AutoDismissPlan.dismissAt(now, now.minusSeconds(1)))
    }

    private val alarmAt = now.plus(Duration.ofMinutes(30))

    private fun decide(
        mode: DismissMode,
        alarmAt: Instant = this.alarmAt,
        reach: ClockReach = ClockReach.Anytime,
        canTimeExactly: Boolean = true,
        canNotify: Boolean = true,
        musicTippedIt: Boolean = false,
    ) = AutoDismissPlan.decide(mode, now, alarmAt, reach, canTimeExactly, canNotify, musicTippedIt)

    @Test
    fun `each choice does its own thing when everything is allowed`() {
        assertEquals(AwakeAction.Ask, decide(DismissMode.Ask))
        assertEquals(AwakeAction.Warn(now.plusSeconds(60)), decide(DismissMode.Warn))
        assertEquals(AwakeAction.DismissNow, decide(DismissMode.Instant))
    }

    @Test
    fun `both automatic choices ask when the Clock app can't be reached`() {
        assertEquals(AwakeAction.Ask, decide(DismissMode.Warn, reach = ClockReach.None))
        assertEquals(AwakeAction.Ask, decide(DismissMode.Instant, reach = ClockReach.None))
    }

    @Test
    fun `a full-screen notification is enough without Display over other apps`() {
        assertEquals(AwakeAction.Warn(now.plusSeconds(60)), decide(DismissMode.Warn, reach = ClockReach.WhenLocked))
        assertEquals(AwakeAction.DismissNow, decide(DismissMode.Instant, reach = ClockReach.WhenLocked))
    }

    @Test
    fun `a full-screen notification needs notifications`() {
        assertEquals(AwakeAction.Ask, decide(DismissMode.Instant, reach = ClockReach.WhenLocked, canNotify = false))
    }

    @Test
    fun `warn first asks when the warning couldn't be seen or finish in time`() {
        assertEquals(AwakeAction.Ask, decide(DismissMode.Warn, canTimeExactly = false))
        assertEquals(AwakeAction.Ask, decide(DismissMode.Warn, canNotify = false))
        assertEquals(AwakeAction.Ask, decide(DismissMode.Warn, alarmAt = now.plusSeconds(30)))
    }

    @Test
    fun `right away needs no warning, so no exact alarm, notifications or spare minute`() {
        assertEquals(
            AwakeAction.DismissNow,
            decide(DismissMode.Instant, alarmAt = now.plusSeconds(30), canTimeExactly = false, canNotify = false),
        )
    }

    @Test
    fun `always asks when music made the difference`() {
        assertEquals(AwakeAction.Ask, decide(DismissMode.Warn, musicTippedIt = true))
        assertEquals(AwakeAction.Ask, decide(DismissMode.Instant, musicTippedIt = true))
    }
}
