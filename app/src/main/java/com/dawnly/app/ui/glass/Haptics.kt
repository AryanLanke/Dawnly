package com.dawnly.app.ui.glass

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * The small vibrations Dawnly gives under the finger, for sliding: a short buzz when a press and
 * hold lets you slide, and a tick as the finger slides past a value. Taps give none. They follow
 * the phone's touch-vibration setting.
 */
class Haptics(private val feedback: HapticFeedback) {
    /** The hold that lets a slide start: a short buzz, so you know you can slide now. */
    fun longPress() = feedback.performHapticFeedback(HapticFeedbackType.LongPress)

    /** A tick as the finger slides past a value. */
    fun tick() = feedback.performHapticFeedback(HapticFeedbackType.SegmentTick)
}

@Composable
fun rememberHaptics(): Haptics {
    val feedback = LocalHapticFeedback.current
    return remember(feedback) { Haptics(feedback) }
}
