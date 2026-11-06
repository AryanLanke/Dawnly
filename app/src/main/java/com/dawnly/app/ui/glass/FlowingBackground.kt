package com.dawnly.app.ui.glass

import android.graphics.BlurMaskFilter
import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.LongState
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.dawnly.app.ui.theme.DarkColors
import com.dawnly.app.ui.theme.LightColors
import com.dawnly.app.ui.theme.LocalNight
import com.dawnly.app.ui.theme.LocalReduceMotion

/*
 * Sizes and timings are the mockup's CSS (a 280 x 610 screen). Lengths are mockup px scaled by the
 * screen's width; positions given in % there are fractions of the screen here.
 */
internal const val W = 280f
internal const val H = 610f

/**
 * The living sky behind the glass, the signature of the app. Morning (light mode): a pale blue
 * sky, a soft sun glow in the corner and slow drifting clouds. Night (dark mode): two soft glows,
 * twinkling stars, shooting stars and a soft moon. It only moves while the screen is drawn
 * (Compose pauses its frame clock when the app is in the background), and stays still when
 * animations are off.
 */
@Composable
fun FlowingBackground(modifier: Modifier = Modifier) {
    // How far into night, read only while drawing: switching light and dark crossfades the two
    // skies without rebuilding their clouds, stars and moon on every frame of the fade.
    val night = LocalNight.current
    val time = skyClock()
    Box(modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .drawWithCache {
                    val sky = morningSky(LightColors)
                    onDrawBehind { if (night.value < 1f) sky(time.longValue) }
                },
        )
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = night.value
                    renderEffect = if (SoftNight && night.value > 0f) nightBlur() else null
                }
                .drawWithCache {
                    val sky = nightSky(DarkColors)
                    onDrawBehind { if (night.value > 0f) sky(time.longValue) }
                },
        )
    }
}

/**
 * The night sky is blurred as one layer, so it's soft like the morning's clouds and sun rather than
 * sharp (the user's ask, 2026-11-03). One GPU blur of one layer (Android 12+), not a blur behind
 * every panel, which made Settings stutter.
 */
internal val SoftNight = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

internal fun GraphicsLayerScope.nightBlur() = BlurEffect(5.dp.toPx(), 5.dp.toPx(), TileMode.Clamp)

/** Milliseconds the sky has been moving. Read only while drawing, so it redraws without recomposing. */
@Composable
internal fun skyClock(): LongState {
    val time = remember { mutableLongStateOf(0L) }
    if (!LocalReduceMotion.current) {
        LaunchedEffect(Unit) {
            val start = withFrameMillis { it } - time.longValue
            while (true) withFrameMillis { time.longValue = it - start }
        }
    }
    return time
}

/** A soft paint for shapes drawn once into a [Sprite]; blurring them every frame is far too slow. */
internal fun blurPaint(color: Color, radius: Float) =
    android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color.toArgb()
        if (radius > 0f) maskFilter = BlurMaskFilter(radius, BlurMaskFilter.Blur.NORMAL)
    }

/**
 * The sun (morning) and the moon (night) are the same soft glow in the same place, so they match
 * when the sky switches (the user's ask, 2026-11-04). Its centre, in mockup px, sits low enough that
 * only its faint edge passes under the status bar's fade; at 60px a bright moon was cut off there.
 */
internal const val ORB_X = 230f
internal const val ORB_Y = 80f
internal const val ORB_RADIUS = 141f

/** A soft glow like the mockup's sun: bright in the middle, gone by 72% of [ORB_RADIUS]. */
internal fun orb(core: Color, middle: Color, edge: Color, width: Float) = Brush.radialGradient(
    0f to core,
    0.25f to middle,
    0.72f to edge,
    center = Offset(ORB_X * width / W, ORB_Y * width / W),
    radius = ORB_RADIUS * width / W,
)
