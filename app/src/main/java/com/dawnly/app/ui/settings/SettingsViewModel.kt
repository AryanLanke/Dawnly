package com.dawnly.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.dawnly.app.DawnlyApp
import com.dawnly.app.data.settings.Appearance
import com.dawnly.app.domain.wake.WakeSignalSettings
import java.time.Duration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SettingsUiState(
    val window: Duration,
    val signals: WakeSignalSettings,
    val appearance: Appearance,
)

class SettingsViewModel(private val app: DawnlyApp) : ViewModel() {
    /** Null until the settings have loaded. */
    val uiState: StateFlow<SettingsUiState?> = combine(
        app.settings.watchWindow,
        app.settings.wakeSignals,
        app.settings.appearance,
        ::SettingsUiState,
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // Writes run in the app scope so they finish even if the user leaves the screen. They start on
    // the main thread, so quick taps reach DataStore in the order they were made.

    fun setWindow(window: Duration) {
        app.scope.launch(Dispatchers.Main.immediate) {
            app.settings.setWatchWindow(window)
            withContext(Dispatchers.Default) { app.watchScheduler.reconcile() }
        }
    }

    fun setSignals(signals: WakeSignalSettings) {
        app.scope.launch(Dispatchers.Main.immediate) { app.settings.setWakeSignals(signals) }
    }

    fun setAppearance(appearance: Appearance) {
        app.scope.launch(Dispatchers.Main.immediate) { app.settings.setAppearance(appearance) }
    }

    companion object {
        fun factory(app: DawnlyApp) = viewModelFactory { initializer { SettingsViewModel(app) } }
    }
}
