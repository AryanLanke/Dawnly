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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.dawnly.app.ui.theme.DawnlyMotion
import com.dawnly.app.ui.theme.DawnlyTheme
import com.dawnly.app.ui.theme.LocalReduceMotion
import java.time.Instant
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * A glass circle with a ring showing how much of the watch window has passed, drawn as a stream of
 * water: translucent blue with a glossy line along it and a soft glow, with a gentle wave that
 * flows forward like a snake. It works out its own progress from the clock every frame, so it
 * fills smoothly instead of stepping, and fills from empty when it first appears.
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

/** The water's colours: its body from tail to head, the glossy line on top, and the glow. */
private class Water(val body: List<Color>, val shine: Color, val glow: Color)

private val LightWater = Water(
    body = listOf(Color(0xFF7CC8FF).copy(alpha = 0.55f), Color(0xFF2F7BFF).copy(alpha = 0.75f)),
    shine = Color.White.copy(alpha = 0.6f),
    glow = Color(0xFF4F9BFF).copy(alpha = 0.16f),
)

private val DarkWater = Water(
    body = listOf(Color(0xFF5FD1FF).copy(alpha = 0.45f), Color(0xFF8FB8FF).copy(alpha = 0.8f)),
    shine = Color.White.copy(alpha = 0.5f),
    glow = Color(0xFF6FC6FF).copy(alpha = 0.18f),
)

/** Wave crests around the whole ring, and how long one takes to flow a crest forward. */
private const val WAVES = 14
private const val FLOW_MILLIS = 1_800L

/**
 * The filled part of the ring, from the top clockwise over [progress] of the circle: a path that
 * weaves a little either side of the circle, the wave moving forward as [time] passes. The wave
 * grows in from the tail, so the stream starts flat on the track.
 */
private fun DrawScope.drawWater(progress: Float, radius: Float, stroke: Float, water: Water, time: Long) {
    val sweep = progress * 2 * PI.toFloat()
    val amplitude = 2.2.dp.toPx()
    val phase = (time % FLOW_MILLIS) / FLOW_MILLIS.toFloat() * 2 * PI.toFloat()
    val steps = max(2, (progress * 240).toInt())
    val path = Path()
    var head = Offset.Zero
    for (i in 0..steps) {
        val angle = sweep * i / steps
        val growIn = min(1f, angle / 0.35f)
        val r = radius + amplitude * growIn * sin(WAVES * angle - phase)
        val point = Offset(center.x + r * cos(angle), center.y + r * sin(angle))
        if (i == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
        head = point
    }
    // Angles above run from 3 o'clock; turn the whole stream so it starts at the top.
    rotate(-90f, pivot = center) {
        drawPath(path, water.glow, style = Stroke(stroke * 2.4f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        val body = Brush.sweepGradient(
            0f to water.body[0],
            progress to water.body[1],
            1f to water.body[1],
            center = center,
        )
        drawPath(path, body, style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawPath(path, water.shine, style = Stroke(stroke * 0.28f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        // A droplet at the head.
        drawCircle(water.body[1], radius = stroke * 0.75f, center = head)
        drawCircle(water.shine, radius = stroke * 0.25f, center = head - Offset(stroke * 0.2f, stroke * 0.2f))
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
