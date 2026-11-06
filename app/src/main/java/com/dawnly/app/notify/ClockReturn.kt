package com.dawnly.app.notify

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.util.Log
import com.dawnly.app.MainActivity
import com.dawnly.app.dawnly
import com.dawnly.app.system.AppUsage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * After Dawnly opens the Clock app, brings Dawnly back to the front once the user leaves the Clock
 * app (Back, Home or another app), so they land back in Dawnly. Android doesn't tell an app when
 * another app goes to the background, so this reads the usage events, which needs Usage access.
 * Without that access nothing is watched and Dawnly stays where Android leaves it.
 */
object ClockReturn {
    private const val TAG = "ClockReturn"
    private const val POLL_MS = 700L

    /** Stops watching after this long, so a forgotten Clock app doesn't keep Dawnly busy. */
    private const val MAX_MS = 3 * 60_000L

    fun watch(context: Context, clockPackage: String) {
        if (!AppUsage.isGranted(context)) {
            Log.i(TAG, "Usage access is off, so Dawnly won't come back from $clockPackage by itself")
            return
        }
        val usage = context.getSystemService(UsageStatsManager::class.java)
        context.dawnly.scope.launch {
            var since = System.currentTimeMillis()
            var left = false
            val event = UsageEvents.Event()
            withTimeoutOrNull(MAX_MS) {
                while (!left) {
                    delay(POLL_MS)
                    val now = System.currentTimeMillis()
                    val events = usage.queryEvents(since, now)
                    since = now
                    while (events.getNextEvent(event)) {
                        if (event.packageName != clockPackage) continue
                        // We just opened it, so it's in front; its next move to the background is the user leaving.
                        @Suppress("DEPRECATION") // ACTIVITY_PAUSED has the same value on Android 10+.
                        if (event.eventType == UsageEvents.Event.MOVE_TO_BACKGROUND) left = true
                    }
                }
            }
            if (left) {
                Log.i(TAG, "Left $clockPackage; back to Dawnly")
                withContext(Dispatchers.Main) { backToDawnly(context) }
            }
        }
    }

    private fun backToDawnly(context: Context) {
        try {
            context.startActivity(MainActivity.backToFront(context).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: RuntimeException) {
            // Android may refuse a start from the background; then the user taps Dawnly's icon.
            Log.w(TAG, "Couldn't come back to Dawnly", e)
        }
    }
}
