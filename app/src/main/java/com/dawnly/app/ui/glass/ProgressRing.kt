package com.dawnly.app.ui.glass

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.dawnly.app.ui.theme.DawnlyMotion
import com.dawnly.app.ui.theme.DawnlyTheme

/**
 * A glass circle with a thin ring showing how much of the watch window has passed. It fills from
 * empty when it first appears, then glides to each new value.
 *
 * @param progress 0 to 1, or null to show only the empty track.
 */
@Composable
fun ProgressRing(
    progress: Float?,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = DawnlyTheme.colors
    val animated = remember { Animatable(0f) }
    val spec = DawnlyMotion.gentle<Float>()
    LaunchedEffect(progress) { animated.animateTo(progress ?: 0f, spec) }

    GlassSurface(
        modifier = modifier.aspectRatio(1f),
        shape = CircleShape,
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            // The mockup's ring: radius 96 in a 210 circle, stroke 5.
            val stroke = 5.dp.toPx()
            val inset = size.minDimension * (9f / 210f)
            val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
            val topLeft = Offset(inset, inset)
            drawArc(colors.track, 0f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
            val sweep = animated.value * 360f
            if (sweep > 0f) {
                drawArc(colors.accent, -90f, sweep, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            }
        }
        Box(contentAlignment = Alignment.Center, content = content)
    }
}
