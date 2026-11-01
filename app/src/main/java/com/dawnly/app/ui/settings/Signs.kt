package com.dawnly.app.ui.settings

import com.dawnly.app.domain.wake.WakeSignalSettings

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
