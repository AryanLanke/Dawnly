package com.dawnly.app.notify

import android.content.Context
import android.util.Log
import java.time.Instant

/**
 * The hand-off from wake detection (Phase 3) to the notification (Phase 4).
 * `WatchService` calls [onUserAwake] at most once per watch window, when the wake score says
 * the user is up.
 */
object AwakeNotifier {
    private const val TAG = "AwakeNotifier"

    fun onUserAwake(context: Context, alarmAt: Instant) {
        // Phase 4 shows "You're up early" here, with Dismiss alarm / Not yet.
        Log.i(TAG, "User is awake before the alarm at $alarmAt")
    }
}
