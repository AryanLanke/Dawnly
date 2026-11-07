package com.dawnly.app.ui.home

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.dawnly.app.R
import com.dawnly.app.data.reminder.ReminderSounds
import com.dawnly.app.data.sound.AlertSound
import com.dawnly.app.domain.reminder.Reminder
import com.dawnly.app.domain.reminder.ReminderDays
import com.dawnly.app.domain.reminder.ReminderSchedule
import com.dawnly.app.domain.reminder.ReminderTimes
import com.dawnly.app.ui.format.clock
import com.dawnly.app.ui.format.formatDuration
import com.dawnly.app.ui.glass.CheckBadge
import com.dawnly.app.ui.glass.DateWheel
import com.dawnly.app.ui.glass.DurationWheel
import com.dawnly.app.ui.glass.GlassButton
import com.dawnly.app.ui.glass.GlassField
import com.dawnly.app.ui.glass.GlassList
import com.dawnly.app.ui.glass.GlassRow
import com.dawnly.app.ui.glass.GlassSegmented
import com.dawnly.app.ui.glass.GlassSurface
import com.dawnly.app.ui.glass.SectionLabel
import com.dawnly.app.ui.glass.TextAction
import com.dawnly.app.ui.glass.TimeWheel
import com.dawnly.app.ui.glass.ValueButton
import com.dawnly.app.ui.settings.rememberSoundPreview
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val ReminderDays.label: Int
    get() = when (this) {
        ReminderDays.EveryDay -> R.string.reminder_days_every_day
        ReminderDays.Today -> R.string.reminder_days_today
        ReminderDays.Tomorrow -> R.string.reminder_days_tomorrow
        ReminderDays.Both -> R.string.reminder_days_both
        ReminderDays.OnDate -> R.string.reminder_days_date
        ReminderDays.Range -> R.string.reminder_days_range
    }

/** The card's other pages: a time on the wheels, a custom repeat, or the reminder's sound. */
private enum class EditorPage { At, From, Until, Interval, Sound, DateA, DateB }

