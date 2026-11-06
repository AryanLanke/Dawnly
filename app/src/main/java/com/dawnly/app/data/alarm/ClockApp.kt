package com.dawnly.app.data.alarm

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.provider.AlarmClock
import android.util.Log
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

/**
 * The hour and minute sent with ACTION_SET_ALARM: 24-hour values, so 00:00 is midnight and 12 is
 * noon. Pure, so it's unit tested; the Clock app reads these as the time of day.
 */
data class SetAlarmExtras(val hour: Int, val minutes: Int) {
    companion object {
        fun of(time: LocalTime) = SetAlarmExtras(time.hour, time.minute)
    }
}

/** An installed app, named the way the user sees it. */
data class AppRef(val packageName: String, val label: String)

sealed interface DismissResult {
    /**
     * [request] went to [handler]. Whether the alarm is really gone shows up as a next-alarm change;
     * if it doesn't, `QuietDismiss` tries the other forms.
     */
    data class Requested(val handler: AppRef, val request: DismissRequest) : DismissResult

    /** No app can dismiss alarms, so [clock] was opened for the user to dismiss it by hand. */
    data class OpenedClock(val clock: AppRef) : DismissResult

    data object NoClockApp : DismissResult
}

/**
 * Talks to the user's Clock app through the public [AlarmClock] intents. Dawnly never touches the
 * alarm itself; it only asks the Clock app.
 */
class ClockApp(context: Context) {
    private val packageManager = context.applicationContext.packageManager

    /** The app that would receive a dismiss request, or null if none can handle it. */
    fun dismissHandler(): AppRef? = resolve(dismissIntent(DismissRequest.Next, null))

    /**
     * Asks the Clock app to dismiss the next alarm, falling back to opening the Clock app.
     * [launcher] must be an Activity context, because this starts another app's activity.
     * [request] is the form to send first; [alarmAt] is needed for the by-time forms.
     */
    fun dismissNextAlarm(
        launcher: Context,
        ownerPackage: String?,
        request: DismissRequest = DismissRequest.Next,
        alarmAt: Instant? = null,
    ): DismissResult {
        val handler = dismissHandler()
        if (handler != null && send(launcher, request, alarmAt)) {
            Log.i(TAG, "Dismiss requested from ${handler.packageName} ($request)")
            return DismissResult.Requested(handler, request)
        }
        Log.i(TAG, "No dismiss handler; opening the Clock app instead")
        return openClock(launcher, ownerPackage)?.let(DismissResult::OpenedClock)
            ?: DismissResult.NoClockApp
    }

    /** Sends one form of the dismiss request. True if it reached the Clock app. */
    fun send(launcher: Context, request: DismissRequest, alarmAt: Instant?): Boolean {
        val intent = dismissIntent(request, alarmAt)
        if (launcher !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return launcher.tryStart(intent)
    }

    /**
     * Sets an alarm for [time] in the Clock app, so it shows there like any alarm and rings with
     * the Clock's own sound. Returns the Clock app that set it, or null if none can. Dawnly then
     * sees it as the next alarm and watches before it as usual.
     */
    fun setAlarm(launcher: Context, time: LocalTime): AppRef? {
        val intent = setAlarmIntent(time)
        val handler = resolve(intent) ?: return null
        if (launcher !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return if (launcher.tryStart(intent)) handler else null
    }

    /** Opens the alarm list, or else the app that owns the alarm. Returns the app opened. */
    fun openClock(launcher: Context, ownerPackage: String?): AppRef? {
        val candidates = listOfNotNull(
            Intent(AlarmClock.ACTION_SHOW_ALARMS),
            ownerPackage?.let(packageManager::getLaunchIntentForPackage),
        )
        for (intent in candidates) {
            val app = resolve(intent) ?: continue
            if (launcher.tryStart(intent)) return app
        }
        return null
    }

    fun appRef(packageName: String): AppRef = AppRef(packageName, label(packageName))

    private fun resolve(intent: Intent): AppRef? =
        intent.resolveActivity(packageManager)?.let { appRef(it.packageName) }

    private fun label(packageName: String): String = try {
        packageManager.getApplicationLabel(applicationInfo(packageName)).toString()
    } catch (e: PackageManager.NameNotFoundException) {
        // Not visible to us (see <queries> in the manifest) or uninstalled.
        packageName
    }

    private fun applicationInfo(packageName: String): ApplicationInfo =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            packageManager.getApplicationInfo(packageName, 0)
        }

    private fun Context.tryStart(intent: Intent): Boolean = try {
        startActivity(intent)
        true
    } catch (e: ActivityNotFoundException) {
        Log.w(TAG, "Couldn't start $intent", e)
        false
    } catch (e: SecurityException) {
        Log.w(TAG, "Not allowed to start $intent", e)
        false
    }

    companion object {
        private const val TAG = "ClockApp"

        /** A new alarm at [time], set without opening the Clock app's screen. */
        fun setAlarmIntent(time: LocalTime): Intent {
            val extras = SetAlarmExtras.of(time)
            return Intent(AlarmClock.ACTION_SET_ALARM)
                .putExtra(AlarmClock.EXTRA_HOUR, extras.hour)
                .putExtra(AlarmClock.EXTRA_MINUTES, extras.minutes)
                .putExtra(AlarmClock.EXTRA_SKIP_UI, true)
        }

        /**
         * `EXTRA_SKIP_UI` is documented only for setting alarms, and Google Clock shows its alarm
         * list anyway, but a Clock app that honours it can dismiss without showing its screen.
         * [alarmAt] is needed for the by-time forms; without it they fall back to [DismissRequest.Next].
         */
        fun dismissIntent(request: DismissRequest, alarmAt: Instant?): Intent {
            val intent = Intent(AlarmClock.ACTION_DISMISS_ALARM)
            if (request == DismissRequest.Next || alarmAt == null) {
                return intent
                    .putExtra(AlarmClock.EXTRA_ALARM_SEARCH_MODE, AlarmClock.ALARM_SEARCH_MODE_NEXT)
                    .putExtra(AlarmClock.EXTRA_SKIP_UI, true)
            }
            val time = TimeSearch.of(alarmAt.atZone(ZoneId.systemDefault()).toLocalTime())
            intent
                .putExtra(AlarmClock.EXTRA_ALARM_SEARCH_MODE, AlarmClock.ALARM_SEARCH_MODE_TIME)
                .putExtra(AlarmClock.EXTRA_HOUR, time.hour)
                .putExtra(AlarmClock.EXTRA_MINUTES, time.minutes)
                .putExtra(AlarmClock.EXTRA_IS_PM, time.isPm)
            if (request == DismissRequest.ByTime) intent.putExtra(AlarmClock.EXTRA_SKIP_UI, true)
            return intent
        }
    }
}
