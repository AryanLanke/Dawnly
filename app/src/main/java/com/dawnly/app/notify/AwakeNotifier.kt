package com.dawnly.app.notify

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.format.DateFormat
import android.util.Log
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.dawnly.app.MainActivity
import com.dawnly.app.R
import com.dawnly.app.dawnly
import com.dawnly.app.data.sound.AlertSound
import java.time.Duration
import java.time.Instant
import java.util.Date
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * The hand-off from wake detection (Phase 3) to the notification (Phase 4).
 * `WatchService` calls [onUserAwake] at most once per watch window, when the wake score says
 * the user is up.
 *
 * By default the prompt only asks: its "Dismiss alarm" button opens [DismissAlarmActivity], which
 * checks the alarm is still the right one before asking the Clock app. With auto-dismiss on
 * (Settings), it warns "Dismissing your 5:30 alarm in 1 minute" instead, and [AutoDismissReceiver]
 * dismisses when the minute is up unless the user tapped "Not yet".
 */
object AwakeNotifier {
    private const val TAG = "AwakeNotifier"

    /**
     * Android fixes a channel's sound once it exists, so each sound gets its own channel:
     * `awake_<sound>`. [useSound] swaps them. "awake" was the silent Phase 4 channel.
     */
    private const val CHANNEL_PREFIX = "awake_"
    private const val OLD_CHANNEL = "awake"

    /** `WatchNotifications.WATCHING_ID` is 1. The ask prompt and the auto-dismiss warning share 2. */
    private const val PROMPT_ID = 2

    /** The alarm the prompt is about, in epoch millis. On the intents and in the notification extras. */
    internal const val EXTRA_ALARM_AT = "com.dawnly.app.extra.PROMPT_ALARM_AT"

    fun onUserAwake(context: Context, alarmAt: Instant) {
        val check = DismissGuard.check(alarmAt, context.dawnly.nextAlarms.current()?.triggerAt, Instant.now())
        if (check != DismissCheck.Ok) {
            Log.i(TAG, "Not asking about the alarm at $alarmAt: $check")
            return
        }
        val app = context.dawnly
        app.scope.launch {
            val dismissAt = autoDismissAt(context, alarmAt)
            if (dismissAt != null) warnThenDismiss(context, alarmAt, dismissAt) else post(context, alarmAt)
        }
    }

    /** Shows the prompt for [alarmAt] without checking it's the next alarm. Debug builds use it directly. */
    internal fun post(context: Context, alarmAt: Instant) {
        if (!canNotify(context)) {
            Log.w(TAG, "Notifications are off, so the user can't be asked")
            return
        }
        val time = formatTime(context, alarmAt)
        val builder = prompt(context, alarmAt, context.getString(R.string.awake_text, time))
            .setAutoCancel(true)
        // Gone by itself once the alarm rings: there's nothing left to dismiss.
        val untilAlarm = Duration.between(Instant.now(), alarmAt)
        if (!untilAlarm.isNegative) builder.setTimeoutAfter(untilAlarm.toMillis())
        notify(context, builder, "Asked about the alarm at $alarmAt")
    }

    /** Removes the prompt or the auto-dismiss warning, and stops a pending auto-dismiss. */
    fun cancel(context: Context) {
        NotificationManagerCompat.from(context).cancel(PROMPT_ID)
        autoDismissIntent(context, null, PendingIntent.FLAG_NO_CREATE)?.let { pending ->
            context.getSystemService(AlarmManager::class.java).cancel(pending)
            pending.cancel()
            Log.i(TAG, "Auto-dismiss stopped")
        }
    }

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
     * Makes the prompt play [sound] (chosen in Settings). Replaces the channel only when the sound
     * changed, so it's cheap to call on every app start to stay in step with the saved choice.
     */
    fun useSound(context: Context, sound: AlertSound) {
        val manager = NotificationManagerCompat.from(context)
        val wanted = CHANNEL_PREFIX + sound.name.lowercase()
        manager.notificationChannelsCompat
            .filter { it.id == OLD_CHANNEL || (it.id.startsWith(CHANNEL_PREFIX) && it.id != wanted) }
            .forEach { manager.deleteNotificationChannel(it.id) }
        if (manager.getNotificationChannelCompat(wanted) == null) createChannel(context, wanted, sound)
    }

