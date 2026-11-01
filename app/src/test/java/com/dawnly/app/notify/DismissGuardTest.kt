package com.dawnly.app.notify

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class DismissGuardTest {
    private val alarm = Instant.parse("2026-11-02T05:30:00Z")
    private val tomorrow = Instant.parse("2026-11-03T05:30:00Z")
    private val early = Instant.parse("2026-11-02T05:04:00Z")

    private fun check(prompt: Instant? = alarm, next: Instant? = alarm, now: Instant = early) =
        DismissGuard.check(prompt, next, now)

    @Test
    fun `prompt's alarm still next and ahead is ok`() {
        assertEquals(DismissCheck.Ok, check())
        assertEquals(DismissCheck.Ok, check(now = alarm.minusMillis(1)))
    }

    @Test
    fun `stale prompt never dismisses tomorrow's alarm`() {
        // Today's alarm was dismissed some other way, so tomorrow's is next now.
        assertEquals(DismissCheck.AlarmChanged, check(next = tomorrow))
    }

    @Test
    fun `alarm moved to a different time is changed`() {
        assertEquals(DismissCheck.AlarmChanged, check(next = alarm.plusSeconds(60)))
        assertEquals(DismissCheck.AlarmChanged, check(next = alarm.minusSeconds(60)))
    }

    @Test
    fun `no alarm left is gone`() {
        assertEquals(DismissCheck.AlarmGone, check(next = null))
    }

    @Test
    fun `prompt's alarm time reached is passed, even if it's still reported as next`() {
        assertEquals(DismissCheck.AlarmPassed, check(now = alarm))
        assertEquals(DismissCheck.AlarmPassed, check(now = alarm.plusSeconds(30)))
        assertEquals(DismissCheck.AlarmPassed, check(next = tomorrow, now = alarm.plusSeconds(30)))
    }

    @Test
    fun `prompt without an alarm is refused`() {
        assertEquals(DismissCheck.AlarmChanged, check(prompt = null))
        assertEquals(DismissCheck.AlarmChanged, check(prompt = null, next = null))
    }
}
