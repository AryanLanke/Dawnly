package com.dawnly.app.ui.home

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.dawnly.app.DawnlyApp
import com.dawnly.app.data.alarm.DismissResult
import com.dawnly.app.notify.QuietDismiss
import com.dawnly.app.service.WatchSession
import java.time.Instant
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
    ) { state, live, settings, now ->
        HomeUiState.from(state, live, settings, now)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState.Loading)

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
