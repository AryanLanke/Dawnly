package com.dawnly.app.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.dawnly.app.R
import com.dawnly.app.domain.reminder.Reminder
import com.dawnly.app.domain.reminder.ReminderDays
import com.dawnly.app.domain.reminder.ReminderSchedule
import com.dawnly.app.ui.format.clock
import com.dawnly.app.ui.format.formatDuration
import com.dawnly.app.ui.glass.GlassIcons
import com.dawnly.app.ui.glass.GlassList
import com.dawnly.app.ui.glass.GlassRow
import com.dawnly.app.ui.glass.GlassSwitch
import com.dawnly.app.ui.glass.SectionLabel
import com.dawnly.app.ui.theme.DawnlyTheme
import java.time.Duration
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

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
                    if (reminder.ring) {
                        Icon(
                            GlassIcons.AlarmClock,
                            contentDescription = stringResource(R.string.reminder_rings_description),
                            tint = DawnlyTheme.colors.textSoft,
                            modifier = Modifier.padding(end = 10.dp).size(18.dp),
                        )
                    }
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
    val first = reminder.firstDay
    val last = reminder.lastDay
    val today = LocalDate.now()
    val f = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
    val days = when (ReminderDays.of(first, last, today)) {
        ReminderDays.EveryDay -> null
        ReminderDays.Today -> stringResource(R.string.reminder_days_today)
        ReminderDays.Tomorrow -> stringResource(R.string.reminder_days_tomorrow)
        ReminderDays.Both -> stringResource(R.string.reminder_days_both)
        ReminderDays.OnDate -> stringResource(R.string.reminder_on_date, first!!.format(f))
        ReminderDays.Range -> stringResource(R.string.reminder_range, first!!.format(f), last!!.format(f))
    }
    val head = if (days == null) schedule else "$schedule · $days"
    return if (reminder.why.isBlank()) head else "$head · ${reminder.why}"
}

