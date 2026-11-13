package com.dawnly.app.domain.readiness

/** A thing that can stop Dawnly ringing reliably, in the order Settings shows them. */
enum class ReadinessItem { ExactAlarms, Battery, Notifications, AlarmVolume }

/** What the phone reports right now for each readiness check. */
data class ReadinessInput(
    val exactAlarms: Boolean,
    val battery: Boolean,
    val notifications: Boolean,
    val alarmAudible: Boolean,
    val fullScreen: Boolean,
)

/**
 * Whether Dawnly can ring on time, and what still needs the user. Full-screen takeover is a nicety,
 * not a requirement: without it the alarm still rings as a notification, so it never blocks "ready".
 */
data class ReadinessResult(
    val ready: Boolean,
    val needsAttention: List<ReadinessItem>,
    val fullScreenOff: Boolean,
)

object AlarmReadiness {
    fun evaluate(input: ReadinessInput): ReadinessResult {
        val needs = buildList {
            if (!input.exactAlarms) add(ReadinessItem.ExactAlarms)
            if (!input.battery) add(ReadinessItem.Battery)
            if (!input.notifications) add(ReadinessItem.Notifications)
            if (!input.alarmAudible) add(ReadinessItem.AlarmVolume)
        }
        return ReadinessResult(ready = needs.isEmpty(), needsAttention = needs, fullScreenOff = !input.fullScreen)
    }
}