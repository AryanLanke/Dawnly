package com.dawnly.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.dawnly.app.DawnlyApp
import com.dawnly.app.data.settings.Appearance
import com.dawnly.app.data.settings.DismissMode
import com.dawnly.app.data.sound.AlertSound
import com.dawnly.app.data.sound.SoundStore
import com.dawnly.app.domain.wake.WakeSignalSettings
import com.dawnly.app.notify.AwakeNotifier
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
    val sound: AlertSound,
    val dismissMode: DismissMode = DismissMode.Ask,
)

class SettingsViewModel(private val app: DawnlyApp) : ViewModel() {
    private val sounds = SoundStore(app)

    /** Null until the settings have loaded. */
    val uiState: StateFlow<SettingsUiState?> = combine(
        app.settings.watchWindow,
        app.settings.wakeSignals,
        app.settings.appearance,
        sounds.selected,
        app.settings.dismissMode,
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

    /** The automatic choices only once "Display over other apps" is allowed; the screen asks for it first. */
    fun setDismissMode(mode: DismissMode) {
        app.scope.launch(Dispatchers.Main.immediate) { app.settings.setDismissMode(mode) }
    }

    fun setSound(sound: AlertSound) {
        app.scope.launch(Dispatchers.Main.immediate) {
            sounds.setSelected(sound)
            AwakeNotifier.useSound(app, sound)
        }
    }

    companion object {
        fun factory(app: DawnlyApp) = viewModelFactory { initializer { SettingsViewModel(app) } }
    }
}
