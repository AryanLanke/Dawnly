package com.dawnly.app.notify

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.dawnly.app.MainActivity
import com.dawnly.app.R
import com.dawnly.app.data.alarm.ClockApp
import com.dawnly.app.data.alarm.DawnlyAlarms
import com.dawnly.app.data.alarm.DismissMemory
import com.dawnly.app.data.alarm.DismissRequest
import com.dawnly.app.data.alarm.DismissResult
import com.dawnly.app.data.alarm.NextAlarm
import com.dawnly.app.dawnly
import java.time.Instant
import java.time.LocalTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Changing or deleting an alarm set from Dawnly. Android lets apps only set alarms and dismiss
 * them, so a change is: switch the old one off in the Clock app (a dismiss), wait until it's really
 * off, come back to Dawnly, and set the new one there ([MainActivity] picks it up). The new one is
 * set only once the old one is off, so the user never ends up with both, or with neither.
 *
 * Unlike [QuietDismiss], it isn't a wake-up: nothing is learned and no "Alarm's off" notice shows.
 */
object AlarmEdit {
    private const val TAG = "AlarmEdit"
    private const val WAIT_MS = 4_000L
    private const val SETTLE_MS = 400L

    @Volatile
    private var pendingSet: LocalTime? = null

    /** The new time to set once Dawnly is back in front, then forgotten. */
    fun takePendingSet(): LocalTime? = pendingSet.also { pendingSet = null }

    /**
     * Switches [alarm] (the next alarm) off, then sets [replaceWith] if it isn't null. [launcher]
     * must be an activity, because this starts the Clock app.
     */
    fun remove(launcher: Activity, alarm: NextAlarm, replaceWith: LocalTime?): DismissResult {
        val clock = ClockApp(launcher)
        val preferred = clock.dismissHandler()?.let { DismissMemory(launcher).worked(it.packageName) }
        val order = DismissRequest.order(preferred, haveTime = true)
        val result = clock.dismissNextAlarm(launcher, alarm.ownerPackage, order.first(), alarm.triggerAt)
        if (result is DismissResult.Requested) {
            keepTrying(launcher.applicationContext, alarm.triggerAt, result.handler.packageName, result.request, order.drop(1), replaceWith)
        }
        return result
    }

    private fun keepTrying(
        context: Context,
        alarmAt: Instant,
        clockPackage: String,
        first: DismissRequest,
        otherForms: List<DismissRequest>,
        replaceWith: LocalTime?,
    ) {
        context.dawnly.scope.launch {
            var sent = first
            val left = otherForms.toMutableList()
            while (true) {
                if (moved(context, alarmAt)) {
                    Log.i(TAG, "$clockPackage switched off the alarm at $alarmAt ($sent)")
                    DismissMemory(context).remember(clockPackage, sent)
                    DawnlyAlarms(context).forget(alarmAt)
                    pendingSet = replaceWith
                    delay(SETTLE_MS)
                    withContext(Dispatchers.Main) {
                        backToDawnly(context)
                        if (replaceWith == null) toast(context, context.getString(R.string.alarm_deleted))
                    }
                    return@launch
                }
                val next = left.removeFirstOrNull() ?: break
                Log.i(TAG, "$clockPackage left the alarm on after $sent; trying $next")
                if (withContext(Dispatchers.Main) { ClockApp(context).send(context, next, alarmAt) }) sent = next
            }
            // The Clock app stays open so the user can do it there; nothing new is set.
            Log.i(TAG, "The alarm at $alarmAt stayed on; leaving it to the user in the Clock app")
            withContext(Dispatchers.Main) {
                toast(context, context.getString(R.string.alarm_edit_failed))
            }
        }
    }

    private suspend fun moved(context: Context, alarmAt: Instant): Boolean =
        withTimeoutOrNull(WAIT_MS) {
            context.dawnly.nextAlarms.nextAlarm.first { it?.triggerAt != alarmAt }
            true
        } ?: false

    private fun backToDawnly(context: Context) {
        try {
            context.startActivity(Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: RuntimeException) {
            Log.w(TAG, "Couldn't come back to Dawnly", e)
        }
    }

    private fun toast(context: Context, text: String) = Toast.makeText(context, text, Toast.LENGTH_LONG).show()
}
