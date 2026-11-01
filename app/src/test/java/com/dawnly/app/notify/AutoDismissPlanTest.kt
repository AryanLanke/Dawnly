package com.dawnly.app.notify

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
}
