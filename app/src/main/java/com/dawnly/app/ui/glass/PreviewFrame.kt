package com.dawnly.app.ui.glass

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.dawnly.app.ui.theme.DawnlyTheme

/** A screen-sized preview: the theme and the flowing background behind [content]. */
@Composable
fun PreviewFrame(dark: Boolean, content: @Composable () -> Unit) {
    DawnlyTheme(darkTheme = dark) {
        Box(Modifier.fillMaxSize()) {
            FlowingBackground()
            content()
        }
    }
}
