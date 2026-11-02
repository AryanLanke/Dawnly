package com.dawnly.app.ui.glass

import android.graphics.BlurMaskFilter
import androidx.compose.animation.core.EaseInOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.LongState
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.util.lerp
import androidx.core.graphics.withTranslation
import com.dawnly.app.ui.theme.DawnlyColors
import com.dawnly.app.ui.theme.DawnlyTheme
import com.dawnly.app.ui.theme.LocalReduceMotion

/*
 * Sizes and timings are the mockup's CSS (a 280 x 610 screen). Lengths are mockup px scaled by the
 * screen's width; positions given in % there are fractions of the screen here.
 */
private const val W = 280f
private const val H = 610f

/** A cloud drifting left to right: its width, its height on screen, and its CSS timing. */
private class Cloud(
    val width: Float,
    val top: Float,
    val left: Float,
    val periodMillis: Int,
    val aheadMillis: Int,
    val alpha: Float,
)

private val Clouds = listOf(
    Cloud(150f, 150 / H, -130f, 48_000, 0, 1f),
    Cloud(110f, 330 / H, -100f, 62_000, 30_000, 0.85f),
    Cloud(170f, 500 / H, -150f, 55_000, 14_000, 0.9f),
)
private const val CLOUD_HEIGHT = 34f
private const val CLOUD_TRAVEL = 460f

/** A shooting star: where it starts, how often it falls, when it first falls, how long it is. */
private class ShootingStar(
    val x: Float,
    val y: Float,
    val periodMillis: Int,
    val waitMillis: Int,
    val length: Float = 110f,
)

private val ShootingStars = listOf(
    ShootingStar(0.85f, 0.04f, 19_800, 0),
    ShootingStar(0.60f, 0.30f, 24_200, 9_900, 90f),
    ShootingStar(0.95f, 0.18f, 13_200, 2_200),
    ShootingStar(0.70f, 0.02f, 16_500, 5_700, 140f),
    ShootingStar(1.00f, 0.45f, 17_600, 8_400),
    ShootingStar(0.45f, 0.08f, 14_300, 11_400, 80f),
    ShootingStar(0.90f, 0.60f, 20_900, 14_100),
    ShootingStar(0.75f, 0.38f, 15_400, 900, 120f),
)
private const val STAR_ANGLE = 145f
private const val STAR_TRAVEL = 420f

/** A still star that twinkles: where it is, and how far into its twinkle it starts. */
private class Twinkle(val x: Float, val y: Float, val aheadMillis: Int)

private val Twinkles = listOf(
    Twinkle(0.18f, 0.12f, 0), Twinkle(0.42f, 0.22f, 1_000), Twinkle(0.80f, 0.30f, 2_000),
    Twinkle(0.12f, 0.48f, 500), Twinkle(0.66f, 0.70f, 1_600), Twinkle(0.30f, 0.84f, 2_400),
)
private const val TWINKLE_MILLIS = 3_500

/**
 * The living sky behind the glass, the signature of the app. Morning (light mode): a pale blue
 * sky, a soft sun glow in the corner and slow drifting clouds. Night (dark mode): two soft glows,
 * twinkling stars, shooting stars and a soft moon. It only moves while the screen is drawn
 * (Compose pauses its frame clock when the app is in the background), and stays still when
 * animations are off.
 */
@Composable
fun FlowingBackground(modifier: Modifier = Modifier) {
    val colors = DawnlyTheme.colors
    val time = skyClock()
    Box(
        modifier
            .fillMaxSize()
            .drawWithCache {
                val sky = if (colors.isDark) nightSky(colors) else morningSky(colors)
                onDrawBehind { sky(time.longValue) }
            },
    )
}

/** Milliseconds the sky has been moving. Read only while drawing, so it redraws without recomposing. */
@Composable
private fun skyClock(): LongState {
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
private fun blurPaint(color: Color, radius: Float) =
    android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color.toArgb()
        if (radius > 0f) maskFilter = BlurMaskFilter(radius, BlurMaskFilter.Blur.NORMAL)
    }

