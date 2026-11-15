package com.dawnly.app.ui.time

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dawnly.app.R
import com.dawnly.app.data.sound.AlertSound
import com.dawnly.app.domain.time.TimeReading
import com.dawnly.app.domain.time.TimerPhase
import com.dawnly.app.domain.time.TimerState
import com.dawnly.app.notify.TimerController
import com.dawnly.app.ui.glass.AccentPill
import com.dawnly.app.ui.glass.GlassButton
import com.dawnly.app.ui.glass.GlassField
import com.dawnly.app.ui.glass.GlassList
import com.dawnly.app.ui.glass.GlassRow
import com.dawnly.app.ui.glass.GlassSegmented
import com.dawnly.app.ui.glass.GlassSurface
import com.dawnly.app.ui.glass.ROW_INSET
import com.dawnly.app.ui.glass.SectionLabel
import com.dawnly.app.ui.glass.TextAction
import com.dawnly.app.ui.glass.ValueButton
import com.dawnly.app.ui.home.SoundChoice
import com.dawnly.app.ui.theme.DawnlyTheme

/** One timer: its name, phase and remaining time, with the action its phase allows. */
@Composable
internal fun TimerCard(
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

/** A permission the timers need, with one tap to its settings screen. */
@Composable
internal fun TimerPermissionCard(title: Int, body: Int, onAllow: () -> Unit) {
    GlassList {
        GlassRow(stringResource(title), subtitle = stringResource(body))
        AccentPill(stringResource(R.string.time_allow), onAllow, Modifier.padding(start = ROW_INSET, bottom = ROW_INSET))
    }
}

/** The glass card for a new timer: name, quick presets or custom time, tone, and finish mode. */
@Composable
internal fun AddTimerCard(onDismiss: () -> Unit, onSave: (String, Long, String?, Boolean) -> Unit) {
    val defaultLabel = stringResource(R.string.time_timer)
    var label by rememberSaveable { mutableStateOf("") }
    var hours by rememberSaveable { mutableStateOf("0") }
    var minutes by rememberSaveable { mutableStateOf("5") }
    var seconds by rememberSaveable { mutableStateOf("0") }
    var sound by rememberSaveable { mutableStateOf<String?>(null) }
    var ring by rememberSaveable { mutableStateOf(false) }
    var pickingSound by rememberSaveable { mutableStateOf(false) }
    var invalid by rememberSaveable { mutableStateOf(false) }
    val presets = listOf(1, 3, 5, 10)

    BackHandler { if (pickingSound) pickingSound = false else onDismiss() }
    // Taps on the card stay on the card; the veil's own tap closes everything.
    Box(Modifier.clickable(remember { MutableInteractionSource() }, indication = null) {}) {
        if (pickingSound) {
            SoundChoice(
                selected = AlertSound.entries.firstOrNull { it.name == sound } ?: AlertSound.PhoneDefault,
                onPick = { picked -> sound = if (picked == AlertSound.PhoneDefault) null else picked.name },
                onDone = { pickingSound = false },
            )
        } else {
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
    }
}

/** The chosen tone's name, or the phone default when nothing is picked. */
private fun soundLabel(sound: String?): Int =
    AlertSound.entries.firstOrNull { it.name == sound }?.label ?: R.string.sound_phone_default
