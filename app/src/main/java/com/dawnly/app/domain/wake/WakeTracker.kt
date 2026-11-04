package com.dawnly.app.domain.wake

import java.time.Duration
import kotlin.math.abs
import kotlin.math.sqrt

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
 * It also follows stretches of activity (screen in use, steps, movement, unplugging), so the score
 * can tell activity that keeps going from one burst that stopped ([WakeSignals.keptGoing]).
 *
 * Not thread-safe; call it from one thread.
 *
 * @param resumeFrom readings saved before the process was killed, to carry on from. Everything
 *   adds on top of them; an open session, step baseline or stretch of activity isn't carried over.
 * @param ignoredApps apps whose opening doesn't count: launchers, the system UI, and Dawnly.
 */
class WakeTracker(
    resumeFrom: WakeSignals = WakeSignals(),
    private val ignoredApps: Set<String> = emptySet(),
) {
    private var closedScreenMillis = resumeFrom.screenOnTime.toMillis()
    private var inUseSince: Long? = null

    private var appsOpened = resumeFrom.appsOpened
    private var lastApp: String? = null

    private var closedSteps = resumeFrom.steps
    private var stepBaseline: Float? = null
    private var stepTotal: Float? = null

    private var movingSeconds = resumeFrom.movingTime.seconds
    private var gravity: Float? = null
    private var lastMovingSecond: Long? = null

    private var musicMillis = resumeFrom.musicTime.toMillis()
    private var lastMusicCheck: Long? = null

    private var unplugged = resumeFrom.unplugged

    private var stretchStart: Long? = null
    private var lastActive: Long? = null

    /** Whether apps opened can be counted (Usage access). Set by whoever reads usage events. */
    var appsAvailable: Boolean = resumeFrom.appsAvailable

    /** Whether steps can be counted right now. Set by whoever owns the step sensor. */
    var stepsAvailable: Boolean = resumeFrom.stepsAvailable

    /** Whether there's an accelerometer. */
    var movementAvailable: Boolean = resumeFrom.movementAvailable

    /** The phone is in use: unlocked, or the screen came on with no lock screen in the way. */
    fun onUnlocked(now: Long) {
        if (inUseSince != null) return
        markActive(now)
        inUseSince = now
    }

    fun onScreenOff(now: Long) {
        val since = inUseSince ?: return
        closedScreenMillis += (now - since).coerceAtLeast(0)
        inUseSince = null
        // In use the whole time, so the stretch ran on until now without a gap.
        lastActive = maxOf(lastActive ?: now, now)
    }

    /** The charger was unplugged. */
    fun onUnplugged(now: Long) {
        unplugged = true
        markActive(now)
    }

    /**
     * An app came to the front. Counts when it's a different app from the last one, so moving
     * between screens of one app doesn't count, but going back to it from the home screen does.
     */
    fun onAppResumed(packageName: String) {
        if (packageName == lastApp) return
        lastApp = packageName
        if (packageName !in ignoredApps) appsOpened++
    }

    /**
     * A reading from the step counter sensor: total steps since the phone booted. [now] is when it
     * arrived, for the stretch of activity; null leaves the stretch alone.
     */
    fun onStepCounter(totalSinceBoot: Float, now: Long? = null) {
        val baseline = stepBaseline
        // The counter resets on reboot; start a new baseline rather than go negative.
        if (baseline == null || totalSinceBoot < baseline) {
            foldSteps()
            stepBaseline = totalSinceBoot
        } else if (now != null && totalSinceBoot > (stepTotal ?: baseline)) {
            markActive(now)
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

    /**
     * An accelerometer sample, in m/s², with the sensor's timestamp in nanoseconds. Gravity is
     * tracked as a slow average of the acceleration's size; a sample that strays from it by more
     * than [MOVING_THRESHOLD] is movement. A phone lying on a table or a pillow stays under it;
     * being picked up or carried goes over. Each second with movement counts once.
     *
     * [now] is when the sample arrived, for the stretch of activity (the sensor's own clock isn't
     * the same on every phone); null leaves the stretch alone.
     */
    fun onAcceleration(x: Float, y: Float, z: Float, timestampNanos: Long, now: Long? = null) {
        val size = sqrt(x * x + y * y + z * z)
        val g = gravity ?: size
        gravity = g + GRAVITY_SMOOTHING * (size - g)
        if (abs(size - g) <= MOVING_THRESHOLD) return
        val second = timestampNanos / 1_000_000_000L
        if (second != lastMovingSecond) {
            lastMovingSecond = second
            movingSeconds++
            if (now != null) markActive(now)
        }
    }

    /** Whether music is playing, checked every few seconds. The time since the last check counts. */
    fun onMusicCheck(playing: Boolean, now: Long) {
        val last = lastMusicCheck
        lastMusicCheck = now
        if (playing && last != null) musicMillis += (now - last).coerceIn(0, MAX_MUSIC_GAP_MILLIS)
    }

    fun signals(now: Long): WakeSignals {
        val openMillis = inUseSince?.let { (now - it).coerceAtLeast(0) } ?: 0
        return WakeSignals(
            screenOnTime = Duration.ofMillis(closedScreenMillis + openMillis),
            appsOpened = appsOpened,
            appsAvailable = appsAvailable,
            steps = closedSteps + currentSteps(),
            stepsAvailable = stepsAvailable,
            movingTime = Duration.ofSeconds(movingSeconds),
            movementAvailable = movementAvailable,
            musicTime = Duration.ofMillis(musicMillis),
            unplugged = unplugged,
            stretch = Duration.ofMillis(stretchMillis(now)),
            idle = latestActive(now)?.let { Duration.ofMillis((now - it).coerceAtLeast(0)) },
        )
    }

    /** While the phone is in use, activity runs right up to [now]. */
    private fun latestActive(now: Long): Long? = if (inUseSince != null) now else lastActive

    private fun stretchMillis(now: Long): Long {
        val start = stretchStart ?: return 0
        val latest = latestActive(now) ?: return 0
        return (latest - start).coerceAtLeast(0)
    }

    /** Activity at [at]: carries the stretch on, or starts a new one after a quiet gap. */
    private fun markActive(at: Long) {
        val latest = latestActive(at)
        if (latest == null || at - latest >= WakeSignals.QUIET_GAP.toMillis()) stretchStart = at
        if (latest == null || at > latest) lastActive = at
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

    companion object {
        /** m/s² away from gravity. Hand tremor and a breathing sleeper stay well below it. */
        const val MOVING_THRESHOLD = 0.6f

        /** How fast the gravity estimate follows; at 5 samples a second, about 2 seconds. */
        private const val GRAVITY_SMOOTHING = 0.1f

        /** A missed check (the process slept) doesn't count as a long stretch of music. */
        private const val MAX_MUSIC_GAP_MILLIS = 15_000L
    }
}
