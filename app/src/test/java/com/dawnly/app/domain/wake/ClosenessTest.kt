package com.dawnly.app.domain.wake

import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ClosenessTest {
    private fun factor(minutes: Long) = Closeness.goalFactor(Duration.ofMinutes(minutes))

    @Test
    fun `near the alarm the goals are lower`() {
        assertEquals(0.8f, factor(0), 0.001f)
        assertEquals(0.8f, factor(10), 0.001f)
        assertEquals(0.9f, factor(20), 0.001f)
    }

    @Test
    fun `half an hour before, the goals are as set`() {
        assertEquals(1f, factor(30), 0.001f)
    }

    @Test
    fun `hours before, the goals are higher`() {
        assertEquals(1.2f, factor(75), 0.001f)
        assertEquals(1.4f, factor(120), 0.001f)
        assertEquals(1.4f, factor(180), 0.001f)
    }

    @Test
    fun `the further from the alarm, the more it takes`() {
        val factors = (0L..180L step 5).map { factor(it) }
        assertTrue(factors.zipWithNext().all { (a, b) -> b >= a })
    }

    @Test
    fun `an alarm already past counts as near`() {
        assertEquals(0.8f, Closeness.goalFactor(Duration.ofMinutes(-3)), 0.001f)
    }
}
