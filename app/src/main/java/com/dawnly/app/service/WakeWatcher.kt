package com.dawnly.app.service

import android.Manifest
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import androidx.core.content.ContextCompat
import com.dawnly.app.dawnly
import com.dawnly.app.data.wake.WakeProgress
import com.dawnly.app.data.wake.WakeProgressStore
import com.dawnly.app.domain.wake.WakeScore
import com.dawnly.app.domain.wake.WakeSignals
import com.dawnly.app.domain.wake.WakeTracker
import com.dawnly.app.notify.AwakeNotifier
import java.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext

/** Something that changes the wake signals. Times are `SystemClock.elapsedRealtime()`. */
private sealed interface WakeEvent {
    data class Unlocked(val at: Long) : WakeEvent
    data class ScreenOff(val at: Long) : WakeEvent
    data class Steps(val totalSinceBoot: Float) : WakeEvent
    data object StepsStopped : WakeEvent
    data class StepsAvailable(val available: Boolean) : WakeEvent
    /** Nothing happened, but time passed (screen time grows) or the settings changed. */
    data object Tick : WakeEvent
}

/**
 * Watches for signs the user is awake before the alarm at [alarmAt]: unlocks and screen use from
 * system broadcasts, and steps from the step counter. Feeds them into a [WakeTracker], publishes
 * the readings to [WatchSession], and calls [AwakeNotifier] once the [WakeScore] says the user is
 * up. That happens at most once per window, even if the process is killed and restarted, because
 * the progress is kept in [WakeProgressStore].
 *
 * Run by [WatchService] for as long as the window lasts.
 */
internal class WakeWatcher(context: Context, private val alarmAt: Instant) {
    private val app = context.dawnly
    private val keyguard = app.getSystemService(KeyguardManager::class.java)
    private val power = app.getSystemService(PowerManager::class.java)
    private val sensors = app.getSystemService(SensorManager::class.java)
    private val store = WakeProgressStore(app)

    // The wake-up variant keeps counting while the phone sleeps; not every phone has one.
    private val stepSensor: Sensor? = sensors.getDefaultSensor(Sensor.TYPE_STEP_COUNTER, true)
        ?: sensors.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    /** Runs until cancelled, which is when the watch window ends. Call on the main thread. */
    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun watch() = coroutineScope {
        val saved = store.load(alarmAt)
        val tracker = WakeTracker(resumeFrom = saved?.signals ?: WakeSignals())
        var notified = saved?.notified ?: false
        var lastSaved = saved
        var lastSavedAt = SystemClock.elapsedRealtime()
        if (saved != null) Log.i(TAG, "Carrying on from $saved")

        val settings = app.settings.wakeSignals.stateIn(this)
        val canCountSteps = ticks(STEP_CHECK_PERIOD_MILLIS).map { canCountSteps() }.distinctUntilChanged()
        val countSteps = combine(settings, canCountSteps) { s, can -> s.useSteps && can }.distinctUntilChanged()

        val events = merge(
            screenEvents(),
            canCountSteps.map { WakeEvent.StepsAvailable(it) },
            countSteps.flatMapLatest { counting ->
                val sensor = stepSensor
                if (counting && sensor != null) stepCounter(sensor) else flowOf(WakeEvent.StepsStopped)
            },
            settings.drop(1).map { WakeEvent.Tick },
            ticks(TICK_PERIOD_MILLIS).map { WakeEvent.Tick },
        )

        try {
            events.collect { event ->
                when (event) {
                    is WakeEvent.Unlocked -> tracker.onUnlocked(event.at)
                    is WakeEvent.ScreenOff -> tracker.onScreenOff(event.at)
                    is WakeEvent.Steps -> tracker.onStepCounter(event.totalSinceBoot)
                    WakeEvent.StepsStopped -> tracker.onStepCounterStopped()
                    is WakeEvent.StepsAvailable -> tracker.stepsAvailable = event.available
                    WakeEvent.Tick -> Unit
                }
                val now = SystemClock.elapsedRealtime()
                val signals = tracker.signals(now)
                WatchSession.publish(signals)

                val awakeNow = !notified && WakeScore.of(signals, settings.value).isAwake
                if (awakeNow) notified = true
                val progress = WakeProgress(alarmAt, signals, notified)
                // Saved before asking, so a restart can never ask twice.
                if (awakeNow || (progress != lastSaved && now - lastSavedAt >= SAVE_PERIOD_MILLIS)) {
                    store.save(progress)
                    lastSaved = progress
                    lastSavedAt = now
                }
                if (awakeNow) onAwake(signals)
            }
        } finally {
            withContext(NonCancellable) {
                store.save(WakeProgress(alarmAt, tracker.signals(SystemClock.elapsedRealtime()), notified))
            }
        }
    }

