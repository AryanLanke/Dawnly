package com.dawnly.app.notify

import java.time.Duration
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DismissRetryTest {
    private val now = Instant.parse("2026-11-03T05:00:00Z")

    @Test
    fun `tries again 55 minutes before an alarm that's still far off`() {
        val alarmAt = now.plus(Duration.ofHours(2))
        assertEquals(alarmAt.minus(Duration.ofMinutes(55)), DismissRetry.retryAt(now, alarmAt, alreadyRetried = false))
    }

    @Test
    fun `no second try when the alarm is already close`() {
        assertNull(DismissRetry.retryAt(now, now.plus(Duration.ofMinutes(50)), alreadyRetried = false))
        assertNull(DismissRetry.retryAt(now, now.plus(Duration.ofMinutes(59)), alreadyRetried = false))
    }

    @Test
    fun `a try just far enough off still goes ahead`() {
        val alarmAt = now.plus(Duration.ofMinutes(60))
        assertEquals(now.plus(Duration.ofMinutes(5)), DismissRetry.retryAt(now, alarmAt, alreadyRetried = false))
    }

    @Test
    fun `never a third try`() {
        assertNull(DismissRetry.retryAt(now, now.plus(Duration.ofHours(3)), alreadyRetried = true))
    }
}
