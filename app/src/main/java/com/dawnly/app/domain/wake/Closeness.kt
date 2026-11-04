package com.dawnly.app.domain.wake

import java.time.Duration

/**
 * Activity close to the alarm counts for more than the same activity hours before it: 10 minutes
 * before a 6:00 alarm someone up and scrolling is almost surely up for the day, at 4:00 they may
 * go back to sleep. Applied as a goal multiplier, like what Dawnly learns (`Learned.goalFactor`).
 */
object Closeness {
    private const val NEAR_MINUTES = 10f
    private const val NEAR_FACTOR = 0.8f
    private const val MID_MINUTES = 30f
    private const val FAR_MINUTES = 120f
    private const val FAR_FACTOR = 1.4f

    /**
     * Goals are multiplied by this with [untilAlarm] to go: 0.8 within 10 minutes, 1 at half an
     * hour, rising to 1.4 at two hours or more, in straight lines between.
     */
    fun goalFactor(untilAlarm: Duration): Float {
        val minutes = untilAlarm.seconds / 60f
        return when {
            minutes <= NEAR_MINUTES -> NEAR_FACTOR
            minutes <= MID_MINUTES -> NEAR_FACTOR + (1f - NEAR_FACTOR) * (minutes - NEAR_MINUTES) / (MID_MINUTES - NEAR_MINUTES)
            minutes <= FAR_MINUTES -> 1f + (FAR_FACTOR - 1f) * (minutes - MID_MINUTES) / (FAR_MINUTES - MID_MINUTES)
            else -> FAR_FACTOR
        }
    }
}
