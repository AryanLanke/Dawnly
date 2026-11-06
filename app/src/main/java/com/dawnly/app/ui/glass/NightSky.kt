package com.dawnly.app.ui.glass

import androidx.compose.animation.core.EaseInOut
import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.util.lerp
import com.dawnly.app.ui.theme.DawnlyColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** A shooting star: where it starts, how often it falls, when it first falls, how long it is. */
internal class ShootingStar(
    val x: Float,
    val y: Float,
    val periodMillis: Int,
    val waitMillis: Int,
    val length: Float = 110f,
)

internal val ShootingStars = listOf(
    ShootingStar(0.85f, 0.04f, 19_800, 0),
    ShootingStar(0.60f, 0.30f, 24_200, 9_900, 90f),
    ShootingStar(0.95f, 0.18f, 13_200, 2_200),
    ShootingStar(0.70f, 0.02f, 16_500, 5_700, 140f),
    ShootingStar(1.00f, 0.45f, 17_600, 8_400),
    ShootingStar(0.45f, 0.08f, 14_300, 11_400, 80f),
    ShootingStar(0.90f, 0.60f, 20_900, 14_100),
    ShootingStar(0.75f, 0.38f, 15_400, 900, 120f),
)
internal const val STAR_ANGLE = 145f

/**
 * How far a shooting star glides, and the share of its period it's seen for. Half the mockup's
 * speed (420px over 35%): the user found them too quick.
 */
internal const val STAR_TRAVEL = 300f
internal const val STAR_SHOWN = 0.45f
internal const val STAR_FADE_IN = 0.05f

/** A still star that twinkles: where it is, and how far into its twinkle it starts. */
internal class Twinkle(val x: Float, val y: Float, val aheadMillis: Int)

internal val Twinkles = listOf(
    Twinkle(0.18f, 0.12f, 0), Twinkle(0.42f, 0.22f, 1_000), Twinkle(0.80f, 0.30f, 2_000),
    Twinkle(0.12f, 0.48f, 500), Twinkle(0.66f, 0.70f, 1_600), Twinkle(0.30f, 0.84f, 2_400),
)
internal const val TWINKLE_MILLIS = 3_500
internal fun CacheDrawScope.nightSky(colors: DawnlyColors): DrawScope.(Long) -> Unit {
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
internal fun DrawScope.drawShootingStar(
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
internal fun sparklePath(outer: Float, inner: Float) = Path().apply {
    for (i in 0 until 8) {
        val angle = i * PI.toFloat() / 4 - PI.toFloat() / 2
        val r = if (i % 2 == 0) outer else inner
        val x = r * cos(angle)
        val y = r * sin(angle)
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}
