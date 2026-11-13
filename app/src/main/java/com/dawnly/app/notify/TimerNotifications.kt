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

/** A durable completion notice also covers a foreground-service start that fails asynchronously. */
internal object TimerNotifications {
    private const val CHANNEL = "timer_finished"
    // Same slot ReminderScheduler.showMissed uses for RING_ID, so missed and finished don't stack.
    private const val NOTIFICATION_ID = 998

    fun showFinished(context: Context): Boolean = show(context, context.getString(R.string.time_timer_finished))
    fun showPaused(context: Context): Boolean = show(context, context.getString(R.string.time_snooze_permission))

    @SuppressLint("MissingPermission") // Checked via areNotificationsEnabled, and permission revocation is caught below.
    private fun show(context: Context, text: String): Boolean = try {
        val manager = NotificationManagerCompat.from(context)
        if (manager.getNotificationChannelCompat(CHANNEL) == null) {
            manager.createNotificationChannel(
                NotificationChannelCompat.Builder(CHANNEL, NotificationManagerCompat.IMPORTANCE_HIGH)
                    .setName(context.getString(R.string.time_timer))
                    .setDescription(context.getString(R.string.time_channel_description))
                    .setSound(
                        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                        AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build(),
                    )
                    .setVibrationEnabled(true)
                    .build(),
            )
        }
        if (!manager.areNotificationsEnabled() || manager.getNotificationChannelCompat(CHANNEL)?.importance == NotificationManager.IMPORTANCE_NONE) {
            false
        } else {
            manager.notify(
                NOTIFICATION_ID,
                NotificationCompat.Builder(context, CHANNEL)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(context.getString(R.string.time_timer))
                    .setContentText(text)
                    .setContentIntent(Notify.openApp(context))
                    .setCategory(NotificationCompat.CATEGORY_ALARM)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setOnlyAlertOnce(true)
                    .setAutoCancel(true)
                    .build(),
            )
            true
        }
    } catch (e: RuntimeException) {
        Log.w("TimeTools", "Could not post timer notification; completion stays in the app", e)
        false
    }

    fun cancel(context: Context) = NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
}
