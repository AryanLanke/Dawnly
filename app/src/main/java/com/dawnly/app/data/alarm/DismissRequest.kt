package com.dawnly.app.data.alarm

import java.time.LocalTime

/**
 * The forms of `ACTION_DISMISS_ALARM` Dawnly can send. Clock apps differ in which they act on:
 * Google Clock takes [Next], but Xiaomi's Clock opened and left the alarm on (a friend's Redmi,
 * 2026-11-03). So the forms are tried in turn until the next alarm moves (see `QuietDismiss`), and
 * the one that worked is tried first next time.
 */
enum class DismissRequest {
    /** "The next alarm", asking the Clock app not to show its screen. */
    Next,

    /** The alarm at its time, for Clock apps that only search by time. */
    ByTime,

    /** By time, letting the Clock app show its screen: some only dismiss with it showing. */
    ByTimeShown,
    ;

    companion object {
        /**
         * The forms to try, [preferred] (the one that last worked with this Clock app) first.
         * Searching by time needs the alarm's time ([haveTime]).
         */
        fun order(preferred: DismissRequest?, haveTime: Boolean): List<DismissRequest> {
            val usable = entries.filter { haveTime || it == Next }
            return listOfNotNull(preferred?.takeIf { it in usable }) + usable.filter { it != preferred }
        }
    }
}

/**
 * The time extras of a by-time search. The hour is 0–23 with [isPm] set to match: Clock apps
 * built on Android's own read either a 24-hour hour or a 12-hour one plus [isPm], and this
 * reads right both ways.
 */
data class TimeSearch(val hour: Int, val minutes: Int, val isPm: Boolean) {
    companion object {
        fun of(time: LocalTime) = TimeSearch(time.hour, time.minute, isPm = time.hour >= 12)
    }
}
