package com.dawnly.app.ui.glass

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The wrapping wheels roll round and round: after 11 comes 12, after 59 comes 00, after PM comes AM. */
class WheelsTest {
    @Test
    fun hourWheelRollsFromElevenBackToTwelve() {
        assertEquals(listOf(10, 11, 0, 1), (10..13).map { wheelValue(it, 12) })
    }

    @Test
    fun minuteWheelRollsFromFiftyNineBackToZero() {
        assertEquals(58, wheelValue(58, 60))
        assertEquals(59, wheelValue(59, 60))
        assertEquals(0, wheelValue(60, 60))
        assertEquals(1, wheelValue(61, 60))
    }

    @Test
    fun amPmWheelRollsBackToAm() {
        assertEquals(0, wheelValue(0, 2))
        assertEquals(1, wheelValue(1, 2))
        assertEquals(0, wheelValue(2, 2))
    }

    @Test
    fun scrollingTheOtherWayWrapsToo() {
        assertEquals(11, wheelValue(-1, 12))
        assertEquals(0, wheelValue(-12, 12))
        assertEquals(59, wheelValue(-61, 60))
    }

    @Test
    fun everyTurnShowsEachValueExactlyOnce() {
        for (count in listOf(2, 12, 24, 60)) {
            assertEquals((0 until count).toList(), (0 until count).map { wheelValue(it + count * 7, count) })
        }
    }

    @Test
    fun aWrappingStripStartsInTheMiddleTurn() {
        val turns = wheelTurns(true)
        assertEquals(501, turns)
        val count = 60
        val start = wheelBase(count, turns)
        assertEquals(count * (turns / 2), start)
        assertEquals(0, wheelValue(start, count))
        // Hundreds of swipes either way before either end is reached.
        assertTrue(start / count > 100)
        assertTrue((count * turns - start) / count > 100)
    }

    @Test
    fun aPlainWheelIsOneTurnStartingAtItsFirstValue() {
        assertEquals(1, wheelTurns(false))
        assertEquals(0, wheelBase(12, wheelTurns(false)))
        assertEquals(0, wheelBase(60, wheelTurns(false)))
    }

    @Test
    fun theValueSittingInTheMiddleIsAlwaysInRange() {
        val count = 24
        val turns = wheelTurns(true)
        val base = wheelBase(count, turns)
        for (selected in 0 until count) {
            val middle = base + selected
            assertEquals(selected, wheelValue(middle, count))
        }
    }
}
