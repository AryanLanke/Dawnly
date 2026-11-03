package com.dawnly.app.domain.learn

import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LearningTest {
    private val zone = ZoneOffset.UTC

    /** 2026-11-02 is a Monday. */
    private fun at(day: Int, hour: Int, minute: Int = 0): Instant =
        LocalDateTime.of(2026, 11, day, hour, minute).toInstant(zone)

    private val screenOnly = SignShares(screen = 1f, apps = null, steps = 0f, movement = 0f)
    private val stepsOnly = SignShares(screen = 0f, apps = null, steps = 1f, movement = 0f)

    private fun up(asked: Instant, after: Long = 60, shares: SignShares = screenOnly) =
        Morning(asked, shares, Answer.Up, asked.plusSeconds(after))

    private fun notYet(asked: Instant, shares: SignShares = stepsOnly) =
        Morning(asked, shares, Answer.NotYet, asked.plusSeconds(30))

    @Test
    fun `nothing answered teaches nothing`() {
        assertEquals(Learned.Nothing, Learning.learn(emptyList(), zone))
        assertEquals(Learned.Nothing, Learning.learn(listOf(Morning(at(2, 6), screenOnly)), zone))
    }

    @Test
    fun `not yet makes it wait for more, quick yeses let it ask sooner`() {
        assertEquals(1.15f, Learning.learn(listOf(notYet(at(2, 5))), zone).caution, 1e-4f)
        assertEquals(0.96f, Learning.learn(listOf(up(at(2, 6))), zone).caution, 1e-4f)
        assertEquals(0.99f, Learning.learn(listOf(up(at(2, 6), after = 600)), zone).caution, 1e-4f)
    }

    @Test
    fun `caution stays within its limits`() {
        val many = (2..20).map { notYet(at(it, 5)) }
        assertEquals(1.5f, Learning.learn(many, zone).caution, 1e-4f)
    }

    @Test
    fun `learns the usual time once it has seen three weekday mornings`() {
        val two = listOf(up(at(2, 6, 30)), up(at(3, 6, 40)))
        assertNull(Learning.learn(two, zone).usualWeekday)
        val three = two + up(at(4, 6, 50))
        assertEquals(LocalTime.of(6, 40), Learning.learn(three, zone).usualWeekday)
        assertNull(Learning.learn(three, zone).usualWeekend)
    }

    @Test
    fun `near the usual time it asks sooner, hours before it waits for more`() {
        val learned = Learning.learn(listOf(up(at(2, 6, 30)), up(at(3, 6, 30)), up(at(4, 6, 30))), zone)
        val wednesday = LocalDateTime.of(2026, 11, 4, 0, 0)
        assertEquals(0.9f, learned.timeFactor(wednesday.withHour(6).withMinute(15)), 1e-4f)
        assertEquals(1.3f, learned.timeFactor(wednesday.withHour(3)), 1e-4f)
        // Past the usual time counts as near.
        assertEquals(0.9f, learned.timeFactor(wednesday.withHour(7)), 1e-4f)
        // No weekend mornings yet: no change on a Saturday.
        assertEquals(1f, learned.timeFactor(LocalDateTime.of(2026, 11, 7, 3, 0)), 1e-4f)
    }

    @Test
    fun `trusts the signs seen on real mornings, less the ones seen on too-early ones`() {
        val learned = Learning.learn(listOf(up(at(2, 6)), up(at(3, 6)), notYet(at(4, 4)), up(at(5, 6))), zone)
        assertTrue(learned.weights.screen > 1f)
        assertTrue(learned.weights.steps < 1f)
        assertEquals(1f, learned.weights.apps, 1e-4f)
    }

    @Test
    fun `the goal factor stays within its limits`() {
        val many = (2..20).map { notYet(at(it, 5)) }
        val learned = Learning.learn(many, zone)
        assertTrue(learned.goalFactor(LocalDateTime.of(2026, 11, 4, 3, 0)) <= Learning.MAX_FACTOR)
    }
}
