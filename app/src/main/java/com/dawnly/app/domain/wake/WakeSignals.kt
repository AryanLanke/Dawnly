package com.dawnly.app.domain.wake

import java.time.Duration

/**
 * Live readings from the wake signals during one watch window, counted from the window start.
 * Produced by [WakeTracker]; judged by [WakeScore].
 */
data class WakeSignals(
    val unlocked: Boolean = false,
    /** Time the phone was in use: from unlocking until the screen went off. */
    val screenOnTime: Duration = Duration.ZERO,
    val steps: Int = 0,
    /** False when steps can't be counted: no step sensor, or activity recognition not allowed. */
    val stepsAvailable: Boolean = true,
)

/** Which signals count as "awake", and how much of each. Chosen by the user in settings. */
data class WakeSignalSettings(
    val useUnlock: Boolean = true,
    val useScreenTime: Boolean = true,
    val screenTimeGoal: Duration = Duration.ofMinutes(5),
    val useSteps: Boolean = false,
    val stepsGoal: Int = 50,
)
