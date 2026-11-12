package com.dawnly.app.notify

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.annotation.StringRes
import com.dawnly.app.R
import com.dawnly.app.dawnly
import com.dawnly.app.data.alarm.DismissResult
import java.time.Instant

/** Who decided to dismiss: what follows depends on it (learning, "Still up?", the notice). */
enum class DismissBy {
    User,

    /** Dawnly saw the user up and dismissed by itself: the "Still up?" check may follow. */
    Dawnly,

    /** Music started instead of the alarm ([WakeMusic]): not a sign the user is up. */
    Music,
}

/**
 * Opened by the prompt's "Dismiss alarm" button, and by [AutoDismissReceiver]. No UI of its own:
 * it checks the next alarm is still the one the prompt was about ([DismissGuard]), then asks the
 * Clock app to dismiss it ([QuietDismiss]), or opens the Clock app if that can't be done. Being an
 * activity, it may start the Clock app, which a receiver launched from a notification may not
 * (Android 12+).
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
        val isRetry = intent.getBooleanExtra(AwakeNotifier.EXTRA_RETRY, false)
        val by = DismissBy.entries.firstOrNull { it.name == intent.getStringExtra(EXTRA_BY) } ?: DismissBy.User
        val next = dawnly.nextAlarms.current()
        val check = DismissGuard.check(promptAlarmAt, next?.triggerAt, Instant.now())
        Log.i(TAG, "Prompt for $promptAlarmAt, next alarm ${next?.triggerAt}: $check")
        when (check) {
            DismissCheck.Ok -> when (val result = QuietDismiss.dismiss(this, next, backToDawnly = false, isRetry = isRetry, by = by)) {
                // The Clock app dismisses it, then the user goes back to the app they were in.
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

    companion object {
        private const val TAG = "DismissAlarmActivity"

        private const val EXTRA_BY = "com.dawnly.app.extra.DISMISS_BY"

        /** [retry] for the second try after the Clock app left the alarm on ([DismissRetry]). */
        fun intent(context: Context, alarmAt: Instant, retry: Boolean = false, by: DismissBy = DismissBy.User): Intent =
            Intent(context, DismissAlarmActivity::class.java)
                .putExtra(AwakeNotifier.EXTRA_ALARM_AT, alarmAt.toEpochMilli())
                .putExtra(AwakeNotifier.EXTRA_RETRY, retry)
                .putExtra(EXTRA_BY, by.name)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
