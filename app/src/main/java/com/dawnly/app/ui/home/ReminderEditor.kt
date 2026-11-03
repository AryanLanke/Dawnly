package com.dawnly.app.ui.home

import android.text.format.DateFormat
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dawnly.app.R
import com.dawnly.app.domain.reminder.Reminder
import com.dawnly.app.domain.reminder.ReminderSchedule
import com.dawnly.app.ui.format.formatDuration
import com.dawnly.app.ui.glass.GlassButton
import com.dawnly.app.ui.glass.GlassSegmented
import com.dawnly.app.ui.glass.GlassShapes
import com.dawnly.app.ui.glass.GlassSurface
import com.dawnly.app.ui.glass.SectionLabel
import com.dawnly.app.ui.glass.TextAction
import com.dawnly.app.ui.glass.rememberHaptics
import com.dawnly.app.ui.theme.DawnlyTheme
import java.time.Duration
import java.time.LocalTime
import kotlin.math.abs

/** Which time the wheel is setting. */
private enum class TimeField { At, From, Until }

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
    var from by remember { mutableStateOf((start as? ReminderSchedule.Every)?.from ?: ReminderSchedule.DAY_START) }
    var until by remember { mutableStateOf((start as? ReminderSchedule.Every)?.until ?: ReminderSchedule.DAY_END) }
    var picking by remember { mutableStateOf<TimeField?>(null) }

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
        ) { field ->
            if (field != null) {
                val label = stringResource(
                    when (field) {
                        TimeField.At -> R.string.reminder_time
                        TimeField.From -> R.string.reminder_from
                        TimeField.Until -> R.string.reminder_until
                    },
                )
                val time = when (field) {
                    TimeField.At -> at
                    TimeField.From -> from
                    TimeField.Until -> until
                }
                TimeWheel(label, time, onDone = { picked ->
                    when (field) {
                        TimeField.At -> at = picked
                        TimeField.From -> from = picked
                        TimeField.Until -> until = picked
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
                        TimeButton(stringResource(R.string.reminder_time), at, Modifier.fillMaxWidth()) { picking = TimeField.At }
                    } else {
                        val repeats = ReminderSchedule.REPEATS
                        GlassSegmented(
                            options = repeats.map { formatDuration(Duration.ofMinutes(it.toLong())) },
                            selected = repeats.indexOf(minutes).coerceAtLeast(0),
                            onSelect = { minutes = repeats[it] },
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TimeButton(stringResource(R.string.reminder_from), from, Modifier.weight(1f)) { picking = TimeField.From }
                            TimeButton(stringResource(R.string.reminder_until), until, Modifier.weight(1f)) { picking = TimeField.Until }
                        }
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

/** A text field made of glass: a soft label above, a rounded translucent pane, and a hint inside. */
@Composable
private fun GlassField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    error: String? = null,
) {
    val colors = DawnlyTheme.colors
    Column {
        SectionLabel(label)
        GlassSurface(Modifier.fillMaxWidth(), shape = GlassShapes.Button) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.text),
                cursorBrush = SolidColor(colors.accent),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { field ->
                    Box(Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
                        if (value.isEmpty()) {
                            Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = colors.textSoft.copy(alpha = 0.6f))
                        }
                        field()
                    }
                },
            )
        }
        if (error != null) {
            Text(error, style = MaterialTheme.typography.bodySmall, color = colors.tick, modifier = Modifier.padding(start = 8.dp, top = 6.dp))
        }
    }
}

/** A glass button showing "[label]  9:00 pm", for opening the time wheel. */
@Composable
private fun TimeButton(label: String, time: LocalTime, modifier: Modifier = Modifier, onClick: () -> Unit) {
    GlassSurface(modifier = modifier, shape = GlassShapes.Button, onClick = onClick) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = DawnlyTheme.colors.textSoft, modifier = Modifier.weight(1f))
            Text(clock(time), style = MaterialTheme.typography.bodyLarge)
        }
    }
}

/** Height of one row of the wheels. */
private val WheelRow = 58.dp

/**
 * Picking a time, laid out like Today's alarm: a glass circle with a small [label] on top and the
 * time in big light digits. The hour, minutes and, on a 12-hour phone, am/pm are wheels: scroll
 * them, and the one in the middle is the time. A tick as each number passes the middle.
 */
