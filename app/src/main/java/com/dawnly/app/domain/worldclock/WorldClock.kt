package com.dawnly.app.domain.worldclock

import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.abs

/**
 * The world clock's sums, kept pure so they can be tested without a phone: what time it is in a
 * city, and how far that is from the phone's own time. Offsets come from the zone rules at [now],
 * so daylight saving is included, all offline.
 */
object WorldClock {
    /** The time it is in [zone] at [now]. */
    fun currentTime(zone: ZoneId, now: Instant): LocalTime = now.atZone(zone).toLocalTime()

    /** How far [zone] is ahead of [local] at [now], in seconds; negative when behind. */
    fun offsetSeconds(zone: ZoneId, now: Instant, local: ZoneId = ZoneId.systemDefault()): Int =
        zone.rules.getOffset(now).totalSeconds - local.rules.getOffset(now).totalSeconds

    /** The offset as a short label: "+5h 30m", "-8h", "+15m", or "0h" for the same time. */
    fun offsetLabel(zone: ZoneId, now: Instant, local: ZoneId = ZoneId.systemDefault()): String {
        val seconds = offsetSeconds(zone, now, local)
        if (seconds == 0) return "0h"
        val sign = if (seconds < 0) "-" else "+"
        val hours = abs(seconds) / 3_600
        val minutes = abs(seconds) % 3_600 / 60
        return when {
            hours == 0 -> "$sign${minutes}m"
            minutes == 0 -> "$sign${hours}h"
            else -> "$sign${hours}h ${minutes}m"
        }
    }
}