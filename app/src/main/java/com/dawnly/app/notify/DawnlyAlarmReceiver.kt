package com.dawnly.app.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.dawnly.app.R
import com.dawnly.app.data.sound.AlertSound
import com.dawnly.app.dawnly
import com.dawnly.app.service.launchAsync

/**
 * A Dawnly alarm is due: ring it like an alarm, then set its next occurrence (alarms repeat).
 * A snoozed one rings again without moving its next time, which is still set.
 */
class DawnlyAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(DawnlyAlarmScheduler.EXTRA_ID, -1L)
        val snoozed = intent.getBooleanExtra(DawnlyAlarmScheduler.EXTRA_SNOOZE, false)
        launchAsync(context) {
            val alarm = context.dawnly.dawnlyAlarms.current().firstOrNull { it.id == id } ?: return@launchAsync
            if (!alarm.on) return@launchAsync
            val sound = alarm.sound?.let { name -> AlertSound.entries.firstOrNull { it.name == name } }
                ?: AlertSound.PhoneDefault
            val why = context.getString(R.string.alarm_ringing_why)
            val ringId = DawnlyAlarmScheduler.ringId(id)
            // Android may refuse the full-screen ring, e.g. after an inexact alarm: notify instead.
            if (!ReminderRingService.start(context, ringId, alarm.label, why, sound)) {
                DawnlyAlarmScheduler.showFallback(context, Ringing(ringId, alarm.label, why), sound)
            }
            if (!snoozed) DawnlyAlarmScheduler.schedule(context, alarm)
            context.dawnly.watchScheduler.reconcile()
        }
    }
}