package com.dawnly.app.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.dawnly.app.R
import com.dawnly.app.data.sound.AlertSound
import com.dawnly.app.dawnly
import com.dawnly.app.domain.alarm.DawnlyAlarm
import com.dawnly.app.domain.alarm.DawnlyAlarmTimes
import com.dawnly.app.domain.reminder.SnoozeTime
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.launch

/**
 * Times Dawnly's own alarms. Each one that's on is set with [AlarmManager.setAlarmClock], so it
 * becomes the system's "next alarm" (see `NextAlarmSource`) with Dawnly as its owner: the watch
 * follows it exactly like a Clock alarm, and the system's alarm icon opens Dawnly. [sync] sets
 * them all again from scratch, so it's safe to call after any change, a reboot or an app update.
 * Clock alarms are left alone: they ring through the Clock app, and Dawnly only watches them.
 */
object DawnlyAlarmScheduler {
    private const val TAG = "DawnlyAlarms"
    internal const val EXTRA_ID = "com.dawnly.app.extra.DAWNALY_ALARM_ID"

    /** On a snoozed alarm's one-off alarm: ring again, but leave its regular next time alone. */
    internal const val EXTRA_SNOOZE = "com.dawnly.app.extra.DAWNALY_ALARM_SNOOZE"
    private const val ACTION_SNOOZED = "com.dawnly.app.action.DAWNALY_ALARM_SNOOZED"

    private const val REQUEST_CODE_BASE = 300_000
    private const val KNOWN = "dawnly_alarm_alarms"
    private const val KNOWN_IDS = "ids"

    /** Android fixes a channel's sound, so each alarm sound has its own: `dawnly_alarm_<sound>`. */
    private const val CHANNEL_PREFIX = "dawnly_alarm_"

    /** A distinct ring id per alarm; far below reminders (positive), timers, the test and still-up. */
    fun ringId(id: Long): Long = -(1_000_000L + id)

    /** The alarm behind a ring id, or null when the ring belongs to a reminder, timer, test or still-up. */
    fun alarmIdFromRing(ringId: Long): Long? = if (ringId <= -1_000_001L) -ringId - 1_000_000L else null

    internal fun requestCode(id: Long): Int = REQUEST_CODE_BASE + id.toInt()

    /**
     * Cancels the alarms of deleted Dawnly alarms and sets the ones that are on for their next
     * time, then reconciles the watch so it follows whatever is next now.
     */
    suspend fun sync(context: Context) {
        val alarms = context.dawnly.dawnlyAlarms.current()
        val known = context.getSharedPreferences(KNOWN, Context.MODE_PRIVATE)
        // Alarms deleted since last time: cancel their alarms too.
        known.getStringSet(KNOWN_IDS, emptySet()).orEmpty().mapNotNull { it.toLongOrNull() }
            .filter { id -> alarms.none { it.id == id } }
            .forEach { cancel(context, it) }
        for (alarm in alarms) schedule(context, alarm)
        known.edit().putStringSet(KNOWN_IDS, alarms.map { it.id.toString() }.toSet()).apply()
        context.dawnly.watchScheduler.reconcile()
    }

    /** Sets [alarm]'s next occurrence, or cancels it when it's off. */
    fun schedule(context: Context, alarm: DawnlyAlarm) {
        if (!alarm.on) {
            cancel(context, alarm.id)
            return
        }
        // From a second on, so an alarm that just went off on time isn't set for the same moment.
        val next = DawnlyAlarmTimes.nextAfter(alarm, LocalDateTime.now().plusSeconds(1))
        if (next == null) {
            cancel(context, alarm.id)
            return
        }
        scheduleAt(context, alarm, next)
    }

    /** Rings alarm [id] again [minutes] from now, without touching its regular next time. */
    fun snooze(context: Context, id: Long, minutes: Int) {
        val at = SnoozeTime.after(System.currentTimeMillis(), minutes)
        setAlarmClock(context, at, snoozePending(context, id))
        Log.i(TAG, "Alarm $id snoozed until ${Instant.ofEpochMilli(at)}")
    }

    /**
     * Dismisses Dawnly's soonest alarm itself, no Clock app involved. A ringing one already has its
     * next occurrence set (the receiver set it when it fired), so only the ring and any snooze end.
     * A snooze still to ring is the soonest: it ends, leaving the regular next time alone.
     * Otherwise the pending occurrence is cancelled and the one after it is set: alarms repeat.
     * True when there was a Dawnly alarm to dismiss.
     */
    fun dismissNext(context: Context): Boolean {
        val app = context.dawnly
        val alarms = app.dawnlyAlarms.alarms.value
        val ringing = ReminderRingService.ringing.value?.id?.let(::alarmIdFromRing)
        if (ringing != null) {
            ReminderRingService.stop(context)
            cancelSnooze(context, ringing)
            app.scope.launch { app.watchScheduler.reconcile() }
            return true
        }
        val snoozed = alarms.firstOrNull { hasSnooze(context, it.id) }
        if (snoozed != null) {
            cancelSnooze(context, snoozed.id)
            Log.i(TAG, "Dismissed the snooze of \"${snoozed.label}\"")
            app.scope.launch { app.watchScheduler.reconcile() }
            return true
        }
        val now = LocalDateTime.now()
        val (alarm, at) = alarms.filter { it.on }
            .mapNotNull { a -> DawnlyAlarmTimes.nextAfter(a, now)?.let { a to it } }
            .minByOrNull { it.second } ?: return false
        val next = DawnlyAlarmTimes.nextAfter(alarm, at)
        cancel(context, alarm.id)
        next?.let { scheduleAt(context, alarm, it) }
        Log.i(TAG, "Dismissed \"${alarm.label}\" at $at; next $next")
        app.scope.launch { app.watchScheduler.reconcile() }
        return true
    }

