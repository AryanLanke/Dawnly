package com.dawnly.app.notify

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.format.DateFormat
import android.util.Log
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.dawnly.app.MainActivity
import com.dawnly.app.R
import com.dawnly.app.dawnly
import java.time.Duration
import java.time.Instant
import java.util.Date

/**
 * The hand-off from wake detection (Phase 3) to the notification (Phase 4).
 * `WatchService` calls [onUserAwake] at most once per watch window, when the wake score says
 * the user is up.
 *
 * The prompt never dismisses anything by itself. Its "Dismiss alarm" button opens
 * [DismissAlarmActivity], which checks the alarm is still the right one before asking the Clock app.
 */
object AwakeNotifier {
    private const val TAG = "AwakeNotifier"
    private const val CHANNEL = "awake"

    /** `WatchNotifications.WATCHING_ID` is 1. */
    private const val PROMPT_ID = 2

    /** The alarm the prompt is about, in epoch millis. On the intents and in the notification extras. */
    internal const val EXTRA_ALARM_AT = "com.dawnly.app.extra.PROMPT_ALARM_AT"

    fun onUserAwake(context: Context, alarmAt: Instant) {
        val check = DismissGuard.check(alarmAt, context.dawnly.nextAlarms.current()?.triggerAt, Instant.now())
        if (check != DismissCheck.Ok) {
            Log.i(TAG, "Not asking about the alarm at $alarmAt: $check")
            return
        }
        post(context, alarmAt)
    }

    /** Shows the prompt for [alarmAt] without checking it's the next alarm. Debug builds use it directly. */
    internal fun post(context: Context, alarmAt: Instant) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "Notifications are off, so the user can't be asked")
            return
        }
        createChannel(context)

        val time = DateFormat.getTimeFormat(context).format(Date.from(alarmAt))
        val builder = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.awake_title))
            .setContentText(context.getString(R.string.awake_text, time))
            .setContentIntent(openApp(context))
            .addAction(0, context.getString(R.string.action_dismiss_alarm), dismissAlarm(context, alarmAt))
            .addAction(0, context.getString(R.string.action_not_yet), notYet(context))
            .addExtras(Bundle().apply { putLong(EXTRA_ALARM_AT, alarmAt.toEpochMilli()) })
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        // Gone by itself once the alarm rings: there's nothing left to dismiss.
        val untilAlarm = Duration.between(Instant.now(), alarmAt)
        if (!untilAlarm.isNegative) builder.setTimeoutAfter(untilAlarm.toMillis())

        try {
            NotificationManagerCompat.from(context).notify(PROMPT_ID, builder.build())
            Log.i(TAG, "Asked about the alarm at $alarmAt")
        } catch (e: SecurityException) {
            Log.w(TAG, "Notifications were turned off just now", e)
        }
    }

    fun cancel(context: Context) = NotificationManagerCompat.from(context).cancel(PROMPT_ID)

    /**
     * Removes the prompt if it's about any alarm other than [nextAlarmAt], so a stale prompt can't
     * be tapped. `WatchScheduler.reconcile` calls this on every next-alarm change.
     */
    fun cancelIfStale(context: Context, nextAlarmAt: Instant?) {
        val prompt = context.getSystemService(NotificationManager::class.java).activeNotifications
            .firstOrNull { it.id == PROMPT_ID } ?: return
        val promptAlarmAt = prompt.notification.extras.getLong(EXTRA_ALARM_AT, 0L)
        if (promptAlarmAt != nextAlarmAt?.toEpochMilli()) {
            Log.i(TAG, "Next alarm is now $nextAlarmAt; removing the prompt about ${Instant.ofEpochMilli(promptAlarmAt)}")
            cancel(context)
        }
    }

    /**
     * Heads-up so it's seen while the phone is in use, but silent: the user is already awake, and
     * this should feel like a quiet question, not an alert.
     */
    private fun createChannel(context: Context) {
        val channel = NotificationChannelCompat.Builder(CHANNEL, NotificationManagerCompat.IMPORTANCE_HIGH)
            .setName(context.getString(R.string.channel_awake))
            .setDescription(context.getString(R.string.channel_awake_description))
            .setSound(null, null)
            .setVibrationEnabled(false)
            .setShowBadge(false)
            .build()
        NotificationManagerCompat.from(context).createNotificationChannel(channel)
    }

    private fun openApp(context: Context): PendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java),
        PendingIntent.FLAG_IMMUTABLE,
    )

    /** Straight to an activity, not through a receiver: Android 12+ blocks notification trampolines. */
    private fun dismissAlarm(context: Context, alarmAt: Instant): PendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, DismissAlarmActivity::class.java)
            .putExtra(EXTRA_ALARM_AT, alarmAt.toEpochMilli())
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun notYet(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, NotYetReceiver::class.java),
        PendingIntent.FLAG_IMMUTABLE,
    )
}
