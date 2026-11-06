package com.dawnly.app.ui.glass

import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dawnly.app.R
import com.dawnly.app.ui.theme.DawnlyTheme
import java.time.Duration
import kotlin.math.abs

/** The big light digits on the wheels: Today's alarm digits, a little smaller so three rows fit the circle. */
@Composable
fun wheelDigits(): TextStyle =
    MaterialTheme.typography.displayLarge.copy(fontSize = 50.sp, lineHeight = 58.sp, textAlign = TextAlign.Center)

/** The glass circle the wheels sit in, like Today's alarm, with a small [label] on top. */
@Composable
fun WheelCircle(label: String, wheels: @Composable () -> Unit) {
    GlassSurface(
        modifier = Modifier.fillMaxWidth(0.86f).widthIn(max = 340.dp).aspectRatio(1f),
        shape = CircleShape,
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = DawnlyTheme.colors.textSoft)
            Row(verticalAlignment = Alignment.CenterVertically) { wheels() }
        }
    }
}

/**
 * Picking a length of time in the glass circle: an hours wheel and a minutes wheel, like 0 hr 05
 * min or 2 hr 00 min, and Done. The hours wheel goes up to [max]'s hours; Done keeps the time
 * between [min] and [max]. Used for a reminder's custom repeat and how early Dawnly starts watching.
 */
@Composable
fun DurationWheel(label: String, value: Duration, min: Duration, max: Duration, onDone: (Duration) -> Unit) {
    val maxHours = max.toHours().toInt()
    val start = value.coerceIn(min, max)
    var hours by remember { mutableStateOf(start.toHours().toInt()) }
    var mins by remember { mutableStateOf((start.toMinutes() % 60).toInt()) }
    val digits = wheelDigits()
    val unit = MaterialTheme.typography.titleMedium.copy(color = DawnlyTheme.colors.textSoft)
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        WheelCircle(label) {
            Wheel(maxHours + 1, hours, 72.dp, digits, { i -> "$i" }) { hours = it }
            Text(stringResource(R.string.wheel_unit_hr), style = unit, modifier = Modifier.padding(end = 6.dp))
            Wheel(60, mins, 72.dp, digits, { i -> "%02d".format(i) }) { mins = it }
            Text(stringResource(R.string.wheel_unit_min), style = unit)
        }
        GlassButton(
            stringResource(R.string.reminder_done),
            { onDone(Duration.ofMinutes(hours * 60L + mins).coerceIn(min, max)) },
            Modifier.width(200.dp),
        )
    }
}

/** Height of one row of the wheels. */
private val WheelRow = 58.dp

/**
 * One wheel of [count] values, three rows tall, snapping so a value sits in the middle. Values
 * fade and shrink away from the middle, done while drawing so scrolling doesn't recompose. A tick
 * as each value passes the middle.
 */
@Composable
fun Wheel(
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
