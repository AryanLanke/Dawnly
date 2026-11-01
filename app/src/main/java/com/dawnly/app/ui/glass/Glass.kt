package com.dawnly.app.ui.glass

import android.graphics.BlurMaskFilter
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.asAndroidPath
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

object GlassShapes {
    val Card = RoundedCornerShape(28.dp)
    val Segmented = RoundedCornerShape(18.dp)
    val Segment = RoundedCornerShape(14.dp)
    val Button = RoundedCornerShape(20.dp)
    val SmallButton = RoundedCornerShape(16.dp)
    val Icon = RoundedCornerShape(12.dp)
    val Pill = CircleShape
}

/** The mockup's `saturate(170%)`: it keeps the blue shapes vivid through the glass. */
private val Saturate = HazeColorEffect.colorFilter(
    ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(1.7f) }),
)

/**
 * Clear blue glass: a light blur of the flowing shapes behind it (Android 12+), a faint tint, a
 * thin white rim that is brightest along the top edge, and a soft inner glow. No drop shadow.
 * Below Android 12 there's no blur, so the tint gets stronger instead.
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
            blurRadius(8.dp)
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

/** The rim and inner glow from the mockup's inset box-shadows. */
private fun Modifier.glassRim(shape: Shape, colors: DawnlyColors): Modifier = this
    .drawWithCache {
        val path = Path().apply { addOutline(shape.createOutline(size, layoutDirection, this@drawWithCache)) }
        // Blurred strokes are only drawn with hardware acceleration from Android 9.
        val glow = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                style = android.graphics.Paint.Style.STROKE
                strokeWidth = 14.dp.toPx()
                color = Color.White.copy(alpha = if (colors.isDark) 0.10f else 0.18f).toArgb()
                maskFilter = BlurMaskFilter(7.dp.toPx(), BlurMaskFilter.Blur.NORMAL)
            }
        } else {
            null
        }
        val androidPath = path.asAndroidPath()
        onDrawWithContent {
            if (glow != null) {
                clipPath(path) { drawIntoCanvas { it.nativeCanvas.drawPath(androidPath, glow) } }
            }
            drawContent()
        }
    }
    .border(
        width = 1.dp,
        brush = Brush.verticalGradient(
            0f to colors.highlight,
            0.25f to colors.edge,
            1f to colors.edge.copy(alpha = colors.edge.alpha * 0.55f),
        ),
        shape = shape,
    )

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
