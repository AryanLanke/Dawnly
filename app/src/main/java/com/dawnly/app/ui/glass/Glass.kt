package com.dawnly.app.ui.glass

import android.graphics.BlurMaskFilter
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dawnly.app.ui.theme.DawnlyColors
import com.dawnly.app.ui.theme.DawnlyTheme
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.hazeBlur
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

object GlassShapes {
    val Card = RoundedCornerShape(28.dp)
    val Segmented = RoundedCornerShape(18.dp)
    val Segment = RoundedCornerShape(14.dp)
    val Button = RoundedCornerShape(20.dp)
    val SmallButton = RoundedCornerShape(16.dp)
    val Icon = RoundedCornerShape(12.dp)
    val Pill = CircleShape
}

/** The mockup's `saturate(135%)`: it keeps the sky a little richer through the glass. */
private val Saturate = HazeColorEffect.colorFilter(
    ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(1.35f) }),
)

/**
 * Clear liquid glass: a light blur of the sky behind it (Android 12+), an almost clear tint, a
 * bright rim where light catches the top-left edge and fades around the shape, thin inner
 * highlights, and a soft shadow lifting it off the sky. Below Android 12 there's no blur, so the
 * tint gets stronger instead.
 *
 * @param strong the brighter tint for selected items.
 * @param onClick makes the glass a button that sinks a little while pressed.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = GlassShapes.Card,
    strong: Boolean = false,
    onClick: (() -> Unit)? = null,
    onClickLabel: String? = null,
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = DawnlyTheme.colors
    val haze = LocalHazeState.current
    val tint = if (strong) colors.glassStrong else colors.glass
    val style = remember(tint, colors.glassFallback) {
        HazeBlurStyle {
            blurRadius(3.dp)
            noiseFactor(0f)
            colorEffects(listOf(Saturate, HazeColorEffect.tint(tint)))
            fallbackColorEffect(HazeColorEffect.tint(colors.glassFallback))
        }
    }
    val interactions = remember { MutableInteractionSource() }
    val scale by pressScale(interactions)

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .outerShadow(shape, colors.lift, blur = 30.dp, offsetY = 10.dp)
            .clip(shape)
            .then(
                if (haze != null) Modifier.hazeBlur(HazeInput.Sources(haze), style)
                else Modifier.background(tint),
            )
            .glassRim(shape, colors)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactions,
                        indication = null,
                        role = Role.Button,
                        onClickLabel = onClickLabel,
                        onClick = onClick,
                    )
                } else {
                    Modifier
                },
            ),
        contentAlignment = contentAlignment,
        content = content,
    )
}

/**
 * The mockup's glass edge, drawn under the content: a faint inner glow, a sheen in the top of the
 * pane, thin inset highlights (top bright, left and bottom dimmer), the 1dp edge, and the liquid
 * rim, a 1.5dp ring whose light is brightest at the top-left, fades out by the middle and comes
 * back a little at the bottom-right. The rim also blooms softly inward, so it reads as light
 * bending through the edge of the glass rather than a drawn outline. Dark mode turns the rim down
 * ([DawnlyColors.rimStrength]); white lines on a night sky look like a cheap border.
 */
private fun Modifier.glassRim(shape: Shape, colors: DawnlyColors): Modifier = drawWithCache {
    val path = Path().apply { addOutline(shape.createOutline(size, layoutDirection, this@drawWithCache)) }
    fun shifted(x: Float, y: Float) = Path().apply { addPath(path, Offset(x, y)) }
    val px = 1.dp.toPx()
    val strength = colors.rimStrength
    val light = if (colors.isDark) Color(0xFFDCE8FF) else Color.White
    // Each inset box-shadow is the shape minus itself moved by 1px: a hairline on one side.
    val insets = listOf(
        shifted(0f, px) to colors.highlight,
        shifted(px, 0f) to light.copy(alpha = 0.35f * strength),
        shifted(0f, -px) to light.copy(alpha = 0.25f * strength),
    )
    // CSS linear-gradient(140deg, ...) runs along this line through the centre.
    val angle = Math.toRadians(140.0)
    val dir = Offset(sin(angle).toFloat(), -cos(angle).toFloat())
    val half = (size.width * abs(dir.x) + size.height * abs(dir.y)) / 2
    val center = Offset(size.width / 2, size.height / 2)
    val rimShader = LinearGradientShader(
        from = center - dir * half,
        to = center + dir * half,
        colors = listOf(1f, 0.25f, 0f, 0.15f, 0.7f).map { light.copy(alpha = it * strength) },
        colorStops = listOf(0f, 0.28f, 0.5f, 0.75f, 1f),
    )
    val rim = ShaderBrush(rimShader)
    val sheen = Brush.verticalGradient(0f to colors.sheen, 0.45f to colors.sheen.copy(alpha = 0f))
    // Blurred strokes are only drawn with hardware acceleration from Android 9.
    val blurs = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
    val glow = if (blurs) {
        android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = 18.dp.toPx()
            color = Color.White.copy(alpha = 0.05f).toArgb()
            maskFilter = BlurMaskFilter(9.dp.toPx(), BlurMaskFilter.Blur.NORMAL)
        }
    } else {
        null
    }
    val bloom = if (blurs) {
        android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = 6.dp.toPx()
            shader = rimShader
            alpha = 150
            maskFilter = BlurMaskFilter(3.dp.toPx(), BlurMaskFilter.Blur.NORMAL)
        }
    } else {
        null
    }
    val androidPath = path.asAndroidPath()
    onDrawWithContent {
        clipPath(path) {
            if (glow != null) drawIntoCanvas { it.nativeCanvas.drawPath(androidPath, glow) }
            if (colors.sheen.alpha > 0f) drawRect(sheen)
            for ((inset, color) in insets) {
                clipPath(inset, ClipOp.Difference) { drawRect(color) }
            }
            // Strokes are centred on the outline, so half of each shows inside the clip.
            drawPath(path, colors.edge, style = Stroke(width = 2 * px))
            if (bloom != null) drawIntoCanvas { it.nativeCanvas.drawPath(androidPath, bloom) }
            drawPath(path, rim, style = Stroke(width = 3 * px))
        }
        drawContent()
    }
}

/**
 * A soft shadow drawn only outside [shape], like CSS box-shadow, so it doesn't grey out the
 * translucent glass on top. Gives selected pills their edge on the light wall.
 */
fun Modifier.outerShadow(shape: Shape, color: Color, blur: Dp = 8.dp, offsetY: Dp = 2.dp): Modifier =
    drawWithCache {
        val path = Path().apply { addOutline(shape.createOutline(size, layoutDirection, this@drawWithCache)) }
        // Blurred fills are only drawn with hardware acceleration from Android 9.
        val paint = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && blur > 0.dp) {
            android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                this.color = color.toArgb()
                maskFilter = BlurMaskFilter(blur.toPx() / 2, BlurMaskFilter.Blur.NORMAL)
            }
        } else {
            null
        }
        val shadow = android.graphics.Path(path.asAndroidPath()).apply { offset(0f, offsetY.toPx()) }
        onDrawBehind {
            if (paint != null) {
                clipPath(path, ClipOp.Difference) {
                    drawIntoCanvas { it.nativeCanvas.drawPath(shadow, paint) }
                }
            }
        }
    }
