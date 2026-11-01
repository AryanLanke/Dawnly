package com.dawnly.app.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.dawnly.app.data.alarm.NextAlarmSource
import com.dawnly.app.data.settings.SettingsRepository
import com.dawnly.app.data.watch.ArmedAlarmStore
import com.dawnly.app.domain.watch.WatchPlanner
import com.dawnly.app.domain.watch.WatchState
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Keeps the watch window in step with the next alarm. [reconcile] is the single entry point:
 * every trigger (app open, settings change, next alarm changed, reboot, our own alarm) calls it,
 * and it works out the state from scratch, so it's safe to call any number of times.
 */
class WatchScheduler(
    context: Context,
    private val nextAlarms: NextAlarmSource,
    private val settings: SettingsRepository,
    private val armed: ArmedAlarmStore,
) {
    private val appContext = context.applicationContext
    private val alarmManager = appContext.getSystemService(AlarmManager::class.java)
    private val mutex = Mutex()

    /** False when the user has turned off "Alarms & reminders" (Android 12+). */
    fun canScheduleExactAlarms(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    /** The live state, for the UI. The service itself is driven by [reconcile]. */
    val state: Flow<WatchState> = combine(
        nextAlarms.nextAlarm,
        settings.watchWindow,
        armed.armedFor,
        ticker(Duration.ofSeconds(15)),
    ) { alarm, window, armedFor, now ->
        WatchPlanner.decide(alarm?.triggerAt, window, now, armedFor)
    }

    /**
     * Schedules the next check and starts or stops [WatchService] to match the current state.
     *
     * @param fromCheckAlarm true when called because our own exact alarm fired. That alarm is what
     *   lets Android start the service from the background, so if it fails there it isn't retried.
     */
    suspend fun reconcile(fromCheckAlarm: Boolean = false): WatchState = mutex.withLock {
        val state = WatchPlanner.decide(
            alarmAt = nextAlarms.current()?.triggerAt,
            window = settings.watchWindow.first(),
            now = Instant.now(),
            armedFor = armed.armedFor.first(),
        )
        Log.i(TAG, "reconcile(fromCheckAlarm=$fromCheckAlarm): $state")
        when (state) {
            WatchState.Idle, is WatchState.Skipped -> {
                cancelCheck()
                armed.set(null)
                stopWatching()
            }
            is WatchState.Waiting -> {
                armed.set(state.alarmAt)
                scheduleCheck(state.startsAt)
                stopWatching()
            }
            is WatchState.Watching -> {
                // Check again when the alarm is due, so the service stops on time.
                scheduleCheck(state.alarmAt)
                startWatching(state, fromCheckAlarm)
            }
        }
        state
    }

    private fun startWatching(state: WatchState.Watching, fromCheckAlarm: Boolean) {
        if (WatchService.isRunning.value) return
        try {
            ContextCompat.startForegroundService(appContext, WatchService.startIntent(appContext, state.alarmAt))
        } catch (e: IllegalStateException) {
            // ForegroundServiceStartNotAllowedException (Android 12+): we're in the background,
            // e.g. reacting to a next-alarm change. An exact alarm firing is allowed to start the
            // service, so check again right away through one.
            if (fromCheckAlarm) {
                Log.w(TAG, "Couldn't start watching, even from our alarm", e)
            } else {
                Log.i(TAG, "Can't start watching from the background; retrying through an alarm")
                scheduleCheck(Instant.now())
            }
        }
    }

    private fun stopWatching() {
        if (WatchService.isRunning.value) appContext.stopService(Intent(appContext, WatchService::class.java))
    }

    /**
     * Only one check is ever pending: the window start while waiting, or the alarm time while
     * watching. Never use `setAlarmClock` here: it would become the "next alarm" we read.
     */
    private fun scheduleCheck(at: Instant) {
        val triggerAt = at.toEpochMilli()
        val operation = checkIntent()
        if (canScheduleExactAlarms()) {
            try {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, operation)
                return
            } catch (e: SecurityException) {
                Log.w(TAG, "Exact alarm permission was just revoked", e)
            }
        }
        // Fallback: Android may run this up to an hour late (seen on Android 16), which can be the
        // whole window. Onboarding must make the case for "Alarms & reminders".
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, operation)
    }

    private fun cancelCheck() = alarmManager.cancel(checkIntent())

    private fun checkIntent(): PendingIntent = PendingIntent.getBroadcast(
        appContext,
        0,
        Intent(appContext, WatchAlarmReceiver::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private companion object {
        const val TAG = "WatchScheduler"

        fun ticker(period: Duration): Flow<Instant> = flow {
            while (true) {
                emit(Instant.now())
                delay(period.toMillis())
            }
        }
    }
}
