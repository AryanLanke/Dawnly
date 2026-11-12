package com.dawnly.app.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.dawnly.app.R
import com.dawnly.app.data.settings.WakeMusicApp
import com.dawnly.app.dawnly
import com.dawnly.app.domain.reminder.Reminder
import com.dawnly.app.service.launchAsync
import java.time.Duration
import java.time.Instant
import kotlin.math.ceil
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

/** Whether and when music plays instead of an alarm. Pure, so it's unit tested. */
object WakeMusicPlan {
    /** Music starts this long before the alarm, so there's time to hear it and turn the alarm off. */
    val LEAD: Duration = Duration.ofSeconds(45)

    /** How long the music app gets to start playing before the alarm is left to ring. */
    val START_TIME: Duration = Duration.ofSeconds(10)

    fun startAt(alarmAt: Instant): Instant = alarmAt.minus(LEAD)

    /**
     * Music only when it can work: online, the app is there, nothing playing already (it couldn't
     * be told apart), and for an alarm, a way to turn the Clock alarm off once it plays.
     */
    fun shouldTry(choice: WakeMusicApp, online: Boolean, installed: Boolean, alreadyPlaying: Boolean, canDismiss: Boolean): Boolean =
        choice != WakeMusicApp.Off && online && installed && !alreadyPlaying && canDismiss

    /** Media volume Dawnly raises a quiet phone to, so the music can wake anyone. */
    fun minVolume(max: Int): Int = ceil(max * 0.5).toInt()
}

/**
 * Wake to music ([WakeMusicApp]). Just before a Clock alarm, Dawnly asks the chosen app to play
 * (a media "play" key, as headphones send), and once music is heard it turns the Clock alarm off.
 * Offline, or no music within [WakeMusicPlan.START_TIME], it does nothing and the alarm rings as
 * usual. Ringing reminders try music first the same way. Music started here isn't a sign the user
 * is up ([isOurs]).
 */
object WakeMusic {
    private const val TAG = "WakeMusic"
    private const val CHANNEL = "wake_music"
    private const val NOTIFICATION_ID = 7
    private const val ACTION_START = "com.dawnly.app.action.WAKE_MUSIC"
    private const val EXTRA_ALARM_AT = "com.dawnly.app.extra.WAKE_MUSIC_ALARM_AT"

    /** Long enough to cover the rest of the watch window; then music is the user's own again. */
    private const val OURS_MS = 30 * 60_000L
    private const val POLL_MS = 500L

    /** A first press can be lost while the app starts up; the second one goes this long after. */
    private const val SECOND_PRESS_MS = 4_000L

    @Volatile
    private var startedAt = 0L

    /** True while music playing could be the music Dawnly put on. */
    fun isOurs(): Boolean = startedAt != 0L && SystemClock.elapsedRealtime() - startedAt < OURS_MS

