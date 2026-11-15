package com.dawnly.app.notify

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.util.Log
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.dawnly.app.R
import com.dawnly.app.data.sound.AlertSound
import com.dawnly.app.data.time.TimeStore

/** A durable completion notice also covers a foreground-service start that fails asynchronously. */
internal object TimerNotifications {
    // Android fixes a channel's sound when it's made, so each tone gets its own: `timer_<sound>`.
    private const val CHANNEL_PREFIX = "timer_"
    private const val DEFAULT_CHANNEL = "timer_default"
    // The single channel used before multiple timers; removed once the per-tone ones exist.
    private const val OLD_CHANNEL = "timer_finished"
    // Per-timer slot, so two finished timers don't overwrite each other. 998 was the old single slot.
    private const val NOTIFICATION_ID_BASE = 998

    private val ALARM_AUDIO: AudioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    private fun notificationId(id: Long): Int = NOTIFICATION_ID_BASE + id.toInt()

    fun showFinished(context: Context, id: Long, label: String): Boolean =
        show(context, id, label, context.getString(R.string.time_timer_finished), resolveSound(context, id), snooze = true)

    fun showPaused(context: Context, id: Long): Boolean =
        show(context, id, context.getString(R.string.time_timer), context.getString(R.string.time_snooze_permission), resolveSound(context, id))

    /** The timer's chosen tone, or null for the phone's default alarm sound. */
    private fun resolveSound(context: Context, id: Long): AlertSound? {
        val name = TimeStore.current(context).timer(id)?.sound ?: return null
        val sound = AlertSound.entries.firstOrNull { it.name == name }
        return if (sound == AlertSound.PhoneDefault) null else sound
    }

    @SuppressLint("MissingPermission") // Checked via areNotificationsEnabled, and permission revocation is caught below.
    private fun show(context: Context, id: Long, title: String, text: String, sound: AlertSound?, snooze: Boolean = false): Boolean = try {
        val manager = NotificationManagerCompat.from(context)
        val channel = channelFor(context, manager, sound)
        if (!manager.areNotificationsEnabled() || manager.getNotificationChannelCompat(channel)?.importance == NotificationManager.IMPORTANCE_NONE) {
            false
        } else {
            val builder = NotificationCompat.Builder(context, channel)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(text)
                .setContentIntent(Notify.openApp(context))
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setOnlyAlertOnce(true)
                .setAutoCancel(true)
            if (snooze) {
                builder.addAction(
                    0,
                    context.getString(R.string.ring_snooze, TimerController.SNOOZE_MINUTES),
                    TimerController.snoozePending(context, id),
                )
            }
            manager.notify(notificationId(id), builder.build())
            true
        }
    } catch (e: RuntimeException) {
        Log.w("TimeTools", "Could not post timer notification; completion stays in the app", e)
        false
    }

    private fun channelFor(context: Context, manager: NotificationManagerCompat, sound: AlertSound?): String {
        val channelId = sound?.let { CHANNEL_PREFIX + it.name.lowercase() } ?: DEFAULT_CHANNEL
        if (manager.getNotificationChannelCompat(OLD_CHANNEL) != null) manager.deleteNotificationChannel(OLD_CHANNEL)
        if (manager.getNotificationChannelCompat(channelId) == null) {
            val uri = sound?.uri(context) ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            manager.createNotificationChannel(
                NotificationChannelCompat.Builder(channelId, NotificationManagerCompat.IMPORTANCE_HIGH)
                    .setName(context.getString(R.string.time_timer))
                    .setDescription(context.getString(R.string.time_channel_description))
                    .setSound(uri, ALARM_AUDIO)
                    .setVibrationEnabled(true)
                    .build(),
            )
        }
        return channelId
    }

    fun cancel(context: Context, id: Long) = NotificationManagerCompat.from(context).cancel(notificationId(id))
}
