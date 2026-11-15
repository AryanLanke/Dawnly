package com.dawnly.app.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.dawnly.app.R
import com.dawnly.app.domain.alarm.DawnlyAlarm
import com.dawnly.app.ui.format.clock
import com.dawnly.app.ui.glass.GlassIcons
import com.dawnly.app.ui.glass.GlassList
import com.dawnly.app.ui.glass.GlassRow
import com.dawnly.app.ui.glass.GlassSwitch
import com.dawnly.app.ui.glass.SectionLabel
import com.dawnly.app.ui.theme.DawnlyTheme
import java.time.DayOfWeek
import java.time.format.TextStyle

/**
 * The user's own alarms on Today: each with its time and repeat, a switch, tap one to change it,
 * and a row to add one. While a Clock alarm is the next one, a row offers to move it into Dawnly.
 * [onEdit] with null starts a new one.
 */
@Composable
internal fun DawnlyAlarmsCard(
    alarms: List<DawnlyAlarm>,
    canImportClock: Boolean,
    onEdit: (DawnlyAlarm?) -> Unit,
    onToggle: (DawnlyAlarm, Boolean) -> Unit,
    onImportClock: () -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        SectionLabel(stringResource(R.string.dawnly_alarms_label))
        GlassList {
            alarms.forEachIndexed { index, alarm ->
                GlassRow(
                    title = alarm.label,
                    subtitle = "${clock(alarm.time)} · ${repeatSummary(alarm.days)}",
                    divider = index > 0,
                    modifier = Modifier.clickable(role = Role.Button) { onEdit(alarm) },
                ) {
                    GlassSwitch(alarm.on, { onToggle(alarm, it) })
                }
            }
            GlassRow(
                title = stringResource(R.string.home_add_alarm),
                subtitle = if (alarms.isEmpty()) stringResource(R.string.dawnly_alarm_add_hint) else null,
                divider = alarms.isNotEmpty(),
                modifier = Modifier.clickable(role = Role.Button) { onEdit(null) },
            ) {
                Icon(GlassIcons.Plus, contentDescription = null, tint = DawnlyTheme.colors.textSoft, modifier = Modifier.size(20.dp))
            }
            if (canImportClock) {
                GlassRow(
                    title = stringResource(R.string.alarm_import_title),
                    subtitle = stringResource(R.string.alarm_import_hint),
                    divider = true,
                    modifier = Modifier.clickable(role = Role.Button) { onImportClock() },
                ) {
                    Icon(
                        GlassIcons.AlarmClock,
                        contentDescription = null,
                        tint = DawnlyTheme.colors.textSoft,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

/** "Every day", "Weekdays", "Weekends", or the picked days: "Mon, Wed, Fri". */
@Composable
private fun repeatSummary(days: Set<DayOfWeek>): String = when {
    days.isEmpty() || days.size == 7 -> stringResource(R.string.reminder_days_every_day)
    days == WEEKDAYS -> stringResource(R.string.alarm_days_weekdays)
    days == WEEKEND -> stringResource(R.string.alarm_days_weekends)
    else -> {
        val locale = LocalConfiguration.current.locales[0]
        days.sorted().joinToString(", ") { it.getDisplayName(TextStyle.SHORT, locale) }
    }
}

private val WEEKDAYS = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)
private val WEEKEND = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)