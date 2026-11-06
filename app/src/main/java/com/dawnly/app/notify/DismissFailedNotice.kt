package com.dawnly.app.notify

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import android.util.Log
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.dawnly.app.MainActivity
import com.dawnly.app.R
import java.time.Instant

/**
 * "Your alarm is still on": the Clock app took the dismiss request but left the alarm on, even
 * after every form of it ([QuietDismiss]). Without this the user would think it's off. Tapping it
 * opens the Clock app's alarm list. The phone's normal notification sound, never the reversed one,
 * which means the alarm is off ([DismissedNotice]).
 */
object DismissFailedNotice {
    private const val TAG = "DismissFailedNotice"
    private const val CHANNEL = "dismiss_failed"

    /** 1 is the watching notification, 2 the prompt and 3 the dismissed notice. */
    private const val NOTICE_ID = 4

    /** [retryAt] is when Dawnly will try again, if it will. */
    fun post(context: Context, alarmAt: Instant, clockLabel: String, retryAt: Instant?) {
        ensureChannel(context)
        val time = Notify.time(context, alarmAt)
        val text = if (retryAt != null) {
            context.getString(R.string.dismiss_failed_retry_text, clockLabel, time, Notify.time(context, retryAt))
        } else {
            context.getString(R.string.dismiss_failed_text, clockLabel, time)
        }
        val builder = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.dismiss_failed_title))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(openClock(context))
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_ERROR)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        try {
            NotificationManagerCompat.from(context).notify(NOTICE_ID, builder.build())
            Log.i(TAG, "Told the user the alarm at $alarmAt is still on (retry at $retryAt)")
        } catch (e: SecurityException) {
            Log.w(TAG, "Notifications are off, so the user wasn't told", e)
        }
    }

    /** Once a later try works, the "still on" notice is wrong. */
    fun cancel(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTICE_ID)
    }

    private fun ensureChannel(context: Context) {
        val manager = NotificationManagerCompat.from(context)
        if (manager.getNotificationChannelCompat(CHANNEL) != null) return
        val channel = NotificationChannelCompat.Builder(CHANNEL, NotificationManagerCompat.IMPORTANCE_HIGH)
            .setName(context.getString(R.string.channel_dismiss_failed))
            .setDescription(context.getString(R.string.channel_dismiss_failed_description))
            .setShowBadge(false)
            .build()
        manager.createNotificationChannel(channel)
    }

    /** The alarm list, or Dawnly if no app shows one. */
    private fun openClock(context: Context): PendingIntent {
        val alarms = Intent(AlarmClock.ACTION_SHOW_ALARMS)
        val intent = if (alarms.resolveActivity(context.packageManager) != null) alarms else Intent(context, MainActivity::class.java)
        return PendingIntent.getActivity(context, NOTICE_ID, intent, PendingIntent.FLAG_IMMUTABLE)
    }
}
