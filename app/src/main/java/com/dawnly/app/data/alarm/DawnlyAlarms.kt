package com.dawnly.app.data.alarm

import android.content.Context
import com.dawnly.app.domain.alarm.AlarmTimes
import java.time.Instant

/**
 * The alarms set from Dawnly (in the Clock app), by when they ring. Only these can be changed or
 * deleted from Today: they're one-off alarms, so switching one off in the Clock app removes it
 * for good. An alarm the user made in the Clock app may repeat, so Dawnly leaves it to the Clock app.
 */
class DawnlyAlarms(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun remember(at: Instant) = save(current() + at.toEpochMilli())

    fun forget(at: Instant) = save(current().filterNot { AlarmTimes.same(it, at.toEpochMilli()) }.toSet())

    fun isOurs(at: Instant): Boolean = current().any { AlarmTimes.same(it, at.toEpochMilli()) }

    /** Those still to ring; ones that have rung are dropped. */
    private fun current(): Set<Long> {
        val now = System.currentTimeMillis()
        return prefs.getStringSet(KEY, emptySet()).orEmpty()
            .mapNotNull { it.toLongOrNull() }
            .filter { it > now - 60_000L }
            .toSet()
    }

    private fun save(times: Set<Long>) {
        prefs.edit().putStringSet(KEY, times.map { it.toString() }.toSet()).apply()
    }

    private companion object {
        const val FILE = "dawnly_alarms"
        const val KEY = "at"
    }
}
