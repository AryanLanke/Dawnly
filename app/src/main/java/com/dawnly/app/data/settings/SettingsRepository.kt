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

/** Light, dark, follow the phone, or follow the clock (night from 7 pm to 6 am, see `NightClock`). Clock is the default. */
enum class Appearance { Light, Dark, System, Clock }

/**
 * What Dawnly does once it's sure the user is up (the user's three choices, 2026-11-03). Both
 * automatic ones need "Display over other apps" (see `AwakeNotifier`).
 */
enum class DismissMode {
    /** A notification asks; the user taps "Dismiss alarm" themselves. The default. */
    Ask,

    /** "Dismissing your 5:30 alarm in 1 minute", which the user can stop, then it's dismissed. */
    Warn,

    /** Dismissed straight away. "Alarm dismissed" and the reversed sound say so afterwards. */
    Instant,
}

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

    /** Ask until the user picks otherwise. Saved before the three choices, auto-dismiss on meant [DismissMode.Warn]. */
    val dismissMode: Flow<DismissMode> = store.safeData
        .map { prefs ->
            prefs[DISMISS_MODE]?.let { name -> DismissMode.entries.firstOrNull { it.name == name } }
                ?: if (prefs[OLD_AUTO_DISMISS] == true) DismissMode.Warn else DismissMode.Ask
        }
        .distinctUntilChanged()

    suspend fun setDismissMode(mode: DismissMode) {
        store.edit {
            it[DISMISS_MODE] = mode.name
            it.remove(OLD_AUTO_DISMISS)
        }
    }

    /** Learning from the user's answers (see `Learning`). On until they turn it off. */
    val learn: Flow<Boolean> = store.safeData
        .map { it[LEARN] ?: true }
        .distinctUntilChanged()

    suspend fun setLearn(on: Boolean) {
        store.edit { it[LEARN] = on }
    }

    val appearance: Flow<Appearance> = store.safeData
        .map { prefs ->
            prefs[APPEARANCE]?.let { name -> Appearance.entries.firstOrNull { it.name == name } }
                ?: Appearance.Clock
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
        val OLD_AUTO_DISMISS = booleanPreferencesKey("auto_dismiss")
        val DISMISS_MODE = stringPreferencesKey("dismiss_mode")
        val APPEARANCE = stringPreferencesKey("appearance")
        val LEARN = booleanPreferencesKey("learn")
    }
}
