package com.dawnly.app.domain.wake

import java.time.Duration
import kotlin.math.roundToInt

/**
 * Live readings from the wake signals during one watch window, counted from the window start.
 * Produced by [WakeTracker]; judged by [WakeScore].
 */
data class WakeSignals(
    /** Time the phone was in use: from unlocking until the screen went off. */
    val screenOnTime: Duration = Duration.ZERO,
    /**
     * Apps opened, from Usage access: the closest safe stand-in for touches. A lit screen can stay
     * on while someone dozes off over a video; opening apps takes someone awake.
     */
    val appsOpened: Int = 0,
    /** False without Usage access. */
    val appsAvailable: Boolean = true,
    val steps: Int = 0,
    /** False when steps can't be counted: no step sensor, or activity recognition not allowed. */
    val stepsAvailable: Boolean = true,
    /** Seconds in which the phone moved, from the accelerometer. */
    val movingTime: Duration = Duration.ZERO,
    /** False on a phone without an accelerometer. */
    val movementAvailable: Boolean = true,
    /** Time music (or other media) played that was started during the window, not sleep music. */
    val musicTime: Duration = Duration.ZERO,
    /**
     * The charger was plugged in or unplugged during the window: either takes a hand, and most
     * people plug in at bedtime or after waking, not at 6 AM in their sleep. A strong
     * sign, but it only adds to the others: never enough on its own.
     */
    val chargerChanged: Boolean = false,
    /**
     * How long the current stretch of activity has lasted: from its first sign to its latest. A
     * quiet gap of [QUIET_GAP] or more starts a new stretch. Not kept across a restart.
     */
    val stretch: Duration = Duration.ZERO,
    /** Time since the latest sign of activity, or null before any. */
    val idle: Duration? = null,
) {
    /**
     * The activity is still going and has lasted a while, not one burst that stopped (a trip to
     * the bathroom at 4 AM, then back to bed). Dawnly only counts someone as up while this holds.
     */
    val keptGoing: Boolean get() = idle != null && idle <= STILL_GOING && stretch >= MIN_STRETCH

    companion object {
        /** Activity this recent counts as still going. Sensor readings can arrive 10 s late. */
        val STILL_GOING: Duration = Duration.ofMinutes(1)

        /** How long activity has to keep going before it counts. */
        val MIN_STRETCH: Duration = Duration.ofMinutes(3)

        /** A quiet gap this long ends a stretch; the next activity starts a new one. */
        val QUIET_GAP: Duration = Duration.ofMinutes(3)
    }
}

/** Which signals count as "awake", and how much of each. Chosen by the user in settings. */
data class WakeSignalSettings(
    val useScreenTime: Boolean = true,
    val screenTimeGoal: Duration = Duration.ofMinutes(5),
    /** Needs Usage access, so it's off until the user allows that. */
    val usePhoneUse: Boolean = false,
    val appsOpenedGoal: Int = 3,
    /** Needs Physical activity, so it's off until the user allows that. */
    val useSteps: Boolean = false,
    val stepsGoal: Int = 50,
    val useMovement: Boolean = true,
    val movementGoal: Duration = Duration.ofMinutes(2),
    val useMusic: Boolean = true,
    /** No permission needed, so it's on by default. */
    val useCharger: Boolean = true,
    /** How many different kinds of sign have to show before Dawnly counts the user as up. */
    val signsNeeded: Int = 2,
) {
    /** At least one sign that can count on its own: music and the charger only add to the others. */
    val canNotice: Boolean get() = useScreenTime || usePhoneUse || useSteps || useMovement

    /**
     * The same settings with every goal multiplied by [factor]: above 1 Dawnly waits for more,
     * below 1 it asks sooner. From what it has learned (`Learned.goalFactor`).
     */
    fun scaledBy(factor: Float): WakeSignalSettings = if (factor == 1f) this else copy(
        screenTimeGoal = Duration.ofMillis((screenTimeGoal.toMillis() * factor).toLong()),
        appsOpenedGoal = (appsOpenedGoal * factor).roundToInt().coerceAtLeast(1),
        stepsGoal = (stepsGoal * factor).roundToInt().coerceAtLeast(1),
        movementGoal = Duration.ofMillis((movementGoal.toMillis() * factor).toLong()),
    )
}
