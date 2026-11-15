package com.dawnly.app.ui.home

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dawnly.app.R
import com.dawnly.app.data.sound.AlertSound
import com.dawnly.app.domain.alarm.DawnlyAlarm
import com.dawnly.app.ui.format.clock
import com.dawnly.app.ui.glass.GlassButton
import com.dawnly.app.ui.glass.GlassField
import com.dawnly.app.ui.glass.GlassShapes
import com.dawnly.app.ui.glass.GlassSurface
import com.dawnly.app.ui.glass.SectionLabel
import com.dawnly.app.ui.glass.TextAction
import com.dawnly.app.ui.glass.TimeWheel
import com.dawnly.app.ui.glass.ValueButton
import com.dawnly.app.ui.theme.DawnlyTheme
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.TextStyle

/** The card's other pages: the time on the wheels, or the alarm's sound. */
private enum class AlarmPage { Time, Sound }

/**
 * Adding or changing a Dawnly alarm, drawn over Today like the reminder editor: a glass card with
 * the time on glass wheels, a label, repeat-day chips and a sound. [initial] null makes a new one;
 * [onUseClock] offers the Clock app instead, and [onDelete] removes an existing one.
 */
@Composable
internal fun DawnlyAlarmEditor(
    initial: DawnlyAlarm?,
    onSave: (DawnlyAlarm) -> Unit,
    onDelete: ((Long) -> Unit)?,
    onUseClock: (() -> Unit)?,
    onClose: () -> Unit,
) {
    var label by remember { mutableStateOf(initial?.label.orEmpty()) }
    var time by remember { mutableStateOf(initial?.time ?: DEFAULT_ALARM) }
    var days by remember { mutableStateOf(initial?.days ?: emptySet()) }
    var sound by remember {
        mutableStateOf(
            initial?.sound?.let { name -> AlertSound.entries.firstOrNull { it.name == name } } ?: AlertSound.PhoneDefault,
        )
    }
    var picking by remember { mutableStateOf<AlarmPage?>(null) }
    val defaultLabel = stringResource(R.string.alarm_tomorrow_name)

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
            label = "alarmPage",
            // Taps on the card stay on the card.
            modifier = Modifier.clickable(remember { MutableInteractionSource() }, indication = null) {},
        ) { page ->
            when (page) {
                AlarmPage.Time -> TimeWheel(stringResource(R.string.alarm_pick_label), time, onDone = {
                    time = it
                    picking = null
                })
                AlarmPage.Sound -> SoundChoice(sound, onPick = { sound = it }, onDone = { picking = null })
                null -> GlassSurface(Modifier.fillMaxWidth().widthIn(max = 480.dp)) {
                    Column(
                        Modifier.verticalScroll(rememberScrollState()).padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Text(
                            stringResource(if (initial == null) R.string.alarm_new else R.string.alarm_edit),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        ValueButton(stringResource(R.string.reminder_time), clock(time), Modifier.fillMaxWidth()) {
                            picking = AlarmPage.Time
                        }
                        GlassField(
                            label = stringResource(R.string.alarm_label),
                            value = label,
                            onValueChange = { label = it },
                            placeholder = stringResource(R.string.alarm_label_hint),
                        )
                        SectionLabel(stringResource(R.string.alarm_repeat))
                        DayChips(days, onDays = { days = it })
                        ValueButton(stringResource(R.string.reminder_sound), stringResource(sound.label), Modifier.fillMaxWidth()) {
                            picking = AlarmPage.Sound
                        }
                        Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            GlassButton(
                                stringResource(R.string.reminder_save),
                                {
                                    onSave(
                                        DawnlyAlarm(
                                            id = initial?.id ?: System.currentTimeMillis(),
                                            label = label.trim().ifBlank { defaultLabel },
                                            time = time,
                                            // All seven picked is every day, kept as the empty set.
                                            days = if (days.size == 7) emptySet() else days,
                                            sound = sound.takeIf { it != AlertSound.PhoneDefault }?.name,
                                            on = initial?.on ?: true,
                                        ),
                                    )
                                },
                                Modifier.weight(1f),
                            )
                            GlassButton(stringResource(R.string.common_cancel), onClose, Modifier.weight(1f))
                        }
                        if (initial != null && onDelete != null) {
                            TextAction(
                                stringResource(R.string.alarm_delete),
                                { onDelete(initial.id) },
                                Modifier.align(Alignment.CenterHorizontally),
                            )
                        }
                        if (onUseClock != null) {
                            TextAction(
                                stringResource(R.string.alarm_use_clock),
                                onUseClock,
                                Modifier.align(Alignment.CenterHorizontally),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** "Every day" plus a chip per weekday; tapping a day toggles it, "Every day" clears the picks. */
@Composable
private fun DayChips(days: Set<DayOfWeek>, onDays: (Set<DayOfWeek>) -> Unit) {
    val locale = LocalConfiguration.current.locales[0]
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DayChip(
            text = stringResource(R.string.reminder_days_every_day),
            selected = days.isEmpty() || days.size == 7,
            onClick = { onDays(emptySet()) },
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            DayOfWeek.entries.forEach { day ->
                DayChip(
                    text = day.getDisplayName(TextStyle.SHORT, locale),
                    selected = day in days,
                    onClick = { onDays(if (day in days) days - day else days + day) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** One repeat-day chip: brighter glass and accent words when picked, like a segmented choice. */
@Composable
private fun DayChip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    GlassSurface(
        modifier = modifier,
        shape = GlassShapes.Segment,
        strong = selected,
        onClick = onClick,
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) DawnlyTheme.colors.accent else DawnlyTheme.colors.text,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp),
        )
    }
}

/** Where the wheels start: a usual morning alarm. */
private val DEFAULT_ALARM: LocalTime = LocalTime.of(7, 0)