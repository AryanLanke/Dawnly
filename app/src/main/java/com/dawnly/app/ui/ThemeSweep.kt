package com.dawnly.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import kotlin.math.hypot

/**
 * Switching light and dark sweeps the new look in from the top-right corner, where the sun and moon
 * sit, across to the bottom-left (the user's choice, 2026-11-03, over a fade of the whole screen).
 * The screen as it was is kept as a picture and laid on top, and a soft-edged circle growing from
 * the corner wipes it away. [content] is drawn with [dark] as it stands during the sweep.
 */
@Composable
fun ThemeSweep(dark: Boolean, still: Boolean, content: @Composable (dark: Boolean) -> Unit) {
    var shown by remember { mutableStateOf(dark) }
    var before by remember { mutableStateOf<ImageBitmap?>(null) }
    val sweep = remember { Animatable(1f) }
    val screen = rememberGraphicsLayer()

    LaunchedEffect(dark) {
        if (dark == shown) return@LaunchedEffect
        if (still) {
            shown = dark
            return@LaunchedEffect
        }
        // The old look, as a picture, before anything changes underneath.
        before = screen.toImageBitmap()
        shown = dark
        sweep.snapTo(0f)
        sweep.animateTo(1f, tween(SWEEP_MS, easing = FastOutSlowInEasing))
        before = null
    }

    Box(
        Modifier
            .fillMaxSize()
            .drawWithContent {
                screen.record { this@drawWithContent.drawContent() }
                drawLayer(screen)
            },
    ) {
        content(shown)
    }
    before?.let { picture ->
        Canvas(Modifier.fillMaxSize().graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }) {
            drawImage(picture)
            val corner = Offset(size.width, 0f)
            // Reaches past the far corner, plus the soft edge, by the end.
            val radius = hypot(size.width, size.height) * 1.15f * sweep.value
            if (radius > 0f) {
                drawCircle(
                    Brush.radialGradient(
                        0f to Color.Black,
                        0.85f to Color.Black,
                        1f to Color.Transparent,
                        center = corner,
                        radius = radius,
                    ),
                    radius = radius,
                    center = corner,
                    blendMode = BlendMode.DstOut,
                )
            }
        }
    }
}

private const val SWEEP_MS = 900
