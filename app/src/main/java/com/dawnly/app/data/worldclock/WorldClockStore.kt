package com.dawnly.app.data.worldclock

import android.content.Context
import androidx.core.content.edit
import java.time.ZoneId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The cities on Today's world clock, kept on the phone as their zone ids, in the order they were
 * added. Like the time tools' store, a small synchronous SharedPreferences store: the list is tiny
 * and only the screen touches it, so no coroutine scope is needed. Unknown ids (a zone the phone's
 * calendar no longer knows) are dropped on read, never shown.
 */
class WorldClockStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(NAME, Context.MODE_PRIVATE)
    private val mutable = MutableStateFlow(read())

    val zones: StateFlow<List<String>> = mutable.asStateFlow()

    /** Adds [zoneId] once; adding a city that's already there changes nothing. */
    fun add(zoneId: String) {
        if (zoneId in mutable.value || runCatching { ZoneId.of(zoneId) }.isFailure) return
        save(mutable.value + zoneId)
    }

    fun remove(zoneId: String) {
        if (zoneId !in mutable.value) return
        save(mutable.value.filterNot { it == zoneId })
    }

    private fun save(list: List<String>) {
        preferences.edit { putString(KEY, list.joinToString(SEPARATOR)) }
        mutable.value = list
    }

    private fun read(): List<String> = preferences.getString(KEY, null)
        ?.split(SEPARATOR)
        ?.filter { it.isNotBlank() && runCatching { ZoneId.of(it) }.isSuccess }
        .orEmpty()

    private companion object {
        const val NAME = "world_clock"
        const val KEY = "zones"
        // Zone ids never contain a pipe, so a joined string keeps the order a StringSet would lose.
        const val SEPARATOR = "|"
    }
}