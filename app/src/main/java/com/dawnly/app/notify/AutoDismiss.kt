package com.dawnly.app.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log
import com.dawnly.app.dawnly
import com.dawnly.app.data.settings.DismissMode
import java.time.Duration
import java.time.Instant

/** What Dawnly does once the user is up. Decided by [AutoDismissPlan.decide]. */
sealed interface AwakeAction {
    /** The "Up already?" prompt; the user dismisses. */
    data object Ask : AwakeAction

    /** The one-minute warning, then [AutoDismissReceiver] dismisses at [dismissAt]. */
    data class Warn(val dismissAt: Instant) : AwakeAction

    /** Dismiss now, no warning. */
    data object DismissNow : AwakeAction
}

/** When an opted-in auto-dismiss goes ahead. Pure, so it's unit tested. */
object AutoDismissPlan {
    /**
     * Turns the user's [mode] into what happens now. Every automatic choice fails safe to asking:
     * without "Display over other apps" ([canOpenClock]) Dawnly can't reach the Clock app from the
     * background, and a warning needs time, an exact alarm, and notifications to be seen and stopped.
     */
    fun decide(
        mode: DismissMode,
        now: Instant,
        alarmAt: Instant,
        canOpenClock: Boolean,
        canTimeExactly: Boolean,
        canNotify: Boolean,
    ): AwakeAction {
        if (mode == DismissMode.Ask || !canOpenClock) return AwakeAction.Ask
        if (mode == DismissMode.Instant) return AwakeAction.DismissNow
        val dismissAt = dismissAt(now, alarmAt)
        return if (dismissAt != null && canTimeExactly && canNotify) AwakeAction.Warn(dismissAt) else AwakeAction.Ask
    }

    /** How long the "Dismissing your 5:30 alarm in 1 minute" warning shows before it acts. */
    val WARNING: Duration = Duration.ofMinutes(1)

    /** Room for the request to reach the Clock app before the alarm rings. */
    private val MARGIN: Duration = Duration.ofSeconds(10)

    /**
     * The moment to dismiss the alarm at [alarmAt], or null when the warning wouldn't finish in
     * time. Then the user is asked instead, as with auto-dismiss off.
     */
    fun dismissAt(now: Instant, alarmAt: Instant): Instant? =
        now.plus(WARNING).takeIf { !it.plus(MARGIN).isAfter(alarmAt) }
}

/**
 * When to try again after the Clock app left the alarm on. Clock apps built on Android's own only
 * dismiss an upcoming alarm once its "upcoming alarm" notice is up, which can be as late as an hour
 * before. So a dismiss tried earlier gets one more go 55 minutes before the alarm. Pure, so it's
 * unit tested.
 */
object DismissRetry {
    /** How long before the alarm the second try goes. */
    val BEFORE_ALARM: Duration = Duration.ofMinutes(55)

    /** Closer than this to now, a second try is pointless: the first just failed. */
    private val MIN_WAIT: Duration = Duration.ofMinutes(5)

    /** When to try again, or null when it's too close or this was the second try already. */
    fun retryAt(now: Instant, alarmAt: Instant, alreadyRetried: Boolean): Instant? {
        if (alreadyRetried) return null
        return alarmAt.minus(BEFORE_ALARM).takeIf { !it.isBefore(now.plus(MIN_WAIT)) }
    }
}

/**
 * Fires when the auto-dismiss warning runs out, or for the second try ([DismissRetry]). Checks the alarm is still the one the warning was
 * about, then opens [DismissAlarmActivity]. Starting an activity from the background is allowed
 * because the user granted "Display over other apps"; if they took it back since, they're asked
 * with the normal prompt instead.
 */
class AutoDismissReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val alarmAt = intent.getLongExtra(AwakeNotifier.EXTRA_ALARM_AT, 0L)
            .takeIf { it > 0L }?.let(Instant::ofEpochMilli) ?: return
        val check = DismissGuard.check(alarmAt, context.dawnly.nextAlarms.current()?.triggerAt, Instant.now())
        if (check != DismissCheck.Ok) {
            // E.g. the user dismissed it by hand during the minute. Nothing to say.
            Log.i(TAG, "Not auto-dismissing the alarm at $alarmAt: $check")
            AwakeNotifier.cancel(context)
            return
        }
        if (!Settings.canDrawOverlays(context)) {
            Log.w(TAG, "Display over other apps was turned off; asking instead")
            AwakeNotifier.post(context, alarmAt)
            return
        }
        val retry = intent.getBooleanExtra(AwakeNotifier.EXTRA_RETRY, false)
        Log.i(TAG, "Auto-dismissing the alarm at $alarmAt${if (retry) " (second try)" else ""}")
        context.startActivity(DismissAlarmActivity.intent(context, alarmAt, retry))
    }

    private companion object {
        const val TAG = "AutoDismiss"
    }
}
