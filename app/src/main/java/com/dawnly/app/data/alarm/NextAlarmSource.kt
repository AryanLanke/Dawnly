package com.dawnly.app.data.alarm

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import java.time.Instant
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/** Reads the next alarm set in the user's Clock app, and follows changes to it. */
class NextAlarmSource(context: Context) {
    private val appContext = context.applicationContext
    private val alarmManager = appContext.getSystemService(AlarmManager::class.java)

    fun current(): NextAlarm? = alarmManager.nextAlarmClock?.let { info ->
        NextAlarm(
            triggerAt = Instant.ofEpochMilli(info.triggerTime),
            ownerPackage = info.showIntent?.creatorPackage,
        )
    }

    /** Emits the current alarm on collection, then again on every `ACTION_NEXT_ALARM_CLOCK_CHANGED`. */
    val nextAlarm: Flow<NextAlarm?> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                trySend(current())
            }
        }
        // A system broadcast, so it still arrives with RECEIVER_NOT_EXPORTED.
        ContextCompat.registerReceiver(
            appContext,
            receiver,
            IntentFilter(AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        trySend(current())
        awaitClose { appContext.unregisterReceiver(receiver) }
    }.distinctUntilChanged()
}
