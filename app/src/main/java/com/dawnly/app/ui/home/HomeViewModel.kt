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
import com.dawnly.app.domain.alarm.AlarmTimes
import com.dawnly.app.data.alarm.DismissResult
import com.dawnly.app.domain.learn.Answer
import com.dawnly.app.domain.reminder.Reminder
import com.dawnly.app.domain.reminder.ReminderSchedule
import com.dawnly.app.notify.Notify
import com.dawnly.app.notify.ReminderScheduler
import com.dawnly.app.notify.QuietDismiss
import com.dawnly.app.service.WatchSession
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
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
     * Sets an alarm for [time] on [day], where null is today. Today goes to the Clock app, where
     * the user can keep editing and use Back naturally. Another day can't be asked of the Clock
     * app (it only takes a time), so Dawnly keeps it as a reminder for that one day, which rings
     * like an alarm.
     */
    fun setAlarm(activity: Activity, time: LocalTime, day: LocalDate?) {
        QuietDismiss.cancelPending(activity)
        val today = LocalDate.now()
        if (day != null && day.isBefore(today)) {
            Toast.makeText(activity, R.string.alarm_date_passed, Toast.LENGTH_LONG).show()
            return
        }
        if (day != null && day.isAfter(today)) {
            setForDay(activity, time, day)
            return
        }
        if (AlarmTimes.nextAt(time, LocalDateTime.now()).toLocalDate() != today) {
            Toast.makeText(activity, R.string.alarm_time_passed, Toast.LENGTH_LONG).show()
            return
        }
        val clockApp = ClockApp(activity)
        val set = clockApp.setAlarm(activity, time)
        if (set == null) {
            clockApp.openClock(activity, app.nextAlarms.current()?.ownerPackage)?.let {
                Toast.makeText(activity, activity.getString(R.string.alarm_set_opened_clock, it.label), Toast.LENGTH_LONG).show()
            } ?: Toast.makeText(activity, R.string.alarm_set_no_clock, Toast.LENGTH_LONG).show()
            return
        }
        val at = AlarmTimes.nextAt(time, LocalDateTime.now()).atZone(ZoneId.systemDefault()).toInstant()
        val message = activity.getString(R.string.alarm_set_toast, Notify.time(activity, at), set.label)
        Toast.makeText(activity, message, Toast.LENGTH_LONG).show()
    }

    /** A one-day reminder that rings like an alarm: it's set for [day] and then finishes. */
    private fun setForDay(activity: Activity, time: LocalTime, day: LocalDate) {
        val reminder = Reminder(
            id = System.currentTimeMillis(),
            name = activity.getString(R.string.alarm_tomorrow_name),
            schedule = ReminderSchedule.Daily(time),
            firstDay = day,
            lastDay = day,
            ring = true,
        )
        app.scope.launch {
            app.reminders.save(reminder)
            ReminderScheduler.schedule(activity.applicationContext, reminder)
        }
        val at = day.atTime(time).atZone(ZoneId.systemDefault()).toInstant()
        val date = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(day)
        Toast.makeText(activity, activity.getString(R.string.alarm_set_date_toast, Notify.time(activity, at), date), Toast.LENGTH_LONG).show()
    }

    /** Changing or deleting an alarm happens in the Clock app, with its own controls. */
    fun openClock(activity: Activity) {
        QuietDismiss.cancelPending(activity)
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
