package com.dawnly.app.ui.debug

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Dev-only screen with live wake-signal values. Phase 3 fills it in; Phase 5 links to it from
 * Settings in debug builds.
 */
@Composable
fun DebugScreen(modifier: Modifier = Modifier) {
    Text(text = "Debug screen (Phase 3)", modifier = modifier)
}
