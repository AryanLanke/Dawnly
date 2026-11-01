package com.dawnly.app.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.dawnly.app.dawnly

/**
 * Debug builds only: stands in for wake detection, so the prompt can be tested from adb.
 *
 * - No extras: calls [AwakeNotifier.onUserAwake] for the next alarm, as `WatchService` will.
 * - `--ez stale true`: posts a prompt about an alarm one minute before the real one, skipping the
 *   up-front check. Tapping "Dismiss alarm" must then refuse and leave the real alarm alone.
 */
class DebugAwakeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val next = context.dawnly.nextAlarms.current()?.triggerAt
        if (next == null) {
            Log.w(TAG, "No alarm set, so there's nothing to ask about")
            return
        }
        if (intent.getBooleanExtra(EXTRA_STALE, false)) {
            Log.i(TAG, "Posting a stale prompt; the real alarm is at $next")
            AwakeNotifier.post(context, next.minusSeconds(60))
        } else {
            AwakeNotifier.onUserAwake(context, next)
        }
    }

    private companion object {
        const val TAG = "DebugAwakeReceiver"
        const val EXTRA_STALE = "stale"
    }
}
