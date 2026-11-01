package com.dawnly.app.ui.permissions

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.dawnly.app.service.WakeWatcher

/** The permissions Dawnly asks for, in onboarding order. */
enum class AppPermission {
    /** To ask before dismissing the alarm (Android 13+). */
    Notifications,

    /** "Alarms & reminders", so the window opens on time (Android 12+). */
    ExactAlarms,

    /** The steps signal (Android 10+). */
    ActivityRecognition,

    /** "Usage access", for the apps-opened signal: which app is in front, during the window only. */
    UsageAccess,

    /** Unrestricted battery, so the phone doesn't stop the watch service. */
    Battery,

    /**
     * "Display over other apps", only for auto-dismiss: it's what lets Dawnly open the Clock app
     * from the background (Android 10+). Asked when that switch is turned on, never in onboarding.
     */
    Overlay,
    ;

    /** Asked in onboarding and listed in Settings. [Overlay] only matters with auto-dismiss on. */
    val askedUpFront: Boolean
        get() = this != Overlay

    /** False on Android versions where this permission doesn't exist (it's granted already). */
    val isApplicable: Boolean
        get() = when (this) {
            Notifications -> Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            ExactAlarms -> Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            ActivityRecognition -> Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
            UsageAccess, Battery, Overlay -> true
        }

    /** The runtime permission behind a system dialog, or null when it's granted in system settings. */
    @get:SuppressLint("InlinedApi") // Only read where isApplicable says the permission exists.
    val runtimePermission: String?
        get() = when (this) {
            Notifications -> if (isApplicable) Manifest.permission.POST_NOTIFICATIONS else null
            ActivityRecognition -> if (isApplicable) Manifest.permission.ACTIVITY_RECOGNITION else null
            ExactAlarms, UsageAccess, Battery, Overlay -> null
        }

    fun isGranted(context: Context): Boolean {
        if (!isApplicable) return true
        runtimePermission?.let {
            return ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        return when (this) {
            ExactAlarms -> Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
            Battery -> context.getSystemService(PowerManager::class.java)
                .isIgnoringBatteryOptimizations(context.packageName)
            Overlay -> Settings.canDrawOverlays(context)
            UsageAccess -> WakeWatcher.hasUsageAccess(context)
            Notifications, ActivityRecognition -> true
        }
    }

    /**
     * Where to send the user to grant this in system settings: the only way for exact alarms,
     * usage access, battery and overlays, and the fallback once a runtime permission has been denied for good.
     */
    fun settingsIntent(context: Context): Intent {
        val packageUri = "package:${context.packageName}".toUri()
        return when (this) {
            Notifications -> Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            ExactAlarms -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, packageUri)
            } else {
                appDetails(packageUri)
            }
            // Shows a one-tap system dialog. Needs REQUEST_IGNORE_BATTERY_OPTIMIZATIONS in the manifest.
            Battery -> Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, packageUri)
            ActivityRecognition -> appDetails(packageUri)
            Overlay -> Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, packageUri)
            // The list of apps; a package Uri isn't honoured on every phone.
            UsageAccess -> Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        }
    }

    companion object {
        fun appDetails(packageUri: android.net.Uri) =
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri)
    }
}
