package com.dawnly.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.dawnly.app.dawnly
import kotlinx.coroutines.launch

/** Our own check alarm, set by [WatchScheduler]: the window opens, or the alarm is due. */
class WatchAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        reconcileAsync(context, fromCheckAlarm = true)
    }
}

/**
 * Events that can change the plan: the next alarm changed, a reboot or app update cleared our
 * alarms, or the user allowed exact alarms. None of these are sent by other apps.
 */
class SystemEventsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.i("SystemEventsReceiver", "Received ${intent.action}")
        reconcileAsync(context, fromCheckAlarm = false)
    }
}

private fun BroadcastReceiver.reconcileAsync(context: Context, fromCheckAlarm: Boolean) {
    val pending = goAsync()
    val app = context.dawnly
    app.scope.launch {
        try {
            app.watchScheduler.reconcile(fromCheckAlarm)
        } finally {
            pending.finish()
        }
    }
}
