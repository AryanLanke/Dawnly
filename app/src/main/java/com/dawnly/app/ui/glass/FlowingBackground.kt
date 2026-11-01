package com.dawnly.app.ui.glass

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.util.lerp
import com.dawnly.app.ui.theme.DawnlyTheme
import com.dawnly.app.ui.theme.LocalReduceMotion
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

/** What glass blurs. Null in previews, where glass falls back to a plain tint. */
val LocalHazeState = staticCompositionLocalOf<HazeState?> { null }

/**
 * One flowing shape, in fractions of the screen so it scales to any phone. Positions and moves are
 * the mockup's CSS keyframes (a 280 x 610 screen), which alternate: start, middle, end, and back.
 */
private class Blob(
    val size: Float,
    val center: Offset,
    val middle: Offset,
    val end: Offset,
    val middleScale: Float,
    val endScale: Float,
    val periodMillis: Int,
)

private const val W = 280f
private const val H = 610f

private val Blobs = listOf(
    Blob(240 / W, Offset(60 / W, 160 / H), Offset(140 / W, 120 / H), Offset(60 / W, 320 / H), 1.15f, 0.9f, 16_000),
    Blob(280 / W, Offset(230 / W, 400 / H), Offset(-150 / W, -160 / H), Offset(-60 / W, 140 / H), 0.9f, 1.2f, 19_000),
    Blob(220 / W, Offset(120 / W, 560 / H), Offset(120 / W, -220 / H), Offset(160 / W, -40 / H), 1.2f, 0.95f, 22_000),
)

/**
 * The plain wall with three soft blue shapes slowly drifting over it: the signature of the app.
 * The shapes are radial gradients, already soft, so no blur is needed. They only move while the
 * screen is drawn (Compose pauses its frame clock when the app is in the background), and they
 * stay still when animations are off.
 */
@Composable
fun FlowingBackground(modifier: Modifier = Modifier) {
    val colors = DawnlyTheme.colors
    val haze = LocalHazeState.current
    val phases = blobPhases()
    Box(
        modifier
            .fillMaxSize()
            .then(if (haze != null) Modifier.hazeSource(haze) else Modifier)
            .background(colors.wall)
            .drawBehind {
                Blobs.forEachIndexed { i, blob -> drawBlob(blob, colors.blobs[i], phases[i].value) }
            },
    )
}

/** Each blob's position along its loop, 0 to 1 and back. Read only while drawing. */
@Composable
private fun blobPhases(): List<State<Float>> {
    if (LocalReduceMotion.current) return remember { Blobs.map { mutableFloatStateOf(0f) } }
    val transition = rememberInfiniteTransition(label = "flow")
    return Blobs.map { blob ->
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                tween(blob.periodMillis, easing = LinearEasing),
                RepeatMode.Reverse,
            ),
            label = "blob",
        )
    }
}

private fun DrawScope.drawBlob(blob: Blob, color: Color, phase: Float) {
    // CSS eases each keyframe segment on its own: start to middle, then middle to end.
    val firstHalf = phase < 0.5f
    val t = FastOutSlowInEasing.transform(if (firstHalf) phase * 2 else phase * 2 - 1)
    val shift = if (firstHalf) lerp(Offset.Zero, blob.middle, t) else lerp(blob.middle, blob.end, t)
    val scale = if (firstHalf) lerp(1f, blob.middleScale, t) else lerp(blob.middleScale, blob.endScale, t)

    val center = Offset(
        (blob.center.x + shift.x) * size.width,
        (blob.center.y + shift.y) * size.height,
    )
    val radius = blob.size * size.width / 2 * scale
    drawCircle(
        brush = Brush.radialGradient(
            0f to color,
            0.68f to color.copy(alpha = 0f),
            center = center,
            radius = radius,
        ),
        radius = radius,
        center = center,
        alpha = 0.9f,
    )
}

private fun lerp(start: Offset, stop: Offset, fraction: Float) =
    Offset(lerp(start.x, stop.x, fraction), lerp(start.y, stop.y, fraction))
