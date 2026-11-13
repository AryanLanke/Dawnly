package com.dawnly.app.notify

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.dawnly.app.R
import java.time.Instant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

class WakeMusicService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var job: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val alarmAt = intent?.getLongExtra(EXTRA_ALARM_AT, 0L)?.takeIf { it > 0L }?.let(Instant::ofEpochMilli)
        if (alarmAt == null) {
            stopSelf(startId)
            return START_NOT_STICKY
        }
        if (job?.isActive == true) return START_NOT_STICKY
        try {
            val notification = NotificationCompat.Builder(this, WakeMusic.channel(this))
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(getString(R.string.channel_wake_music))
                .setContentIntent(Notify.openApp(this))
                .setOngoing(true)
                .setSilent(true)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .build()
            ServiceCompat.startForeground(
                this, 8, notification,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0,
            )
            wakeLock = getSystemService(PowerManager::class.java)
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "dawnly:wakeMusic")
                .apply { acquire(20_000L) }
            job = scope.launch {
                try {
                    withTimeout(15_000L) { WakeMusic.onAlarm(this@WakeMusicService, alarmAt) }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: RuntimeException) {
                    Log.w("WakeMusic", "Music attempt failed; Clock alarm stays on", e)
                } finally {
                    wakeLock?.let { if (it.isHeld) it.release() }
                    ServiceCompat.stopForeground(this@WakeMusicService, ServiceCompat.STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }
        } catch (e: RuntimeException) {
            Log.w("WakeMusic", "Could not start music service; Clock alarm stays on", e)
            stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        wakeLock?.let { if (it.isHeld) it.release() }
        super.onDestroy()
    }

    companion object {
        private const val EXTRA_ALARM_AT = "com.dawnly.app.extra.MUSIC_SERVICE_ALARM_AT"

        fun start(context: Context, alarmAt: Instant) {
            try {
                ContextCompat.startForegroundService(context, Intent(context, WakeMusicService::class.java)
                    .putExtra(EXTRA_ALARM_AT, alarmAt.toEpochMilli()))
            } catch (e: RuntimeException) {
                Log.w("WakeMusic", "Music service not allowed; Clock alarm stays on", e)
            }
        }
    }
}
