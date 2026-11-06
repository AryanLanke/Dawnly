package com.dawnly.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.dawnly.app.dawnly
import com.dawnly.app.notify.ReminderScheduler

/** Our own check alarm, set by [WatchScheduler]: the window opens, or the alarm is due. */
class WatchAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        reconcileAsync(context, fromCheckAlarm = true, resetReminders = false)
    }
}

/**
 * Events that can change the plan: the next alarm changed, a reboot or app update cleared our
 * alarms, or the user allowed exact alarms. None of these are sent by other apps. A reboot or an
 * update also cleared the reminders' alarms, so they're set again too.
 */
class SystemEventsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.i("SystemEventsReceiver", "Received ${intent.action}")
        val cleared = intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED
        reconcileAsync(context, fromCheckAlarm = false, resetReminders = cleared)
    }
}

/** Runs inside [goAsync], so the process isn't killed before the reminders are set again. */
private fun BroadcastReceiver.reconcileAsync(context: Context, fromCheckAlarm: Boolean, resetReminders: Boolean) {
    launchAsync(context) {
        if (resetReminders) ReminderScheduler.sync(context)
        context.dawnly.watchScheduler.reconcile(fromCheckAlarm)
    }
}
