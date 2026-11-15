package com.dawnly.app.ui.home

import com.dawnly.app.domain.learn.Learned
import com.dawnly.app.domain.wake.Closeness
import com.dawnly.app.domain.wake.WakeScore
import com.dawnly.app.domain.wake.WakeSignalSettings
import com.dawnly.app.domain.wake.WakeSignals
import com.dawnly.app.domain.watch.WatchState
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/** Everything the Home screen shows. Built by [HomeUiState.from], a pure function. */
data class HomeUiState(
    val status: HomeStatus,
    /** The signals the user turned on, in settings order. */
    val signals: List<SignalRow> = emptyList(),
) {
    companion object {
        val Loading = HomeUiState(HomeStatus.Loading)

        /**
         * [learned] and how close the alarm is scale the goals, and [learned] weighs the signs,
         * exactly as the watch does, so the "Are you up?" bar fills as the real decision would.
         */
        fun from(
            state: WatchState,
            live: WakeSignals?,
            mySettings: WakeSignalSettings,
            now: Instant,
            learned: Learned = Learned.Nothing,
            notYetAt: Instant? = null,
        ): HomeUiState = withLearning(
            state,
            live,
            mySettings.scaledBy(
                learned.goalFactor(LocalDateTime.ofInstant(now, ZoneId.systemDefault())) *
                    // Only while watching: before the window, Home shows the goals as set.
                    ((state as? WatchState.Watching)?.let { Closeness.goalFactor(Duration.between(now, it.alarmAt)) } ?: 1f),
            ),
            now,
            learned,
            notYetAt,
        )

        private fun withLearning(
            state: WatchState,
            live: WakeSignals?,
            settings: WakeSignalSettings,
            now: Instant,
            learned: Learned,
            notYetAt: Instant?,
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
                val score = WakeScore.of(signals, settings, learned.weights)
                HomeUiState(
                    status = HomeStatus.Watching(
                        alarmAt = state.alarmAt,
                        startedAt = state.startedAt,
                        progress = state.progress(now),
                        wakeProgress = score.value,
                        awake = score.isAwake,
                        notYet = notYetAt != null && !notYetAt.isBefore(state.startedAt),
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
            if (settings.useCharger) {
                // Shown as "Not yet" or "Spotted".
                add(SignalRow(SignalKind.Charger, live?.chargerChanged?.let { if (it) 1 else 0 }, goal = 1))
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
    /** When the alarm on Today rings, or null with none. */
    val ringsAt: Instant?
        get() = when (this) {
            is Waiting -> alarmAt
            is Watching -> alarmAt
            is Skipped -> alarmAt
            NoAlarm, Loading -> null
        }

    /** Before the first reading; the screen shows only the background. */
    data object Loading : HomeStatus

    data object NoAlarm : HomeStatus

    data class Waiting(val alarmAt: Instant, val startsAt: Instant) : HomeStatus

    /**
     * Inside the watch window. [progress] is how much of the window has passed. [wakeProgress] and
     * [awake] come from the same [WakeScore] that sends the prompt, so Home fills its "Signs you're
     * up" bar and says "You're up early" exactly as the prompt goes out. [notYet] once the user
     * answered Not yet in this window: then Home stops cheering and says the alarm will ring.
     */
    data class Watching(
        val alarmAt: Instant,
        val startedAt: Instant,
        val progress: Float,
        val wakeProgress: Float,
        val awake: Boolean,
        val notYet: Boolean = false,
    ) : HomeStatus

    data class Skipped(val alarmAt: Instant) : HomeStatus
}

enum class SignalKind { ScreenTime, PhoneUse, Steps, Movement, Charger, Music }

/**
 * One wake signal on Home. [current] is null before the window opens, when only the goal is shown.
 * Screen time and movement are in whole minutes; the charger and music are 0 or 1 out of 1.
 */
data class SignalRow(val kind: SignalKind, val current: Int?, val goal: Int) {
    val met: Boolean get() = current != null && current >= goal
}
