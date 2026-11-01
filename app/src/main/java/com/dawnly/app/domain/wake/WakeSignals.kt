package com.dawnly.app.domain.wake

import java.time.Duration

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
    /** Time music (or other media) was playing. Only counts together with movement. */
    val musicTime: Duration = Duration.ZERO,
)

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
) {
    /** At least one sign that can count on its own: music only adds to movement. */
    val canNotice: Boolean get() = useScreenTime || usePhoneUse || useSteps || useMovement
}
