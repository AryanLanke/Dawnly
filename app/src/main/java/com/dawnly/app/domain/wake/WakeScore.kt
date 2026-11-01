package com.dawnly.app.domain.wake

/**
 * How close the user is to counting as awake, signal by signal. Each progress value runs from 0
 * to 1, and is null when that signal doesn't count: switched off in settings, or (for steps) not
 * available on this phone.
 *
 * The user counts as awake once **every** counted signal has reached its goal. With the default
 * settings that means unlocking the phone and using it for 5 minutes, so a quick time check in the
 * night doesn't count.
 */
data class WakeScore(
    val unlock: Float?,
    val screenTime: Float?,
    val steps: Float?,
) {
    private val counted: List<Float> get() = listOfNotNull(unlock, screenTime, steps)

    /** The average progress of the counted signals, from 0 to 1. 0 when nothing counts. */
    val value: Float get() = counted.takeIf { it.isNotEmpty() }?.average()?.toFloat() ?: 0f

    /** Never true when no signal counts: then Dawnly has nothing to go on. */
    val isAwake: Boolean get() = counted.isNotEmpty() && counted.all { it >= 1f }

    companion object {
        fun of(signals: WakeSignals, settings: WakeSignalSettings): WakeScore = WakeScore(
            unlock = if (settings.useUnlock) (if (signals.unlocked) 1f else 0f) else null,
            screenTime = if (settings.useScreenTime) {
                progress(signals.screenOnTime.toMillis(), settings.screenTimeGoal.toMillis())
            } else {
                null
            },
            steps = if (settings.useSteps && signals.stepsAvailable) {
                progress(signals.steps.toLong(), settings.stepsGoal.toLong())
            } else {
                null
            },
        )

        private fun progress(value: Long, goal: Long): Float =
            if (goal <= 0) 1f else (value.toFloat() / goal).coerceIn(0f, 1f)
    }
}
