package com.dawnly.app.service

import android.app.Notification
import android.content.Context
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.dawnly.app.R
import com.dawnly.app.notify.Notify
import java.time.Instant

object WatchNotifications {
    const val WATCHING_ID = 1
    private const val WATCHING_CHANNEL = "watching"

    fun createChannels(context: Context) {
        val channel = NotificationChannelCompat.Builder(WATCHING_CHANNEL, NotificationManagerCompat.IMPORTANCE_LOW)
            .setName(context.getString(R.string.channel_watching))
            .setDescription(context.getString(R.string.channel_watching_description))
            .setShowBadge(false)
            .build()
        NotificationManagerCompat.from(context).createNotificationChannel(channel)
    }

    /** The quiet ongoing notification shown while the service runs. */
    fun watching(context: Context, alarmAt: Instant?): Notification {
        val text = alarmAt?.let {
            context.getString(R.string.watching_until, Notify.time(context, it))
        }
        return NotificationCompat.Builder(context, WATCHING_CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.watching_title))
            .setContentText(text)
            .setContentIntent(Notify.openApp(context))
            .setOngoing(true)
            .setSilent(true)
            .setShowWhen(false)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
