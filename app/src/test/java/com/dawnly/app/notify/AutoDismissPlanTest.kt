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
        canOpenClock: Boolean = true,
        canTimeExactly: Boolean = true,
        canNotify: Boolean = true,
    ) = AutoDismissPlan.decide(mode, now, alarmAt, canOpenClock, canTimeExactly, canNotify)

    @Test
    fun `each choice does its own thing when everything is allowed`() {
        assertEquals(AwakeAction.Ask, decide(DismissMode.Ask))
        assertEquals(AwakeAction.Warn(now.plusSeconds(60)), decide(DismissMode.Warn))
        assertEquals(AwakeAction.DismissNow, decide(DismissMode.Instant))
    }

    @Test
    fun `both automatic choices ask without Display over other apps`() {
        assertEquals(AwakeAction.Ask, decide(DismissMode.Warn, canOpenClock = false))
        assertEquals(AwakeAction.Ask, decide(DismissMode.Instant, canOpenClock = false))
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
}
