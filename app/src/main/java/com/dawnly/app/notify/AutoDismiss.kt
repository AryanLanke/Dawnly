package com.dawnly.app.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log
import com.dawnly.app.dawnly
import com.dawnly.app.data.settings.DismissMode
import com.dawnly.app.ui.permissions.AppPermission
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

/**
 * How Dawnly can open the Clock app when nobody tapped anything. Android blocks starting apps from
 * the background, with a few exceptions.
 */
enum class ClockReach {
    /** "Display over other apps": any time, locked or not. Android locks it for apps installed from a file. */
    Anytime,

    /**
     * A full-screen notification ([AppPermission.FullScreen]): Android opens it straight away with the
     * phone locked, and shows it for one tap while the phone is in use.
     */
    WhenLocked,

    /** Neither: the user is asked. */
    None,
    ;

    companion object {
        fun of(context: Context): ClockReach = when {
            Settings.canDrawOverlays(context) -> Anytime
            AppPermission.FullScreen.isGranted(context) -> WhenLocked
            else -> None
        }
    }
}

/** When an opted-in auto-dismiss goes ahead. Pure, so it's unit tested. */
object AutoDismissPlan {
    /**
     * Turns the user's [mode] into what happens now. Every automatic choice fails safe to asking:
     * Dawnly has to be able to reach the Clock app from the background ([reach]), a full-screen
     * notification needs notifications, and a warning needs time, an exact alarm, and notifications
     * to be seen and stopped. When music made the difference ([musicTippedIt]), it always asks:
     * people fall asleep to music, and an unanswered question lets the alarm ring.
     */
    fun decide(
        mode: DismissMode,
        now: Instant,
        alarmAt: Instant,
        reach: ClockReach,
        canTimeExactly: Boolean,
        canNotify: Boolean,
        musicTippedIt: Boolean = false,
    ): AwakeAction {
        // Music can play while someone sleeps: ask, so silence lets the alarm ring.
        if (mode == DismissMode.Ask || reach == ClockReach.None || musicTippedIt) return AwakeAction.Ask
        if (reach == ClockReach.WhenLocked && !canNotify) return AwakeAction.Ask
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
 * Fires when the auto-dismiss warning runs out, or for the second try ([DismissRetry]). Checks the
 * alarm is still the one the warning was about, then opens [DismissAlarmActivity]: straight away
 * with "Display over other apps", else through a full-screen notification ([ClockReach]). If both
 * were taken back since, the user is asked with the normal prompt instead.
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
        val retry = intent.getBooleanExtra(AwakeNotifier.EXTRA_RETRY, false)
        Log.i(TAG, "Auto-dismissing the alarm at $alarmAt${if (retry) " (second try)" else ""}")
        AwakeNotifier.openDismiss(context, alarmAt, retry)
    }

    private companion object {
        const val TAG = "AutoDismiss"
    }
}
