package com.dawnly.app.ui.permissions

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.AppOpsManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.os.Process
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

    /**
     * "Usage access", for the apps-opened signal: which app is in front, during the window only.
     * Asked only when the user turns that sign on, never in onboarding: Android locks it for apps
     * installed from a file (see [canBeLocked]), and Dawnly works without it.
     */
    UsageAccess,

    /** Unrestricted battery, so the phone doesn't stop the watch service. */
    Battery,

    /**
     * "Display over other apps", only for auto-dismiss: it's what lets Dawnly open the Clock app
     * from the background (Android 10+). Asked when that switch is turned on, never in onboarding.
     */
    Overlay,

    /**
     * Full-screen notifications (Android 14+ lists it; granted at install unless taken back), the
     * kind alarm apps use. Without [Overlay], auto-dismiss goes through one: Android opens it
     * straight away with the phone locked, and shows it for a tap while the phone is in use.
     * Android doesn't lock this one for apps installed from a file.
     */
    FullScreen,
    ;

    /**
     * Asked in onboarding and always listed in Settings. The two Android can lock aren't:
     * [UsageAccess] only matters with the apps-opened sign on, [Overlay] only with auto-dismiss on.
     */
    val askedUpFront: Boolean
        get() = this != Overlay && this != UsageAccess && this != FullScreen

    /** False on Android versions where this permission doesn't exist (it's granted already). */
    val isApplicable: Boolean
        get() = when (this) {
            Notifications -> Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            ExactAlarms -> Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            ActivityRecognition -> Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
            UsageAccess, Battery, Overlay -> true
            // Before Android 14 the manifest entry is enough.
            FullScreen -> Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
        }

    /** The runtime permission behind a system dialog, or null when it's granted in system settings. */
    @get:SuppressLint("InlinedApi") // Only read where isApplicable says the permission exists.
    val runtimePermission: String?
        get() = when (this) {
            Notifications -> if (isApplicable) Manifest.permission.POST_NOTIFICATIONS else null
            ActivityRecognition -> if (isApplicable) Manifest.permission.ACTIVITY_RECOGNITION else null
            ExactAlarms, UsageAccess, Battery, Overlay, FullScreen -> null
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
            FullScreen -> Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE ||
                context.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()
            Notifications, ActivityRecognition -> true
        }
    }

    /**
     * Where to send the user to grant this in system settings, best first: the only way for exact
     * alarms, usage access, battery and overlays, and the fallback once a runtime permission has
     * been denied for good. Some phones accept a screen's intent and then show nothing, so the
     * later ones are tried when the first doesn't appear ([PermissionsController]). The last is
     * always Dawnly's own App info page.
     */
    fun settingsIntents(context: Context): List<Intent> {
        val packageUri = "package:${context.packageName}".toUri()
        val screens = when (this) {
            Notifications -> listOf(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
            )
            ExactAlarms -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                listOf(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, packageUri))
            } else {
                emptyList()
            }
            // A one-tap system dialog (needs REQUEST_IGNORE_BATTERY_OPTIMIZATIONS in the manifest).
            // Xiaomi phones often swallow it, so then the brand's own screen, then Android's list.
            Battery -> listOfNotNull(
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, packageUri),
                OemBattery.detect(Build.MANUFACTURER)?.batterySaverScreen(context),
                Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),
            )
            ActivityRecognition -> emptyList()
            Overlay -> listOf(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, packageUri))
            // The list of apps; a package Uri isn't honoured on every phone.
            UsageAccess -> listOf(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            FullScreen -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                listOf(Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, packageUri))
            } else {
                emptyList()
            }
        }
        return screens + appDetails(packageUri)
    }

    /**
     * Android 13+ locks a few switches, Usage access and Display over other apps among them, for
     * apps installed from a file rather than an app store. Turning one on then shows "Restricted
     * setting" until the user unlocks it from App info: ⋮ > Allow restricted settings.
     */
    val canBeLocked: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && (this == UsageAccess || this == Overlay)

    companion object {
        fun appDetails(packageUri: android.net.Uri) =
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri)

        fun appDetails(context: Context) = appDetails("package:${context.packageName}".toUri())

        /**
         * True when Android says Dawnly's restricted settings are locked, false when they're open,
         * null when it can't tell. The app op behind "Allow restricted settings" isn't in the
         * public SDK, so this is a hint and the help also shows after a second failed try.
         */
        fun restrictedSettingsLocked(context: Context): Boolean? {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return false
            return try {
                val mode = context.getSystemService(AppOpsManager::class.java).unsafeCheckOpNoThrow(
                    "android:access_restricted_settings",
                    Process.myUid(),
                    context.packageName,
                )
                mode == AppOpsManager.MODE_ERRORED || mode == AppOpsManager.MODE_IGNORED
            } catch (e: RuntimeException) {
                null
            }
        }
    }
}
