package com.dawnly.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.dawnly.app.DawnlyApp
import com.dawnly.app.data.onboarding.OnboardingStore
import com.dawnly.app.data.settings.Appearance
import com.dawnly.app.data.sound.SoundStore
import com.dawnly.app.notify.AwakeNotifier
import com.dawnly.app.ui.settings.Sign
import com.dawnly.app.ui.settings.withSign
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** App-wide UI state: the appearance, and whether onboarding is done. */
class MainViewModel(private val app: DawnlyApp) : ViewModel() {
    private val onboarding = OnboardingStore(app)

    init {
        // Keeps the prompt's channel on the saved sound, e.g. after an update or restored data.
        app.scope.launch { AwakeNotifier.useSound(app, SoundStore(app).selected.first()) }
    }

    /** Null until loaded, so the first frame doesn't flash the wrong theme or screen. */
    val appearance: StateFlow<Appearance?> =
        app.settings.appearance.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val onboardingDone: StateFlow<Boolean?> =
        onboarding.completed.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun completeOnboarding() {
        app.scope.launch(Dispatchers.Main.immediate) { onboarding.setCompleted() }
    }

    /** [sign]'s permission was just allowed during onboarding, so start counting it. */
    fun useSign(sign: Sign) {
        app.scope.launch(Dispatchers.Main.immediate) {
            val signals = app.settings.wakeSignals.first()
            signals.withSign(sign, true)?.let { app.settings.setWakeSignals(it) }
        }
    }

    companion object {
        fun factory(app: DawnlyApp) = viewModelFactory { initializer { MainViewModel(app) } }
    }
}
