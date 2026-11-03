package com.dawnly.app.ui.home

import android.app.TimePickerDialog
import android.text.format.DateFormat
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.dawnly.app.R
import com.dawnly.app.domain.reminder.Reminder
import com.dawnly.app.domain.reminder.ReminderSchedule
import com.dawnly.app.ui.format.formatDuration
import com.dawnly.app.ui.format.formatTime
import com.dawnly.app.ui.glass.GlassButton
import com.dawnly.app.ui.glass.GlassIcons
import com.dawnly.app.ui.glass.GlassList
import com.dawnly.app.ui.glass.GlassRow
import com.dawnly.app.ui.glass.GlassSegmented
import com.dawnly.app.ui.glass.GlassShapes
import com.dawnly.app.ui.glass.GlassSurface
import com.dawnly.app.ui.glass.GlassSwitch
import com.dawnly.app.ui.glass.PrimaryButton
import com.dawnly.app.ui.glass.SectionLabel
import com.dawnly.app.ui.glass.TextAction
import com.dawnly.app.ui.theme.DawnlyTheme
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * The user's reminders on Today, like "Drink water, every 20 minutes": each with a switch, tap one
 * to change it, and a row to add one. [onEdit] with null starts a new one.
 */
@Composable
internal fun RemindersCard(
    reminders: List<Reminder>,
    onEdit: (Reminder?) -> Unit,
    onToggle: (Reminder, Boolean) -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        SectionLabel(stringResource(R.string.reminders_label))
        GlassList {
            reminders.forEachIndexed { index, reminder ->
                GlassRow(
                    title = reminder.name,
                    subtitle = describe(reminder),
                    divider = index > 0,
                    modifier = Modifier.clickable(role = Role.Button) { onEdit(reminder) },
                ) {
                    GlassSwitch(reminder.on, { onToggle(reminder, it) })
                }
            }
            GlassRow(
                title = stringResource(R.string.reminder_add),
                subtitle = if (reminders.isEmpty()) stringResource(R.string.reminder_add_hint) else null,
                divider = reminders.isNotEmpty(),
                modifier = Modifier.clickable(role = Role.Button) { onEdit(null) },
            ) {
                Icon(GlassIcons.Plus, contentDescription = null, tint = DawnlyTheme.colors.textSoft, modifier = Modifier.size(20.dp))
            }
        }
    }
}

/** "Every day at 9:00 pm" or "Every 20 min, 8:00 am to 10:00 pm", and the why under it. */
@Composable
private fun describe(reminder: Reminder): String {
    val schedule = when (val s = reminder.schedule) {
        is ReminderSchedule.Daily -> stringResource(R.string.reminder_daily, clock(s.at))
        is ReminderSchedule.Every -> stringResource(
            R.string.reminder_every,
            formatDuration(Duration.ofMinutes(s.minutes.toLong())),
            clock(s.from),
            clock(s.until),
        )
    }
    return if (reminder.why.isBlank()) schedule else "$schedule · ${reminder.why}"
}

@Composable
private fun clock(time: LocalTime): String =
    formatTime(LocalDate.now().atTime(time).atZone(ZoneId.systemDefault()).toInstant())

/**
 * Adding or changing a reminder: its name, why, and when (once a day at a time, or every so many
 * minutes between two times). [initial] null makes a new one.
 */
