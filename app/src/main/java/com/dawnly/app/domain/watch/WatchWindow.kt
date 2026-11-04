package com.dawnly.app.domain.watch

import java.time.Duration

/** How long before the alarm Dawnly starts watching. Chosen by the user in settings. */
object WatchWindow {
    val DEFAULT: Duration = Duration.ofHours(1)
    val PRESETS: List<Duration> = listOf(Duration.ofMinutes(30), Duration.ofHours(1), Duration.ofHours(2))

    /** The shortest window: less leaves no time to notice someone is up. */
    val MIN: Duration = Duration.ofMinutes(5)

    /** Below this (5, 6 or 7 minutes), Settings says the window is short. */
    val RECOMMENDED_MIN: Duration = Duration.ofMinutes(8)

    /**
     * Battery-friendly cap, so Dawnly never watches all night: the custom wheels go up to 3 hours
     * and any minutes, so 3 hr 02 min is kept as picked.
     */
    val MAX: Duration = Duration.ofHours(3).plusMinutes(59)

    fun clamp(window: Duration): Duration = window.coerceIn(MIN, MAX)
}
