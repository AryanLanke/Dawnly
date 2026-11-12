package com.dawnly.app.notify

import java.time.Duration
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class StillUpPlanTest {
    private val now = Instant.parse("2026-11-12T05:00:00Z")

    @Test
    fun `asks so the alarm rings at its own time without an answer`() {
        val alarmAt = now.plus(Duration.ofMinutes(30))
        val askAt = StillUpPlan.askAt(now, alarmAt)
        assertEquals(alarmAt.minus(Duration.ofMinutes(2)), askAt)
        assertEquals(alarmAt, StillUpPlan.ringAt(askAt))
    }

    @Test
    fun `never asks straight after the dismiss`() {
        val alarmAt = now.plus(Duration.ofMinutes(1))
        assertEquals(now.plus(Duration.ofMinutes(3)), StillUpPlan.askAt(now, alarmAt))
    }

    @Test
    fun `an alarm already past is checked a few minutes on`() {
        val alarmAt = now.minus(Duration.ofMinutes(5))
        assertEquals(now.plus(Duration.ofMinutes(3)), StillUpPlan.askAt(now, alarmAt))
    }
}
