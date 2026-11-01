package com.dawnly.app.data.wake

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.dawnly.app.data.safeData
import com.dawnly.app.domain.wake.WakeSignals
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.flow.first

private val Context.wakeProgressStore by preferencesDataStore(name = "wake_progress")

/** What was seen during the watch window for the alarm at [alarmAt]. */
data class WakeProgress(
    val alarmAt: Instant,
    val signals: WakeSignals,
    /** Whether the user was already told they seem awake. Asked at most once per window. */
    val notified: Boolean,
)

/**
 * Keeps the current window's [WakeProgress] on disk. If the system kills the process mid-window
 * (Xiaomi and Samsung often do), the restarted service carries on from it, and won't ask twice.
 * Internal state, not a setting. Only the latest window is kept.
 */
class WakeProgressStore(context: Context) {
    private val store = context.applicationContext.wakeProgressStore

    /** The saved progress for the alarm at [alarmAt], or null if it's for another alarm. */
    suspend fun load(alarmAt: Instant): WakeProgress? {
        val prefs = store.safeData.first()
        if (prefs[ALARM_AT] != alarmAt.toEpochMilli()) return null
        return WakeProgress(
            alarmAt = alarmAt,
            signals = WakeSignals(
                unlocked = prefs[UNLOCKED] ?: false,
                screenOnTime = Duration.ofMillis(prefs[SCREEN_ON_MILLIS] ?: 0L),
                steps = prefs[STEPS] ?: 0,
            ),
            notified = prefs[NOTIFIED] ?: false,
        )
    }

    suspend fun save(progress: WakeProgress) {
        store.edit { prefs ->
            prefs[ALARM_AT] = progress.alarmAt.toEpochMilli()
            prefs[UNLOCKED] = progress.signals.unlocked
            prefs[SCREEN_ON_MILLIS] = progress.signals.screenOnTime.toMillis()
            prefs[STEPS] = progress.signals.steps
            prefs[NOTIFIED] = progress.notified
        }
    }

    private companion object {
        val ALARM_AT = longPreferencesKey("alarm_at")
        val UNLOCKED = booleanPreferencesKey("unlocked")
        val SCREEN_ON_MILLIS = longPreferencesKey("screen_on_millis")
        val STEPS = intPreferencesKey("steps")
        val NOTIFIED = booleanPreferencesKey("notified")
    }
}
