package com.dawnly.app.data.time

import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import com.dawnly.app.domain.time.StopwatchState
import com.dawnly.app.domain.time.TimeReading
import com.dawnly.app.domain.time.TimeToolsState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** One lock covers preferences, UI commands, and receiver delivery in the application's process. */
object TimeStore {
    private var preferences: SharedPreferences? = null
    private val mutable = MutableStateFlow(TimeToolsState())
    private val exposed = mutable.asStateFlow()

    @Synchronized
    fun states(context: Context): StateFlow<TimeToolsState> {
        current(context)
        return exposed
    }

    @Synchronized
    internal fun current(context: Context): TimeToolsState {
        if (preferences == null) {
            preferences = context.applicationContext.getSharedPreferences("time_tools", Context.MODE_PRIVATE)
            mutable.value = TimeStateJson.decode(preferences!!.getString("state", null))
        }
        return mutable.value
    }

    /** Commit before scheduling or ringing: apply() could lose a cancellation on process death. */
    @Synchronized
    internal fun save(context: Context, state: TimeToolsState): Boolean {
        if (current(context) == state) return true
        if (!preferences!!.edit().putString("state", TimeStateJson.encode(state)).commit()) {
            Log.e("TimeTools", "Could not persist time tools state")
            return false
        }
        mutable.value = state
        return true
    }

    fun now(context: Context): TimeReading {
        val boot = try {
            Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, -1)
        } catch (_: RuntimeException) { -1 }
        return TimeReading(SystemClock.elapsedRealtime(), boot)
    }

    /** Checkpoint only, without claiming completion. TimerController.sync delivers due timers. */
    @Synchronized
    fun checkpoint(context: Context): Boolean = save(context, current(context).checkpoint(now(context)))

    @Synchronized
    fun startStopwatch(context: Context): Boolean = editStopwatch(context) { it.start(now(context)) }

    @Synchronized
    fun pauseStopwatch(context: Context): Boolean = editStopwatch(context) { it.pause(now(context)) }

    @Synchronized
    fun lap(context: Context): Boolean = editStopwatch(context) { it.lap(now(context)) }

    @Synchronized
    fun resetStopwatch(context: Context): Boolean = editStopwatch(context) { it.reset() }

    private fun editStopwatch(context: Context, change: (StopwatchState) -> StopwatchState): Boolean {
        val state = current(context)
        return save(context, state.copy(stopwatch = change(state.stopwatch)))
    }
}