@Composable
private fun TimeWheel(label: String, time: LocalTime, onDone: (LocalTime) -> Unit) {
    val twelveHour = !DateFormat.is24HourFormat(LocalContext.current)
    var hour by remember { mutableStateOf(time.hour) }
    var minute by remember { mutableStateOf(time.minute) }
    // Today's alarm digits, a little smaller so three rows fit the circle.
    val digits = MaterialTheme.typography.displayLarge.copy(fontSize = 50.sp, lineHeight = 58.sp, textAlign = TextAlign.Center)
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        GlassSurface(
            modifier = Modifier.fillMaxWidth(0.86f).widthIn(max = 340.dp).aspectRatio(1f),
            shape = CircleShape,
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(label, style = MaterialTheme.typography.bodyMedium, color = DawnlyTheme.colors.textSoft)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (twelveHour) {
                        // 12, 1, 2 ... 11.
                        Wheel(12, (hour % 12), 86.dp, digits, { i -> if (i == 0) "12" else "$i" }) { i ->
                            hour = i + if (hour >= 12) 12 else 0
                        }
                    } else {
                        Wheel(24, hour, 86.dp, digits, { i -> "%02d".format(i) }) { hour = it }
                    }
                    Text(":", style = digits, modifier = Modifier.padding(bottom = 6.dp))
                    Wheel(60, minute, 86.dp, digits, { i -> "%02d".format(i) }) { minute = it }
                    if (twelveHour) {
                        val marks = listOf(stringResource(R.string.time_am), stringResource(R.string.time_pm))
                        Wheel(
                            2,
                            if (hour >= 12) 1 else 0,
                            44.dp,
                            MaterialTheme.typography.titleMedium.copy(color = DawnlyTheme.colors.textSoft, textAlign = TextAlign.Center),
                            { marks[it] },
                        ) { pm -> hour = hour % 12 + if (pm == 1) 12 else 0 }
                    }
                }
            }
        }
        GlassButton(stringResource(R.string.reminder_done), { onDone(LocalTime.of(hour, minute)) }, Modifier.width(200.dp))
    }
}

/**
 * One wheel of [count] values, three rows tall, snapping so a value sits in the middle. Values
 * fade and shrink away from the middle, done while drawing so scrolling doesn't recompose.
 */
@Composable
private fun Wheel(
    count: Int,
    selected: Int,
    width: Dp,
    style: TextStyle,
    label: (Int) -> String,
    onSelect: (Int) -> Unit,
) {
    val state = rememberLazyListState(initialFirstVisibleItemIndex = selected)
    val haptic = rememberHaptics()
    val rowPx = with(LocalDensity.current) { WheelRow.toPx() }
    val middle by remember { derivedStateOf { centred(state, rowPx) } }
    var first by remember { mutableStateOf(true) }
    LaunchedEffect(middle) {
        if (first) first = false else haptic.tick()
        onSelect(middle.coerceIn(0, count - 1))
    }
    LazyColumn(
        state = state,
        flingBehavior = rememberSnapFlingBehavior(state),
        contentPadding = PaddingValues(vertical = WheelRow),
        modifier = Modifier.width(width).height(WheelRow * 3),
    ) {
        items(count) { i ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(WheelRow)
                    .graphicsLayer {
                        val info = state.layoutInfo
                        val item = info.visibleItemsInfo.firstOrNull { it.index == i }
                        val away = if (item == null) 1f else {
                            val centre = (info.viewportStartOffset + info.viewportEndOffset) / 2f
                            (abs(item.offset + item.size / 2f - centre) / rowPx).coerceAtMost(1f)
                        }
                        alpha = 1f - 0.75f * away
                        scaleX = 1f - 0.3f * away
                        scaleY = scaleX
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(label(i), style = style)
            }
        }
    }
}

/** The index of the row nearest the middle of the wheel. */
private fun centred(state: LazyListState, rowPx: Float): Int =
    state.firstVisibleItemIndex + if (state.firstVisibleItemScrollOffset > rowPx / 2) 1 else 0
