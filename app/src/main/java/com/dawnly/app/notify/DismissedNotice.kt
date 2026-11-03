package com.dawnly.app.notify

import android.app.KeyguardManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.text.format.DateFormat
import android.util.Log
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.dawnly.app.MainActivity
import com.dawnly.app.R
import com.dawnly.app.data.sound.AlertSound
import java.time.Duration
import java.time.Instant
import java.util.Date

/**
 * "Your 5:30 alarm is dismissed" after every dismiss (auto or tapped). With the phone locked in a
 * pocket, the sound is how the user knows; the notification waits on the lock screen for when they
 * look. With Ask me and Warn first it plays the chosen sound backwards, the undo of the forward
 * sound that asked or started the countdown. Right away has no earlier sound, so it plays the
 * sound forwards (the user's choice, 2026-11-03).
 */
object DismissedNotice {
    private const val TAG = "DismissedNotice"

    /** Like the prompt's channels (see [AwakeNotifier.useSound]): one per sound, `dismissed_<sound>`. */
    private const val CHANNEL_PREFIX = "dismissed_"

    /** The same notice with the sound played forwards, for Right away: `done_now_<sound>`. */
    private const val FORWARD_PREFIX = "done_now_"

    /** 1 is the watching notification and 2 the prompt. */
    private const val NOTICE_ID = 3

    /** Unlocked, the user just saw it pop up, so it clears itself; locked, it waits to be seen. */
    private val SEEN_TIMEOUT: Duration = Duration.ofMinutes(1)

    fun timeout(locked: Boolean): Duration? = SEEN_TIMEOUT.takeUnless { locked }

    /** [forward] plays the sound forwards (Right away) rather than backwards. */
    fun post(context: Context, alarmAt: Instant, forward: Boolean = false) {
        val locked = context.getSystemService(KeyguardManager::class.java).isKeyguardLocked
        val time = DateFormat.getTimeFormat(context).format(Date.from(alarmAt))
        val builder = NotificationCompat.Builder(context, currentChannel(context, if (forward) FORWARD_PREFIX else CHANNEL_PREFIX))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.dismissed_title))
            .setContentText(context.getString(R.string.dismiss_done, time))
            .setContentIntent(openApp(context))
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        timeout(locked)?.let { builder.setTimeoutAfter(it.toMillis()) }
        try {
            NotificationManagerCompat.from(context).notify(NOTICE_ID, builder.build())
            Log.i(TAG, "Told the user the alarm at $alarmAt is dismissed (locked: $locked, forward: $forward)")
        } catch (e: SecurityException) {
            Log.w(TAG, "Notifications are off, so the user wasn't told", e)
        }
    }

    /** Keeps both channels' sounds in step with the prompt's. [AwakeNotifier.useSound] calls it. */
    fun useSound(context: Context, sound: AlertSound) {
        val manager = NotificationManagerCompat.from(context)
        for (prefix in listOf(CHANNEL_PREFIX, FORWARD_PREFIX)) {
            val wanted = prefix + sound.name.lowercase()
            manager.notificationChannelsCompat
                .filter { it.id.startsWith(prefix) && it.id != wanted }
                .forEach { manager.deleteNotificationChannel(it.id) }
            if (manager.getNotificationChannelCompat(wanted) == null) createChannel(context, wanted, sound, prefix == FORWARD_PREFIX)
        }
    }

    private fun currentChannel(context: Context, prefix: String): String {
        val existing = NotificationManagerCompat.from(context).notificationChannelsCompat
            .firstOrNull { it.id.startsWith(prefix) }
        if (existing != null) return existing.id
        useSound(context, AlertSound.DEFAULT)
        return prefix + AlertSound.DEFAULT.name.lowercase()
    }

    /** Heads-up and no vibration, like the prompt. */
    private fun createChannel(context: Context, id: String, sound: AlertSound, forward: Boolean) {
        val audio = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val channel = NotificationChannelCompat.Builder(id, NotificationManagerCompat.IMPORTANCE_HIGH)
            .setName(context.getString(if (forward) R.string.channel_dismissed_now else R.string.channel_dismissed))
            .setDescription(
                context.getString(if (forward) R.string.channel_dismissed_now_description else R.string.channel_dismissed_description),
            )
            .setSound(if (forward) sound.uri(context) else sound.reversedUri(context), audio)
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
}
