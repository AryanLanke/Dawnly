package com.dawnly.app.ui.settings

import com.dawnly.app.domain.wake.WakeSignalSettings
import java.time.Duration

/**
 * How much screen use and walking count as "awake". One choice instead of two goal pickers.
 * Medium matches the defaults in [WakeSignalSettings].
 */
enum class Sensitivity(val screenTimeGoal: Duration, val stepsGoal: Int) {
    Low(Duration.ofMinutes(10), 100),
    Medium(Duration.ofMinutes(5), 50),
    High(Duration.ofMinutes(3), 30),
    ;

    fun applyTo(settings: WakeSignalSettings) =
        settings.copy(screenTimeGoal = screenTimeGoal, stepsGoal = stepsGoal)

    companion object {
        /** The level matching [settings]' goals, or null if they were set some other way. */
        fun of(settings: WakeSignalSettings): Sensitivity? = entries.firstOrNull {
            it.screenTimeGoal == settings.screenTimeGoal && it.stepsGoal == settings.stepsGoal
        }
    }
}

/** Turning a sign off is refused when it's the last one on: with none, Dawnly could never notice. */
fun WakeSignalSettings.withSign(sign: Sign, on: Boolean): WakeSignalSettings? {
    val updated = when (sign) {
        Sign.Unlock -> copy(useUnlock = on)
        Sign.ScreenTime -> copy(useScreenTime = on)
        Sign.Steps -> copy(useSteps = on)
    }
    return updated.takeIf { it.useUnlock || it.useScreenTime || it.useSteps }
}

enum class Sign { Unlock, ScreenTime, Steps }
