package com.dawnly.app.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.dawnly.app.dawnly
import com.dawnly.app.service.launchAsync

/** A reminder is due: show it, then set its next time. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(ReminderScheduler.EXTRA_ID, -1L)
        launchAsync(context) {
            val reminder = context.dawnly.reminders.current().firstOrNull { it.id == id } ?: return@launchAsync
            if (!reminder.on) return@launchAsync
            ReminderScheduler.show(context, reminder)
            ReminderScheduler.schedule(context, reminder)
        }
    }
}
