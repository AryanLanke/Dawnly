package com.dawnly.app.system

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process

/** What Dawnly needs to know about the apps on the phone, for the "apps opened" sign and going back. */
object UsageAccess {
    const val SYSTEM_UI = "com.android.systemui"

    /** Usage access is an app op the user grants in system settings, not a runtime permission. */
    fun isGranted(context: Context): Boolean {
        val ops = context.getSystemService(AppOpsManager::class.java)
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        } else {
            @Suppress("DEPRECATION")
            ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /** The home screen apps (launchers). */
    fun launchers(context: Context): Set<String> {
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        return context.packageManager.queryIntentActivities(home, 0).map { it.activityInfo.packageName }.toSet()
    }

    /** Opening these isn't a sign of being up: the home screen, the system UI, and Dawnly itself. */
    fun ignoredApps(context: Context): Set<String> = launchers(context) + SYSTEM_UI + context.packageName
}
