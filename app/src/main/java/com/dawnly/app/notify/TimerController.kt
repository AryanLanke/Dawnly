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

/** One elapsed-realtime exact alarm per timer, separate from Clock alarms and Dawnly's reminders. */
object TimerController {
    internal const val ACTION_EXPIRED = "com.dawnly.app.action.TIMER_EXPIRED"
    internal const val ACTION_SNOOZE = "com.dawnly.app.action.TIMER_SNOOZE"
    internal const val EXTRA_GENERATION = "com.dawnly.app.extra.TIMER_GENERATION"
    internal const val EXTRA_ID = "com.dawnly.app.extra.TIMER_ID"
    private const val REQUEST_CODE_BASE = 200_002

    /** The notification's snooze length, the same as the ring's, so both behave alike. */
    const val SNOOZE_MINUTES = ReminderRingService.SNOOZE_MINUTES

    enum class Result { Success, ExactAlarmRequired, InvalidDuration, StorageError, SchedulingFailed }

    /** A distinct ring id per timer; negative so it never collides with reminders (positive) or tests. */
    fun ringId(id: Long): Long = -(100L + id)

    /**
     * The timer behind a ring id, or null when the ring belongs to a reminder, test, still-up check
     * or Dawnly alarm. Timer ids count up from 1, so their rings stay above the alarms' band
     * (see [DawnlyAlarmScheduler.ringId]).
     */
    fun timerIdFromRing(ringId: Long): Long? = if (ringId in -1_000_000L..-101L) -ringId - 100L else null

    private fun requestCode(id: Long): Int = REQUEST_CODE_BASE + id.toInt()

