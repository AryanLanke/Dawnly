package com.dawnly.app.ui.glass

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * Something soft drawn once into a bitmap and then reused every frame. Blurred shapes
 * (BlurMaskFilter) are rasterised on the CPU each time they're drawn, which made every frame of the
 * moving sky take ~40 ms; a bitmap costs almost nothing to draw again. Blurs also draw on a software
 * canvas on every Android version, so they work below Android 9 too.
 *
 * @property box the size of the thing drawn, which sits [margin] in from the bitmap's edges so a
 *   blur can spill outside it.
 */
internal class Sprite private constructor(val image: ImageBitmap, val box: Size, val margin: Float, val scale: Float) {
    companion object {
        /** Draws [block] in a [box]-sized space with [margin] around it, at [scale] resolution. */
        fun draw(
            density: Density,
            layoutDirection: LayoutDirection,
            box: Size,
            margin: Float,
            scale: Float = 1f,
            block: DrawScope.() -> Unit,
        ): Sprite {
            val width = ceil((box.width + 2 * margin) * scale).toInt().coerceAtLeast(1)
            val height = ceil((box.height + 2 * margin) * scale).toInt().coerceAtLeast(1)
            val image = ImageBitmap(width, height)
            CanvasDrawScope().draw(density, layoutDirection, Canvas(image), Size(width.toFloat(), height.toFloat())) {
                scale(scale, scale, pivot = Offset.Zero) {
                    translate(margin, margin, block)
                }
            }
            return Sprite(image, box, margin, scale)
        }
    }
}

/** Draws [sprite] with its box's top-left at [topLeft]. Fractional positions stay smooth. */
internal fun DrawScope.drawSprite(sprite: Sprite, topLeft: Offset = Offset.Zero, alpha: Float = 1f) {
    translate(topLeft.x, topLeft.y) {
        drawImage(
            sprite.image,
            dstOffset = IntOffset(-sprite.margin.roundToInt(), -sprite.margin.roundToInt()),
            dstSize = IntSize(
                (sprite.image.width / sprite.scale).roundToInt(),
                (sprite.image.height / sprite.scale).roundToInt(),
            ),
            alpha = alpha,
            filterQuality = FilterQuality.Low,
        )
    }
}
