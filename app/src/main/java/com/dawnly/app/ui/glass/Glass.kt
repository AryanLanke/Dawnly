package com.dawnly.app.ui.glass

import android.graphics.BlurMaskFilter
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dawnly.app.ui.theme.DawnlyColors
import com.dawnly.app.ui.theme.DawnlyTheme
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazePerformanceMode
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
 * The sky moves every frame, so glass is redrawn every frame too. Its soft parts (the shadow and
 * the rim's glow) are blurred once into small bitmaps and reused, and the sky blur runs at reduced
 * resolution (it's only 3dp, so that doesn't show). Drawing them fresh each frame took ~40 ms.
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
    val haptic = rememberHaptics()

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .outerShadow(shape, colors.lift, blur = 30.dp, offsetY = 10.dp)
            .clip(shape)
            .then(
                if (haze != null) {
                    Modifier.hazeBlur(HazeInput.Sources(haze), style, HazePerformanceMode.Balanced)
                } else {
                    Modifier.background(tint)
                },
            )
            .then(GlassRimElement(shape, colors))
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactions,
                        indication = null,
                        role = Role.Button,
                        onClickLabel = onClickLabel,
                        onClick = {
                            haptic.click()
                            onClick()
                        },
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
 * The mockup's glass edge, drawn under the content: a sheen in the top of the pane, thin inset
 * highlights (top bright, left and bottom dimmer), the 1dp edge, and the liquid rim, a 1.5dp ring
 * whose light is brightest at the top-left, fades out by the middle and comes back a little at the
 * bottom-right. The rim also blooms softly inward, so it reads as light bending through the edge of
 * the glass rather than a drawn outline. Dark mode turns the rim down ([DawnlyColors.rimStrength]);
 * white lines on a night sky look like a cheap border.
 */
private data class GlassRimElement(val shape: Shape, val colors: DawnlyColors) : ModifierNodeElement<GlassRimNode>() {
    override fun create() = GlassRimNode(shape, colors)

    override fun update(node: GlassRimNode) = node.update(shape, colors)
}

private class GlassRimNode(private var shape: Shape, private var colors: DawnlyColors) :
    Modifier.Node(), DrawModifierNode {
    private var cache: Rim? = null

    fun update(shape: Shape, colors: DawnlyColors) {
        if (shape == this.shape && colors == this.colors) return
        this.shape = shape
        this.colors = colors
        cache = null
        invalidateDraw()
    }

    override fun ContentDrawScope.draw() {
        val rim = cache?.takeIf { it.size == size } ?: Rim(this, shape, colors).also { cache = it }
        clipPath(rim.path) {
            if (colors.sheen.alpha > 0f) drawRect(rim.sheen)
            for ((inset, color) in rim.insets) {
                clipPath(inset, ClipOp.Difference) { drawRect(color) }
            }
            // Strokes are centred on the outline, so half of each shows inside the clip.
            drawPath(rim.path, colors.edge, style = Stroke(width = 2.dp.toPx()))
            drawSprite(rim.bloom)
            drawPath(rim.path, ShaderBrush(rim.light), style = Stroke(width = 3.dp.toPx()))
        }
        drawContent()
    }
}

/** Everything the rim needs for one size of glass. */
private class Rim(scope: DrawScope, shape: Shape, colors: DawnlyColors) {
    val size: Size = scope.size
    val path = Path().apply { addOutline(shape.createOutline(size, scope.layoutDirection, scope)) }
    val insets: List<Pair<Path, Color>>
    val light: Shader
    val sheen = Brush.verticalGradient(0f to colors.sheen, 0.45f to colors.sheen.copy(alpha = 0f))
    val bloom: Sprite

    init {
        val px = with(scope) { 1.dp.toPx() }
        val strength = colors.rimStrength
        val tone = if (colors.isDark) Color(0xFFDCE8FF) else Color.White
        fun shifted(x: Float, y: Float) = Path().apply { addPath(path, Offset(x, y)) }
        // Each inset box-shadow is the shape minus itself moved by 1px: a hairline on one side.
        insets = listOf(
            shifted(0f, px) to colors.highlight,
            shifted(px, 0f) to tone.copy(alpha = 0.35f * strength),
            shifted(0f, -px) to tone.copy(alpha = 0.25f * strength),
        )
        // CSS linear-gradient(140deg, ...) runs along this line through the centre.
        val angle = Math.toRadians(140.0)
        val dir = Offset(sin(angle).toFloat(), -cos(angle).toFloat())
        val half = (size.width * abs(dir.x) + size.height * abs(dir.y)) / 2
        val center = Offset(size.width / 2, size.height / 2)
        light = LinearGradientShader(
            from = center - dir * half,
            to = center + dir * half,
            colors = listOf(1f, 0.25f, 0f, 0.15f, 0.7f).map { tone.copy(alpha = it * strength) },
            colorStops = listOf(0f, 0.28f, 0.5f, 0.75f, 1f),
        )
        val bloomPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = 6 * px
            shader = light
            alpha = 150
            maskFilter = BlurMaskFilter(3 * px, BlurMaskFilter.Blur.NORMAL)
        }
        val outline = path.asAndroidPath()
        bloom = Sprite.draw(scope, scope.layoutDirection, size, margin = 0f, scale = 0.5f) {
            drawIntoCanvas { it.nativeCanvas.drawPath(outline, bloomPaint) }
        }
    }
}

/**
 * A soft shadow drawn only outside [shape], like CSS box-shadow, so it doesn't grey out the
 * translucent glass on top. Blurred once into a half-size bitmap per size, then reused.
 */
fun Modifier.outerShadow(shape: Shape, color: Color, blur: Dp = 8.dp, offsetY: Dp = 2.dp): Modifier =
    this then OuterShadowElement(shape, color, blur, offsetY)

private data class OuterShadowElement(val shape: Shape, val color: Color, val blur: Dp, val offsetY: Dp) :
    ModifierNodeElement<OuterShadowNode>() {
    override fun create() = OuterShadowNode(this)

    override fun update(node: OuterShadowNode) = node.update(this)
}

private class OuterShadowNode(private var spec: OuterShadowElement) : Modifier.Node(), DrawModifierNode {
    private var path: Path? = null
    private var sprite: Sprite? = null
    private var spriteSize = Size.Unspecified

    fun update(spec: OuterShadowElement) {
        if (spec == this.spec) return
        this.spec = spec
        sprite = null
        invalidateDraw()
    }

    override fun ContentDrawScope.draw() {
        if (sprite == null || spriteSize != size) {
            val outline = Path().apply { addOutline(spec.shape.createOutline(size, layoutDirection, this@draw)) }
            val blur = spec.blur.toPx()
            val dy = spec.offsetY.toPx()
            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = spec.color.toArgb()
                if (blur > 0f) maskFilter = BlurMaskFilter(blur / 2, BlurMaskFilter.Blur.NORMAL)
            }
            val shadow = android.graphics.Path(outline.asAndroidPath()).apply { offset(0f, dy) }
            path = outline
            sprite = Sprite.draw(this, layoutDirection, size, margin = blur + abs(dy), scale = 0.5f) {
                drawIntoCanvas { it.nativeCanvas.drawPath(shadow, paint) }
            }
            spriteSize = size
        }
        clipPath(path!!, ClipOp.Difference) { drawSprite(sprite!!) }
        drawContent()
    }
}
