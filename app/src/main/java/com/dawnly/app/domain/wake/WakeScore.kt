package com.dawnly.app.domain.wake

/**
 * How close the user is to counting as awake, sign by sign. Each progress value runs from 0 to 1
 * (1 = that sign's goal reached), and is null when the sign doesn't count: switched off in
 * settings, or not available on this phone (steps, apps opened, movement).
 *
 * The progress of the counted signs adds up, and the user counts as awake once it reaches 1. So
 * any one sign reaching its goal is enough (5 minutes of screen use, or 50 steps), and so is a mix
 * (3 minutes of screen use and 20 steps). A quick time check in the night adds only a sliver.
 *
 * Music never counts on its own, because people fall asleep to music: it adds at most as much as
 * movement has. Music for a while and half the movement goal makes 1.
 */
data class WakeScore(
    val screenTime: Float?,
    val phoneUse: Float?,
    val steps: Float?,
    val movement: Float?,
    val music: Float?,
) {
    /** What music adds: capped by movement, and nothing without it. */
    val musicShare: Float? get() = music?.let { minOf(it, movement ?: 0f) }

    /** The sum of the counted signs' progress. 1 or more means awake. */
    val total: Float get() = listOfNotNull(screenTime, phoneUse, steps, movement, musicShare).sum()

    /** How close to awake, from 0 to 1. */
    val value: Float get() = total.coerceIn(0f, 1f)

    /** Never true when no sign counts: then Dawnly has nothing to go on. */
    val isAwake: Boolean get() = total >= 1f - EPSILON

    /**
     * Awake only thanks to music: without it the other signs fall short. People fall asleep to
     * music, so then Dawnly always asks, even with Warn first or Right away, and an unanswered
     * question lets the alarm ring.
     */
    val musicTippedIt: Boolean get() = isAwake && total - (musicShare ?: 0f) < 1f - EPSILON

    companion object {
        /** So 0.6 + 0.4 counts as 1 despite float rounding. */
        private const val EPSILON = 1e-4f

        /** How long music has to play to count fully (still capped by movement). */
        private const val MUSIC_GOAL_MILLIS = 60_000L

        fun of(signals: WakeSignals, settings: WakeSignalSettings): WakeScore = WakeScore(
            screenTime = settings.useScreenTime.then {
                progress(signals.screenOnTime.toMillis(), settings.screenTimeGoal.toMillis())
            },
            phoneUse = (settings.usePhoneUse && signals.appsAvailable).then {
                progress(signals.appsOpened.toLong(), settings.appsOpenedGoal.toLong())
            },
            steps = (settings.useSteps && signals.stepsAvailable).then {
                progress(signals.steps.toLong(), settings.stepsGoal.toLong())
            },
            movement = (settings.useMovement && signals.movementAvailable).then {
                progress(signals.movingTime.toMillis(), settings.movementGoal.toMillis())
            },
            music = settings.useMusic.then { progress(signals.musicTime.toMillis(), MUSIC_GOAL_MILLIS) },
        )

        private inline fun Boolean.then(value: () -> Float): Float? = if (this) value() else null

        private fun progress(value: Long, goal: Long): Float =
            if (goal <= 0) 1f else (value.toFloat() / goal).coerceIn(0f, 1f)
    }
}