@Composable
internal fun ReminderEditor(
    initial: Reminder?,
    onSave: (Reminder) -> Unit,
    onDelete: (Long) -> Unit,
    onClose: () -> Unit,
) {
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var why by remember { mutableStateOf(initial?.why.orEmpty()) }
    var nameMissing by remember { mutableStateOf(false) }
    val start = initial?.schedule
    var every by remember { mutableStateOf(start !is ReminderSchedule.Daily) }
    var at by remember { mutableStateOf((start as? ReminderSchedule.Daily)?.at ?: LocalTime.of(21, 0)) }
    var minutes by remember { mutableStateOf((start as? ReminderSchedule.Every)?.minutes ?: 20) }
    var from by remember { mutableStateOf((start as? ReminderSchedule.Every)?.from ?: ReminderSchedule.DAY_START) }
    var until by remember { mutableStateOf((start as? ReminderSchedule.Every)?.until ?: ReminderSchedule.DAY_END) }

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.padding(16.dp).clip(GlassShapes.Card).background(DawnlyTheme.colors.wall.copy(alpha = 0.97f))) {
            GlassSurface(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        stringResource(if (initial == null) R.string.reminder_new else R.string.reminder_edit),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it; nameMissing = false },
                        label = { Text(stringResource(R.string.reminder_name)) },
                        placeholder = { Text(stringResource(R.string.reminder_name_hint)) },
                        isError = nameMissing,
                        supportingText = if (nameMissing) {
                            { Text(stringResource(R.string.reminder_name_needed)) }
                        } else {
                            null
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = why,
                        onValueChange = { why = it },
                        label = { Text(stringResource(R.string.reminder_why)) },
                        placeholder = { Text(stringResource(R.string.reminder_why_hint)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    GlassSegmented(
                        options = listOf(stringResource(R.string.reminder_kind_time), stringResource(R.string.reminder_kind_every)),
                        selected = if (every) 1 else 0,
                        onSelect = { every = it == 1 },
                    )
                    AnimatedContent(
                        targetState = every,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "reminderKind",
                    ) { repeating ->
                        if (!repeating) {
                            TimeButton(stringResource(R.string.reminder_time), at, Modifier.fillMaxWidth()) { at = it }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                val repeats = ReminderSchedule.REPEATS
                                GlassSegmented(
                                    options = repeats.map { formatDuration(Duration.ofMinutes(it.toLong())) },
                                    selected = repeats.indexOf(minutes).coerceAtLeast(0),
                                    onSelect = { minutes = repeats[it] },
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TimeButton(stringResource(R.string.reminder_from), from, Modifier.weight(1f)) { from = it }
                                    TimeButton(stringResource(R.string.reminder_until), until, Modifier.weight(1f)) { until = it }
                                }
                            }
                        }
                    }
                    Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PrimaryButton(
                            stringResource(R.string.reminder_save),
                            {
                                if (name.isBlank()) {
                                    nameMissing = true
                                } else {
                                    val schedule = if (every) ReminderSchedule.Every(minutes, from, until) else ReminderSchedule.Daily(at)
                                    onSave(
                                        Reminder(
                                            id = initial?.id ?: System.currentTimeMillis(),
                                            name = name.trim(),
                                            why = why.trim(),
                                            schedule = schedule,
                                            on = initial?.on ?: true,
                                        ),
                                    )
                                }
                            },
                            Modifier.weight(1f),
                        )
                        GlassButton(stringResource(R.string.common_cancel), onClose, Modifier.weight(1f))
                    }
                    if (initial != null) {
                        TextAction(
                            stringResource(R.string.reminder_delete),
                            { onDelete(initial.id) },
                            Modifier.align(Alignment.CenterHorizontally),
                        )
                    }
                }
            }
        }
    }
}

/** A glass button showing "[label]  9:00 pm" that opens the phone's time picker. */
@Composable
private fun TimeButton(label: String, time: LocalTime, modifier: Modifier = Modifier, onPick: (LocalTime) -> Unit) {
    val context = LocalContext.current
    GlassSurface(
        modifier = modifier,
        shape = GlassShapes.Button,
        onClick = {
            TimePickerDialog(
                context,
                { _, hour, minute -> onPick(LocalTime.of(hour, minute)) },
                time.hour,
                time.minute,
                DateFormat.is24HourFormat(context),
            ).show()
        },
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = DawnlyTheme.colors.textSoft, modifier = Modifier.weight(1f))
            Text(clock(time), style = MaterialTheme.typography.bodyLarge)
        }
    }
}
