package com.dawnly.app.notify

import java.time.Instant

/** Whether a dismiss request would hit the alarm the prompt is about. */
enum class DismissCheck {
    /** The prompt's alarm is still the next one, and still ahead. */
    Ok,

    /** The prompt's alarm time has come; it's ringing, snoozed, or done. */
    AlarmPassed,

    /** No alarm is set any more, so the prompt's alarm is already off. */
    AlarmGone,

    /** Another alarm is next now, e.g. the prompt's alarm was dismissed and tomorrow's is next. */
    AlarmChanged,
}

/**
 * Guards against dismissing the wrong alarm. `ACTION_DISMISS_ALARM` with search mode "next"
 * dismisses whatever alarm is next *when the request arrives*, so a stale prompt about today's
 * alarm would skip tomorrow's. Only [DismissCheck.Ok] may send the request.
 */
object DismissGuard {
    fun check(promptAlarmAt: Instant?, nextAlarmAt: Instant?, now: Instant): DismissCheck = when {
        promptAlarmAt == null -> DismissCheck.AlarmChanged
        !now.isBefore(promptAlarmAt) -> DismissCheck.AlarmPassed
        nextAlarmAt == null -> DismissCheck.AlarmGone
        nextAlarmAt != promptAlarmAt -> DismissCheck.AlarmChanged
        else -> DismissCheck.Ok
    }
}
