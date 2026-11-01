package com.dawnly.app.domain.watch

import java.time.Duration

/** How long before the alarm Dawnly starts watching. Chosen by the user in settings. */
object WatchWindow {
    val DEFAULT: Duration = Duration.ofHours(1)
    val PRESETS: List<Duration> = listOf(Duration.ofMinutes(30), Duration.ofHours(1), Duration.ofHours(2))

    /** Custom windows move in this step, between [MIN] and [MAX]. */
    val STEP: Duration = Duration.ofMinutes(5)
    val MIN: Duration = Duration.ofMinutes(5)

    /** Battery-friendly cap: Dawnly never watches all night. */
    val MAX: Duration = Duration.ofHours(3)

    fun clamp(window: Duration): Duration = window.coerceIn(MIN, MAX)
}