private fun CacheDrawScope.morningSky(colors: DawnlyColors): DrawScope.(Long) -> Unit {
    val u = size.width / W
    val sky = Brush.verticalGradient(0f to colors.sky[0], 0.55f to colors.sky[1], 1f to colors.sky[2])
    val sunCenter = Offset(230 * u, 60 * u)
    val sun = Brush.radialGradient(
        0f to Color(0xFFFFF4D6),
        0.25f to Color(0xFFFFECBE).copy(alpha = 0.75f),
        0.7f to Color(0xFFFFECBE).copy(alpha = 0f),
        center = sunCenter,
        radius = 141 * u,
    )
    val cloudPaint = blurPaint(Color.White, 4 * u)
    val shadowPaint = blurPaint(Color(0xFF78A0E6).copy(alpha = 0.25f), 6 * u)
    // Each cloud and its shadow, blurred once. The puffs reach about 19px above the pill.
    val clouds = Clouds.map { cloud ->
        val box = Size(cloud.width * u, CLOUD_HEIGHT * u)
        val shape = cloudShape(box.width, box.height)
        Sprite.draw(this, layoutDirection, box, margin = 26 * u) {
            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.withTranslation(0f, 6 * u) { drawPath(shape, shadowPaint) }
                canvas.nativeCanvas.drawPath(shape, cloudPaint)
            }
        }
    }
    return { time ->
        drawRect(sky)
        drawCircle(sun, radius = 141 * u, center = sunCenter)
        Clouds.forEachIndexed { i, cloud ->
            val phase = ((time + cloud.aheadMillis) % cloud.periodMillis) / cloud.periodMillis.toFloat()
            val x = (cloud.left + CLOUD_TRAVEL * phase) * u
            drawSprite(clouds[i], Offset(x, cloud.top * size.height), alpha = cloud.alpha)
        }
    }
}

/** A pill with two puffs on top, as one shape so its shadow doesn't double where they overlap. */
private fun cloudShape(width: Float, height: Float) = android.graphics.Path().apply {
    addRoundRect(0f, 0f, width, height, height / 2, height / 2, android.graphics.Path.Direction.CW)
    addOval(0.18f * width, -0.55f * height, 0.64f * width, 0.95f * height, android.graphics.Path.Direction.CW)
    addOval(0.52f * width, -0.35f * height, 0.86f * width, 0.85f * height, android.graphics.Path.Direction.CW)
}

private fun CacheDrawScope.nightSky(colors: DawnlyColors): DrawScope.(Long) -> Unit {
    val u = size.width / W
    // CSS radial-gradient(rx ry at x y, glow, transparent 70%): centre and radii of each ellipse.
    val glows = listOf(
        Rect(Offset(0.7f * size.width, 0.1f * size.height), Size(0.7f * size.width, 0.4f * size.height)),
        Rect(Offset(0.2f * size.width, 0.95f * size.height), Size(0.6f * size.width, 0.35f * size.height)),
    )
    val trail = Brush.horizontalGradient(0f to Color.Transparent, 1f to colors.star)
    val moon = Moon(this, u)
    return { time ->
        drawRect(colors.wall)
        for (glow in glows) {
            val center = glow.topLeft
            val rx = glow.width
            scale(1f, glow.height / rx, pivot = center) {
                drawCircle(
                    Brush.radialGradient(0f to colors.glow, 0.7f to Color.Transparent, center = center, radius = rx),
                    radius = rx,
                    center = center,
                )
            }
        }
        for (star in Twinkles) {
            val q = ((time + star.aheadMillis) % (2 * TWINKLE_MILLIS)) / TWINKLE_MILLIS.toFloat()
            val alpha = lerp(0.5f, 0.15f, EaseInOut.transform(if (q > 1f) 2f - q else q))
            drawCircle(Color.White, radius = u, center = Offset(star.x * size.width, star.y * size.height), alpha = alpha)
        }
        for (star in ShootingStars) drawShootingStar(star, time, u, trail, colors.star, colors.starHead)
        moon.draw(this, time)
    }
}

/**
 * The mockup's `shoot` keyframes: fades in over the first 4% of its period, then glides 420px along
 * its 145° line while fading out, gone by 35%. Hidden for the rest, and until its first wait ends.
 */
private fun DrawScope.drawShootingStar(
    star: ShootingStar,
    time: Long,
    u: Float,
    trail: Brush,
    glow: Color,
    head: Color,
) {
    if (time < star.waitMillis) return
    val p = ((time - star.waitMillis) % star.periodMillis) / star.periodMillis.toFloat()
    if (p >= 0.35f) return
    val alpha = if (p < 0.04f) p / 0.04f else 1f - (p - 0.04f) / 0.31f
    val length = star.length * u
    withTransform({
        translate(star.x * size.width, star.y * size.height)
        rotate(STAR_ANGLE, pivot = Offset.Zero)
        translate(STAR_TRAVEL * u * p / 0.35f, 0f)
    }) {
        drawLine(trail, Offset.Zero, Offset(length, 0f), strokeWidth = 2 * u, cap = StrokeCap.Round, alpha = alpha)
        // The CSS box-shadow glow, as a gradient: cheap to draw every frame, unlike a blur.
        val tip = Offset(length, 0f)
        drawCircle(
            Brush.radialGradient(0f to glow, 0.45f to glow.copy(alpha = 0.5f), 1f to glow.copy(alpha = 0f), center = tip, radius = 6 * u),
            radius = 6 * u,
            center = tip,
            alpha = alpha,
        )
        drawCircle(head, radius = 2.5f * u, center = tip, alpha = alpha)
    }
}

