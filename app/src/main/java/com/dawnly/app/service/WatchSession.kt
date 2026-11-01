package com.dawnly.app.service

import com.dawnly.app.domain.wake.WakeSignals
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * What the running [WatchService] is seeing, for the UI (Home and the debug screen).
 * null when not watching. Only [WatchService] publishes to it.
 */
object WatchSession {
    private val current = MutableStateFlow<WakeSignals?>(null)

    val signals: StateFlow<WakeSignals?> = current.asStateFlow()

    fun publish(signals: WakeSignals?) {
        current.value = signals
    }
}
