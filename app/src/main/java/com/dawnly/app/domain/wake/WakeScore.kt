package com.dawnly.app.domain.wake

import com.dawnly.app.domain.learn.SignShares
import com.dawnly.app.domain.learn.SignWeights

/**
 * How close the user is to counting as awake, sign by sign. Each progress value runs from 0 to 1
 * (1 = that sign's goal reached), and is null when the sign doesn't count: switched off in
 * settings, or not available on this phone (steps, apps opened, movement).
 *
 * The progress of the counted signs adds up, and the user counts as awake once it reaches 1 and
 * [needed] different kinds of sign have shown (phone use, steps, movement, music, the charger), at
 * least one of them phone use, steps or movement. So 5 minutes on the phone alone isn't enough by
 * default, but 3 minutes on it and a walk to the kitchen is. A quick time check adds only a sliver.
 *
 * Music and the charger never count alone. Sleep music is left out before it gets here
 * ([WakeTracker]), so only music started during the window counts.
 *
 * The activity also has to be still going and have lasted a few minutes ([keptGoing], from
 * [WakeSignals.keptGoing]), so one burst that stopped doesn't count.
 */
data class WakeScore(
    val screenTime: Float?,
    val phoneUse: Float?,
    val steps: Float?,
    val movement: Float?,
    val music: Float?,
    val charger: Float? = null,
    val keptGoing: Boolean = true,
    /** How many different kinds of sign have to show, out of those that can count. */
    val needed: Int = 1,
) {
    /** The sum of the counted signs' progress. 1 or more means awake (with enough kinds). */
    val total: Float get() = listOfNotNull(screenTime, phoneUse, steps, movement, music, charger).sum()

    /** Each sign's part, for learning which signs show on the user's real mornings. */
    val shares: SignShares get() = SignShares(screenTime, phoneUse, steps, movement)

    /** How close to awake, from 0 to 1. */
    val value: Float get() = total.coerceIn(0f, 1f)

    /** Screen time and apps opened are one kind: both are using the phone. */
    private val phone: Float?
        get() = if (screenTime == null && phoneUse == null) null else maxOf(screenTime ?: 0f, phoneUse ?: 0f)

    private val mainKinds: List<Float?> get() = listOf(phone, steps, movement)

    private val allKinds: List<Float?> get() = mainKinds + listOf(music, charger)

    /** How many kinds of sign have shown clearly (at least [SHOWN] of the way to their goal). */
    val kindsShown: Int get() = allKinds.count { it.shown() }

    /** The kinds that have to show: [needed], but never more than can count here. */
    val kindsNeeded: Int get() = needed.coerceIn(1, allKinds.count { it != null }.coerceAtLeast(1))

    /** Never true when no sign counts: then Dawnly has nothing to go on. */
    val isAwake: Boolean
        get() = keptGoing &&
            total >= 1f - EPSILON &&
            mainKinds.any { it.shown() } &&
            kindsShown >= kindsNeeded

    private fun Float?.shown() = this != null && this >= SHOWN - EPSILON

    companion object {
        /** So 0.6 + 0.4 counts as 1 despite float rounding. */
        private const val EPSILON = 1e-4f

        /** What a charger change adds: a strong sign, but not enough alone. */
        const val CHARGER_SHARE = 0.4f

        /** A sign has shown once it's this far to its goal: a glance at the clock isn't one. */
        const val SHOWN = 0.3f

        /** How long music has to play to count fully. */
        private const val MUSIC_GOAL_MILLIS = 60_000L

        /**
         * [weights] is how much each sign is trusted, learned from the user's answers
         * (`Learning`); evenly by default. A trusted sign's progress counts for a little more.
         */
        fun of(signals: WakeSignals, settings: WakeSignalSettings, weights: SignWeights = SignWeights.Even): WakeScore = WakeScore(
            screenTime = settings.useScreenTime.then {
                weights.screen * progress(signals.screenOnTime.toMillis(), settings.screenTimeGoal.toMillis())
            },
            phoneUse = (settings.usePhoneUse && signals.appsAvailable).then {
                weights.apps * progress(signals.appsOpened.toLong(), settings.appsOpenedGoal.toLong())
            },
            steps = (settings.useSteps && signals.stepsAvailable).then {
                weights.steps * progress(signals.steps.toLong(), settings.stepsGoal.toLong())
            },
            movement = (settings.useMovement && signals.movementAvailable).then {
                weights.movement * progress(signals.movingTime.toMillis(), settings.movementGoal.toMillis())
            },
            music = settings.useMusic.then { progress(signals.musicTime.toMillis(), MUSIC_GOAL_MILLIS) },
            charger = settings.useCharger.then { if (signals.chargerChanged) CHARGER_SHARE else 0f },
            keptGoing = signals.keptGoing,
            needed = settings.signsNeeded,
        )

        private inline fun Boolean.then(value: () -> Float): Float? = if (this) value() else null

        private fun progress(value: Long, goal: Long): Float =
            if (goal <= 0) 1f else (value.toFloat() / goal).coerceIn(0f, 1f)
    }
}
