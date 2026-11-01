package com.dawnly.app.ui.home

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
                val rows = signalRows(settings, live ?: WakeSignals())
                HomeUiState(
                    status = HomeStatus.Watching(
                        alarmAt = state.alarmAt,
                        startedAt = state.startedAt,
                        progress = state.progress(now),
                        allSignsSeen = rows.isNotEmpty() && rows.all { it.met },
                    ),
                    signals = rows,
                )
            }
        }

        private fun signalRows(settings: WakeSignalSettings, live: WakeSignals?): List<SignalRow> = buildList {
            if (settings.useUnlock) {
                val seen = live?.unlocked
                add(SignalRow(SignalKind.Unlock, current = seen?.let { if (it) 1 else 0 }, goal = 1))
            }
            if (settings.useScreenTime) {
                add(
                    SignalRow(
                        SignalKind.ScreenTime,
                        current = live?.screenOnTime?.toMinutes()?.toInt(),
                        goal = settings.screenTimeGoal.toMinutes().toInt(),
                    ),
                )
            }
            if (settings.useSteps) {
                add(SignalRow(SignalKind.Steps, current = live?.steps, goal = settings.stepsGoal))
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
     * Inside the watch window. [allSignsSeen] is a display hint only ("You're up early"); whether
     * to send the prompt is decided by the wake scoring in `domain/wake`.
     */
    data class Watching(
        val alarmAt: Instant,
        val startedAt: Instant,
        val progress: Float,
        val allSignsSeen: Boolean,
    ) : HomeStatus

    data class Skipped(val alarmAt: Instant) : HomeStatus
}

enum class SignalKind { Unlock, ScreenTime, Steps }

/**
 * One wake signal on Home. [current] is null before the window opens, when only the goal is shown.
 * Screen time is in whole minutes; unlock is 0 or 1 out of 1.
 */
data class SignalRow(val kind: SignalKind, val current: Int?, val goal: Int) {
    val met: Boolean get() = current != null && current >= goal
}
