package com.dawnly.app.domain.wake

import java.time.Duration

/** Live readings from the wake signals during one watch window. Filled in by Phase 3. */
data class WakeSignals(
    val unlocked: Boolean = false,
    val screenOnTime: Duration = Duration.ZERO,
    val steps: Int = 0,
)

/** Which signals count as "awake", and how much of each. Chosen by the user in settings. */
data class WakeSignalSettings(
    val useUnlock: Boolean = true,
    val useScreenTime: Boolean = true,
    val screenTimeGoal: Duration = Duration.ofMinutes(5),
    val useSteps: Boolean = false,
    val stepsGoal: Int = 50,
)
