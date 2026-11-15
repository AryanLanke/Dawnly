package com.dawnly.app.ui.time

import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.dawnly.app.R
import com.dawnly.app.data.time.TimeStore
import com.dawnly.app.domain.time.TimerPhase
import com.dawnly.app.notify.TimerController
import com.dawnly.app.ui.glass.FlowingBackground
import com.dawnly.app.ui.glass.GlassIcons
import com.dawnly.app.ui.glass.GlassSurface
import com.dawnly.app.ui.glass.PrimaryButton
import com.dawnly.app.ui.permissions.AppPermission
import com.dawnly.app.ui.glass.EDITOR_MS
import com.dawnly.app.ui.theme.DawnlyTheme
import com.dawnly.app.ui.theme.clockSaysNight
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * Several timers at once, each with its own name, tone and finish mode. They keep running while this
 * screen is closed, the phone is rebooted (paused at the last saved time) or another app is in front.
 */
class TimerScreen : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DawnlyTheme(darkTheme = clockSaysNight()) {
                Box(Modifier.fillMaxSize()) {
                    FlowingBackground()
                    TimerScreenContent(onBack = { finish() })
                }
            }
        }
    }
}

@Composable
private fun TimerScreenContent(onBack: () -> Unit) {
    val activity = LocalContext.current
    val context = activity.applicationContext
    val lifecycle = LocalLifecycleOwner.current
    val state by remember(context) { TimeStore.states(context) }.collectAsStateWithLifecycle()
    var now by remember { mutableStateOf(TimeStore.now(context)) }
    var error by rememberSaveable { mutableStateOf<Int?>(null) }
    var adding by rememberSaveable { mutableStateOf(false) }
    var exactGranted by remember { mutableStateOf(AppPermission.ExactAlarms.isGranted(context)) }
    var notifGranted by remember { mutableStateOf(AppPermission.Notifications.isGranted(context)) }

    fun openSettings(permission: AppPermission) {
        val intent = permission.settingsIntents(context).firstOrNull() ?: return
        try {
            activity.startActivity(intent)
        } catch (_: Exception) {
            // Some phones swallow a settings screen; the permission card stays as a hint.
        }
    }

    fun result(value: TimerController.Result) {
        error = when (value) {
            TimerController.Result.Success -> null
            TimerController.Result.InvalidDuration -> R.string.time_invalid_duration
            TimerController.Result.ExactAlarmRequired -> R.string.time_exact_body
            TimerController.Result.StorageError -> R.string.time_storage_error
            TimerController.Result.SchedulingFailed -> R.string.time_schedule_error
        }
        if (value == TimerController.Result.ExactAlarmRequired) openSettings(AppPermission.ExactAlarms)
    }

    LifecycleResumeEffect(context) {
        now = TimeStore.now(context)
        exactGranted = AppPermission.ExactAlarms.isGranted(context)
        notifGranted = AppPermission.Notifications.isGranted(context)
        val synced = TimerController.sync(context)
        if (synced == TimerController.Result.StorageError) error = R.string.time_storage_error
        if (synced == TimerController.Result.SchedulingFailed) error = R.string.time_schedule_error
        onPauseOrDispose { TimeStore.checkpoint(context) }
    }
    LaunchedEffect(context, lifecycle) {
        lifecycle.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            now = TimeStore.now(context)
            var checkpointAt = now.elapsedMs
            while (isActive) {
                now = now.copy(elapsedMs = SystemClock.elapsedRealtime())
                // A bounded reboot recovery checkpoint, not a ticking persisted counter.
                if (now.elapsedMs - checkpointAt >= 5_000L) {
                    TimeStore.checkpoint(context)
                    checkpointAt = now.elapsedMs
                }
                delay(50L)
            }
        }
    }
    val anyDue = state.timers.any { it.completionPending || (it.phase == TimerPhase.Running && it.remaining(now) == 0L) }
    LaunchedEffect(anyDue) { if (anyDue) TimerController.sync(context) }

    // The add-timer card rises over a frosted screen, like the alarm and reminder editors on Today.
    val canFrost = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val editorShown by animateFloatAsState(
        targetValue = if (adding) 1f else 0f,
        animationSpec = tween(EDITOR_MS, easing = FastOutSlowInEasing),
        label = "editor",
    )
    val frosted = if (canFrost) {
        Modifier.graphicsLayer {
            val radius = 18.dp.toPx() * editorShown
            renderEffect = if (radius > 0.5f) BlurEffect(radius, radius, TileMode.Clamp) else null
        }
    } else {
        Modifier
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().then(frosted).safeDrawingPadding().verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 18.dp)) {
                GlassSurface(
                    modifier = Modifier.size(44.dp),
                    shape = CircleShape,
                    onClick = onBack,
                    onClickLabel = stringResource(R.string.timers_back),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(GlassIcons.Back, contentDescription = stringResource(R.string.timers_back), modifier = Modifier.size(22.dp))
                }
                Text(
                    stringResource(R.string.timers_title),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(start = 14.dp),
                )
            }
            if (!exactGranted) {
                TimerPermissionCard(R.string.time_exact_title, R.string.time_exact_body) { openSettings(AppPermission.ExactAlarms) }
            }
            if (!notifGranted) {
                TimerPermissionCard(R.string.time_notifications_title, R.string.time_notifications_body) { openSettings(AppPermission.Notifications) }
            }
            error?.let { Text(stringResource(it), style = MaterialTheme.typography.bodyMedium, color = DawnlyTheme.colors.textSoft) }

            if (state.timers.isEmpty()) {
                Text(
                    stringResource(R.string.timers_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = DawnlyTheme.colors.textSoft,
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
            state.timers.forEach { timer ->
                TimerCard(timer, now, onResult = { result(it) }, onRefresh = { now = TimeStore.now(context) })
            }

            PrimaryButton(stringResource(R.string.timers_add), onClick = { adding = true })
            Spacer(Modifier.height(8.dp))
        }

        AnimatedVisibility(
            visible = adding,
            enter = fadeIn(tween(EDITOR_MS)),
            exit = fadeOut(tween(EDITOR_MS)),
        ) {
            Box(Modifier.fillMaxSize()) {
                // The veil: brighter without the frost on older phones, so the card stays readable.
                Box(
                    Modifier.fillMaxSize()
                        .background(DawnlyTheme.colors.wall.copy(alpha = if (canFrost) 0.25f else 0.7f))
                        .clickable(remember { MutableInteractionSource() }, indication = null, onClick = { adding = false }),
                )
                Box(
                    Modifier.fillMaxSize().safeDrawingPadding().imePadding().padding(16.dp)
                        .animateEnterExit(
                            enter = scaleIn(tween(EDITOR_MS, easing = FastOutSlowInEasing), initialScale = 0.92f),
                            exit = scaleOut(tween(EDITOR_MS, easing = FastOutSlowInEasing), targetScale = 0.92f),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    AddTimerCard(
                        onDismiss = { adding = false },
                        onSave = { label, duration, sound, ring ->
                            val outcome = TimerController.add(context, label, duration, sound, ring)
                            result(outcome)
                            if (outcome == TimerController.Result.Success) {
                                adding = false
                                now = TimeStore.now(context)
                            }
                        },
                    )
                }
            }
        }
    }
}
