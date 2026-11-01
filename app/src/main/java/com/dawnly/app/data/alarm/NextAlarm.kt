package com.dawnly.app.data.alarm

import java.time.Instant

/**
 * The next alarm the system knows about, from `AlarmManager.getNextAlarmClock()`.
 *
 * [ownerPackage] is the app that set it (normally the Clock app). It's null when the alarm has no
 * show intent, which the platform allows.
 */
data class NextAlarm(
    val triggerAt: Instant,
    val ownerPackage: String?,
)
