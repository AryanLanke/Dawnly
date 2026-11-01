package com.dawnly.app.data.watch

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.dawnly.app.data.safeData
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private val Context.watchStateStore by preferencesDataStore(name = "watch_state")

/**
 * Remembers which alarm's window Dawnly scheduled ahead of time (see `WatchPlanner.decide`).
 * Internal state, not a setting. Kept on disk so watching can resume after a reboot.
 */
class ArmedAlarmStore(context: Context) {
    private val store = context.applicationContext.watchStateStore

    val armedFor: Flow<Instant?> = store.safeData
        .map { prefs -> prefs[ARMED_ALARM_AT]?.let(Instant::ofEpochMilli) }
        .distinctUntilChanged()

    suspend fun set(alarmAt: Instant?) {
        store.edit { prefs ->
            if (alarmAt == null) prefs.remove(ARMED_ALARM_AT)
            else prefs[ARMED_ALARM_AT] = alarmAt.toEpochMilli()
        }
    }

    private companion object {
        val ARMED_ALARM_AT = longPreferencesKey("armed_alarm_at")
    }
}
