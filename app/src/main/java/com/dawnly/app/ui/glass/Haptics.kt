package com.dawnly.app.ui.glass

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * The small vibrations Dawnly gives under the finger. Android's own touch feedback
 * (`performHapticFeedback`) is dropped entirely when the phone's "Touch feedback" setting is off,
 * which it is on the user's phone; the user asked for Dawnly to vibrate anyway (2026-11-03). So
 * this drives the vibrator itself, as media vibration, which that setting doesn't silence. The
 * "Media vibration" strength in system settings still applies.
 */
// The EFFECT_* constants are plain ints, only played on Android 10+; older phones get a short buzz.
@SuppressLint("InlinedApi")
class Haptics(context: Context) {
    private val vibrator: Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        }

    /** A glass button pressed. */
    fun click() = play(VibrationEffect.EFFECT_CLICK, fallbackMillis = 12)

    /** A choice or a page changing: segments, tabs, swiping between Today and Settings. */
    fun tick() = play(VibrationEffect.EFFECT_TICK, fallbackMillis = 8)

    /** A switch flipped. */
    fun toggle(on: Boolean) = if (on) click() else tick()

    /** Press and hold: a firmer buzz that says "now slide". */
    fun longPress() = play(VibrationEffect.EFFECT_HEAVY_CLICK, fallbackMillis = 25)

    private fun play(effect: Int, fallbackMillis: Long) {
        val vibrator = vibrator?.takeIf { it.hasVibrator() } ?: return
        val vibration = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            VibrationEffect.createPredefined(effect)
        } else {
            VibrationEffect.createOneShot(fallbackMillis, VibrationEffect.DEFAULT_AMPLITUDE)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            vibrator.vibrate(vibration, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_MEDIA))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(vibration, MediaVibration)
        }
    }

    private companion object {
        val MediaVibration: AudioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
    }
}

@Composable
fun rememberHaptics(): Haptics {
    val context = LocalContext.current
    return remember(context) { Haptics(context.applicationContext) }
}
