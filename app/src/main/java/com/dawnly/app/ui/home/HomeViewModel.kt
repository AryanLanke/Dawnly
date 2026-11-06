package com.dawnly.app.ui.home

import android.app.Activity
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.dawnly.app.DawnlyApp
import com.dawnly.app.R
import com.dawnly.app.data.alarm.ClockApp
import com.dawnly.app.data.alarm.DawnlyAlarms
import com.dawnly.app.domain.alarm.AlarmTimes
import com.dawnly.app.notify.AlarmEdit
import com.dawnly.app.data.alarm.DismissResult
import com.dawnly.app.domain.learn.Answer
import com.dawnly.app.domain.reminder.Reminder
import com.dawnly.app.notify.Notify
import com.dawnly.app.notify.ReminderScheduler
import com.dawnly.app.notify.QuietDismiss
import com.dawnly.app.service.WatchSession
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(private val app: DawnlyApp) : ViewModel() {
    val uiState: StateFlow<HomeUiState> = combine(
        app.watchScheduler.state,
        WatchSession.signals,
        app.settings.wakeSignals,
        ticker(),
        // What's learned, and when the user last said Not yet (for "Okay, not yet" on Today).
        combine(app.learning.learned(app.settings), app.learning.mornings) { learned, mornings ->
            learned to mornings.lastOrNull()?.takeIf { it.answer == Answer.NotYet }?.askedAt
        },
    ) { state, live, settings, now, (learned, notYetAt) ->
        HomeUiState.from(state, live, settings, now, learned, notYetAt)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState.Loading)

    /** The user's reminders, shown on Today under the alarm. */
    val reminders: StateFlow<List<Reminder>> =
        app.reminders.reminders.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Adds or updates [reminder] and sets its alarm. */
    fun saveReminder(reminder: Reminder) = changeReminders { app.reminders.save(reminder) }

    fun deleteReminder(id: Long) = changeReminders { app.reminders.delete(id) }

    private fun changeReminders(change: suspend () -> Unit) {
        app.scope.launch {
            change()
            ReminderScheduler.sync(app)
        }
    }

    private val _dismissResult = MutableStateFlow<DismissResult?>(null)

    /** The last dismiss that didn't go straight to the Clock app, to explain what happened. */
    val dismissResult: StateFlow<DismissResult?> = _dismissResult.asStateFlow()

    /**
     * Asks the Clock app to dismiss the next alarm, or opens it if it can't. [activity] starts it.
     * Dawnly comes back over the Clock app once the alarm is dismissed, with the reversed sound.
     */
    fun dismissAlarm(activity: Activity) {
        val result = QuietDismiss.dismiss(activity, app.nextAlarms.current(), backToDawnly = true)
        _dismissResult.value = result.takeUnless { it is DismissResult.Requested }
        app.scope.launch { app.watchScheduler.reconcile() }
    }

    /**
     * Sets an alarm for [time] in the Clock app and says so in a toast. The ring updates by itself
     * once the Clock app has it (the next alarm changes).
     */
    fun setAlarm(activity: Activity, time: LocalTime) {
        val clockApp = ClockApp(activity)
        val set = clockApp.setAlarm(activity, time)
        val message = if (set != null) {
            val at = AlarmTimes.nextAt(time, LocalDateTime.now()).atZone(ZoneId.systemDefault()).toInstant()
            // Remembered, so it can be changed or deleted from Today.
            DawnlyAlarms(activity).remember(at)
            activity.getString(R.string.alarm_set_toast, Notify.time(activity, at), set.label)
        } else {
            clockApp.openClock(activity, null)?.let { activity.getString(R.string.alarm_set_opened_clock, it.label) }
                ?: activity.getString(R.string.alarm_set_no_clock)
        }
        Toast.makeText(activity, message, Toast.LENGTH_LONG).show()
    }

    /**
     * Changes the alarm at [alarmAt] (set from Dawnly) to [newTime], or deletes it when [newTime] is
     * null: it's switched off in the Clock app, then the new one is set ([AlarmEdit]).
     */
    fun editAlarm(activity: Activity, alarmAt: Instant, newTime: LocalTime?) {
        val next = app.nextAlarms.current()
        if (next == null || next.triggerAt != alarmAt) {
            Toast.makeText(activity, R.string.alarm_edit_moved, Toast.LENGTH_LONG).show()
            return
        }
        if (newTime != null && newTime == alarmAt.atZone(ZoneId.systemDefault()).toLocalTime().withSecond(0).withNano(0)) return
        val message = when (val result = AlarmEdit.remove(activity, next, newTime)) {
            is DismissResult.Requested -> null
            is DismissResult.OpenedClock -> activity.getString(R.string.alarm_edit_in_clock, result.clock.label)
            DismissResult.NoClockApp -> activity.getString(R.string.alarm_set_no_clock)
        }
        message?.let { Toast.makeText(activity, it, Toast.LENGTH_LONG).show() }
    }

    /** True if the alarm at [at] was set from Dawnly, so Today can change it. */
    fun isDawnlyAlarm(at: Instant): Boolean = DawnlyAlarms(app).isOurs(at)

    /** An alarm made in the Clock app: changed there, since it may repeat. */
    fun openClock(activity: Activity) {
        ClockApp(activity).openClock(activity, app.nextAlarms.current()?.ownerPackage)
    }

    fun clearDismissResult() {
        _dismissResult.value = null
    }

    companion object {
        fun factory(app: DawnlyApp) = viewModelFactory { initializer { HomeViewModel(app) } }

        /** Moves the ring along between the scheduler's own updates. */
        private fun ticker() = flow {
            while (true) {
                emit(Instant.now())
                delay(5_000)
            }
        }
    }
}