/**
 * The soft moon in the top corner, from the mockup's SVG (a 100-unit drawing shown 120px wide): a
 * cream disc, three blurred seas, craters with a lit rim, and shade on its far side, with a warm
 * glow around it, all at half strength. Drawn once into a sprite; each frame only floats it up a
 * little and turns it 2° and back.
 */
private class Moon(scope: CacheDrawScope, private val u: Float) {
    private val glow = Brush.radialGradient(
        0f to Glow.copy(alpha = 0.6f),
        0.42f to Glow.copy(alpha = 0.55f),
        0.6f to Glow.copy(alpha = 0.2f),
        1f to Glow.copy(alpha = 0f),
        center = Center,
        radius = 115f,
    )
    private val disc: Sprite

    init {
        val body = Brush.radialGradient(
            0f to Color(0xFFFBF8EE), 0.7f to Color(0xFFE6E0CF), 1f to Color(0xFFB9B3A3),
            center = Offset(40.4f, 38.5f), radius = 62.4f,
        )
        val shade = Brush.radialGradient(
            0.55f to Color(0x000A0B12), 1f to Color(0x8C0A0B12),
            center = Offset(19.3f, 40.4f), radius = 86.4f,
        )
        val seas = android.graphics.Path().apply {
            for (d in Seas) addPath(PathParser().parsePathString(d).toPath().asAndroidPath())
        }
        val seaPaint = blurPaint(Color(0xFFCFC8B5).copy(alpha = 0.75f), 2.2f)
        val s = 1.2f * u
        disc = Sprite.draw(scope, scope.layoutDirection, Size(100 * s, 100 * s), margin = 2 * u) {
            scale(s, s, pivot = Offset.Zero) {
                drawCircle(body, radius = 48f, center = Center)
                drawIntoCanvas { it.nativeCanvas.drawPath(seas, seaPaint) }
                for (c in Craters) drawCrater(c)
                drawCircle(shade, radius = 48f, center = Center)
            }
        }
    }

    fun draw(scope: DrawScope, time: Long) = with(scope) {
        // moonfloat: 16 s each way, eased.
        val q = (time % 32_000) / 16_000f
        val t = EaseInOut.transform(if (q > 1f) 2f - q else q)
        withTransform({
            translate(size.width - 98 * u, -6 * u - 4 * u * t)
            rotate(2 * t, pivot = Offset(60 * u, 60 * u))
        }) {
            scale(1.2f * u, 1.2f * u, pivot = Offset.Zero) {
                drawCircle(glow, radius = 115f, center = Center, alpha = 0.5f)
            }
            drawSprite(disc, alpha = 0.5f)
        }
    }

    private fun DrawScope.drawCrater(c: Crater) {
        val center = Offset(c.x, c.y)
        drawCircle(
            Brush.radialGradient(
                0f to Color(0xFFC9C2AE),
                1f to Color(0xFFADA693),
                center = Offset(c.x - 0.2f * c.r, c.y - 0.3f * c.r),
                radius = 1.2f * c.r,
            ),
            radius = c.r,
            center = center,
        )
        if (c.rim > 0f) {
            drawCircle(
                Color.White.copy(alpha = c.rim),
                radius = c.r,
                center = center - Offset(0.15f * c.r, 0.17f * c.r),
                style = Stroke(width = 0.15f * c.r),
            )
        }
    }

    private class Crater(val x: Float, val y: Float, val r: Float, val rim: Float = 0f)

    private companion object {
        val Center = Offset(50f, 50f)
        val Glow = Color(0xFFFFF6DC)
        val Seas = listOf(
            "M28 30 C36 22 50 24 52 34 C54 42 44 46 36 44 C28 42 22 36 28 30Z",
            "M56 52 C64 46 76 50 76 60 C76 68 66 72 58 68 C52 64 50 56 56 52Z",
            "M30 60 C36 56 44 60 42 66 C40 72 32 72 29 68 C27 65 27 62 30 60Z",
        )
        val Craters = listOf(
            Crater(66f, 28f, 6f, 0.5f), Crater(40f, 78f, 4.5f, 0.5f), Crater(78f, 44f, 3f),
            Crater(22f, 50f, 2.6f), Crater(56f, 86f, 2.2f), Crater(48f, 18f, 2f),
            Crater(70f, 76f, 3.4f, 0.45f),
        )
    }
}
