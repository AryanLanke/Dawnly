package com.dawnly.app.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.dawnly.app.R
import com.dawnly.app.data.settings.AwakeCheck
import com.dawnly.app.data.sound.AlertSound
import com.dawnly.app.dawnly
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** When the "Still up?" check asks and when the alarm rings without an answer. Pure, so it's unit tested. */
object StillUpPlan {
    /** How long the user has to answer before the alarm rings. */
    val ANSWER_TIME: Duration = Duration.ofMinutes(2)

    /** Asking straight after the dismiss is pointless: the user was just seen to be up. */
    val MIN_WAIT: Duration = Duration.ofMinutes(3)

    /** Asks so an unanswered check rings at the alarm's own time, but never sooner than [MIN_WAIT] from now. */
    fun askAt(now: Instant, alarmAt: Instant): Instant = maxOf(alarmAt.minus(ANSWER_TIME), now.plus(MIN_WAIT))

    fun ringAt(askedAt: Instant): Instant = askedAt.plus(ANSWER_TIME)
}

/**
 * The optional "Still up?" check ([AwakeCheck]). After Dawnly turns an alarm off by itself, it asks
 * once around the alarm's time. "I'm up" ends it; no answer and Dawnly rings like an alarm itself
 * ([ReminderRingService]), in case the user fell back asleep.
 */
object StillUpCheck {
    private const val TAG = "StillUp"
    private const val CHANNEL = "still_up"
    private const val NOTIFICATION_ID = 6

    /** The ring's id in [ReminderRingService]; reminders' ids are all positive. */
    const val RING_ID = -1L

    private const val ACTION_ASK = "com.dawnly.app.action.STILL_UP_ASK"
    private const val ACTION_RING = "com.dawnly.app.action.STILL_UP_RING"
    private const val ACTION_UP = "com.dawnly.app.action.STILL_UP_ANSWERED"
    private const val EXTRA_ALARM_AT = "com.dawnly.app.extra.STILL_UP_ALARM_AT"

    private val asking = MutableStateFlow(false)

    /** True while "Still up?" waits for an answer; [StillUpActivity] closes when it turns false. */
    val isAsking: StateFlow<Boolean> = asking.asStateFlow()

