package com.dawnly.app.domain.watch

import java.time.Duration

/** How long before the alarm Dawnly starts watching. Chosen by the user in settings. */
object WatchWindow {
    val DEFAULT: Duration = Duration.ofHours(1)
    val PRESETS: List<Duration> = listOf(Duration.ofMinutes(30), Duration.ofHours(1), Duration.ofHours(2))

    /** The custom window's minus and plus move by this. */
    val FINE_STEP: Duration = Duration.ofMinutes(1)

    /** Allowed, but shorter than [RECOMMENDED_MIN] leaves little time to notice someone is up. */
    val MIN: Duration = Duration.ofMinutes(1)

    /** Below this, Settings says the window is very short. */
    val RECOMMENDED_MIN: Duration = Duration.ofMinutes(5)

    /** Up to this the slider moves a minute at a time; past it, 5 minutes. */
    private val FINE_UNTIL: Duration = Duration.ofMinutes(30)

    /** Battery-friendly cap: Dawnly never watches all night. */
    val MAX: Duration = Duration.ofHours(3)

    /**
     * Where the custom window's slider can stop: every minute up to [FINE_UNTIL], where exact
     * times matter, then every 5 minutes up to [MAX].
     */
    val SLIDER_STOPS: List<Duration> = buildList {
        var minutes = MIN.toMinutes()
        while (minutes <= MAX.toMinutes()) {
            add(Duration.ofMinutes(minutes))
            minutes += if (minutes < FINE_UNTIL.toMinutes()) 1 else 5
        }
    }

    fun clamp(window: Duration): Duration = window.coerceIn(MIN, MAX)
}
