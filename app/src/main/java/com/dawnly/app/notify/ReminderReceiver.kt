package com.dawnly.app.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.dawnly.app.dawnly
import com.dawnly.app.service.launchAsync

/**
 * A reminder is due: ring it or show it, then set its next time. A snoozed one rings again
 * without moving its next time, which is still set.
 */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(ReminderScheduler.EXTRA_ID, -1L)
        val snoozed = intent.getBooleanExtra(ReminderScheduler.EXTRA_SNOOZE, false)
        launchAsync(context) {
            val reminder = context.dawnly.reminders.current().firstOrNull { it.id == id } ?: return@launchAsync
            if (!reminder.on) return@launchAsync
            when {
                !reminder.ring -> ReminderScheduler.show(context, reminder)
                WakeMusic.playForReminder(context, reminder) -> Unit
                // Android may refuse, e.g. after an inexact alarm: then a notification whose sound repeats.
                !ReminderRingService.start(context, reminder) -> ReminderScheduler.show(context, reminder, insistent = true)
            }
            if (!snoozed) ReminderScheduler.schedule(context, reminder)
        }
    }
}
