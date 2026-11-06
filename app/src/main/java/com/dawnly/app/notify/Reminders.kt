package com.dawnly.app.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.util.Log
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.dawnly.app.MainActivity
import com.dawnly.app.R
import com.dawnly.app.dawnly
import com.dawnly.app.data.sound.AlertSound
import com.dawnly.app.domain.reminder.Reminder
import com.dawnly.app.domain.reminder.ReminderTimes
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.launch

/**
 * Times the user's reminders: one alarm per reminder that's on, for its next time. When it goes
 * off, [ReminderReceiver] shows the notification and sets the one after. [sync] sets them all
 * again from scratch, so it's safe to call after any change, a reboot or an app update.
 */
object ReminderScheduler {
    private const val TAG = "Reminders"
    internal const val EXTRA_ID = "com.dawnly.app.extra.REMINDER_ID"

    /** Before 1.3.0 every reminder used this one channel, with the phone's own sound. */
    private const val OLD_CHANNEL = "reminders"

    /** Android fixes a channel's sound, so each reminder sound has its own: `reminder_<sound>`. */
    private const val CHANNEL_PREFIX = "reminder_"

    /**
     * Reminders' own sound until the user picks another: Bubble, so a reminder never sounds like
     * the "Up already?" question or "Alarm's off" (the prompt's Flute by default).
     */
    val DEFAULT_SOUND = AlertSound.Bubble

    fun soundOf(reminder: Reminder): AlertSound =
        reminder.sound?.let { key -> AlertSound.entries.firstOrNull { it.name == key } } ?: DEFAULT_SOUND

    /** Removes the old channel that used the phone's own sound; the new ones are made as needed. */
    fun createChannel(context: Context) {
        NotificationManagerCompat.from(context).deleteNotificationChannel(OLD_CHANNEL)
    }

    /** The channel for [sound], made the first time a reminder uses it. */
    private fun channelFor(context: Context, sound: AlertSound): String {
        val id = CHANNEL_PREFIX + sound.name.lowercase()
        val manager = NotificationManagerCompat.from(context)
        if (manager.getNotificationChannelCompat(id) == null) {
            val audio = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            val channel = NotificationChannelCompat.Builder(id, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                .setName(context.getString(R.string.channel_reminders_sound, context.getString(sound.label)))
                .setDescription(context.getString(R.string.channel_reminders_description))
                .setSound(sound.uri(context), audio)
                .build()
            manager.createNotificationChannel(channel)
        }
        return id
    }

    /** Cancels every reminder's alarm and sets the ones that are on for their next time. */
    suspend fun sync(context: Context) {
        val reminders = context.dawnly.reminders.current()
        val known = context.getSharedPreferences(KNOWN, Context.MODE_PRIVATE)
        // Reminders deleted since last time: cancel their alarms too.
        known.getStringSet(KNOWN_IDS, emptySet()).orEmpty().mapNotNull { it.toLongOrNull() }
            .filter { id -> reminders.none { it.id == id } }
            .forEach { cancel(context, it) }
        for (reminder in reminders) schedule(context, reminder)
        known.edit().putStringSet(KNOWN_IDS, reminders.map { it.id.toString() }.toSet()).apply()
    }

    /** Sets [reminder]'s next alarm, or cancels it when it's off. */
    fun schedule(context: Context, reminder: Reminder) {
        if (!reminder.on) {
            cancel(context, reminder.id)
            return
        }
        val zone = ZoneId.systemDefault()
        // From a second on, so a reminder that just went off on time isn't set for the same moment.
        val after = LocalDateTime.now(zone).plusSeconds(1)
        // Null once its last day has passed: nothing more to set.
        val next = ReminderTimes.next(reminder.schedule, after, reminder.firstDay, reminder.lastDay)
        if (next == null) {
            cancel(context, reminder.id)
            return
        }
        val at = next.atZone(zone).toInstant().toEpochMilli()
        val alarms = context.getSystemService(AlarmManager::class.java)
        val intent = pending(context, reminder.id)
        try {
            if (alarms.canScheduleExactAlarmsCompat()) {
                alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, intent)
            } else {
                alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, intent)
            }
        } catch (e: SecurityException) {
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, intent)
        }
        Log.i(TAG, "\"${reminder.name}\" next at ${java.time.Instant.ofEpochMilli(at)}")
    }

    private fun cancel(context: Context, id: Long) {
        context.getSystemService(AlarmManager::class.java).cancel(pending(context, id))
    }

    internal fun show(context: Context, reminder: Reminder) {
        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val builder = NotificationCompat.Builder(context, channelFor(context, soundOf(reminder)))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(reminder.name)
            .setContentIntent(open)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
        if (reminder.why.isNotBlank()) {
            builder.setContentText(reminder.why).setStyle(NotificationCompat.BigTextStyle().bigText(reminder.why))
        }
        try {
            NotificationManagerCompat.from(context).notify(notificationId(reminder.id), builder.build())
        } catch (e: SecurityException) {
            Log.w(TAG, "Notifications are off, so \"${reminder.name}\" wasn't shown", e)
        }
    }

    private fun pending(context: Context, id: Long): PendingIntent = PendingIntent.getBroadcast(
        context,
        requestCode(id),
        Intent(context, ReminderReceiver::class.java).putExtra(EXTRA_ID, id),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    /** One alarm and one notification per reminder, kept apart from Dawnly's own (ids 1 to 4). */
    private fun requestCode(id: Long) = (id % 1_000_000_000L).toInt()
    private fun notificationId(id: Long) = 1_000 + (id % 1_000_000L).toInt()

    private fun AlarmManager.canScheduleExactAlarmsCompat() =
        android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S || canScheduleExactAlarms()

    private const val KNOWN = "reminder_alarms"
    private const val KNOWN_IDS = "ids"
}

/** A reminder is due: show it, then set its next time. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(ReminderScheduler.EXTRA_ID, -1L)
        val pending = goAsync()
        val app = context.dawnly
        app.scope.launch {
            try {
                val reminder = app.reminders.current().firstOrNull { it.id == id } ?: return@launch
                if (!reminder.on) return@launch
                ReminderScheduler.show(context, reminder)
                ReminderScheduler.schedule(context, reminder)
            } finally {
                pending.finish()
            }
        }
    }
}
