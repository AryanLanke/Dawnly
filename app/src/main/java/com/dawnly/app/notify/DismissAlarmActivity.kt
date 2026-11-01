package com.dawnly.app.notify

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.annotation.StringRes
import com.dawnly.app.R
import com.dawnly.app.dawnly
import com.dawnly.app.data.alarm.ClockApp
import com.dawnly.app.data.alarm.DismissResult
import java.time.Instant

/**
 * Opened by the prompt's "Dismiss alarm" button. No UI of its own: it checks the next alarm is
 * still the one the prompt was about ([DismissGuard]), then asks the Clock app to dismiss it, or
 * opens the Clock app if that can't be done. Being an activity, it may start the Clock app, which
 * a receiver launched from a notification may not (Android 12+).
 */
class DismissAlarmActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AwakeNotifier.cancel(this)
        // Not again after a configuration change; the request has already gone.
        if (savedInstanceState == null) dismissIfStillNext()
        finish()
    }

    private fun dismissIfStillNext() {
        val promptAlarmAt = intent.getLongExtra(AwakeNotifier.EXTRA_ALARM_AT, 0L)
            .takeIf { it > 0L }?.let(Instant::ofEpochMilli)
        val next = dawnly.nextAlarms.current()
        val check = DismissGuard.check(promptAlarmAt, next?.triggerAt, Instant.now())
        Log.i(TAG, "Prompt for $promptAlarmAt, next alarm ${next?.triggerAt}: $check")
        when (check) {
            DismissCheck.Ok -> when (val result = ClockApp(this).dismissNextAlarm(this, next?.ownerPackage)) {
                // The Clock app shows what happened, and the next alarm moves on.
                is DismissResult.Requested -> Unit
                is DismissResult.OpenedClock -> toast(R.string.dismiss_opened_clock, result.clock.label)
                DismissResult.NoClockApp -> toast(R.string.dismiss_no_clock)
            }
            DismissCheck.AlarmPassed -> toast(R.string.dismiss_alarm_passed)
            DismissCheck.AlarmGone -> toast(R.string.dismiss_alarm_gone)
            DismissCheck.AlarmChanged -> toast(R.string.dismiss_alarm_changed)
        }
    }

    private fun toast(@StringRes message: Int, vararg args: Any) {
        Toast.makeText(applicationContext, getString(message, *args), Toast.LENGTH_LONG).show()
    }

    private companion object {
        const val TAG = "DismissAlarmActivity"
    }
}
