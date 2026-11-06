package com.dawnly.app.ui.glass

import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.core.graphics.withTranslation
import com.dawnly.app.ui.theme.DawnlyColors

/** A cloud drifting left to right: its width, its height on screen, and its CSS timing. */
internal class Cloud(
    val width: Float,
    val top: Float,
    val left: Float,
    val periodMillis: Int,
    val aheadMillis: Int,
    val alpha: Float,
)

internal val Clouds = listOf(
    Cloud(150f, 150 / H, -130f, 48_000, 0, 1f),
    Cloud(110f, 330 / H, -100f, 62_000, 30_000, 0.85f),
    Cloud(170f, 500 / H, -150f, 55_000, 14_000, 0.9f),
)
internal const val CLOUD_HEIGHT = 34f
internal const val CLOUD_TRAVEL = 460f
internal fun CacheDrawScope.morningSky(colors: DawnlyColors): DrawScope.(Long) -> Unit {
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
internal fun cloudShape(width: Float, height: Float) = android.graphics.Path().apply {
    addRoundRect(0f, 0f, width, height, height / 2, height / 2, android.graphics.Path.Direction.CW)
    addOval(0.18f * width, -0.55f * height, 0.64f * width, 0.95f * height, android.graphics.Path.Direction.CW)
    addOval(0.52f * width, -0.35f * height, 0.86f * width, 0.85f * height, android.graphics.Path.Direction.CW)
}