/**
 * Adding or changing a reminder, drawn over Today (in the app's own window, so it keeps the
 * screen's 120 Hz): a glass card with glass fields for the name and why, and when it comes. Tapping
 * a time turns the card into a glass circle like Today's alarm, with big digits on wheels.
 * [initial] null makes a new one.
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
    // A repeat that isn't one of the offered ones, like every 5 minutes or every 2 hours.
    var custom by remember { mutableStateOf(minutes !in ReminderSchedule.REPEATS) }
    var from by remember { mutableStateOf((start as? ReminderSchedule.Every)?.from ?: ReminderSchedule.DAY_START) }
    var until by remember { mutableStateOf((start as? ReminderSchedule.Every)?.until ?: ReminderSchedule.DAY_END) }
    var sound by remember { mutableStateOf(initial?.let(ReminderSounds::of) ?: ReminderSounds.DEFAULT) }
    var ring by remember { mutableStateOf(initial?.ring ?: false) }
    var picking by remember { mutableStateOf<EditorPage?>(null) }
    val today = LocalDate.now()
    val context = LocalContext.current
    var days by remember { mutableStateOf(ReminderDays.of(initial?.firstDay, initial?.lastDay, today)) }
    var date by remember { mutableStateOf(initial?.firstDay?.takeIf { it >= today } ?: today) }
    var dateTo by remember { mutableStateOf(initial?.lastDay?.takeIf { it >= date } ?: date.plusDays(6)) }

    BackHandler { if (picking != null) picking = null else onClose() }
    Box(
        Modifier
            .fillMaxSize()
            // Taps around the card close it; Today behind is frosted (see MainTabs).
            .clickable(remember { MutableInteractionSource() }, indication = null, onClick = onClose)
            .safeDrawingPadding()
            .imePadding()
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = picking,
            transitionSpec = { (fadeIn() + scaleIn(initialScale = 0.94f)) togetherWith fadeOut() },
            label = "reminderPage",
            // Taps on the card stay on the card.
            modifier = Modifier.clickable(remember { MutableInteractionSource() }, indication = null) {},
        ) { page ->
            if (page == EditorPage.Sound) {
                SoundChoice(sound, onPick = { sound = it }, onDone = { picking = null })
                return@AnimatedContent
            }
            if (page == EditorPage.DateA || page == EditorPage.DateB) {
                val isTo = page == EditorPage.DateB
                DateWheel(
                    stringResource(if (days == ReminderDays.Range) (if (isTo) R.string.reminder_date_to else R.string.reminder_date_from) else R.string.reminder_date),
                    if (isTo) dateTo else date,
                    today,
                    onDone = { picked ->
                        if (isTo) dateTo = picked else date = picked
                        if (days == ReminderDays.Range) {
                            if (!isTo && dateTo < picked) dateTo = picked
                            if (isTo && picked < date) date = picked
                        }
                        picking = null
                    },
                )
                return@AnimatedContent
            }
            if (page == EditorPage.Interval) {
                DurationWheel(
                    stringResource(R.string.reminder_repeats_every),
                    Duration.ofMinutes(minutes.toLong()),
                    REPEAT_MIN,
                    REPEAT_MAX,
                    onDone = { picked ->
                        minutes = picked.toMinutes().toInt()
                        picking = null
                    },
                )
                return@AnimatedContent
            }
            if (page != null) {
                val label = stringResource(
                    when (page) {
                        EditorPage.At -> R.string.reminder_time
                        EditorPage.From -> R.string.reminder_from
                        else -> R.string.reminder_until
                    },
                )
                val time = when (page) {
                    EditorPage.At -> at
                    EditorPage.From -> from
                    else -> until
                }
                TimeWheel(label, time, onDone = { picked ->
                    when (page) {
                        EditorPage.At -> at = picked
                        EditorPage.From -> from = picked
                        else -> until = picked
                    }
                    picking = null
                })
                return@AnimatedContent
            }
            GlassSurface(Modifier.fillMaxWidth().widthIn(max = 480.dp)) {
                Column(
                    Modifier.verticalScroll(rememberScrollState()).padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Text(
                        stringResource(if (initial == null) R.string.reminder_new else R.string.reminder_edit),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    GlassField(
                        label = stringResource(R.string.reminder_name),
                        value = name,
                        onValueChange = { name = it; nameMissing = false },
                        placeholder = stringResource(R.string.reminder_name_hint),
                        error = if (nameMissing) stringResource(R.string.reminder_name_needed) else null,
                    )
                    GlassField(
                        label = stringResource(R.string.reminder_why),
                        value = why,
                        onValueChange = { why = it },
                        placeholder = stringResource(R.string.reminder_why_hint),
                    )
                    GlassSegmented(
                        options = listOf(stringResource(R.string.reminder_kind_time), stringResource(R.string.reminder_kind_every)),
                        selected = if (every) 1 else 0,
                        onSelect = { every = it == 1 },
                    )
                    if (!every) {
                        TimeButton(stringResource(R.string.reminder_time), at, Modifier.fillMaxWidth()) { picking = EditorPage.At }
                    } else {
                        val repeats = ReminderSchedule.REPEATS
                        GlassSegmented(
                            options = repeats.map { formatDuration(Duration.ofMinutes(it.toLong())) } +
                                stringResource(R.string.reminder_custom),
                            selected = if (custom) repeats.size else repeats.indexOf(minutes).coerceAtLeast(0),
                            onSelect = { i ->
                                if (i < repeats.size) {
                                    custom = false
                                    minutes = repeats[i]
                                } else {
                                    custom = true
                                    picking = EditorPage.Interval
                                }
                            },
                        )
                        if (custom) {
                            ValueButton(
                                stringResource(R.string.reminder_repeats_every),
                                formatDuration(Duration.ofMinutes(minutes.toLong())),
                                Modifier.fillMaxWidth(),
                            ) { picking = EditorPage.Interval }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TimeButton(stringResource(R.string.reminder_from), from, Modifier.weight(1f)) { picking = EditorPage.From }
                            TimeButton(stringResource(R.string.reminder_until), until, Modifier.weight(1f)) { picking = EditorPage.Until }
                        }
                    }
                    SectionLabel(stringResource(R.string.reminder_days))
                    GlassList(Modifier.selectableGroup()) {
                        ReminderDays.entries.forEachIndexed { index, option ->
                            GlassRow(
                                title = stringResource(option.label),
                                divider = index > 0,
                                modifier = Modifier.selectable(selected = option == days, role = Role.RadioButton) {
                                    days = option
                                    if (option == ReminderDays.OnDate) picking = EditorPage.DateA
                                },
                            ) {
                                if (option == days) CheckBadge()
                            }
                        }
                    }
                    if (days == ReminderDays.OnDate) {
                        ValueButton(
                            stringResource(R.string.reminder_date),
                            date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)),
                            Modifier.fillMaxWidth(),
                        ) { picking = EditorPage.DateA }
                    }
                    if (days == ReminderDays.Range) {
                        ValueButton(
                            stringResource(R.string.reminder_date_from),
                            date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)),
                            Modifier.fillMaxWidth(),
                        ) { picking = EditorPage.DateA }
                        ValueButton(
                            stringResource(R.string.reminder_date_to),
                            dateTo.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)),
                            Modifier.fillMaxWidth(),
                        ) { picking = EditorPage.DateB }
                    }
                    // A plain notification, or ringing like an alarm until Stop or Snooze.
                    SectionLabel(stringResource(R.string.reminder_alert))
                    GlassSegmented(
                        options = listOf(stringResource(R.string.reminder_alert_notify), stringResource(R.string.reminder_alert_ring)),
                        selected = if (ring) 1 else 0,
                        onSelect = { ring = it == 1 },
                    )
                    // Its own sound, so a reminder is never mistaken for the alarm's question.
                    ValueButton(stringResource(R.string.reminder_sound), stringResource(sound.label), Modifier.fillMaxWidth()) {
                        picking = EditorPage.Sound
                    }
                    Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Save and Cancel are the same glass button, side by side.
                        GlassButton(
                            stringResource(R.string.reminder_save),
                            {
                                if (name.isBlank()) {
                                    nameMissing = true
                                } else {
                                    val schedule = if (every) ReminderSchedule.Every(minutes, from, until) else ReminderSchedule.Daily(at)
                                    val (firstDay, lastDay) = days.span(today, date, dateTo)
                                    val reminder = Reminder(
                                        id = initial?.id ?: System.currentTimeMillis(),
                                        name = name.trim(),
                                        why = why.trim(),
                                        schedule = schedule,
                                        on = initial?.on ?: true,
                                        sound = sound.name,
                                        firstDay = firstDay,
                                        lastDay = lastDay,
                                        ring = ring,
                                    )
                                    // A reminder for today (or one day) whose time has passed would never come: say so instead.
                                    if (ReminderTimes.hasTimeLeft(reminder, LocalDateTime.now())) {
                                        onSave(reminder)
                                    } else {
                                        Toast.makeText(context, R.string.reminder_time_passed, Toast.LENGTH_LONG).show()
                                    }
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

/** A glass button showing "[label]  9:00 pm", for opening the time wheel. */
@Composable
private fun TimeButton(label: String, time: LocalTime, modifier: Modifier = Modifier, onClick: () -> Unit) =
    ValueButton(label, clock(time), modifier, onClick)

