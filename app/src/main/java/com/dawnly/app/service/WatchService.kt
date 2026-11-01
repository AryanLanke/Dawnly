package com.dawnly.app.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import com.dawnly.app.dawnly
import java.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Runs only during the watch window, with a low-priority ongoing notification. Started and
 * stopped by [WatchScheduler]. The wake signals (Phase 3) will live here.
 *
 * Foreground service type: `specialUse` (Android 14+). Watching for signs someone woke up
 * before their alarm doesn't fit a standard type. `health` comes closest, but it needs the
 * activity permission granted before every start, and the user may switch the steps signal off.
 */
class WatchService : Service() {
    override fun onCreate() {
        super.onCreate()
        running.value = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // A null intent means the system restarted us after killing the process.
        val alarmAt = intent?.getLongExtra(EXTRA_ALARM_AT, 0L)?.takeIf { it > 0L }
            ?.let(Instant::ofEpochMilli)
            ?: dawnly.nextAlarms.current()?.triggerAt
        val notification = WatchNotifications.watching(this, alarmAt)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(WatchNotifications.WATCHING_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(WatchNotifications.WATCHING_ID, notification)
        }
        // Stops us again if the window ended while we were starting (or while we were dead).
        dawnly.scope.launch { dawnly.watchScheduler.reconcile() }
        return START_STICKY
    }

    override fun onDestroy() {
        running.value = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val EXTRA_ALARM_AT = "com.dawnly.app.extra.ALARM_AT"

        private val running = MutableStateFlow(false)

        /** Whether the service is running in this process. */
        val isRunning: StateFlow<Boolean> = running.asStateFlow()

        fun startIntent(context: Context, alarmAt: Instant): Intent =
            Intent(context, WatchService::class.java).putExtra(EXTRA_ALARM_AT, alarmAt.toEpochMilli())
    }
}
