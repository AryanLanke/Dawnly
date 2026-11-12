package com.dawnly.app.notify

import android.app.Activity
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.util.Log
import com.dawnly.app.MainActivity
import com.dawnly.app.data.alarm.AppRef
import com.dawnly.app.data.alarm.ClockApp
import com.dawnly.app.data.alarm.DismissMemory
import com.dawnly.app.data.alarm.DismissRequest
import com.dawnly.app.data.alarm.DismissResult
import com.dawnly.app.data.alarm.NextAlarm
import com.dawnly.app.data.settings.DismissMode
import com.dawnly.app.dawnly
import com.dawnly.app.domain.learn.Answer
import com.dawnly.app.system.AppUsage
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
 * and [DismissedNotice] says so with the sound played backwards (forwards with Right away).
 *
 * Not every Clock app acts on the same form of the request (Xiaomi's opened and left the alarm on),
 * so if the alarm doesn't move, the other forms ([DismissRequest]) are sent in turn, and the one
 * that worked goes first next time ([DismissMemory]). If none works, the Clock app stays open,
 * [DismissFailedNotice] says the alarm is still on, and if it's early there's one more try later
 * ([DismissRetry]).
 */
object QuietDismiss {
    private const val TAG = "QuietDismiss"

    /** How long the Clock app gets to dismiss, per form of the request, before we try the next. */
    private const val WAIT_MS = 4_000L

    /** Google Clock opens its alarm list just before it dismisses; let it land so we end up on top. */
    private const val SETTLE_MS = 400L

    /** How far back to look for the app the user was in. A locked phone can sit for hours. */
    private const val LOOKBACK_MS = 24 * 60 * 60 * 1000L

    /**
     * [launcher] starts the Clock app, so it must be an activity. [backToDawnly] when the user
     * tapped dismiss in Dawnly itself; otherwise they go back to the app they were in. [isRetry]
     * for the second try, after which there's no third.
     */
    fun dismiss(
        launcher: Activity,
        alarm: NextAlarm?,
        backToDawnly: Boolean,
        isRetry: Boolean = false,
        by: DismissBy = DismissBy.User,
    ): DismissResult {
        val requestedAt = System.currentTimeMillis()
        val clock = ClockApp(launcher)
        val memory = DismissMemory(launcher)
        val preferred = clock.dismissHandler()?.let { memory.worked(it.packageName) }
        val order = DismissRequest.order(preferred, haveTime = alarm != null)
        val result = clock.dismissNextAlarm(launcher, alarm?.ownerPackage, order.first(), alarm?.triggerAt)
        if (result is DismissResult.Requested && alarm != null) {
            val attempt = Attempt(alarm.triggerAt, result.handler, order.drop(1), requestedAt, backToDawnly, isRetry, by)
            keepTrying(launcher.applicationContext, attempt, result.request)
        }
        return result
    }

    private class Attempt(
        val alarmAt: Instant,
        val clock: AppRef,
        val otherForms: List<DismissRequest>,
        val requestedAt: Long,
        val backToDawnly: Boolean,
        val isRetry: Boolean,
        val by: DismissBy,
    )

    /** Waits for the alarm to move after [first]; if it doesn't, sends the other forms in turn. */
    private fun keepTrying(context: Context, attempt: Attempt, first: DismissRequest) {
        val app = context.dawnly
        app.scope.launch {
            var sent = first
            val left = attempt.otherForms.toMutableList()
            while (true) {
                if (moved(context, attempt.alarmAt)) {
                    Log.i(TAG, "${attempt.clock.packageName} dismissed the alarm at ${attempt.alarmAt} ($sent)")
                    DismissMemory(context).remember(attempt.clock.packageName, sent)
                    // They were up: Dawnly learns from it (if it had asked). Music says nothing about that.
                    if (attempt.by != DismissBy.Music) context.dawnly.learning.answer(Answer.Up, Instant.now())
                    if (attempt.by == DismissBy.Dawnly) StillUpCheck.schedule(context, attempt.alarmAt)
                    returnToUser(context, attempt)
                    return@launch
                }
                val next = left.removeFirstOrNull() ?: break
                Log.i(TAG, "${attempt.clock.packageName} left the alarm on after $sent; trying $next")
                if (withContext(Dispatchers.Main) { ClockApp(context).send(context, next, attempt.alarmAt) }) sent = next
            }
            Log.i(TAG, "The alarm at ${attempt.alarmAt} didn't move after any request; leaving the Clock app open")
            val retryAt = DismissRetry.retryAt(Instant.now(), attempt.alarmAt, attempt.isRetry)
                ?.takeIf { AwakeNotifier.scheduleRetry(context, attempt.alarmAt, it) }
            DismissFailedNotice.post(context, attempt.alarmAt, attempt.clock.label, retryAt)
        }
    }

    /** True once the next alarm is no longer the one at [alarmAt], within [WAIT_MS]. */
    private suspend fun moved(context: Context, alarmAt: Instant): Boolean =
        withTimeoutOrNull(WAIT_MS) {
            context.dawnly.nextAlarms.nextAlarm.first { it?.triggerAt != alarmAt }
            true
        } ?: false

    private suspend fun returnToUser(context: Context, attempt: Attempt) {
        val target = if (attempt.backToDawnly) {
            ReturnTo.Dawnly
        } else {
            withContext(Dispatchers.IO) { whereTheUserWas(context, attempt.requestedAt) }
        }
        // Right away played no sound before, so its notice plays the sound forwards, not backwards.
        val forward = context.dawnly.settings.dismissMode.first() == DismissMode.Instant
        delay(SETTLE_MS)
        withContext(Dispatchers.Main) {
            goBack(context, target)
            DismissFailedNotice.cancel(context)
            if (attempt.by == DismissBy.Music) {
                WakeMusic.postPlaying(context, attempt.alarmAt)
            } else {
                DismissedNotice.post(context, attempt.alarmAt, forward)
            }
        }
    }

    private fun whereTheUserWas(context: Context, before: Long): ReturnTo {
        val lastApp = if (AppUsage.isGranted(context)) lastAppInFront(context, before) else null
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

    private fun homeApps(context: Context): Set<String> = AppUsage.launchers(context) + AppUsage.SYSTEM_UI

    private fun home(): Intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)

    /** Brings [target] back over the Clock app, as if the user had tapped its icon. */
    private fun goBack(context: Context, target: ReturnTo) {
        val intent = when (target) {
            ReturnTo.Dawnly -> MainActivity.backToFront(context)
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
