package com.dawnly.app.ui.glass

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dawnly.app.R
import com.dawnly.app.ui.format.formatDuration
import java.time.Duration

/**
 * Picking a custom length of time on glass: minus, the time, plus, and a slider under them. The
 * slider stops only at [stops] (shortest first); minus and plus move by [step], within the first
 * and last stop. While dragging, the time follows the finger and [onChange] comes on letting go,
 * so a swipe is one change rather than dozens. [onDrag] reports the time under the finger, or
 * null once let go. Used for how early Dawnly starts watching and for a reminder's custom repeat.
 */
@Composable
fun GlassDurationPicker(
    value: Duration,
    stops: List<Duration>,
    step: Duration,
    sliderLabel: String,
    onChange: (Duration) -> Unit,
    modifier: Modifier = Modifier,
    onDrag: (Duration?) -> Unit = {},
) {
    val min = stops.first()
    val max = stops.last()
    val last = stops.lastIndex
    // The nearest stop to a saved time.
    fun toStep(d: Duration) = stops.indices.minBy { (stops[it] - d).abs() }
    fun toDuration(index: Int): Duration = stops[index.coerceIn(0, last)]
    var dragging by remember { mutableStateOf<Int?>(null) }
    val shown = dragging?.let(::toDuration) ?: value
    GlassSurface(modifier.fillMaxWidth()) {
        Column(Modifier.padding(10.dp)) {
            GlassStepper(
                value = formatDuration(shown),
                onDecrease = { onChange((value - step).coerceIn(min, max)) },
                onIncrease = { onChange((value + step).coerceIn(min, max)) },
                decreaseEnabled = value > min,
                increaseEnabled = value < max,
                decreaseLabel = stringResource(R.string.settings_shorter),
                increaseLabel = stringResource(R.string.settings_longer),
            )
            GlassSlider(
                value = dragging ?: toStep(value),
                max = last,
                onValueChange = {
                    dragging = it
                    onDrag(toDuration(it))
                },
                onValueChangeFinished = {
                    dragging?.let { onChange(toDuration(it)) }
                    dragging = null
                    onDrag(null)
                },
                label = sliderLabel,
                modifier = Modifier.padding(horizontal = 6.dp),
            )
        }
    }
}
