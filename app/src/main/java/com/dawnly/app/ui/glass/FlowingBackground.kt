package com.dawnly.app.ui.glass

import android.graphics.BlurMaskFilter
import android.os.Build
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
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.core.graphics.withTranslation
import com.dawnly.app.ui.theme.DawnlyColors
import com.dawnly.app.ui.theme.DarkColors
import com.dawnly.app.ui.theme.LightColors
import com.dawnly.app.ui.theme.LocalNight
import com.dawnly.app.ui.theme.LocalReduceMotion
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

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

/**
 * How far a shooting star glides, and the share of its period it's seen for. Half the mockup's
 * speed (420px over 35%): the user found them too quick.
 */
private const val STAR_TRAVEL = 300f
private const val STAR_SHOWN = 0.45f
private const val STAR_FADE_IN = 0.05f

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
private val SoftNight = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

private fun GraphicsLayerScope.nightBlur() = BlurEffect(5.dp.toPx(), 5.dp.toPx(), TileMode.Clamp)

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

/**
 * The sun (morning) and the moon (night) are the same soft glow in the same place, so they match
 * when the sky switches (the user's ask, 2026-11-04). Its centre, in mockup px, sits low enough that
 * only its faint edge passes under the status bar's fade; at 60px a bright moon was cut off there.
 */
private const val ORB_X = 230f
private const val ORB_Y = 80f
private const val ORB_RADIUS = 141f

/** A soft glow like the mockup's sun: bright in the middle, gone by 72% of [ORB_RADIUS]. */
private fun orb(core: Color, middle: Color, edge: Color, width: Float) = Brush.radialGradient(
    0f to core,
    0.25f to middle,
    0.72f to edge,
    center = Offset(ORB_X * width / W, ORB_Y * width / W),
    radius = ORB_RADIUS * width / W,
)

private fun CacheDrawScope.morningSky(colors: DawnlyColors): DrawScope.(Long) -> Unit {
    val u = size.width / W
    val sky = Brush.verticalGradient(0f to colors.sky[0], 0.55f to colors.sky[1], 1f to colors.sky[2])
    // A little brighter than the mockup's (0.75 in the middle), to sit level with the moon.
    val sun = orb(Color(0xFFFFF4D6), Color(0xFFFFECBE).copy(alpha = 0.85f), Color(0x00FFECBE), size.width)
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
        drawCircle(sun, radius = ORB_RADIUS * u, center = Offset(ORB_X * u, ORB_Y * u))
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
    // Cool white moonlight, softer than the sun: on a black sky the same strength is blinding,
    // and white text sits over it.
    val moon = orb(Color(0xFFF4F7FF).copy(alpha = 0.8f), Color(0xFFE8EEFF).copy(alpha = 0.4f), Color(0x00E8EEFF), size.width)
    // Soft like the morning's clouds: stars as small blurred glows rather than sharp dots.
    val starDot = Sprite.draw(this, layoutDirection, Size(2 * u, 2 * u), margin = 3 * u) {
        drawIntoCanvas { it.nativeCanvas.drawCircle(u, u, 1.1f * u, blurPaint(Color.White, 1.4f * u)) }
    }
    val sparkle = sparklePath(outer = 4.2f * u, inner = 1.1f * u)
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
            drawSprite(starDot, Offset(star.x * size.width - u, star.y * size.height - u), alpha = alpha)
        }
        for (star in ShootingStars) drawShootingStar(star, time, u, trail, colors.star, colors.starHead, sparkle)
        drawCircle(moon, radius = ORB_RADIUS * u, center = Offset(ORB_X * u, ORB_Y * u))
    }
}

/**
 * After the mockup's `shoot` keyframes, slowed down: fades in over the first 5% of its period, then
 * glides [STAR_TRAVEL] along its 145° line while fading out, gone by [STAR_SHOWN]. Hidden for the
 * rest, and until its first wait ends. A soft glow runs along the trail, and the head is a little
 * four-point star kept upright, not a dot.
 */
private fun DrawScope.drawShootingStar(
    star: ShootingStar,
    time: Long,
    u: Float,
    trail: Brush,
    glow: Color,
    head: Color,
    sparkle: Path,
) {
    if (time < star.waitMillis) return
    val p = ((time - star.waitMillis) % star.periodMillis) / star.periodMillis.toFloat()
    if (p >= STAR_SHOWN) return
    val alpha = if (p < STAR_FADE_IN) p / STAR_FADE_IN else 1f - (p - STAR_FADE_IN) / (STAR_SHOWN - STAR_FADE_IN)
    val length = star.length * u
    withTransform({
        translate(star.x * size.width, star.y * size.height)
        rotate(STAR_ANGLE, pivot = Offset.Zero)
        translate(STAR_TRAVEL * u * p / STAR_SHOWN, 0f)
    }) {
        drawLine(trail, Offset.Zero, Offset(length, 0f), strokeWidth = 5 * u, cap = StrokeCap.Round, alpha = alpha * 0.22f)
        drawLine(trail, Offset.Zero, Offset(length, 0f), strokeWidth = 1.6f * u, cap = StrokeCap.Round, alpha = alpha)
        // The CSS box-shadow glow, as a gradient: cheap to draw every frame, unlike a blur.
        val tip = Offset(length, 0f)
        drawCircle(
            Brush.radialGradient(0f to glow, 0.45f to glow.copy(alpha = 0.5f), 1f to glow.copy(alpha = 0f), center = tip, radius = 7 * u),
            radius = 7 * u,
            center = tip,
            alpha = alpha,
        )
        withTransform({
            translate(tip.x, tip.y)
            rotate(-STAR_ANGLE, pivot = Offset.Zero)
        }) {
            drawPath(sparkle, head, alpha = alpha)
        }
    }
}

/** A four-point star centred on (0, 0): long points [outer] out, pinched to [inner] between them. */
private fun sparklePath(outer: Float, inner: Float) = Path().apply {
    for (i in 0 until 8) {
        val angle = i * PI.toFloat() / 4 - PI.toFloat() / 2
        val r = if (i % 2 == 0) outer else inner
        val x = r * cos(angle)
        val y = r * sin(angle)
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}
