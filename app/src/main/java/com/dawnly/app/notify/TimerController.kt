package com.dawnly.app.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.dawnly.app.R
import com.dawnly.app.data.sound.AlertSound
import com.dawnly.app.data.time.TimeStore
import com.dawnly.app.domain.time.TimerPhase
import com.dawnly.app.domain.time.TimerState

/** One elapsed-realtime exact alarm, separate from Clock alarms and Dawnly's reminders. */
object TimerController {
    const val RING_ID = -2L
    internal const val ACTION_EXPIRED = "com.dawnly.app.action.TIMER_EXPIRED"
    internal const val EXTRA_GENERATION = "com.dawnly.app.extra.TIMER_GENERATION"
    private const val REQUEST_CODE = 200_002

    enum class Result { Success, ExactAlarmRequired, InvalidDuration, StorageError, SchedulingFailed }

    fun canScheduleExact(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

    fun start(context: Context, durationMs: Long): Result = synchronized(TimeStore) {
        if (durationMs !in 1..TimerState.MAX_DURATION_MS) return@synchronized Result.InvalidDuration
        if (!canScheduleExact(context)) return@synchronized Result.ExactAlarmRequired
        val state = TimeStore.current(context)
        val timer = state.timer.start(durationMs, TimeStore.now(context))
        if (timer == state.timer) return@synchronized Result.Success
        if (!TimeStore.save(context, state.copy(timer = timer))) return@synchronized Result.StorageError
        clearCompletion(context)
        scheduleOrPause(context, timer)
    }

    fun pause(context: Context): Result = synchronized(TimeStore) {
        val state = TimeStore.current(context)
        val timer = state.timer.pause(TimeStore.now(context))
        if (!TimeStore.save(context, state.copy(timer = timer))) return@synchronized Result.StorageError
        cancelAlarm(context)
        deliverCompletion(context)
        Result.Success
    }

    fun resume(context: Context): Result = synchronized(TimeStore) {
        if (!canScheduleExact(context)) return@synchronized Result.ExactAlarmRequired
        val state = TimeStore.current(context)
        val timer = state.timer.resume(TimeStore.now(context))
        if (timer == state.timer) return@synchronized Result.Success
        if (!TimeStore.save(context, state.copy(timer = timer))) return@synchronized Result.StorageError
        scheduleOrPause(context, timer)
    }

    fun reset(context: Context): Result = synchronized(TimeStore) {
        val state = TimeStore.current(context)
        // Invalidate delivery first, including an already queued broadcast, then cancel the OS alarm.
        if (!TimeStore.save(context, state.copy(timer = state.timer.reset()))) return@synchronized Result.StorageError
        cancelAlarm(context)
        clearCompletion(context)
        Result.Success
    }

    /** Call after boot/package replacement and on app resume; safe to repeat. Also reconciles stopwatch boot state. */
    fun sync(context: Context): Result = synchronized(TimeStore) {
        val state = TimeStore.current(context).checkpoint(TimeStore.now(context))
        if (!TimeStore.save(context, state)) return@synchronized Result.StorageError
        if (state.timer.phase == TimerPhase.Running) {
            scheduleOrPause(context, state.timer)
        } else {
            cancelAlarm(context)
            deliverCompletion(context)
            Result.Success
        }
    }

    /** Called by ReminderRingService for RING_ID; a newer timer is never replaced by an old snooze. */
    fun snooze(context: Context, minutes: Int): Result = synchronized(TimeStore) {
        val duration = minutes.toLong() * 60_000L
        if (duration !in 1..TimerState.MAX_DURATION_MS) return@synchronized Result.InvalidDuration
        if (TimeStore.current(context).timer.phase != TimerPhase.Finished) return@synchronized Result.Success
        val result = start(context, duration)
        if (result == Result.ExactAlarmRequired) {
            // The service will stop ringing after snooze. Keep the requested snooze visible, not lost.
            val state = TimeStore.current(context)
            val paused = state.timer.start(duration, TimeStore.now(context)).pause(TimeStore.now(context))
            if (!TimeStore.save(context, state.copy(timer = paused))) return@synchronized Result.StorageError
            cancelAlarm(context)
            TimerNotifications.showPaused(context)
        }
        result
    }

    internal fun receive(context: Context, generation: Long) = synchronized(TimeStore) {
        val timer = TimeStore.current(context).timer
        if (timer.generation != generation) return@synchronized
        val now = TimeStore.now(context)
        // Early broadcasts may rearm, but only a current, due generation may ring. A checkpoint may
        // already have persisted Finished before this broadcast arrived, so deliver that pending claim too.
        if (timer.acceptsAlarm(generation, now) || timer.completionPending) {
            sync(context)
        } else if (timer.phase == TimerPhase.Running) {
            sync(context)
        }
    }

    private fun scheduleOrPause(context: Context, timer: TimerState): Result {
        if (timer.phase != TimerPhase.Running) return Result.Success
        var result = Result.ExactAlarmRequired
        if (canScheduleExact(context)) {
            try {
                context.getSystemService(AlarmManager::class.java).setExactAndAllowWhileIdle(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    timer.anchorMs + timer.remainingMs,
                    pending(context, timer.generation),
                )
                return Result.Success
            } catch (e: SecurityException) {
                Log.w("TimeTools", "Exact timer permission was revoked", e)
            } catch (e: RuntimeException) {
                Log.w("TimeTools", "Could not schedule timer", e)
                result = Result.SchedulingFailed
            }
        }
        val state = TimeStore.current(context)
        val paused = state.timer.pause(TimeStore.now(context))
        if (!TimeStore.save(context, state.copy(timer = paused))) return Result.StorageError
        cancelAlarm(context)
        deliverCompletion(context)
        return result
    }

    private fun deliverCompletion(context: Context) {
        val state = TimeStore.current(context)
        if (state.timer.phase != TimerPhase.Finished || !state.timer.completionPending) return
        // The ringing service owns the sound and its own notification; posting a second alert here
        // would double the sound and stack two notices. Only fall back to a notice if it can't ring.
        val ringing = ReminderRingService.start(
            context,
            id = RING_ID,
            name = context.getString(R.string.time_timer),
            why = context.getString(R.string.time_timer_finished),
            sound = AlertSound.PhoneDefault,
        )
        val alerted = ringing || TimerNotifications.showFinished(context)
        if (alerted) {
            TimeStore.save(context, state.copy(timer = state.timer.completionDelivered()))
        }
        // If both are blocked, Finished + completionPending remain durable; sync can retry later.
    }

    private fun clearCompletion(context: Context) {
        TimerNotifications.cancel(context)
        if (ReminderRingService.ringing.value?.id == RING_ID) ReminderRingService.stop(context)
    }

    private fun intent(context: Context) = Intent(context, TimerReceiver::class.java).setAction(ACTION_EXPIRED)

    private fun pending(context: Context, generation: Long): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_CODE,
        intent(context).putExtra(EXTRA_GENERATION, generation),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun cancelAlarm(context: Context) {
        val pending = PendingIntent.getBroadcast(
            context, REQUEST_CODE, intent(context), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE,
        ) ?: return
        context.getSystemService(AlarmManager::class.java).cancel(pending)
        pending.cancel()
    }
}
