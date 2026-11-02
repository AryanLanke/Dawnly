package com.dawnly.app

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.dawnly.app.ui.DawnlyRoot
import com.dawnly.app.ui.MainViewModel
import com.dawnly.app.ui.home.HomeViewModel
import com.dawnly.app.ui.settings.SettingsViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val main: MainViewModel by viewModels { MainViewModel.factory(dawnly) }
    private val home: HomeViewModel by viewModels { HomeViewModel.factory(dawnly) }
    private val settings: SettingsViewModel by viewModels { SettingsViewModel.factory(dawnly) }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        preferFastestRefreshRate()
        setContent {
            DawnlyRoot(main, home, settings)
        }
    }

    /**
     * Asks for the screen's fastest refresh rate (120 Hz on the test phone) while Dawnly is open.
     * Without it Android may run the moving sky and swipes at 60 Hz.
     */
    private fun preferFastestRefreshRate() {
        val screen = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) display else null
        @Suppress("DEPRECATION")
        val current = (screen ?: windowManager.defaultDisplay) ?: return
        val mode = current.mode
        val fastest = current.supportedModes
            .filter { it.physicalWidth == mode.physicalWidth && it.physicalHeight == mode.physicalHeight }
            .maxByOrNull { it.refreshRate } ?: return
        window.attributes = window.attributes.apply { preferredDisplayModeId = fastest.modeId }
    }

    override fun onResume() {
        super.onResume()
        // Covers a fresh install (no broadcast has arrived yet) and returning from permission settings.
        dawnly.scope.launch { dawnly.watchScheduler.reconcile() }
    }
}
