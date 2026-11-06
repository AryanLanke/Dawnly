package com.dawnly.app.service

import android.Manifest
import android.app.KeyguardManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioManager
import android.os.Build
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import androidx.core.content.ContextCompat
import com.dawnly.app.data.wake.WakeProgress
import com.dawnly.app.data.wake.WakeProgressStore
import com.dawnly.app.dawnly
import com.dawnly.app.domain.wake.Closeness
import com.dawnly.app.domain.wake.WakeScore
import com.dawnly.app.domain.wake.WakeSignals
import com.dawnly.app.domain.wake.WakeTracker
import com.dawnly.app.notify.AwakeNotifier
import com.dawnly.app.system.UsageAccess
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import kotlinx.coroutines.Dispatchers
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
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext

/**
 * Watches for signs the user is awake before the alarm at [alarmAt]: screen use from system
 * broadcasts, apps opened from Usage access, steps from the step counter, movement from the
 * accelerometer, the charger being unplugged, and whether music is playing. Feeds them into a [WakeTracker], publishes the
 * readings to [WatchSession], and calls [AwakeNotifier] once the [WakeScore] says the user is up.
 * That happens at most once per window, even if the process is killed and restarted, because the
 * progress is kept in [WakeProgressStore].
 *
 * Unlocking alone isn't a sign (the phone can be unlocked by someone asleep, e.g. by face or
 * fingerprint in bed); it only starts the screen-use clock.
 *
 * Run by [WatchService] for as long as the window lasts.
 */
internal class WakeWatcher(context: Context, private val alarmAt: Instant) {
    private val app = context.dawnly
    private val keyguard = app.getSystemService(KeyguardManager::class.java)
    private val power = app.getSystemService(PowerManager::class.java)
    private val sensors = app.getSystemService(SensorManager::class.java)
    private val audio = app.getSystemService(AudioManager::class.java)
    private val usage = app.getSystemService(UsageStatsManager::class.java)
    private val store = WakeProgressStore(app)

    // The wake-up variant keeps counting while the phone sleeps; not every phone has one.
    private val stepSensor: Sensor? = sensors.getDefaultSensor(Sensor.TYPE_STEP_COUNTER, true)
        ?: sensors.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    private val accelerometer: Sensor? = sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    /** Runs until cancelled, which is when the watch window ends. Call on the main thread. */
    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun watch() = coroutineScope {
        val saved = store.load(alarmAt)
        val tracker = WakeTracker(resumeFrom = saved?.signals ?: WakeSignals(), ignoredApps = UsageAccess.ignoredApps(app))
        tracker.movementAvailable = accelerometer != null
        var notified = saved?.notified ?: false
        var lastSaved = saved
        var lastSavedAt = SystemClock.elapsedRealtime()
        if (saved != null) Log.i(TAG, "Carrying on from $saved")

        val settings = app.settings.wakeSignals.stateIn(this)
        // What Dawnly has learned from the user's answers: how cautious to be, their usual time,
        // which signs to trust. Read once per window; it only changes when they answer.
        val learned = app.learning.current(app.settings)
        if (learned.mornings > 0) Log.i(TAG, "Learned from ${learned.mornings} mornings: $learned")
        val canCountSteps = ticks(PERMISSION_CHECK_PERIOD_MILLIS).map { canCountSteps() }.distinctUntilChanged()
        val countSteps = combine(settings, canCountSteps) { s, can -> s.useSteps && can }.distinctUntilChanged()

        val events = merge(
            screenEvents(),
            canCountSteps.map { WakeEvent.StepsAvailable(it) },
            countSteps.flatMapLatest { counting ->
                val sensor = stepSensor
                if (counting && sensor != null) stepCounter(sensor) else flowOf(WakeEvent.StepsStopped)
            },
            settings.map { it.useMovement }.distinctUntilChanged().flatMapLatest { on ->
                val sensor = accelerometer
                if (on && sensor != null) movement(sensor) else emptyFlow()
            },
            settings.map { it.usePhoneUse }.distinctUntilChanged().flatMapLatest { on ->
                if (on) appsOpened() else emptyFlow()
            },
            settings.drop(1).map { WakeEvent.Tick },
            ticks(TICK_PERIOD_MILLIS).map { WakeEvent.Tick },
        )

        try {
            events.collect { event ->
                when (event) {
                    is WakeEvent.Unlocked -> tracker.onUnlocked(event.at)
                    is WakeEvent.ScreenOff -> tracker.onScreenOff(event.at)
                    is WakeEvent.Unplugged -> tracker.onUnplugged(event.at)
                    is WakeEvent.Steps -> tracker.onStepCounter(event.totalSinceBoot, event.at)
                    WakeEvent.StepsStopped -> tracker.onStepCounterStopped()
                    is WakeEvent.StepsAvailable -> tracker.stepsAvailable = event.available
                    is WakeEvent.AppResumed -> tracker.onAppResumed(event.packageName)
                    is WakeEvent.AppsAvailable -> tracker.appsAvailable = event.available
                    is WakeEvent.Acceleration -> {
                        // Several a second: just counted here, published on the next tick.
                        tracker.onAcceleration(event.x, event.y, event.z, event.timestampNanos, event.at)
                        return@collect
                    }
                    WakeEvent.Tick -> tracker.onMusicCheck(audio.isMusicActive, SystemClock.elapsedRealtime())
                }
                val now = SystemClock.elapsedRealtime()
                val signals = tracker.signals(now)
                WatchSession.publish(signals)

                // What's learned, and how close the alarm is: activity near it counts for more.
                val untilAlarm = Duration.between(Instant.now(), alarmAt)
                val goals = settings.value.scaledBy(learned.goalFactor(LocalDateTime.now()) * Closeness.goalFactor(untilAlarm))
                val score = WakeScore.of(signals, goals, learned.weights)
                val awakeNow = !notified && score.isAwake
                if (awakeNow) notified = true
                val progress = WakeProgress(alarmAt, signals, notified)
                // Saved before asking, so a restart can never ask twice.
                if (awakeNow || (progress != lastSaved && now - lastSavedAt >= SAVE_PERIOD_MILLIS)) {
                    store.save(progress)
                    lastSaved = progress
                    lastSavedAt = now
                }
                if (awakeNow) {
                    // Nothing is recorded while the user has learning off.
                    if (app.settings.learn.first()) app.learning.asked(Instant.now(), score.shares)
                    onAwake(signals, score.musicTippedIt)
                }
            }
        } finally {
            withContext(NonCancellable) {
                store.save(WakeProgress(alarmAt, tracker.signals(SystemClock.elapsedRealtime()), notified))
            }
        }
    }