    private fun scheduleAt(context: Context, alarm: DawnlyAlarm, at: LocalDateTime) {
        val millis = at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        setAlarmClock(context, millis, pending(context, alarm.id))
        Log.i(TAG, "\"${alarm.label}\" next at ${Instant.ofEpochMilli(millis)}")
    }

    /**
     * An alarm clock, so it shows in the system's alarm icon and becomes the next alarm the watch
     * reads. Without the exact permission it falls back to a plain exact alarm, then an inexact
     * one: late is better than never.
     */
    private fun setAlarmClock(context: Context, at: Long, operation: PendingIntent) {
        val alarms = context.getSystemService(AlarmManager::class.java)
        try {
            alarms.setAlarmClock(AlarmManager.AlarmClockInfo(at, Notify.openApp(context)), operation)
            return
        } catch (e: SecurityException) {
            Log.w(TAG, "Exact alarm permission was just revoked; setting a plain alarm", e)
        }
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms.canScheduleExactAlarms()) {
                alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, operation)
            } else {
                alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, operation)
            }
        } catch (e: SecurityException) {
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, operation)
        }
    }

    private fun cancel(context: Context, id: Long) {
        val alarms = context.getSystemService(AlarmManager::class.java)
        alarms.cancel(pending(context, id))
        alarms.cancel(snoozePending(context, id))
    }

    private fun cancelSnooze(context: Context, id: Long) {
        context.getSystemService(AlarmManager::class.java).cancel(snoozePending(context, id))
    }

    /** Whether alarm [id] has a snooze still to ring (FLAG_NO_CREATE finds it only while it's pending). */
    private fun hasSnooze(context: Context, id: Long): Boolean = PendingIntent.getBroadcast(
        context,
        requestCode(id),
        Intent(context, DawnlyAlarmReceiver::class.java).setAction(ACTION_SNOOZED),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE,
    ) != null

    private fun pending(context: Context, id: Long): PendingIntent = PendingIntent.getBroadcast(
        context,
        requestCode(id),
        Intent(context, DawnlyAlarmReceiver::class.java).putExtra(EXTRA_ID, id),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun snoozePending(context: Context, id: Long): PendingIntent = PendingIntent.getBroadcast(
        context,
        requestCode(id),
        // Its own action, so it's a different alarm from the alarm's regular one.
        Intent(context, DawnlyAlarmReceiver::class.java).setAction(ACTION_SNOOZED)
            .putExtra(EXTRA_ID, id).putExtra(EXTRA_SNOOZE, true),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    /**
     * The alarm as a notification whose sound repeats: for when Android won't let it take over the
     * screen (a full-screen ring after an inexact alarm, say). Never a second ring on top of one.
     */
    internal fun showFallback(context: Context, ringing: Ringing, sound: AlertSound) {
        val id = alarmIdFromRing(ringing.id) ?: return
        val builder = NotificationCompat.Builder(context, channelFor(context, sound))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(ringing.name)
            .setContentIntent(Notify.openApp(context))
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
        if (ringing.why.isNotBlank()) builder.setContentText(ringing.why)
        val notification = builder.build()
        notification.flags = notification.flags or android.app.Notification.FLAG_INSISTENT
        try {
            NotificationManagerCompat.from(context).notify(notificationId(id), notification)
        } catch (e: SecurityException) {
            Log.w(TAG, "Notifications are off, so \"${ringing.name}\" wasn't shown", e)
        }
    }

    /** A ringing alarm nobody stopped: left as a quiet notification, so it isn't lost. */
    internal fun showMissed(context: Context, ringing: Ringing) {
        val id = alarmIdFromRing(ringing.id) ?: return
        val builder = NotificationCompat.Builder(context, channelFor(context, AlertSound.PhoneDefault))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.ring_missed, ringing.name))
            .setContentIntent(Notify.openApp(context))
            .setAutoCancel(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
        if (ringing.why.isNotBlank()) builder.setContentText(ringing.why)
        try {
            NotificationManagerCompat.from(context).notify(notificationId(id), builder.build())
        } catch (e: SecurityException) {
            Log.w(TAG, "Notifications are off, so the missed alarm wasn't shown", e)
        }
    }

    /** The channel for [sound], made the first time an alarm uses it. */
    private fun channelFor(context: Context, sound: AlertSound): String {
        val id = SoundChannels.id(CHANNEL_PREFIX, sound)
        val manager = NotificationManagerCompat.from(context)
        if (manager.getNotificationChannelCompat(id) == null) {
            manager.createNotificationChannel(
                NotificationChannelCompat.Builder(id, NotificationManagerCompat.IMPORTANCE_HIGH)
                    .setName(context.getString(R.string.alarm_channel_sound, context.getString(sound.label)))
                    .setDescription(context.getString(R.string.alarm_channel_description))
                    .setSound(sound.uri(context), Notify.audio)
                    .build(),
            )
        }
        return id
    }

    /** One notification per alarm, kept apart from reminders (1000+) and timers (998+). */
    internal fun notificationId(id: Long): Int = 2_000_000 + (id % 1_000_000L).toInt()
}