/**
 * Choosing the reminder's sound: Dawnly's sounds in a glass list. Tapping one picks it and plays
 * it, so the user hears what their reminder will sound like.
 */
@Composable
private fun SoundChoice(selected: AlertSound, onPick: (AlertSound) -> Unit, onDone: () -> Unit) {
    val preview = rememberSoundPreview()
    GlassSurface(Modifier.fillMaxWidth().widthIn(max = 480.dp)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(stringResource(R.string.reminder_sound), style = MaterialTheme.typography.titleMedium)
            Column(Modifier.heightIn(max = 380.dp).verticalScroll(rememberScrollState())) {
                GlassList(Modifier.selectableGroup()) {
                    AlertSound.entries.forEachIndexed { index, sound ->
                        GlassRow(
                            title = stringResource(sound.label),
                            divider = index > 0,
                            modifier = Modifier.selectable(selected = sound == selected, role = Role.RadioButton) {
                                onPick(sound)
                                preview.play(sound)
                            },
                        ) {
                            if (sound == selected) CheckBadge()
                        }
                    }
                }
            }
            GlassButton(
                stringResource(R.string.reminder_done),
                {
                    preview.stop()
                    onDone()
                },
                Modifier.fillMaxWidth(),
            )
        }
    }
}

/** A custom repeat: from a minute up to 23 hours 59 minutes. */
private val REPEAT_MIN: Duration = Duration.ofMinutes(1)
private val REPEAT_MAX: Duration = Duration.ofHours(23).plusMinutes(59)
