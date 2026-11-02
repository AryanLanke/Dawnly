package com.dawnly.app.ui.glass

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/** The small vibrations Dawnly gives under the finger. They follow the phone's touch-vibration setting. */
class Haptics(private val feedback: HapticFeedback) {
    /** A glass button pressed. */
    fun click() = feedback.performHapticFeedback(HapticFeedbackType.ContextClick)

    /** A choice or a page changing: segments, tabs, swiping between Today and Settings. */
    fun tick() = feedback.performHapticFeedback(HapticFeedbackType.SegmentTick)

    /** A switch flipped. */
    fun toggle(on: Boolean) =
        feedback.performHapticFeedback(if (on) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)

    /** Press and hold. */
    fun longPress() = feedback.performHapticFeedback(HapticFeedbackType.LongPress)
}

@Composable
fun rememberHaptics(): Haptics {
    val feedback = LocalHapticFeedback.current
    return remember(feedback) { Haptics(feedback) }
}
