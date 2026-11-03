package com.dawnly.app.ui.glass

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.invalidateDraw
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * How tappable things look while pressed: a soft glow grows from the finger and spreads over the
 * whole row or button, words included, fading in as it goes, then fades away on release. Like a
 * ripple, but soft-edged and filling the whole shape rather than a box. Set for the app in
 * `DawnlyTheme`.
 */
class GlassPress(private val tint: Color) : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode = Node(interactionSource, tint)

    override fun equals(other: Any?) = other is GlassPress && other.tint == tint

    override fun hashCode() = tint.hashCode()

    private class Node(private val source: InteractionSource, private val tint: Color) :
        Modifier.Node(), DrawModifierNode {
        private val shown = Animatable(0f)
        private val spread = Animatable(0f)
        private var origin: Offset? = null

        override fun onAttach() {
            coroutineScope.launch {
                var pressed = 0
                var fadingIn: Job? = null
                source.interactions.collect { interaction ->
                    when (interaction) {
                        is PressInteraction.Press -> {
                            pressed++
                            origin = interaction.pressPosition
                            launch {
                                spread.snapTo(0f)
                                spread.animateTo(1f, tween(SPREAD_MS, easing = FastOutSlowInEasing)) { invalidateDraw() }
                            }
                            fadingIn = launch { shown.animateTo(1f, tween(FADE_IN_MS)) { invalidateDraw() } }
                        }
                        is PressInteraction.Release, is PressInteraction.Cancel -> {
                            pressed = maxOf(0, pressed - 1)
                            // A quick tap reports press and release together: let the glow show
                            // fully first, or it would never be seen.
                            val shownFirst = fadingIn
                            launch {
                                shownFirst?.join()
                                if (pressed == 0) shown.animateTo(0f, tween(FADE_OUT_MS)) { invalidateDraw() }
                            }
                        }
                    }
                }
            }
        }

        override fun ContentDrawScope.draw() {
            drawContent()
            val alpha = shown.value
            if (alpha <= 0f) return
            val from = origin ?: center
            // Far enough to reach every corner from the finger, so it ends up covering it all.
            val reach = maxOf(
                (from - Offset.Zero).getDistance(),
                (from - Offset(size.width, 0f)).getDistance(),
                (from - Offset(0f, size.height)).getDistance(),
                (from - Offset(size.width, size.height)).getDistance(),
            )
            val radius = reach * (0.3f + 0.9f * spread.value)
            drawRect(
                Brush.radialGradient(
                    0f to tint,
                    0.75f to tint,
                    1f to tint.copy(alpha = 0f),
                    center = from,
                    radius = radius.coerceAtLeast(1f),
                ),
                alpha = alpha,
            )
        }
    }

    private companion object {
        const val FADE_IN_MS = 160
        const val SPREAD_MS = 520
        const val FADE_OUT_MS = 450
    }
}