    fun isInstalled(context: Context, app: WakeMusicApp): Boolean {
        val pkg = app.packageName ?: return true
        return try {
            context.packageManager.getPackageInfo(pkg, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    /** Sets the music for the alarm at [alarmAt], or clears it. `WatchScheduler.reconcile` calls this. */
    suspend fun schedule(context: Context, alarmAt: Instant?) {
        val alarms = context.getSystemService(AlarmManager::class.java)
        val choice = context.dawnly.settings.wakeMusic.first()
        val at = alarmAt?.let(WakeMusicPlan::startAt)
        val exact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms.canScheduleExactAlarms()
        // Late is no good: an inexact alarm can fire after the Clock alarm already rang.
        if (choice == WakeMusicApp.Off || at == null || !at.isAfter(Instant.now()) || !exact) {
            alarms.cancel(pending(context, null))
            return
        }
        try {
            alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at.toEpochMilli(), pending(context, alarmAt))
            Log.i(TAG, "Music ($choice) at $at for the alarm at $alarmAt")
        } catch (e: SecurityException) {
            Log.w(TAG, "Exact alarms were just turned off; no music", e)
        }
    }

    internal suspend fun onAlarm(context: Context, alarmAt: Instant) {
        val app = context.dawnly
        val check = DismissGuard.check(alarmAt, app.nextAlarms.current()?.triggerAt, Instant.now())
        if (check != DismissCheck.Ok) {
            Log.i(TAG, "No music for the alarm at $alarmAt: $check")
            return
        }
        val reach = ClockReach.of(context)
        val canDismiss = reach == ClockReach.Anytime ||
            (reach == ClockReach.WhenLocked && NotificationManagerCompat.from(context).areNotificationsEnabled())
        val choice = app.settings.wakeMusic.first()
        if (!play(context, choice, canDismiss)) return
        Log.i(TAG, "Music's on; turning off the alarm at $alarmAt")
        if (reach == ClockReach.Anytime) {
            try {
                context.startActivity(DismissAlarmActivity.intent(context, alarmAt, by = DismissBy.Music))
                return
            } catch (e: RuntimeException) {
                Log.w(TAG, "Couldn't open the dismiss; trying a full-screen notification", e)
            }
        }
        dismissFullScreen(context, alarmAt)
    }

    /**
     * Music instead of [reminder]'s ring. True if it's playing, and then a quiet notification says
     * what the reminder was; false and the reminder rings as usual.
     */
    suspend fun playForReminder(context: Context, reminder: Reminder): Boolean {
        val settings = context.dawnly.settings
        if (!settings.wakeMusicReminders.first()) return false
        if (!play(context, settings.wakeMusic.first(), canDismiss = true)) return false
        val builder = NotificationCompat.Builder(context, channel(context))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(reminder.name)
            .setContentText(reminder.why.ifBlank { context.getString(R.string.wake_music_reminder_text) })
            .setContentIntent(Notify.openApp(context))
            .setAutoCancel(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
        notify(context, ReminderScheduler.notificationId(reminder.id), builder)
        return true
    }

    /** "Music's on" once the Clock alarm is off, silent so it doesn't talk over the song. */
    fun postPlaying(context: Context, alarmAt: Instant) {
        val text = context.getString(R.string.wake_music_done_text, Notify.time(context, alarmAt))
        val builder = NotificationCompat.Builder(context, channel(context))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.wake_music_done_title))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(Notify.openApp(context))
            .setAutoCancel(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        notify(context, NOTIFICATION_ID, builder)
    }

    /** Like auto-dismiss without Display over other apps: opens straight away with the phone locked. */
    private fun dismissFullScreen(context: Context, alarmAt: Instant) {
        val open = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            DismissAlarmActivity.intent(context, alarmAt, by = DismissBy.Music),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val text = context.getString(R.string.wake_music_tap_text, Notify.time(context, alarmAt))
        val builder = NotificationCompat.Builder(context, channel(context))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.wake_music_done_title))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(open)
            .setFullScreenIntent(open, true)
            .setAutoCancel(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setTimeoutAfter(WakeMusicPlan.LEAD.toMillis())
        notify(context, NOTIFICATION_ID, builder)
    }

    /** Asks [choice] to play and waits to hear it. False when it can't or didn't start. */
    private suspend fun play(context: Context, choice: WakeMusicApp, canDismiss: Boolean): Boolean {
        val audio = context.getSystemService(AudioManager::class.java)
        val online = isOnline(context)
        val installed = isInstalled(context, choice)
        val alreadyPlaying = audio.isMusicActive
        if (!WakeMusicPlan.shouldTry(choice, online, installed, alreadyPlaying, canDismiss)) {
            if (choice != WakeMusicApp.Off) {
                Log.i(TAG, "No music ($choice): online=$online installed=$installed playing=$alreadyPlaying canDismiss=$canDismiss")
            }
            return false
        }
        raiseVolume(audio)
        startedAt = SystemClock.elapsedRealtime()
        val pkg = choice.packageName
        pressPlay(context, audio, pkg)
        val deadline = SystemClock.elapsedRealtime() + WakeMusicPlan.START_TIME.toMillis()
        var pressedAgain = false
        while (SystemClock.elapsedRealtime() < deadline) {
            delay(POLL_MS)
            if (audio.isMusicActive) {
                Log.i(TAG, "Music started ($choice)")
                return true
            }
            if (!pressedAgain && SystemClock.elapsedRealtime() - startedAt >= SECOND_PRESS_MS) {
                pressedAgain = true
                // Some apps only listen once they're open; Display over other apps may open them.
                if (pkg != null && Settings.canDrawOverlays(context)) openApp(context, pkg)
                pressPlay(context, audio, pkg)
            }
        }
        startedAt = 0L
        Log.i(TAG, "No music after ${WakeMusicPlan.START_TIME.seconds} s ($choice); the alarm rings")
        return false
    }

    /** A media "play" key: to [pkg] only, or to whichever app played last. */
    private fun pressPlay(context: Context, audio: AudioManager, pkg: String?) {
        for (action in listOf(KeyEvent.ACTION_DOWN, KeyEvent.ACTION_UP)) {
            val key = KeyEvent(action, KeyEvent.KEYCODE_MEDIA_PLAY)
            if (pkg == null) {
                audio.dispatchMediaKeyEvent(key)
            } else {
                context.sendBroadcast(Intent(Intent.ACTION_MEDIA_BUTTON).setPackage(pkg).putExtra(Intent.EXTRA_KEY_EVENT, key))
            }
        }
    }

    private fun openApp(context: Context, pkg: String) {
        val launch = context.packageManager.getLaunchIntentForPackage(pkg) ?: return
        try {
            context.startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: RuntimeException) {
            Log.w(TAG, "Couldn't open $pkg", e)
        }
    }

    /** A phone left on silent overnight would play the music to no one. */
    private fun raiseVolume(audio: AudioManager) {
        val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val min = WakeMusicPlan.minVolume(max)
        if (audio.getStreamVolume(AudioManager.STREAM_MUSIC) >= min) return
        try {
            audio.setStreamVolume(AudioManager.STREAM_MUSIC, min, 0)
        } catch (e: SecurityException) {
            Log.w(TAG, "Couldn't raise the media volume", e)
        }
    }

    private fun isOnline(context: Context): Boolean {
        val connectivity = context.getSystemService(ConnectivityManager::class.java)
        val caps = connectivity.getNetworkCapabilities(connectivity.activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private fun notify(context: Context, id: Int, builder: NotificationCompat.Builder) {
        try {
            NotificationManagerCompat.from(context).notify(id, builder.build())
        } catch (e: SecurityException) {
            Log.w(TAG, "Notifications are off", e)
        }
    }

    private fun pending(context: Context, alarmAt: Instant?): PendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, WakeMusicReceiver::class.java).setAction(ACTION_START).apply {
            if (alarmAt != null) putExtra(EXTRA_ALARM_AT, alarmAt.toEpochMilli())
        },
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    /** High importance so the dismiss can open over the lock screen; silent, the music is the sound. */
    private fun channel(context: Context): String {
        val manager = NotificationManagerCompat.from(context)
        if (manager.getNotificationChannelCompat(CHANNEL) == null) {
            manager.createNotificationChannel(
                NotificationChannelCompat.Builder(CHANNEL, NotificationManagerCompat.IMPORTANCE_HIGH)
                    .setName(context.getString(R.string.channel_wake_music))
                    .setDescription(context.getString(R.string.channel_wake_music_description))
                    .setSound(null, null)
                    .setVibrationEnabled(false)
                    .build(),
            )
        }
        return CHANNEL
    }

    internal fun alarmAtOf(intent: Intent): Instant? =
        intent.getLongExtra(EXTRA_ALARM_AT, 0L).takeIf { it > 0L }?.let(Instant::ofEpochMilli)
}

class WakeMusicReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val alarmAt = WakeMusic.alarmAtOf(intent) ?: return
        launchAsync(context) { WakeMusic.onAlarm(context, alarmAt) }
    }
}
