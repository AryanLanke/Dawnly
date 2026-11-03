package com.dawnly.app.ui.glass

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.invalidateDraw
import kotlinx.coroutines.launch

/**
 * How tappable things look while pressed: the whole of them, words included, softens under a light
 * tint that fades in on touch and out on release. Replaces the ripple, which spread from the finger
 * and lit only part of a row. Set for the app in `DawnlyTheme`.
 */
class GlassPress(private val tint: Color) : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode = Node(interactionSource, tint)

    override fun equals(other: Any?) = other is GlassPress && other.tint == tint

    override fun hashCode() = tint.hashCode()

    private class Node(private val source: InteractionSource, private val tint: Color) :
        Modifier.Node(), DrawModifierNode {
        private val shown = Animatable(0f)

        override fun onAttach() {
            coroutineScope.launch {
                var pressed = 0
                source.interactions.collect { interaction ->
                    when (interaction) {
                        is PressInteraction.Press -> pressed++
                        is PressInteraction.Release, is PressInteraction.Cancel -> pressed = maxOf(0, pressed - 1)
                        else -> return@collect
                    }
                    val target = if (pressed > 0) 1f else 0f
                    launch {
                        shown.animateTo(target, tween(if (target > 0f) FADE_IN_MS else FADE_OUT_MS)) { invalidateDraw() }
                    }
                }
            }
        }

        override fun ContentDrawScope.draw() {
            drawContent()
            if (shown.value > 0f) drawRect(tint.copy(alpha = tint.alpha * shown.value))
        }
    }

    private companion object {
        const val FADE_IN_MS = 90
        const val FADE_OUT_MS = 320
    }
}
