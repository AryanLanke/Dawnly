package com.dawnly.app.domain.wake

import java.time.Duration

/**
 * How much of each sign counts as "awake". One choice instead of a goal picker per sign.
 * Medium matches the defaults in [WakeSignalSettings].
 */
enum class Sensitivity(
    val screenTimeGoal: Duration,
    val appsOpenedGoal: Int,
    val stepsGoal: Int,
    val movementGoal: Duration,
) {
    Low(Duration.ofMinutes(10), 5, 100, Duration.ofMinutes(3)),
    Medium(Duration.ofMinutes(5), 3, 50, Duration.ofMinutes(2)),
    High(Duration.ofMinutes(3), 2, 30, Duration.ofMinutes(1)),
    ;

    fun applyTo(settings: WakeSignalSettings) = settings.copy(
        screenTimeGoal = screenTimeGoal,
        appsOpenedGoal = appsOpenedGoal,
        stepsGoal = stepsGoal,
        movementGoal = movementGoal,
    )

    companion object {
        /** The level matching [settings]' goals, or null if they were set some other way. */
        fun of(settings: WakeSignalSettings): Sensitivity? = entries.firstOrNull { it.applyTo(settings) == settings }
    }
}
