package com.dawnly.app.ui.home

import com.dawnly.app.domain.wake.WakeScore
import com.dawnly.app.domain.wake.WakeSignalSettings
import com.dawnly.app.domain.wake.WakeSignals
import com.dawnly.app.domain.watch.WatchState
import java.time.Instant

/** Everything the Home screen shows. Built by [HomeUiState.from], a pure function. */
data class HomeUiState(
    val status: HomeStatus,
    /** The signals the user turned on, in settings order. */
    val signals: List<SignalRow> = emptyList(),
) {
    companion object {
        val Loading = HomeUiState(HomeStatus.Loading)

        fun from(
            state: WatchState,
            live: WakeSignals?,
            settings: WakeSignalSettings,
            now: Instant,
        ): HomeUiState = when (state) {
            WatchState.Idle -> HomeUiState(HomeStatus.NoAlarm)
            is WatchState.Skipped -> HomeUiState(HomeStatus.Skipped(state.alarmAt))
            is WatchState.Waiting -> HomeUiState(
                status = HomeStatus.Waiting(state.alarmAt, state.startsAt),
                signals = signalRows(settings, live = null),
            )
            is WatchState.Watching -> {
                // The service publishes as soon as it starts; until then nothing has been seen yet.
                val signals = live ?: WakeSignals()
                HomeUiState(
                    status = HomeStatus.Watching(
                        alarmAt = state.alarmAt,
                        startedAt = state.startedAt,
                        progress = state.progress(now),
                        awake = WakeScore.of(signals, settings).isAwake,
                    ),
                    signals = signalRows(settings, signals),
                )
            }
        }

        private fun signalRows(settings: WakeSignalSettings, live: WakeSignals?): List<SignalRow> = buildList {
            if (settings.useScreenTime) {
                val minutes = live?.screenOnTime?.toMinutes()?.toInt()
                add(SignalRow(SignalKind.ScreenTime, minutes, settings.screenTimeGoal.toMinutes().toInt()))
            }
            if (settings.usePhoneUse) {
                add(SignalRow(SignalKind.PhoneUse, live?.appsOpened, settings.appsOpenedGoal))
            }
            if (settings.useSteps) {
                add(SignalRow(SignalKind.Steps, live?.steps, settings.stepsGoal))
            }
            if (settings.useMovement) {
                val minutes = live?.movingTime?.toMinutes()?.toInt()
                add(SignalRow(SignalKind.Movement, minutes, settings.movementGoal.toMinutes().toInt()))
            }
            if (settings.useMusic) {
                // Shown as "Not yet" or "Seen": playing for a minute counts.
                val played = live?.musicTime?.let { if (it.toMinutes() >= 1) 1 else 0 }
                add(SignalRow(SignalKind.Music, played, goal = 1))
            }
        }
    }
}

sealed interface HomeStatus {
    /** Before the first reading; the screen shows only the background. */
    data object Loading : HomeStatus

    data object NoAlarm : HomeStatus

    data class Waiting(val alarmAt: Instant, val startsAt: Instant) : HomeStatus

    /**
     * Inside the watch window. [awake] comes from the same [WakeScore] that sends the prompt, so
     * Home says "You're up early" exactly when the prompt goes out.
     */
    data class Watching(
        val alarmAt: Instant,
        val startedAt: Instant,
        val progress: Float,
        val awake: Boolean,
    ) : HomeStatus

    data class Skipped(val alarmAt: Instant) : HomeStatus
}

enum class SignalKind { ScreenTime, PhoneUse, Steps, Movement, Music }

/**
 * One wake signal on Home. [current] is null before the window opens, when only the goal is shown.
 * Screen time and movement are in whole minutes; music is 0 or 1 out of 1.
 */
data class SignalRow(val kind: SignalKind, val current: Int?, val goal: Int) {
    val met: Boolean get() = current != null && current >= goal
}
