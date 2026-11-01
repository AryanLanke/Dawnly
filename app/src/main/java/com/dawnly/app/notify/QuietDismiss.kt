package com.dawnly.app.notify

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.text.format.DateFormat
import android.util.Log
import android.widget.Toast
import com.dawnly.app.MainActivity
import com.dawnly.app.R
import com.dawnly.app.dawnly
import com.dawnly.app.data.alarm.ClockApp
import com.dawnly.app.data.alarm.DismissResult
import com.dawnly.app.data.alarm.NextAlarm
import java.time.Instant
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Dismisses through the Clock app with as little Clock app on screen as possible.
 *
 * The request carries `EXTRA_SKIP_UI`, which is documented only for setting alarms; Google Clock
 * opens its alarm list for every dismiss anyway. So once the next alarm moves on (the dismiss
 * worked), Dawnly comes back to the front over the Clock app, which leaves a short flicker. If the
 * alarm doesn't move, the Clock app stays open so the user can see what went wrong.
 */
object QuietDismiss {
    private const val TAG = "QuietDismiss"

    /** How long the Clock app gets to dismiss before we stop waiting and leave it open. */
    private const val WAIT_MS = 5_000L

    /** Google Clock opens its alarm list just before it dismisses; let it land so we end up on top. */
    private const val SETTLE_MS = 400L

    /** [launcher] starts the Clock app, so it must be an activity. */
    fun dismiss(launcher: Activity, alarm: NextAlarm?): DismissResult {
        val result = ClockApp(launcher).dismissNextAlarm(launcher, alarm?.ownerPackage)
        if (result is DismissResult.Requested && alarm != null) {
            returnWhenDismissed(launcher.applicationContext, alarm.triggerAt)
        }
        return result
    }

    private fun returnWhenDismissed(context: Context, alarmAt: Instant) {
        val app = context.dawnly
        app.scope.launch {
            val moved = withTimeoutOrNull(WAIT_MS) {
                app.nextAlarms.nextAlarm.first { it?.triggerAt != alarmAt }
                true
            } ?: false
            if (!moved) {
                Log.i(TAG, "The alarm at $alarmAt didn't move; leaving the Clock app open")
                return@launch
            }
            delay(SETTLE_MS)
            withContext(Dispatchers.Main) {
                try {
                    context.startActivity(
                        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                } catch (e: RuntimeException) {
                    Log.w(TAG, "Couldn't bring Dawnly back", e)
                }
                val time = DateFormat.getTimeFormat(context).format(Date.from(alarmAt))
                Toast.makeText(context, context.getString(R.string.dismiss_done, time), Toast.LENGTH_SHORT).show()
            }
        }
    }
}
