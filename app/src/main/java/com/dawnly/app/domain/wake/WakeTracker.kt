package com.dawnly.app.domain.wake

import java.time.Duration

/**
 * Turns raw phone events into [WakeSignals] for one watch window. Pure Kotlin, so it's easy to
 * test; `WakeWatcher` feeds it from Android.
 *
 * Times are milliseconds on a monotonic clock (`SystemClock.elapsedRealtime()`), so changing the
 * phone's clock can't add or remove screen time.
 *
 * Screen time only counts while the phone is unlocked: lighting up the lock screen to check the
 * time doesn't count.
 *
 * Not thread-safe; call it from one thread.
 *
 * @param resumeFrom readings saved before the process was killed, to carry on from. Screen time
 *   and steps add on top of them; an open session or step baseline isn't carried over.
 */
class WakeTracker(resumeFrom: WakeSignals = WakeSignals()) {
    private var unlocked = resumeFrom.unlocked
    private var closedScreenMillis = resumeFrom.screenOnTime.toMillis()
    private var inUseSince: Long? = null

    private var closedSteps = resumeFrom.steps
    private var stepBaseline: Float? = null
    private var stepTotal: Float? = null

    /** Whether steps can be counted right now. Set by whoever owns the step sensor. */
    var stepsAvailable: Boolean = resumeFrom.stepsAvailable

    /** The phone is in use: unlocked, or the screen came on with no lock screen in the way. */
    fun onUnlocked(now: Long) {
        unlocked = true
        if (inUseSince == null) inUseSince = now
    }

    fun onScreenOff(now: Long) {
        val since = inUseSince ?: return
        closedScreenMillis += (now - since).coerceAtLeast(0)
        inUseSince = null
    }

    /** A reading from the step counter sensor: total steps since the phone booted. */
    fun onStepCounter(totalSinceBoot: Float) {
        val baseline = stepBaseline
        // The counter resets on reboot; start a new baseline rather than go negative.
        if (baseline == null || totalSinceBoot < baseline) {
            foldSteps()
            stepBaseline = totalSinceBoot
        }
        stepTotal = totalSinceBoot
    }

    /**
     * The step sensor was unregistered. Keeps the steps so far, and the next reading starts a new
     * baseline, so steps taken while not counting are left out.
     */
    fun onStepCounterStopped() {
        foldSteps()
        stepBaseline = null
        stepTotal = null
    }

    fun signals(now: Long): WakeSignals {
        val openMillis = inUseSince?.let { (now - it).coerceAtLeast(0) } ?: 0
        return WakeSignals(
            unlocked = unlocked,
            screenOnTime = Duration.ofMillis(closedScreenMillis + openMillis),
            steps = closedSteps + currentSteps(),
            stepsAvailable = stepsAvailable,
        )
    }

    private fun currentSteps(): Int {
        val baseline = stepBaseline ?: return 0
        val total = stepTotal ?: return 0
        return (total - baseline).toInt().coerceAtLeast(0)
    }

    private fun foldSteps() {
        closedSteps += currentSteps()
        stepBaseline = stepTotal
    }
}
