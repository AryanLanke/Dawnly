package com.dawnly.app.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.dawnly.app.data.safeData
import com.dawnly.app.domain.wake.Sensitivity
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
            val screenTimeGoal = prefs[SCREEN_TIME_GOAL_MINUTES]
                ?.let { Duration.ofMinutes(it.toLong()) }
                ?: default.screenTimeGoal
            val stepsGoal = prefs[STEPS_GOAL] ?: default.stepsGoal
            // Saved before apps opened and movement existed: give them the goals of the level the
            // user picked, so Settings still shows that level.
            val level = Sensitivity.entries
                .firstOrNull { it.screenTimeGoal == screenTimeGoal && it.stepsGoal == stepsGoal }
                ?: Sensitivity.Medium
            WakeSignalSettings(
                useScreenTime = prefs[USE_SCREEN_TIME] ?: default.useScreenTime,
                screenTimeGoal = screenTimeGoal,
                usePhoneUse = prefs[USE_PHONE_USE] ?: default.usePhoneUse,
                appsOpenedGoal = prefs[APPS_OPENED_GOAL] ?: level.appsOpenedGoal,
                useSteps = prefs[USE_STEPS] ?: default.useSteps,
                stepsGoal = stepsGoal,
                useMovement = prefs[USE_MOVEMENT] ?: default.useMovement,
                movementGoal = prefs[MOVEMENT_GOAL_MINUTES]
                    ?.let { Duration.ofMinutes(it.toLong()) }
                    ?: level.movementGoal,
                useMusic = prefs[USE_MUSIC] ?: default.useMusic,
            )
        }
        .distinctUntilChanged()

    suspend fun setWakeSignals(settings: WakeSignalSettings) {
        store.edit { prefs ->
            prefs[USE_SCREEN_TIME] = settings.useScreenTime
            prefs[SCREEN_TIME_GOAL_MINUTES] = settings.screenTimeGoal.toMinutes().toInt()
            prefs[USE_PHONE_USE] = settings.usePhoneUse
            prefs[APPS_OPENED_GOAL] = settings.appsOpenedGoal
            prefs[USE_STEPS] = settings.useSteps
            prefs[STEPS_GOAL] = settings.stepsGoal
            prefs[USE_MOVEMENT] = settings.useMovement
            prefs[MOVEMENT_GOAL_MINUTES] = settings.movementGoal.toMinutes().toInt()
            prefs[USE_MUSIC] = settings.useMusic
            // "Phone unlocked" was dropped as a sign: the phone can be unlocked while asleep.
            prefs.remove(OLD_USE_UNLOCK)
        }
    }

    /**
     * Dismiss the alarm without asking, after a one-minute warning that can be stopped. Off by
     * default; turning it on needs "Display over other apps" (see `AwakeNotifier`).
     */
    val autoDismiss: Flow<Boolean> = store.safeData
        .map { it[AUTO_DISMISS] ?: false }
        .distinctUntilChanged()

    suspend fun setAutoDismiss(on: Boolean) {
        store.edit { it[AUTO_DISMISS] = on }
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
        val OLD_USE_UNLOCK = booleanPreferencesKey("use_unlock")
        val USE_SCREEN_TIME = booleanPreferencesKey("use_screen_time")
        val SCREEN_TIME_GOAL_MINUTES = intPreferencesKey("screen_time_goal_minutes")
        val USE_PHONE_USE = booleanPreferencesKey("use_phone_use")
        val APPS_OPENED_GOAL = intPreferencesKey("apps_opened_goal")
        val USE_STEPS = booleanPreferencesKey("use_steps")
        val STEPS_GOAL = intPreferencesKey("steps_goal")
        val USE_MOVEMENT = booleanPreferencesKey("use_movement")
        val MOVEMENT_GOAL_MINUTES = intPreferencesKey("movement_goal_minutes")
        val USE_MUSIC = booleanPreferencesKey("use_music")
        val AUTO_DISMISS = booleanPreferencesKey("auto_dismiss")
        val APPEARANCE = stringPreferencesKey("appearance")
    }
}
