package com.dawnly.app

import android.app.Application
import android.content.Context
import com.dawnly.app.data.alarm.NextAlarmSource
import com.dawnly.app.data.reminder.ReminderStore
import com.dawnly.app.data.settings.SettingsRepository
import com.dawnly.app.data.watch.ArmedAlarmStore
import com.dawnly.app.notify.ReminderScheduler
import com.dawnly.app.service.WatchNotifications
import com.dawnly.app.service.WatchScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Holds the app-wide singletons. Small enough that manual wiring beats a DI library. */
class DawnlyApp : Application() {
    /** For work that must outlive a screen or a receiver, like [WatchScheduler.reconcile]. */
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val nextAlarms by lazy { NextAlarmSource(this) }
    val settings by lazy { SettingsRepository(this) }
    val watchScheduler by lazy { WatchScheduler(this, nextAlarms, settings, ArmedAlarmStore(this)) }
    val reminders by lazy { ReminderStore(this) }

    override fun onCreate() {
        super.onCreate()
        WatchNotifications.createChannels(this)
        ReminderScheduler.createChannel(this)
    }
}

val Context.dawnly: DawnlyApp get() = applicationContext as DawnlyApp
