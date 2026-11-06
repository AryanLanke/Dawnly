package com.dawnly.app.ui.glass

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.dawnly.app.ui.theme.DawnlyMotion
import com.dawnly.app.ui.theme.DawnlyTheme

/** The mockup's switch: 44 x 26, a springy thumb, accent when on. */
@Composable
fun GlassSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = DawnlyTheme.colors
    val haptic = rememberHaptics()
    val thumbOffset by animateDpAsState(if (checked) 21.dp else 3.dp, DawnlyMotion.bouncy(), label = "thumb")
    val track by animateColorAsState(if (checked) colors.accent else colors.track, label = "track")
    val thumb by animateColorAsState(if (checked) colors.onAccent else Color.White, label = "thumbColor")
    Box(
        modifier = modifier
            .size(width = 44.dp, height = 26.dp)
            .clip(CircleShape)
            .background(track)
            .toggleable(value = checked, role = Role.Switch, onValueChange = { on ->
                haptic.toggle(on)
                onCheckedChange(on)
            }),
    ) {
        Box(
            Modifier
                .offset { IntOffset(thumbOffset.roundToPx(), 3.dp.roundToPx()) }
                .size(20.dp)
                .shadow(1.5.dp, CircleShape)
                .background(thumb, CircleShape),
        )
    }
}