    fun canScheduleExact(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

    /** Creates a new timer and starts it. The id is unique among the saved timers. */
    fun add(context: Context, label: String, durationMs: Long, sound: String?, ringOnFinish: Boolean): Result = synchronized(TimeStore) {
        if (durationMs !in 1..TimerState.MAX_DURATION_MS) return@synchronized Result.InvalidDuration
        if (!canScheduleExact(context)) return@synchronized Result.ExactAlarmRequired
        val state = TimeStore.current(context)
        val ready = TimerState(
            durationMs = durationMs, remainingMs = durationMs,
            id = state.nextId(), label = label, sound = sound, ringOnFinish = ringOnFinish,
        )
        val timer = ready.start(durationMs, TimeStore.now(context))
        if (!TimeStore.save(context, state.add(timer))) return@synchronized Result.StorageError
        scheduleOrPause(context, timer)
    }

    fun delete(context: Context, id: Long): Result = synchronized(TimeStore) {
        val state = TimeStore.current(context)
        if (state.timer(id) == null) return@synchronized Result.Success
        if (!TimeStore.save(context, state.remove(id))) return@synchronized Result.StorageError
        cancelAlarm(context, id)
        clearCompletion(context, id)
        Result.Success
    }

    /** Changes a timer's name, tone or finish mode without disturbing its countdown. */
    fun update(context: Context, id: Long, label: String, sound: String?, ringOnFinish: Boolean): Result = synchronized(TimeStore) {
        val state = TimeStore.current(context)
        val timer = state.timer(id) ?: return@synchronized Result.Success
        val changed = timer.copy(label = label, sound = sound, ringOnFinish = ringOnFinish)
        if (changed == timer) return@synchronized Result.Success
        if (!TimeStore.save(context, state.update(id) { changed })) return@synchronized Result.StorageError
        Result.Success
    }

    fun start(context: Context, id: Long, durationMs: Long): Result = synchronized(TimeStore) {
        if (durationMs !in 1..TimerState.MAX_DURATION_MS) return@synchronized Result.InvalidDuration
        if (!canScheduleExact(context)) return@synchronized Result.ExactAlarmRequired
        val state = TimeStore.current(context)
        val existing = state.timer(id) ?: return@synchronized Result.Success
        val timer = existing.start(durationMs, TimeStore.now(context))
        if (timer == existing) return@synchronized Result.Success
        if (!TimeStore.save(context, state.update(id) { timer })) return@synchronized Result.StorageError
        clearCompletion(context, id)
        scheduleOrPause(context, timer)
    }

    fun pause(context: Context, id: Long): Result = synchronized(TimeStore) {
        val state = TimeStore.current(context)
        val existing = state.timer(id) ?: return@synchronized Result.Success
        val timer = existing.pause(TimeStore.now(context))
        if (!TimeStore.save(context, state.update(id) { timer })) return@synchronized Result.StorageError
        cancelAlarm(context, id)
        deliverCompletion(context, id)
        Result.Success
    }

    fun resume(context: Context, id: Long): Result = synchronized(TimeStore) {
        if (!canScheduleExact(context)) return@synchronized Result.ExactAlarmRequired
        val state = TimeStore.current(context)
        val existing = state.timer(id) ?: return@synchronized Result.Success
        val timer = existing.resume(TimeStore.now(context))
        if (timer == existing) return@synchronized Result.Success
        if (!TimeStore.save(context, state.update(id) { timer })) return@synchronized Result.StorageError
        scheduleOrPause(context, timer)
    }

    fun reset(context: Context, id: Long): Result = synchronized(TimeStore) {
        val state = TimeStore.current(context)
        val existing = state.timer(id) ?: return@synchronized Result.Success
        // Invalidate delivery first, including an already queued broadcast, then cancel the OS alarm.
        if (!TimeStore.save(context, state.update(id) { existing.reset() })) return@synchronized Result.StorageError
        cancelAlarm(context, id)
        clearCompletion(context, id)
        Result.Success
    }

    /** Call after boot/package replacement and on app resume; safe to repeat. Also reconciles stopwatch boot state. */
    fun sync(context: Context): Result = synchronized(TimeStore) {
        val state = TimeStore.current(context).checkpoint(TimeStore.now(context))
        if (!TimeStore.save(context, state)) return@synchronized Result.StorageError
        var result = Result.Success
        for (timer in state.timers) {
            val outcome = if (timer.phase == TimerPhase.Running) {
                scheduleOrPause(context, timer)
            } else {
                cancelAlarm(context, timer.id)
                deliverCompletion(context, timer.id)
                Result.Success
            }
            if (outcome != Result.Success && result == Result.Success) result = outcome
        }
        result
    }

    /** Called by ReminderRingService for a timer's ring id; a newer timer is never replaced by an old snooze. */
    fun snooze(context: Context, id: Long, minutes: Int): Result = synchronized(TimeStore) {
        val duration = minutes.toLong() * 60_000L
        if (duration !in 1..TimerState.MAX_DURATION_MS) return@synchronized Result.InvalidDuration
        if (TimeStore.current(context).timer(id)?.phase != TimerPhase.Finished) return@synchronized Result.Success
        val result = start(context, id, duration)
        if (result == Result.ExactAlarmRequired) {
            // The service will stop ringing after snooze. Keep the requested snooze visible, not lost.
            val state = TimeStore.current(context)
            val existing = state.timer(id) ?: return@synchronized Result.StorageError
            val paused = existing.start(duration, TimeStore.now(context)).pause(TimeStore.now(context))
            if (!TimeStore.save(context, state.update(id) { paused })) return@synchronized Result.StorageError
            cancelAlarm(context, id)
            TimerNotifications.showPaused(context, id)
        }
        result
    }

    internal fun receive(context: Context, id: Long, generation: Long) = synchronized(TimeStore) {
        val timer = TimeStore.current(context).timer(id) ?: return@synchronized
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
                    pending(context, timer.id, timer.generation),
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
        val existing = state.timer(timer.id) ?: return result
        val paused = existing.pause(TimeStore.now(context))
        if (!TimeStore.save(context, state.update(timer.id) { paused })) return Result.StorageError
        cancelAlarm(context, timer.id)
        deliverCompletion(context, timer.id)
        return result
    }

    private fun deliverCompletion(context: Context, id: Long) {
        val timer = TimeStore.current(context).timer(id) ?: return
        if (timer.phase != TimerPhase.Finished || !timer.completionPending) return
        val sound = timer.sound?.let { name -> AlertSound.entries.firstOrNull { it.name == name } } ?: AlertSound.PhoneDefault
        val label = timer.label
        val finished = context.getString(R.string.time_timer_finished)
        // The ringing service owns the sound and its own notification; posting a second alert here
        // would double the sound and stack two notices. Only fall back to a notice if it can't ring.
        val alerted = if (timer.ringOnFinish && ReminderRingService.ringing.value == null) {
            ReminderRingService.start(context, ringId(id), label, finished, sound) ||
                TimerNotifications.showFinished(context, id, label)
        } else {
            // Notification mode, or another timer already owns the ring: don't stack a second ring.
            TimerNotifications.showFinished(context, id, label)
        }
        if (alerted) {
            TimeStore.save(context, TimeStore.current(context).update(id) { it.completionDelivered() })
        }
        // If both are blocked, Finished + completionPending remain durable; sync can retry later.
    }

    private fun clearCompletion(context: Context, id: Long) {
        TimerNotifications.cancel(context, id)
        if (ReminderRingService.ringing.value?.id == ringId(id)) ReminderRingService.stop(context)
    }

    private fun intent(context: Context, id: Long) =
        Intent(context, TimerReceiver::class.java).setAction(ACTION_EXPIRED).putExtra(EXTRA_ID, id)

    /** The finished notification's Snooze button: rings [id] again [SNOOZE_MINUTES] from now. */
    fun snoozePending(context: Context, id: Long): PendingIntent = PendingIntent.getBroadcast(
        context,
        requestCode(id),
        Intent(context, TimerReceiver::class.java).setAction(ACTION_SNOOZE).putExtra(EXTRA_ID, id),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun pending(context: Context, id: Long, generation: Long): PendingIntent = PendingIntent.getBroadcast(
        context,
        requestCode(id),
        intent(context, id).putExtra(EXTRA_GENERATION, generation),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun cancelAlarm(context: Context, id: Long) {
        val pending = PendingIntent.getBroadcast(
            context, requestCode(id), intent(context, id), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE,
        ) ?: return
        context.getSystemService(AlarmManager::class.java).cancel(pending)
        pending.cancel()
    }
}