    private fun onAwake(signals: WakeSignals) {
        Log.i(TAG, "Awake before the $alarmAt alarm: $signals")
        AwakeNotifier.onUserAwake(app, alarmAt)
        // Not in v1: a "Still awake?" nudge, for falling back asleep after dismissing, would start
        // here. The service stops once the alarm is dismissed, so WatchScheduler would also need to
        // keep it running for a while; a fresh WakeTracker could then watch for a long screen-off.
    }

    private fun screenEvents(): Flow<WakeEvent> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val now = SystemClock.elapsedRealtime()
                when (intent.action) {
                    Intent.ACTION_USER_PRESENT -> trySend(WakeEvent.Unlocked(now))
                    // No lock screen in the way: none is set, or the screen came back on within the
                    // lock delay. Android doesn't send USER_PRESENT then.
                    Intent.ACTION_SCREEN_ON -> if (!keyguard.isKeyguardLocked) trySend(WakeEvent.Unlocked(now))
                    Intent.ACTION_SCREEN_OFF -> trySend(WakeEvent.ScreenOff(now))
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_USER_PRESENT)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        // USER_PRESENT and the screen broadcasts can't be declared in the manifest. They're system
        // broadcasts, so they still arrive with RECEIVER_NOT_EXPORTED.
        ContextCompat.registerReceiver(app, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        // Already in use when watching started, e.g. up and on the phone when the window opened.
        if (power.isInteractive && !keyguard.isKeyguardLocked) trySend(WakeEvent.Unlocked(SystemClock.elapsedRealtime()))
        awaitClose { app.unregisterReceiver(receiver) }
    }

    private fun stepCounter(sensor: Sensor): Flow<WakeEvent> = callbackFlow {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                trySend(WakeEvent.Steps(event.values[0]))
            }

            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit
        }
        // Batched to save battery: readings may arrive up to STEP_BATCH_MICROS late.
        val registered = sensors.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL, STEP_BATCH_MICROS)
        if (!registered) Log.w(TAG, "Couldn't register the step counter")
        awaitClose { sensors.unregisterListener(listener) }
    }

    /** Needs a step counter, and the activity recognition permission on Android 10+. */
    private fun canCountSteps(): Boolean = stepSensor != null && (
        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            ContextCompat.checkSelfPermission(app, Manifest.permission.ACTIVITY_RECOGNITION) ==
            PackageManager.PERMISSION_GRANTED
        )

    private companion object {
        const val TAG = "WakeWatcher"
        const val TICK_PERIOD_MILLIS = 5_000L

        /** How often to notice that activity recognition was allowed mid-window. */
        const val STEP_CHECK_PERIOD_MILLIS = 15_000L
        const val SAVE_PERIOD_MILLIS = 30_000L
        const val STEP_BATCH_MICROS = 10_000_000

        fun ticks(periodMillis: Long): Flow<Unit> = flow {
            while (true) {
                emit(Unit)
                delay(periodMillis)
            }
        }
    }
}
