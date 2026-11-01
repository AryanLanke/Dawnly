package com.dawnly.app.data.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.dawnly.app.data.safeData
import com.dawnly.app.domain.watch.WatchWindow
import java.time.Duration
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private val Context.settingsStore by preferencesDataStore(name = "settings")

/** The user's settings. */
class SettingsRepository(context: Context) {
    private val store = context.applicationContext.settingsStore

    val watchWindow: Flow<Duration> = store.safeData
        .map { prefs ->
            prefs[WATCH_WINDOW_MINUTES]
                ?.let { WatchWindow.clamp(Duration.ofMinutes(it.toLong())) }
                ?: WatchWindow.DEFAULT
        }
        .distinctUntilChanged()

    suspend fun setWatchWindow(window: Duration) {
        store.edit { it[WATCH_WINDOW_MINUTES] = WatchWindow.clamp(window).toMinutes().toInt() }
    }

    private companion object {
        val WATCH_WINDOW_MINUTES = intPreferencesKey("watch_window_minutes")
    }
}
