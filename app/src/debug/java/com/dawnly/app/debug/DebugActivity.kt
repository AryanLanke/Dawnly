package com.dawnly.app.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.dawnly.app.ui.debug.DebugScreen
import com.dawnly.app.ui.theme.DawnlyTheme

/** Debug builds only: opens [DebugScreen] from its own "Dawnly debug" launcher icon. */
class DebugActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            DawnlyTheme {
                Surface(Modifier.fillMaxSize()) {
                    DebugScreen(Modifier.safeDrawingPadding())
                }
            }
        }
    }
}
