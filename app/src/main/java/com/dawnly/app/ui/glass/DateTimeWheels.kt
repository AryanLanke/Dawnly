package com.dawnly.app.ui.glass

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dawnly.app.R
import com.dawnly.app.ui.theme.DawnlyTheme
import java.time.LocalDate
import java.time.LocalTime

/**
 * Picking a time, laid out like Today's alarm: a glass circle with a small [label] on top and the
 * time in big light digits. The hour, minutes and, on a 12-hour phone, am/pm are wheels: scroll
 * them, and the one in the middle is the time. A tick as each number passes the middle.
 */
@Composable
fun TimeWheel(label: String, time: LocalTime, onDone: (LocalTime) -> Unit) {
    val twelveHour = !DateFormat.is24HourFormat(LocalContext.current)
    var hour by remember { mutableStateOf(time.hour) }
    var minute by remember { mutableStateOf(time.minute) }
    val digits = wheelDigits()
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        WheelCircle(label) {
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
        GlassButton(stringResource(R.string.reminder_done), { onDone(LocalTime.of(hour, minute)) }, Modifier.width(200.dp))
    }
}

/**
 * Picking a date, like the time wheel: a glass circle with a small [label] and day, month and year
 * on wheels. The year wheel starts at this year ([today]) or the date's own, if earlier.
 */
@Composable
fun DateWheel(label: String, date: LocalDate, today: LocalDate, onDone: (LocalDate) -> Unit) {
    var day by remember { mutableStateOf(date.dayOfMonth) }
    var month by remember { mutableStateOf(date.monthValue) }
    val firstYear = minOf(today.year, date.year)
    var year by remember { mutableStateOf(date.year) }
    val digits = wheelDigits()
    val months = remember { java.time.Month.entries.map { it.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.getDefault()) } }
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        WheelCircle(label) {
            Wheel(31, day - 1, 70.dp, digits, { i -> "${i + 1}" }) { day = it + 1 }
            Wheel(12, month - 1, 96.dp, digits, { i -> months[i] }) { month = it + 1 }
            Wheel(6, year - firstYear, 110.dp, digits, { i -> "${firstYear + i}" }) { year = firstYear + it }
        }
        GlassButton(
            stringResource(R.string.reminder_done),
            { onDone(LocalDate.of(year, month, day.coerceAtMost(java.time.YearMonth.of(year, month).lengthOfMonth()))) },
            Modifier.width(200.dp),
        )
    }
}
