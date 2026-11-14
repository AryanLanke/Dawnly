package com.dawnly.app.ui.time

import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.dawnly.app.R
import com.dawnly.app.data.sound.AlertSound
import com.dawnly.app.data.time.TimeStore
import com.dawnly.app.domain.time.TimeReading
import com.dawnly.app.domain.time.TimerPhase
import com.dawnly.app.domain.time.TimerState
import com.dawnly.app.notify.TimerController
import com.dawnly.app.ui.glass.AccentPill
import com.dawnly.app.ui.glass.CheckBadge
import com.dawnly.app.ui.glass.FlowingBackground
import com.dawnly.app.ui.glass.GlassButton
import com.dawnly.app.ui.glass.GlassField
import com.dawnly.app.ui.glass.GlassIcons
import com.dawnly.app.ui.glass.GlassList
import com.dawnly.app.ui.glass.GlassRow
import com.dawnly.app.ui.glass.GlassSegmented
import com.dawnly.app.ui.glass.GlassSurface
import com.dawnly.app.ui.glass.PrimaryButton
import com.dawnly.app.ui.glass.ROW_INSET
import com.dawnly.app.ui.glass.SectionLabel
import com.dawnly.app.ui.glass.TextAction
import com.dawnly.app.ui.glass.ValueButton
import com.dawnly.app.ui.permissions.AppPermission
import com.dawnly.app.ui.settings.rememberSoundPreview
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

    Column(
        Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
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
            PermissionCard(R.string.time_exact_title, R.string.time_exact_body) { openSettings(AppPermission.ExactAlarms) }
        }
        if (!notifGranted) {
            PermissionCard(R.string.time_notifications_title, R.string.time_notifications_body) { openSettings(AppPermission.Notifications) }
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

    if (adding) {
        AddTimerDialog(
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

@Composable
private fun TimerCard(
    timer: TimerState,
    now: TimeReading,
    onResult: (TimerController.Result) -> Unit,
    onRefresh: () -> Unit,
) {
    val context = LocalContext.current.applicationContext
    GlassList {
        GlassRow(
            title = timer.label,
            subtitle = stringResource(
                when (timer.phase) {
                    TimerPhase.Ready -> R.string.timers_ready
                    TimerPhase.Running -> R.string.time_running
                    TimerPhase.Paused -> if (timer.pausedAfterReboot) R.string.time_reboot_paused else R.string.time_paused
                    TimerPhase.Finished -> R.string.time_timer_finished
                },
            ),
            end = {
                Text(
                    timerText(timer.remaining(now)),
                    style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum"),
                )
            },
        )
        Row(
            Modifier.fillMaxWidth().padding(horizontal = ROW_INSET).padding(bottom = ROW_INSET),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            when (timer.phase) {
                TimerPhase.Ready -> AccentPill(stringResource(R.string.timers_start), {
                    onResult(TimerController.start(context, timer.id, timer.durationMs)); onRefresh()
                })
                TimerPhase.Running -> AccentPill(stringResource(R.string.time_pause), {
                    onResult(TimerController.pause(context, timer.id)); onRefresh()
                })
                TimerPhase.Paused -> AccentPill(stringResource(R.string.time_resume), {
                    onResult(TimerController.resume(context, timer.id)); onRefresh()
                })
                TimerPhase.Finished -> AccentPill(stringResource(R.string.time_reset), {
                    onResult(TimerController.reset(context, timer.id)); onRefresh()
                })
            }
            if (timer.phase == TimerPhase.Running || timer.phase == TimerPhase.Paused) {
                TextAction(stringResource(R.string.time_reset), {
                    onResult(TimerController.reset(context, timer.id)); onRefresh()
                })
            }
            Spacer(Modifier.weight(1f))
            TextAction(stringResource(R.string.timers_delete), {
                onResult(TimerController.delete(context, timer.id)); onRefresh()
            })
        }
    }
}

@Composable
private fun PermissionCard(title: Int, body: Int, onAllow: () -> Unit) {
    GlassList {
        GlassRow(stringResource(title), subtitle = stringResource(body))
        AccentPill(stringResource(R.string.time_allow), onAllow, Modifier.padding(start = ROW_INSET, bottom = ROW_INSET))
    }
}

/** The glass overlay for a new timer: name, quick presets or custom time, tone, and finish mode. */
@Composable
private fun AddTimerDialog(onDismiss: () -> Unit, onSave: (String, Long, String?, Boolean) -> Unit) {
    val defaultLabel = stringResource(R.string.time_timer)
    var label by rememberSaveable { mutableStateOf("") }
    var hours by rememberSaveable { mutableStateOf("0") }
    var minutes by rememberSaveable { mutableStateOf("5") }
    var seconds by rememberSaveable { mutableStateOf("0") }
    var sound by rememberSaveable { mutableStateOf<String?>(null) }
    var ring by rememberSaveable { mutableStateOf(false) }
    var pickingSound by rememberSaveable { mutableStateOf(false) }
    var invalid by rememberSaveable { mutableStateOf(false) }
    val preview = rememberSoundPreview()
    val presets = listOf(1, 3, 5, 10)

    Dialog(onDismissRequest = onDismiss) {
        GlassSurface(Modifier.fillMaxWidth().widthIn(max = 480.dp)) {
            Column(
                Modifier.verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(stringResource(R.string.timers_add), style = MaterialTheme.typography.titleMedium)
                GlassField(
                    label = stringResource(R.string.timers_label),
                    value = label,
                    onValueChange = { label = it },
                    placeholder = defaultLabel,
                )
                SectionLabel(stringResource(R.string.timers_quick))
                GlassSegmented(
                    options = presets.map { stringResource(R.string.timers_preset, it) },
                    selected = -1,
                    onSelect = { index -> hours = "0"; minutes = presets[index].toString(); seconds = "0"; invalid = false },
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DurationField(hours, { hours = it; invalid = false }, R.string.time_hours, 99, Modifier.weight(1f))
                    DurationField(minutes, { minutes = it; invalid = false }, R.string.time_minutes, 59, Modifier.weight(1f))
                    DurationField(seconds, { seconds = it; invalid = false }, R.string.time_seconds, 59, Modifier.weight(1f))
                }
                ValueButton(
                    stringResource(R.string.reminder_sound),
                    stringResource(soundLabel(sound)),
                    Modifier.fillMaxWidth(),
                ) { pickingSound = true }
                SectionLabel(stringResource(R.string.timers_finish))
                GlassSegmented(
                    options = listOf(stringResource(R.string.reminder_alert_notify), stringResource(R.string.timers_ring)),
                    selected = if (ring) 1 else 0,
                    onSelect = { ring = it == 1 },
                )
                if (invalid) {
                    Text(stringResource(R.string.time_invalid_duration), style = MaterialTheme.typography.bodySmall, color = DawnlyTheme.colors.tick)
                }
                Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassButton(
                        stringResource(R.string.reminder_save),
                        {
                            val duration = ((hours.toLongOrNull() ?: 0L) * 3600 + (minutes.toLongOrNull() ?: 0L) * 60 + (seconds.toLongOrNull() ?: 0L)) * 1000
                            if (duration < 1) {
                                invalid = true
                            } else {
                                invalid = false
                                onSave(label.trim().ifEmpty { defaultLabel }, duration, sound, ring)
                            }
                        },
                        Modifier.weight(1f),
                    )
                    GlassButton(stringResource(R.string.common_cancel), onDismiss, Modifier.weight(1f))
                }
            }
        }
    }
    if (pickingSound) {
        SoundChoiceDialog(
            selected = sound,
            onPick = { picked ->
                sound = if (picked == AlertSound.PhoneDefault) null else picked.name
                preview.play(picked)
            },
            onDone = { preview.stop(); pickingSound = false },
        )
    }
}

/** Dawnly's tones in a glass list; tapping one picks it and plays it, like the reminder editor. */
@Composable
private fun SoundChoiceDialog(selected: String?, onPick: (AlertSound) -> Unit, onDone: () -> Unit) {
    val current = AlertSound.entries.firstOrNull { it.name == selected } ?: AlertSound.PhoneDefault
    Dialog(onDismissRequest = onDone) {
        GlassSurface(Modifier.fillMaxWidth().widthIn(max = 480.dp)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(stringResource(R.string.reminder_sound), style = MaterialTheme.typography.titleMedium)
                Column(Modifier.heightIn(max = 380.dp).verticalScroll(rememberScrollState())) {
                    GlassList(Modifier.selectableGroup()) {
                        AlertSound.entries.forEachIndexed { index, sound ->
                            GlassRow(
                                title = stringResource(sound.label),
                                divider = index > 0,
                                modifier = Modifier.selectable(selected = sound == current, role = Role.RadioButton) { onPick(sound) },
                            ) {
                                if (sound == current) CheckBadge()
                            }
                        }
                    }
                }
                GlassButton(stringResource(R.string.reminder_done), onDone, Modifier.fillMaxWidth())
            }
        }
    }
}

/** The chosen tone's name, or the phone default when nothing is picked. */
private fun soundLabel(sound: String?): Int =
    AlertSound.entries.firstOrNull { it.name == sound }?.label ?: R.string.sound_phone_default