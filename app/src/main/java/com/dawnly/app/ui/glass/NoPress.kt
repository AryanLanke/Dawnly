package com.dawnly.app.ui.glass

import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.DelegatableNode

/**
 * No highlight on press, anywhere: the user tried a ripple (too boxy), a fading tint and a
 * spreading glow, and preferred none (2026-11-03). What moves on a tap, like the tick sliding into
 * the chosen sound, is the feedback. Set for the app in `DawnlyTheme`.
 */
object NoPress : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode = object : Modifier.Node() {}

    override fun equals(other: Any?) = other === this

    override fun hashCode() = 0
}
