package com.dawnly.app.service

import android.os.SystemClock

/** Something that changes the wake signals. Times are `SystemClock.elapsedRealtime()`. */
internal sealed interface WakeEvent {
    data class Unlocked(val at: Long) : WakeEvent
    data class ScreenOff(val at: Long) : WakeEvent
    data class ChargerChanged(val at: Long) : WakeEvent
    data class Steps(val totalSinceBoot: Float, val at: Long) : WakeEvent
    data object StepsStopped : WakeEvent
    data class StepsAvailable(val available: Boolean) : WakeEvent
    data class AppResumed(val packageName: String) : WakeEvent
    data class AppsAvailable(val available: Boolean) : WakeEvent
    class Acceleration(val x: Float, val y: Float, val z: Float, val timestampNanos: Long, val at: Long) : WakeEvent
    /** Nothing happened, but time passed (screen time grows) or the settings changed. */
    data object Tick : WakeEvent
}