    /** Sets the check for the alarm at [alarmAt] that was just turned off, if the user wants one. */
    fun schedule(context: Context, alarmAt: Instant) {
        val app = context.dawnly
        app.scope.launch {
            if (app.settings.awakeCheck.first() == AwakeCheck.Off) return@launch
            if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
                Log.w(TAG, "Notifications are off, so there's no one to ask; no check")
                return@launch
            }
            val at = StillUpPlan.askAt(Instant.now(), alarmAt)
            setAlarm(context, at, pending(context, ACTION_ASK, alarmAt))
            Log.i(TAG, "Asking \"Still up?\" at $at about the alarm at $alarmAt")
        }
    }

    /** Rings again in [minutes], after Snooze on the ring. */
    fun snooze(context: Context, minutes: Int) {
        setAlarm(context, Instant.now().plus(Duration.ofMinutes(minutes.toLong())), pending(context, ACTION_RING, null))
    }

    /** "I'm up": no ring, and the question goes away. */
    fun answer(context: Context) {
        cancelAlarm(context, ACTION_ASK)
        cancelAlarm(context, ACTION_RING)
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
        asking.value = false
        Log.i(TAG, "Up, no ring")
    }

    /** Whether there's still a question to answer, even after Android restarted the app. */
    fun isWaiting(context: Context): Boolean = asking.value || PendingIntent.getBroadcast(
        context,
        ACTION_RING.hashCode(),
        Intent(context, StillUpReceiver::class.java).setAction(ACTION_RING),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE,
    ) != null

    /** [done] once the work is finished, so the receiver's process lives until then. */
    internal fun onReceive(context: Context, intent: Intent, done: () -> Unit) {
        when (intent.action) {
            ACTION_ASK -> return ask(context, Instant.ofEpochMilli(intent.getLongExtra(EXTRA_ALARM_AT, 0L)), done)
            ACTION_RING -> ring(context)
            ACTION_UP -> answer(context)
        }
        done()
    }

    private fun ask(context: Context, alarmAt: Instant, done: () -> Unit) {
        val app = context.dawnly
        app.scope.launch {
            try {
                ask(context, alarmAt, app.settings.awakeCheck.first())
            } finally {
                done()
            }
        }
    }

    private fun ask(context: Context, alarmAt: Instant, check: AwakeCheck) {
        cancelAlarm(context, ACTION_ASK)
        // Turned off since it was set.
        if (check == AwakeCheck.Off) return
        val now = Instant.now()
        setAlarm(context, StillUpPlan.ringAt(now), pending(context, ACTION_RING, null))
        val text = context.getString(
            R.string.still_up_text,
            Notify.time(context, alarmAt),
            StillUpPlan.ANSWER_TIME.toMinutes().toInt(),
        )
        val builder = NotificationCompat.Builder(context, channel(context))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.still_up_title))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(screen(context))
            .addAction(0, context.getString(R.string.still_up_action), pending(context, ACTION_UP, null))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setTimeoutAfter(StillUpPlan.ANSWER_TIME.toMillis())
        if (check == AwakeCheck.FullScreen) builder.setFullScreenIntent(screen(context), true)
        asking.value = true
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build())
            Log.i(TAG, "Asked \"Still up?\" ($check); ringing at ${StillUpPlan.ringAt(now)} without an answer")
        } catch (e: SecurityException) {
            Log.w(TAG, "Notifications were turned off; ringing without asking", e)
        }
    }

    private fun ring(context: Context) {
        cancelAlarm(context, ACTION_RING)
        asking.value = false
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
        val name = context.getString(R.string.still_up_ring_name)
        val why = context.getString(R.string.still_up_ring_why)
        Log.i(TAG, "No answer; ringing")
        if (!ReminderRingService.start(context, RING_ID, name, why, AlertSound.PhoneDefault)) {
            val notification = NotificationCompat.Builder(context, channel(context))
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(name)
                .setContentText(why)
                .setContentIntent(Notify.openApp(context))
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .build()
            notification.flags = notification.flags or android.app.Notification.FLAG_INSISTENT
            try {
                NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
            } catch (e: SecurityException) {
                Log.w(TAG, "Couldn't ring or notify", e)
            }
        }
    }

    private fun setAlarm(context: Context, at: Instant, intent: PendingIntent) {
        val alarms = context.getSystemService(AlarmManager::class.java)
        val exact = android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S || alarms.canScheduleExactAlarms()
        try {
            if (exact) {
                alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at.toEpochMilli(), intent)
            } else {
                alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at.toEpochMilli(), intent)
            }
        } catch (e: SecurityException) {
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at.toEpochMilli(), intent)
        }
    }

    private fun cancelAlarm(context: Context, action: String) {
        val intent = PendingIntent.getBroadcast(
            context,
            action.hashCode(),
            Intent(context, StillUpReceiver::class.java).setAction(action),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE,
        ) ?: return
        context.getSystemService(AlarmManager::class.java).cancel(intent)
        intent.cancel()
    }

    private fun pending(context: Context, action: String, alarmAt: Instant?): PendingIntent = PendingIntent.getBroadcast(
        context,
        action.hashCode(),
        Intent(context, StillUpReceiver::class.java).setAction(action).apply {
            if (alarmAt != null) putExtra(EXTRA_ALARM_AT, alarmAt.toEpochMilli())
        },
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun screen(context: Context): PendingIntent = PendingIntent.getActivity(
        context,
        NOTIFICATION_ID,
        Intent(context, StillUpActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    /** High importance so it can pop up and take over the screen, with a soft sound and no vibration. */
    private fun channel(context: Context): String {
        val manager = NotificationManagerCompat.from(context)
        if (manager.getNotificationChannelCompat(CHANNEL) == null) {
            manager.createNotificationChannel(
                NotificationChannelCompat.Builder(CHANNEL, NotificationManagerCompat.IMPORTANCE_HIGH)
                    .setName(context.getString(R.string.channel_still_up))
                    .setDescription(context.getString(R.string.channel_still_up_description))
                    .setSound(AlertSound.DEFAULT.uri(context), Notify.audio)
                    .setVibrationEnabled(false)
                    .build(),
            )
        }
        return CHANNEL
    }
}

class StillUpReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val result = goAsync()
        StillUpCheck.onReceive(context, intent) { result.finish() }
    }
}
