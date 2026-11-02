package com.dawnly.app.notify

import android.app.Activity
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.util.Log
import com.dawnly.app.MainActivity
import com.dawnly.app.dawnly
import com.dawnly.app.data.alarm.ClockApp
import com.dawnly.app.data.alarm.DismissResult
import com.dawnly.app.data.alarm.NextAlarm
import com.dawnly.app.service.WakeWatcher
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Dismisses through the Clock app with as little Clock app on screen as possible.
 *
 * The request carries `EXTRA_SKIP_UI`, which is documented only for setting alarms; Google Clock
 * opens its alarm list for every dismiss anyway, locked or not. So once the next alarm moves on
 * (the dismiss worked), the user goes back to where they were ([ReturnPlan]) over the Clock app,
 * and [DismissedNotice] says so with the sound played backwards. If the alarm doesn't move, the
 * Clock app stays open so the user can see what went wrong.
 */
object QuietDismiss {
    private const val TAG = "QuietDismiss"

    /** How long the Clock app gets to dismiss before we stop waiting and leave it open. */
    private const val WAIT_MS = 5_000L

    /** Google Clock opens its alarm list just before it dismisses; let it land so we end up on top. */
    private const val SETTLE_MS = 400L

    /** How far back to look for the app the user was in. A locked phone can sit for hours. */
    private const val LOOKBACK_MS = 24 * 60 * 60 * 1000L

    private const val SYSTEM_UI = "com.android.systemui"

    /**
     * [launcher] starts the Clock app, so it must be an activity. [backToDawnly] when the user
     * tapped dismiss in Dawnly itself; otherwise they go back to the app they were in.
     */
    fun dismiss(launcher: Activity, alarm: NextAlarm?, backToDawnly: Boolean): DismissResult {
        val requestedAt = System.currentTimeMillis()
        val result = ClockApp(launcher).dismissNextAlarm(launcher, alarm?.ownerPackage)
        if (result is DismissResult.Requested && alarm != null) {
            returnWhenDismissed(launcher.applicationContext, alarm.triggerAt, requestedAt, backToDawnly)
        }
        return result
    }

    private fun returnWhenDismissed(context: Context, alarmAt: Instant, requestedAt: Long, backToDawnly: Boolean) {
        val app = context.dawnly
        app.scope.launch {
            val moved = withTimeoutOrNull(WAIT_MS) {
                app.nextAlarms.nextAlarm.first { it?.triggerAt != alarmAt }
                true
            } ?: false
            if (!moved) {
                Log.i(TAG, "The alarm at $alarmAt didn't move; leaving the Clock app open")
                return@launch
            }
            val target = if (backToDawnly) {
                ReturnTo.Dawnly
            } else {
                withContext(Dispatchers.IO) { whereTheUserWas(context, requestedAt) }
            }
            delay(SETTLE_MS)
            withContext(Dispatchers.Main) {
                goBack(context, target)
                DismissedNotice.post(context, alarmAt)
            }
        }
    }

    private fun whereTheUserWas(context: Context, before: Long): ReturnTo {
        val lastApp = if (WakeWatcher.hasUsageAccess(context)) lastAppInFront(context, before) else null
        return ReturnPlan.pick(lastApp, context.packageName, homeApps(context))
    }

    /** The last app to come to the front before [before], not counting the dismiss itself. */
    private fun lastAppInFront(context: Context, before: Long): String? {
        val events = context.getSystemService(UsageStatsManager::class.java)
            .queryEvents(before - LOOKBACK_MS, before)
        val event = UsageEvents.Event()
        var last: String? = null
        while (events.getNextEvent(event)) {
            @Suppress("DEPRECATION") // ACTIVITY_RESUMED (Android 10+) has the same value.
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND &&
                event.className != DismissAlarmActivity::class.java.name
            ) {
                last = event.packageName
            }
        }
        return last
    }

    private fun homeApps(context: Context): Set<String> =
        context.packageManager.queryIntentActivities(home(), 0).map { it.activityInfo.packageName }.toSet() + SYSTEM_UI

    private fun home(): Intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)

    /** Brings [target] back over the Clock app, as if the user had tapped its icon. */
    private fun goBack(context: Context, target: ReturnTo) {
        val intent = when (target) {
            ReturnTo.Dawnly -> Intent(context, MainActivity::class.java)
            is ReturnTo.App -> context.packageManager.getLaunchIntentForPackage(target.packageName) ?: home()
            ReturnTo.Home -> home()
        }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
            Log.i(TAG, "Back to $target")
        } catch (e: RuntimeException) {
            Log.w(TAG, "Couldn't go back to $target", e)
        }
    }
}
