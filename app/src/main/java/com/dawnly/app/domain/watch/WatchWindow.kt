package com.dawnly.app.domain.watch

import java.time.Duration

/** How long before the alarm Dawnly starts watching. Chosen by the user in settings. */
object WatchWindow {
    val DEFAULT: Duration = Duration.ofHours(1)
    val PRESETS: List<Duration> = listOf(Duration.ofMinutes(30), Duration.ofHours(1), Duration.ofHours(2))

    /** The custom window's slider jumps in this step; its minus and plus move by [FINE_STEP]. */
    val STEP: Duration = Duration.ofMinutes(5)
    val FINE_STEP: Duration = Duration.ofMinutes(1)

    /** Allowed, but shorter than [RECOMMENDED_MIN] leaves little time to notice someone is up. */
    val MIN: Duration = Duration.ofMinutes(1)

    /** Below this, Settings says the window is very short. */
    val RECOMMENDED_MIN: Duration = Duration.ofMinutes(5)

    /** Battery-friendly cap: Dawnly never watches all night. */
    val MAX: Duration = Duration.ofHours(3)

    fun clamp(window: Duration): Duration = window.coerceIn(MIN, MAX)
}
