package com.dawnly.app.ui.settings

import com.dawnly.app.domain.wake.WakeSignalSettings
import java.time.Duration

/**
 * How much of each sign counts as "awake". One choice instead of a goal picker per sign.
 * Medium matches the defaults in [WakeSignalSettings].
 */
enum class Sensitivity(
    val screenTimeGoal: Duration,
    val appsOpenedGoal: Int,
    val stepsGoal: Int,
    val movementGoal: Duration,
) {
    Low(Duration.ofMinutes(10), 5, 100, Duration.ofMinutes(3)),
    Medium(Duration.ofMinutes(5), 3, 50, Duration.ofMinutes(2)),
    High(Duration.ofMinutes(3), 2, 30, Duration.ofMinutes(1)),
    ;

    fun applyTo(settings: WakeSignalSettings) = settings.copy(
        screenTimeGoal = screenTimeGoal,
        appsOpenedGoal = appsOpenedGoal,
        stepsGoal = stepsGoal,
        movementGoal = movementGoal,
    )

    companion object {
        /** The level matching [settings]' goals, or null if they were set some other way. */
        fun of(settings: WakeSignalSettings): Sensitivity? = entries.firstOrNull { it.applyTo(settings) == settings }
    }
}

/**
 * Turning a sign off is refused when nothing that counts on its own would be left: with no signs,
 * or only music (which only adds to movement), Dawnly could never notice.
 */
fun WakeSignalSettings.withSign(sign: Sign, on: Boolean): WakeSignalSettings? {
    val updated = when (sign) {
        Sign.ScreenTime -> copy(useScreenTime = on)
        Sign.PhoneUse -> copy(usePhoneUse = on)
        Sign.Steps -> copy(useSteps = on)
        Sign.Movement -> copy(useMovement = on)
        Sign.Music -> copy(useMusic = on)
    }
    return updated.takeIf { it.canNotice }
}

/** The signs in Settings order. */
enum class Sign { ScreenTime, PhoneUse, Steps, Movement, Music }
