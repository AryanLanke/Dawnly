package com.dawnly.app.data.alarm

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.provider.AlarmClock
import android.util.Log

/** An installed app, named the way the user sees it. */
data class AppRef(val packageName: String, val label: String)

sealed interface DismissResult {
    /** The request went to [handler]. Whether the alarm is really gone shows up as a next-alarm change. */
    data class Requested(val handler: AppRef) : DismissResult

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
    fun dismissHandler(): AppRef? = resolve(dismissNextAlarmIntent())

    /**
     * Asks the Clock app to dismiss the next alarm, falling back to opening the Clock app.
     * [launcher] must be an Activity context, because this starts another app's activity.
     */
    fun dismissNextAlarm(launcher: Context, ownerPackage: String?): DismissResult {
        val handler = dismissHandler()
        if (handler != null && launcher.tryStart(dismissNextAlarmIntent())) {
            Log.i(TAG, "Dismiss requested from ${handler.packageName}")
            return DismissResult.Requested(handler)
        }
        Log.i(TAG, "No dismiss handler; opening the Clock app instead")
        return openClock(launcher, ownerPackage)?.let(DismissResult::OpenedClock)
            ?: DismissResult.NoClockApp
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

        fun dismissNextAlarmIntent(): Intent = Intent(AlarmClock.ACTION_DISMISS_ALARM)
            .putExtra(AlarmClock.EXTRA_ALARM_SEARCH_MODE, AlarmClock.ALARM_SEARCH_MODE_NEXT)
    }
}
