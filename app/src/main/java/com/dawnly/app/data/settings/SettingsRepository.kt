package com.dawnly.app.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.dawnly.app.data.safeData
import com.dawnly.app.domain.wake.WakeSignalSettings
import com.dawnly.app.domain.watch.WatchWindow
import java.time.Duration
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private val Context.settingsStore by preferencesDataStore(name = "settings")

/** Light, dark, or follow the phone. */
enum class Appearance { Light, Dark, System }

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

    val wakeSignals: Flow<WakeSignalSettings> = store.safeData
        .map { prefs ->
            val default = WakeSignalSettings()
            WakeSignalSettings(
                useUnlock = prefs[USE_UNLOCK] ?: default.useUnlock,
                useScreenTime = prefs[USE_SCREEN_TIME] ?: default.useScreenTime,
                screenTimeGoal = prefs[SCREEN_TIME_GOAL_MINUTES]
                    ?.let { Duration.ofMinutes(it.toLong()) }
                    ?: default.screenTimeGoal,
                useSteps = prefs[USE_STEPS] ?: default.useSteps,
                stepsGoal = prefs[STEPS_GOAL] ?: default.stepsGoal,
            )
        }
        .distinctUntilChanged()

    suspend fun setWakeSignals(settings: WakeSignalSettings) {
        store.edit { prefs ->
            prefs[USE_UNLOCK] = settings.useUnlock
            prefs[USE_SCREEN_TIME] = settings.useScreenTime
            prefs[SCREEN_TIME_GOAL_MINUTES] = settings.screenTimeGoal.toMinutes().toInt()
            prefs[USE_STEPS] = settings.useSteps
            prefs[STEPS_GOAL] = settings.stepsGoal
        }
    }

    val appearance: Flow<Appearance> = store.safeData
        .map { prefs ->
            prefs[APPEARANCE]?.let { name -> Appearance.entries.firstOrNull { it.name == name } }
                ?: Appearance.System
        }
        .distinctUntilChanged()

    suspend fun setAppearance(appearance: Appearance) {
        store.edit { it[APPEARANCE] = appearance.name }
    }

    private companion object {
        val WATCH_WINDOW_MINUTES = intPreferencesKey("watch_window_minutes")
        val USE_UNLOCK = booleanPreferencesKey("use_unlock")
        val USE_SCREEN_TIME = booleanPreferencesKey("use_screen_time")
        val SCREEN_TIME_GOAL_MINUTES = intPreferencesKey("screen_time_goal_minutes")
        val USE_STEPS = booleanPreferencesKey("use_steps")
        val STEPS_GOAL = intPreferencesKey("steps_goal")
        val APPEARANCE = stringPreferencesKey("appearance")
    }
}
