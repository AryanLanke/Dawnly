package com.dawnly.app.ui.glass

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.roundToInt

/**
 * Press and hold a row of equal options, then slide: it buzzes when the hold lets you slide, the
 * bright pill follows the finger, each option ticks as the finger crosses into it, and letting go
 * picks the one under the finger. A plain tap still works the usual way. Used by the tab bar and every
 * segmented choice. Put [modifier] on the box that spans exactly the options.
 */
@Stable
class HoldAndSlide internal constructor(private val haptic: Haptics) {
    private var held by mutableStateOf(false)
    private var at by mutableFloatStateOf(0f)

    /** Where the pill should be while held (0 is the first option, 0.5 halfway on), or null. */
    val scrub: Float? get() = if (held) at else null

    internal var count = 1
    internal var onHover: (Int) -> Unit = {}
    internal var onRelease: (index: Int, at: Float) -> Unit = { _, _ -> }

    val modifier: Modifier = Modifier.pointerInput(Unit) {
        // The pill's left edge sits half an option to the left of the finger.
        fun position(x: Float) = (x / (size.width / count.toFloat()) - 0.5f).coerceIn(0f, count - 1f)
        fun letGo() {
            if (!held) return
            held = false
            onRelease(at.roundToInt(), at)
        }
        detectDragGesturesAfterLongPress(
            onDragStart = { start ->
                haptic.longPress()
                at = position(start.x)
                held = true
            },
            onDrag = { change, _ ->
                change.consume()
                val before = at.roundToInt()
                at = position(change.position.x)
                val now = at.roundToInt()
                if (now != before) {
                    haptic.tick()
                    onHover(now)
                }
            },
            onDragEnd = ::letGo,
            onDragCancel = ::letGo,
        )
    }
}

/**
 * @param onHover called as the finger crosses into another option while held.
 * @param onRelease called on letting go, with the option picked and where the pill was.
 */
@Composable
fun rememberHoldAndSlide(
    count: Int,
    onHover: (Int) -> Unit = {},
    onRelease: (index: Int, at: Float) -> Unit,
): HoldAndSlide {
    val haptic = rememberHaptics()
    return remember(haptic) { HoldAndSlide(haptic) }.apply {
        this.count = count
        this.onHover = onHover
        this.onRelease = onRelease
    }
}