    private fun onAwake(signals: WakeSignals, musicTippedIt: Boolean) {
        Log.i(TAG, "Awake before the $alarmAt alarm: $signals (music tipped it: $musicTippedIt)")
        AwakeNotifier.onUserAwake(app, alarmAt, musicTippedIt)
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
                    Intent.ACTION_POWER_DISCONNECTED -> trySend(WakeEvent.Unplugged(now))
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_USER_PRESENT)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
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
                trySend(WakeEvent.Steps(event.values[0], SystemClock.elapsedRealtime()))
            }

            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit
        }
        // Batched to save battery: readings may arrive up to SENSOR_BATCH_MICROS late.
        val registered = sensors.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL, SENSOR_BATCH_MICROS)
        if (!registered) Log.w(TAG, "Couldn't register the step counter")
        awaitClose { sensors.unregisterListener(listener) }
    }

    /**
     * The accelerometer at its slowest standard rate (about 5 a second), batched. No permission is
     * needed, and it only runs during the window. While the phone sleeps, samples the sensor can't
     * hold are lost; steps cover walking around with the screen off.
     */
    private fun movement(sensor: Sensor): Flow<WakeEvent> = callbackFlow {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val (x, y, z) = event.values
                trySend(WakeEvent.Acceleration(x, y, z, event.timestamp, SystemClock.elapsedRealtime()))
            }

            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit
        }
        val registered = sensors.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL, SENSOR_BATCH_MICROS)
        if (!registered) Log.w(TAG, "Couldn't register the accelerometer")
        awaitClose { sensors.unregisterListener(listener) }
    }

    /**
     * Apps coming to the front, read from Usage access every few seconds. Without the permission it
     * says so, and checks again in case the user allows it mid-window.
     */
    private fun appsOpened(): Flow<WakeEvent> = flow {
        var lastSeen = System.currentTimeMillis()
        while (true) {
            delay(APPS_CHECK_PERIOD_MILLIS)
            val allowed = UsageAccess.isGranted(app)
            emit(WakeEvent.AppsAvailable(allowed))
            if (!allowed) continue
            val now = System.currentTimeMillis()
            // Events can land a moment after they happen, so look back a little; the timestamp
            // check keeps any event from being counted twice.
            val events = usage.queryEvents(maxOf(lastSeen + 1, now - APPS_LOOKBACK_MILLIS), now)
            val event = UsageEvents.Event()
            while (events.getNextEvent(event)) {
                @Suppress("DEPRECATION") // ACTIVITY_RESUMED (Android 10+) has the same value.
                if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND && event.timeStamp > lastSeen) {
                    lastSeen = event.timeStamp
                    emit(WakeEvent.AppResumed(event.packageName))
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    /** Needs a step counter, and the activity recognition permission on Android 10+. */
    private fun canCountSteps(): Boolean = stepSensor != null && (
        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            ContextCompat.checkSelfPermission(app, Manifest.permission.ACTIVITY_RECOGNITION) ==
            PackageManager.PERMISSION_GRANTED
        )

    companion object {
        private const val TAG = "WakeWatcher"
        private const val TICK_PERIOD_MILLIS = 5_000L

        /** How often to notice that activity recognition was allowed mid-window. */
        private const val PERMISSION_CHECK_PERIOD_MILLIS = 15_000L
        private const val APPS_CHECK_PERIOD_MILLIS = 10_000L
        private const val APPS_LOOKBACK_MILLIS = 60_000L
        private const val SAVE_PERIOD_MILLIS = 30_000L
        private const val SENSOR_BATCH_MICROS = 10_000_000

        private fun ticks(periodMillis: Long): Flow<Unit> = flow {
            while (true) {
                emit(Unit)
                delay(periodMillis)
            }
        }
    }
}