    /**
     * When to auto-dismiss, or null to ask instead. Every condition fails safe: without a warning
     * the user can see and stop, or without a way to act on time, Dawnly just asks.
     */
    private suspend fun autoDismissAt(context: Context, alarmAt: Instant): Instant? {
        val app = context.dawnly
        if (!app.settings.autoDismiss.first()) return null
        val dismissAt = AutoDismissPlan.dismissAt(Instant.now(), alarmAt)
        val reason = when {
            dismissAt == null -> "the alarm is too close for a one-minute warning"
            !Settings.canDrawOverlays(context) -> "Display over other apps is off"
            !app.watchScheduler.canScheduleExactAlarms() -> "without exact alarms the minute could stretch to an hour"
            !canNotify(context) -> "notifications are off, so there'd be no warning"
            else -> return dismissAt
        }
        Log.i(TAG, "Auto-dismiss is on, but asking instead: $reason")
        return null
    }

    /** "Dismissing your 5:30 alarm in 1 minute", with a live countdown, then [AutoDismissReceiver]. */
    private fun warnThenDismiss(context: Context, alarmAt: Instant, dismissAt: Instant) {
        try {
            context.getSystemService(AlarmManager::class.java).setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                dismissAt.toEpochMilli(),
                autoDismissIntent(context, alarmAt, PendingIntent.FLAG_UPDATE_CURRENT)!!,
            )
        } catch (e: SecurityException) {
            Log.w(TAG, "Exact alarms were just turned off; asking instead", e)
            post(context, alarmAt)
            return
        }
        val time = formatTime(context, alarmAt)
        val builder = prompt(context, alarmAt, context.getString(R.string.auto_dismiss_text, time))
            // Swiping the warning away counts as "Not yet": never dismiss once the user waved it off.
            .setDeleteIntent(notYet(context))
            .setWhen(dismissAt.toEpochMilli())
            .setShowWhen(true)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
        notify(context, builder, "Auto-dismissing the alarm at $alarmAt at $dismissAt unless stopped")
    }

    /** What the ask prompt and the auto-dismiss warning share. */
    private fun prompt(context: Context, alarmAt: Instant, text: String): NotificationCompat.Builder =
        NotificationCompat.Builder(context, currentChannel(context))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.awake_title))
            .setContentText(text)
            // The whole question when expanded; one line cuts it off on most phones.
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(openApp(context))
            .addAction(0, context.getString(R.string.action_dismiss_alarm), dismissAlarm(context, alarmAt))
            .addAction(0, context.getString(R.string.action_not_yet), notYet(context))
            .addExtras(Bundle().apply { putLong(EXTRA_ALARM_AT, alarmAt.toEpochMilli()) })
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

    private fun notify(context: Context, builder: NotificationCompat.Builder, log: String) {
        try {
            NotificationManagerCompat.from(context).notify(PROMPT_ID, builder.build())
            Log.i(TAG, log)
        } catch (e: SecurityException) {
            Log.w(TAG, "Notifications were turned off just now", e)
        }
    }

    private fun canNotify(context: Context): Boolean {
        val allowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        return allowed && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    private fun formatTime(context: Context, at: Instant): String =
        DateFormat.getTimeFormat(context).format(Date.from(at))

    /** The channel for the chosen sound; the phone's own sound until the user picks one. */
    private fun currentChannel(context: Context): String {
        val existing = NotificationManagerCompat.from(context).notificationChannelsCompat
            .firstOrNull { it.id.startsWith(CHANNEL_PREFIX) }
        if (existing != null) return existing.id
        useSound(context, AlertSound.PhoneDefault)
        return CHANNEL_PREFIX + AlertSound.PhoneDefault.name.lowercase()
    }

    /**
     * Heads-up so it's seen while the phone is in use, with a short soft sound and no vibration:
     * a quiet question, not an alarm.
     */
    private fun createChannel(context: Context, id: String, sound: AlertSound) {
        val audio = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val channel = NotificationChannelCompat.Builder(id, NotificationManagerCompat.IMPORTANCE_HIGH)
            .setName(context.getString(R.string.channel_awake))
            .setDescription(context.getString(R.string.channel_awake_description))
            .setSound(sound.uri(context), audio)
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
        DismissAlarmActivity.intent(context, alarmAt),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun notYet(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, NotYetReceiver::class.java),
        PendingIntent.FLAG_IMMUTABLE,
    )

    /** One pending auto-dismiss at most. Extras don't count when matching, so null finds it to cancel. */
    private fun autoDismissIntent(context: Context, alarmAt: Instant?, flags: Int): PendingIntent? =
        PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, AutoDismissReceiver::class.java).apply {
                if (alarmAt != null) putExtra(EXTRA_ALARM_AT, alarmAt.toEpochMilli())
            },
            PendingIntent.FLAG_IMMUTABLE or flags,
        )
}
