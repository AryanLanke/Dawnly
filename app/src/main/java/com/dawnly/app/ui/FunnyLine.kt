package com.dawnly.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dawnly.app.R
import com.dawnly.app.ui.glass.GlassShapes
import com.dawnly.app.ui.theme.DawnlyTheme
import kotlin.random.Random

/**
 * A light line above the tabs, a different one each time Dawnly opens. Tap it for another.
 * [shown] is how visible it is, read only while drawing so a swipe doesn't recompose it.
 */
@Composable
internal fun FunnyLine(shown: () -> Float, tappable: Boolean, modifier: Modifier = Modifier) {
    val lines = stringArrayResource(R.array.funny_lines)
    var index by rememberSaveable { mutableIntStateOf(Random.nextInt(lines.size)) }
    AnimatedContent(
        targetState = index,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "funnyLine",
        modifier = modifier.graphicsLayer { alpha = shown() },
    ) { line ->
        Text(
            lines[line % lines.size],
            style = MaterialTheme.typography.bodySmall,
            color = DawnlyTheme.colors.textSoft,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .clip(GlassShapes.Pill)
                .clickable(enabled = tappable, onClickLabel = stringResource(R.string.funny_line_next)) {
                    // Any other line, never the same one twice in a row.
                    index = (index + 1 + Random.nextInt(lines.size - 1)) % lines.size
                }
                .padding(horizontal = 14.dp, vertical = 6.dp),
        )
    }
}
