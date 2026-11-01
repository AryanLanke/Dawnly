package com.dawnly.app.ui.format

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.dawnly.app.R
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** A clock time split for display: big digits, and a small AM/PM marker when the phone uses one. */
data class ClockFace(val digits: String, val marker: String?)

/** Times follow the phone's 12/24-hour setting and locale. */
@Composable
@ReadOnlyComposable
fun formatTime(instant: Instant): String = pattern(instant, if (is24Hour()) "Hm" else "hm")

@Composable
@ReadOnlyComposable
fun clockFace(instant: Instant): ClockFace {
    if (is24Hour()) return ClockFace(pattern(instant, "Hm"), null)
    val full = DateFormat.getBestDateTimePattern(Locale.getDefault(), "hm")
    // Drop the marker (and the space next to it) from the pattern; show it separately, smaller.
    val digitsPattern = full.replace(Regex("\\s*a+\\s*"), "").trim()
    val zoned = instant.atZone(ZoneId.systemDefault())
    return ClockFace(
        digits = DateTimeFormatter.ofPattern(digitsPattern).format(zoned),
        marker = DateTimeFormatter.ofPattern("a").format(zoned),
    )
}

/** "Today", "Tomorrow", or a short date. */
@Composable
@ReadOnlyComposable
fun dayLabel(instant: Instant, now: Instant): String {
    val zone = ZoneId.systemDefault()
    val today = now.atZone(zone).toLocalDate()
    return when (instant.atZone(zone).toLocalDate()) {
        today -> stringResource(R.string.home_day_today)
        today.plusDays(1) -> stringResource(R.string.home_day_tomorrow)
        else -> pattern(instant, "EEEdMMM")
    }
}

/** "30 min", "1 hr", "1 hr 30 min". */
@Composable
@ReadOnlyComposable
fun formatDuration(duration: Duration): String {
    val hours = duration.toHours().toInt()
    val minutes = (duration.toMinutes() % 60).toInt()
    return when {
        hours == 0 -> stringResource(R.string.duration_min, minutes)
        minutes == 0 -> stringResource(R.string.duration_hr, hours)
        else -> stringResource(R.string.duration_hr_min, hours, minutes)
    }
}

@Composable
@ReadOnlyComposable
private fun is24Hour() = DateFormat.is24HourFormat(LocalContext.current)

private fun pattern(instant: Instant, skeleton: String): String {
    val pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), skeleton)
    return DateTimeFormatter.ofPattern(pattern).format(instant.atZone(ZoneId.systemDefault()))
}
