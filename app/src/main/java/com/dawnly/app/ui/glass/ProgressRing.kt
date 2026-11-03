package com.dawnly.app.ui.glass

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.dawnly.app.ui.theme.DawnlyMotion
import com.dawnly.app.ui.theme.DawnlyTheme
import com.dawnly.app.ui.theme.LocalReduceMotion
import java.time.Instant

/**
 * A glass circle with a ring showing how much of the watch window has passed, drawn as light blue
 * water flowing round it: translucent, with a soft glow, a glossy line along it, and brighter
 * ripples drifting forward. It works out its own progress from the clock every frame, so it fills
 * smoothly instead of stepping, and fills from empty when it first appears.
 *
 * @param startedAt when the watch window opened, or null to show only the empty track.
 * @param endsAt when it closes (the alarm).
 */
@Composable
fun ProgressRing(
    startedAt: Instant?,
    endsAt: Instant?,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = DawnlyTheme.colors
    val still = LocalReduceMotion.current
    val intro = remember { Animatable(0f) }
    val spec = DawnlyMotion.gentle<Float>()
    val now = remember { mutableLongStateOf(System.currentTimeMillis()) }
    val watching = startedAt != null && endsAt != null
    LaunchedEffect(watching) {
        intro.animateTo(if (watching) 1f else 0f, spec)
    }
    LaunchedEffect(watching) {
        // Read only while drawing, so the ring redraws every frame without recomposing.
        while (watching) withFrameMillis { now.longValue = System.currentTimeMillis() }
    }
    val water = if (colors.isDark) DarkWater else LightWater

    GlassSurface(
        modifier = modifier.aspectRatio(1f),
        shape = CircleShape,
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            // The mockup's ring: radius 96 in a 210 circle, stroke 5.
            val stroke = 5.dp.toPx()
            val inset = size.minDimension * (9f / 210f)
            val radius = size.minDimension / 2 - inset
            drawCircle(colors.track, radius = radius, style = Stroke(stroke))
            val progress = if (startedAt != null && endsAt != null) {
                windowProgress(startedAt.toEpochMilli(), endsAt.toEpochMilli(), now.longValue) * intro.value
            } else {
                0f
            }
            if (progress > 0f) drawWater(progress, radius, stroke, water, if (still) 0L else now.longValue)
        }
        Box(contentAlignment = Alignment.Center, content = content)
    }
}

/** How much of the window from [start] to [end] has passed at [now], from 0 to 1. */
private fun windowProgress(start: Long, end: Long, now: Long): Float {
    val total = end - start
    if (total <= 0) return 1f
    return ((now - start).toFloat() / total).coerceIn(0f, 1f)
}

/** The water's colours: its body from tail to head, the glossy line, the glow and the ripples. */
private class Water(val body: List<Color>, val shine: Color, val glow: Color, val ripple: Color)

private val LightWater = Water(
    body = listOf(Color(0xFFBFE6FF).copy(alpha = 0.6f), Color(0xFF8CCBFF).copy(alpha = 0.8f)),
    shine = Color.White.copy(alpha = 0.65f),
    glow = Color(0xFF9CD3FF).copy(alpha = 0.22f),
    ripple = Color.White.copy(alpha = 0.55f),
)

private val DarkWater = Water(
    body = listOf(Color(0xFFB8E8FF).copy(alpha = 0.4f), Color(0xFF9EDBFF).copy(alpha = 0.7f)),
    shine = Color.White.copy(alpha = 0.5f),
    glow = Color(0xFF9EDBFF).copy(alpha = 0.16f),
    ripple = Color.White.copy(alpha = 0.4f),
)

/** Bright ripples round the whole circle, and how long the water takes to flow once round. */
private const val RIPPLES = 5
private const val FLOW_MILLIS = 9_000L

/**
 * The filled part of the ring, from the top clockwise over [progress] of the circle. On top of the
 * water, soft brighter ripples drift forward as [time] passes, so it looks like it's flowing.
 */
private fun DrawScope.drawWater(progress: Float, radius: Float, stroke: Float, water: Water, time: Long) {
    val sweep = progress * 360f
    val topLeft = Offset(center.x - radius, center.y - radius)
    val arc = Size(radius * 2, radius * 2)
    // Angles from 3 o'clock; turn everything so the water starts at the top.
    rotate(-90f, pivot = center) {
        drawArc(water.glow, 0f, sweep, false, topLeft, arc, style = Stroke(stroke * 2.6f, cap = StrokeCap.Round))
        val body = Brush.sweepGradient(0f to water.body[0], progress to water.body[1], 1f to water.body[1], center = center)
        drawArc(body, 0f, sweep, false, topLeft, arc, style = Stroke(stroke, cap = StrokeCap.Round))
        // The ripples: a ring of soft bright bands, turned a little further each frame. The arc is
        // drawn turned back by the same amount, so only the bands move.
        val turn = (time % FLOW_MILLIS) / FLOW_MILLIS.toFloat() * 360f
        rotate(turn, pivot = center) {
            val stops = buildList {
                for (i in 0 until RIPPLES) {
                    val at = i / RIPPLES.toFloat()
                    val width = 1f / RIPPLES
                    add(at to Color.Transparent)
                    add(at + width * 0.5f to water.ripple)
                    add(at + width to Color.Transparent)
                }
            }.toTypedArray()
            drawArc(
                Brush.sweepGradient(*stops, center = center),
                -turn,
                sweep,
                false,
                topLeft,
                arc,
                style = Stroke(stroke * 0.8f, cap = StrokeCap.Round),
            )
        }
        drawArc(water.shine, 0f, sweep, false, topLeft, arc, style = Stroke(stroke * 0.25f, cap = StrokeCap.Round))
    }
}

/**
 * The ring's straight sibling: a thin rounded bar on the same track and accent colours. It fills
 * from empty when it first appears, then glides to each new value.
 *
 * @param progress 0 to 1.
 */
@Composable
fun ProgressBar(progress: Float, modifier: Modifier = Modifier) {
    val colors = DawnlyTheme.colors
    val animated = remember { Animatable(0f) }
    val spec = DawnlyMotion.gentle<Float>()
    LaunchedEffect(progress) { animated.animateTo(progress.coerceIn(0f, 1f), spec) }

    Canvas(modifier.fillMaxWidth().height(5.dp)) {
        val radius = CornerRadius(size.height / 2)
        drawRoundRect(colors.track, cornerRadius = radius)
        // Never thinner than a dot, so a sliver of progress still reads as round.
        val width = (size.width * animated.value).coerceAtLeast(if (animated.value > 0f) size.height else 0f)
        if (width > 0f) drawRoundRect(colors.accent, size = Size(width, size.height), cornerRadius = radius)
    }
}
