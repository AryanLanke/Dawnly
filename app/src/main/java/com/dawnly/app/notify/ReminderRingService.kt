package com.dawnly.app.notify

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.dawnly.app.R
import com.dawnly.app.data.reminder.ReminderSounds
import com.dawnly.app.data.sound.AlertSound
import com.dawnly.app.domain.reminder.Reminder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** The reminder ringing right now, for [ReminderRingActivity]. */
data class Ringing(val id: Long, val name: String, val why: String)

/**
 * A reminder set to ring like an alarm. It takes over the screen ([ReminderRingActivity], through a
 * full-screen notification) and plays the reminder's sound on the alarm stream again and again,
 * with a short pause between, so each play's soft fade-out is heard every time. The volume rises
 * gently at first. Stop or Snooze ends it; after [TIMEOUT_MS] it stops by itself and leaves a
 * "Missed" notification.
 */
class ReminderRingService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private var player: MediaPlayer? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var vibrator: Vibrator? = null
    private var startedAt = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> finish(missed = false)
            ACTION_SNOOZE -> {
                current.value?.let { ReminderScheduler.snooze(this, it.id, SNOOZE_MINUTES) }
                finish(missed = false)
            }
            else -> {
                val ringing = Ringing(
                    id = intent?.getLongExtra(EXTRA_ID, -1L) ?: -1L,
                    name = intent?.getStringExtra(EXTRA_NAME).orEmpty(),
                    why = intent?.getStringExtra(EXTRA_WHY).orEmpty(),
                )
                val sound = AlertSound.entries.firstOrNull { it.name == intent?.getStringExtra(EXTRA_SOUND) }
                    ?: ReminderSounds.DEFAULT
                // A second reminder while one is ringing takes its place.
                stopSound()
                current.value = ringing
                try {
                    ServiceCompat.startForeground(
                        this,
                        NOTIFICATION_ID,
                        notification(ringing),
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                        } else {
                            0
                        },
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Couldn't ring \"${ringing.name}\" in the foreground", e)
                    current.value = null
                    stopSelf()
                    return START_NOT_STICKY
                }
                ring(sound)
            }
        }
        return START_NOT_STICKY
    }

    private fun ring(sound: AlertSound) {
        startedAt = System.currentTimeMillis()
        wakeLock = getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "dawnly:reminderRing")
            .apply { acquire(TIMEOUT_MS + 10_000L) }
        play(soundUri(sound))
        vibrate()
        handler.post(rampVolume)
        handler.postDelayed({ finish(missed = true) }, TIMEOUT_MS)
    }

    /**
     * Plays [uri] once, then again after [GAP_MS], until stopped. If the sound can't be played (a
     * missing file, say), plays Dawnly's default instead, once, so the ring isn't silent.
     */
    private fun play(uri: Uri, isFallback: Boolean = false) {
        val mp = MediaPlayer()
        player = mp
        fun fallBack(reason: String, cause: Throwable?) {
            Log.w(TAG, "Couldn't play $uri ($reason)", cause)
            if (isFallback) return
            mp.release()
            play(ReminderSounds.DEFAULT.uri(this), isFallback = true)
        }
        try {
            mp.setAudioAttributes(ALARM_AUDIO)
            mp.setDataSource(this, uri)
            mp.setVolume(volumeNow(), volumeNow())
            mp.setOnPreparedListener { it.start() }
            mp.setOnCompletionListener {
                handler.postDelayed({ if (player === mp) mp.start() }, GAP_MS)
            }
            mp.setOnErrorListener { _, what, extra ->
                fallBack("error $what, $extra", null)
                true
            }
            mp.prepareAsync()
        } catch (e: Exception) {
            fallBack("setup", e)
        }
    }

    /** From [START_VOLUME] up to full over [RAMP_MS], so it doesn't start with a jolt. */
    private fun volumeNow(): Float {
        val passed = (System.currentTimeMillis() - startedAt).coerceAtLeast(0L)
        return (START_VOLUME + (1f - START_VOLUME) * passed / RAMP_MS).coerceAtMost(1f)
    }

    private val rampVolume = object : Runnable {
        override fun run() {
            val volume = volumeNow()
            try {
                player?.setVolume(volume, volume)
            } catch (e: IllegalStateException) {
                // Not ready yet; the next tick sets it.
            }
            if (volume < 1f) handler.postDelayed(this, 500L)
        }
    }

    private fun soundUri(sound: AlertSound): Uri =
        if (sound == AlertSound.PhoneDefault) {
            RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: sound.uri(this)
        } else {
            sound.uri(this)
        }

    private fun vibrate() {
        val v = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Vibrator::class.java)
        }
        if (v == null || !v.hasVibrator()) return
        vibrator = v
        val effect = VibrationEffect.createWaveform(VIBRATION, 0)
        @Suppress("DEPRECATION")
        v.vibrate(effect, ALARM_AUDIO)
    }

    private fun stopSound() {
        handler.removeCallbacksAndMessages(null)
        player?.let {
            try {
                it.stop()
            } catch (e: IllegalStateException) {
                // Wasn't playing yet.
            }
            it.release()
        }
        player = null
        vibrator?.cancel()
        vibrator = null
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
    }

    private fun finish(missed: Boolean) {
        val ringing = current.value
        stopSound()
        current.value = null
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        if (missed && ringing != null) ReminderScheduler.showMissed(this, ringing)
        stopSelf()
    }

    override fun onDestroy() {
        stopSound()
        current.value = null
        super.onDestroy()
    }

    private fun notification(ringing: Ringing) =
        NotificationCompat.Builder(this, channel(this))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(ringing.name)
            .apply { if (ringing.why.isNotBlank()) setContentText(ringing.why) }
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(ringScreen(this))
            .setFullScreenIntent(ringScreen(this), true)
            .addAction(0, getString(R.string.ring_snooze, SNOOZE_MINUTES), command(this, ACTION_SNOOZE))
            .addAction(0, getString(R.string.ring_stop), command(this, ACTION_STOP))
            .build()

    companion object {
        private const val TAG = "ReminderRing"
        private const val NOTIFICATION_ID = 5
        private const val CHANNEL = "reminder_ring"

        private const val ACTION_RING = "com.dawnly.app.action.RING_REMINDER"
        private const val ACTION_STOP = "com.dawnly.app.action.STOP_REMINDER"
        private const val ACTION_SNOOZE = "com.dawnly.app.action.SNOOZE_REMINDER"
        private const val EXTRA_ID = "com.dawnly.app.extra.RING_ID"
        private const val EXTRA_NAME = "com.dawnly.app.extra.RING_NAME"
        private const val EXTRA_WHY = "com.dawnly.app.extra.RING_WHY"
        private const val EXTRA_SOUND = "com.dawnly.app.extra.RING_SOUND"

        const val SNOOZE_MINUTES = 10

        /** Rings for at most five minutes, like most alarms. */
        private const val TIMEOUT_MS = 5 * 60_000L

        /** The pause between plays, after each sound's own fade-out. */
        private const val GAP_MS = 1_500L
        private const val START_VOLUME = 0.3f
        private const val RAMP_MS = 20_000f
        private val VIBRATION = longArrayOf(0L, 600L, 900L)

        private val ALARM_AUDIO: AudioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        private val current = MutableStateFlow<Ringing?>(null)

        /** What's ringing now, or null; the ring screen closes when it turns null. */
        val ringing: StateFlow<Ringing?> = current.asStateFlow()

        /** Starts ringing [reminder]. False if Android wouldn't allow it, so the caller can notify instead. */
        fun start(context: Context, reminder: Reminder): Boolean = try {
            val intent = Intent(context, ReminderRingService::class.java)
                .setAction(ACTION_RING)
                .putExtra(EXTRA_ID, reminder.id)
                .putExtra(EXTRA_NAME, reminder.name)
                .putExtra(EXTRA_WHY, reminder.why)
                .putExtra(EXTRA_SOUND, ReminderSounds.of(reminder).name)
            ContextCompat.startForegroundService(context, intent)
            true
        } catch (e: Exception) {
            // ForegroundServiceStartNotAllowedException (Android 12+), e.g. after an inexact alarm.
            Log.w(TAG, "Not allowed to ring \"${reminder.name}\"; showing a notification instead", e)
            false
        }

        fun stop(context: Context) = send(context, ACTION_STOP)
        fun snooze(context: Context) = send(context, ACTION_SNOOZE)

        private fun send(context: Context, action: String) {
            try {
                context.startService(Intent(context, ReminderRingService::class.java).setAction(action))
            } catch (e: Exception) {
                Log.w(TAG, "Couldn't send $action", e)
            }
        }

        private fun command(context: Context, action: String): PendingIntent = PendingIntent.getService(
            context,
            action.hashCode(),
            Intent(context, ReminderRingService::class.java).setAction(action),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        private fun ringScreen(context: Context): PendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            Intent(context, ReminderRingActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        /** High importance so it can take over the screen; silent, because the service plays the sound. */
        internal fun channel(context: Context): String {
            val manager = NotificationManagerCompat.from(context)
            if (manager.getNotificationChannelCompat(CHANNEL) == null) {
                manager.createNotificationChannel(
                    NotificationChannelCompat.Builder(CHANNEL, NotificationManagerCompat.IMPORTANCE_HIGH)
                        .setName(context.getString(R.string.channel_reminder_ring))
                        .setDescription(context.getString(R.string.channel_reminder_ring_description))
                        .setSound(null, null)
                        .setVibrationEnabled(false)
                        .build(),
                )
            }
            return CHANNEL
        }
    }
}
