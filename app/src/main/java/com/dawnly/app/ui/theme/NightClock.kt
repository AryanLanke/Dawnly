package com.dawnly.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import kotlinx.coroutines.delay

/**
 * The hours of the night sky for the "Clock" appearance: from 7 pm until 6 am the app looks like
 * night (dark), the rest of the day like morning (light).
 */
object NightClock {
    val NightStarts: LocalTime = LocalTime.of(19, 0)
    val NightEnds: LocalTime = LocalTime.of(6, 0)

    fun isNight(time: LocalTime): Boolean = !time.isBefore(NightStarts) || time.isBefore(NightEnds)

    /** How long until the look next changes: the next 6 am or 7 pm after [now]. */
    fun untilChange(now: LocalDateTime): Duration {
        val boundary = if (isNight(now.toLocalTime())) NightEnds else NightStarts
        var next = now.toLocalDate().atTime(boundary)
        if (!next.isAfter(now)) next = next.plusDays(1)
        return Duration.between(now, next)
    }
}

/**
 * Whether the clock says it's night. Checked again every time the app comes back on screen, and
 * at 6 am and 7 pm while it stays open, so the sky changes on its own.
 */
@Composable
fun clockSaysNight(): Boolean {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val night by produceState(NightClock.isNight(LocalTime.now()), lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                value = NightClock.isNight(LocalTime.now())
                // A second past the boundary, so the check lands on the new side of it.
                delay(NightClock.untilChange(LocalDateTime.now()).toMillis() + 1_000)
            }
        }
    }
    return night
}
