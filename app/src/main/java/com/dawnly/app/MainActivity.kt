package com.dawnly.app

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import com.dawnly.app.data.sound.AlertSound
import com.dawnly.app.notify.ReminderScheduler
import com.dawnly.app.ui.DawnlyRoot
import com.dawnly.app.ui.MainViewModel
import com.dawnly.app.ui.home.HomeViewModel
import com.dawnly.app.ui.settings.SettingsViewModel
import com.dawnly.app.ui.settings.SoundPreview
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    companion object {
        /** Brings the Dawnly screen that's already open back in front, rather than a new one. */
        fun backToFront(context: android.content.Context): Intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)

        /** True until the first screen of this process is created. */
        private var newProcess = true
    }

    private val main: MainViewModel by viewModels { MainViewModel.factory(dawnly) }
    private val home: HomeViewModel by viewModels { HomeViewModel.factory(dawnly) }
    private val settings: SettingsViewModel by viewModels { SettingsViewModel.factory(dawnly) }

    /**
     * Dawnly's opening sound, like a computer's start-up chime: the flute, whatever sound the
     * prompt uses. On the notification stream, so silent mode keeps it quiet.
     */
    private val chime by lazy { SoundPreview(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        preferFastestRefreshRate()
        // Once per opening, not again when the screen turns. A fresh process counts as an opening
        // even with saved state: HyperOS kills Dawnly in the background and the icon restores it.
        // Only when opened from its icon: not when it comes back from the Clock app, a
        // notification or a ringing reminder.
        val freshOpening = savedInstanceState == null || newProcess
        newProcess = false
        if (freshOpening && openedFromIcon(intent)) {
            lifecycleScope.launch {
                val on = dawnly.settings.startupSound.first()
                if (on && lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) chime.play(AlertSound.Flute)
            }
        }
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

    private fun openedFromIcon(intent: Intent?): Boolean =
        intent?.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_LAUNCHER)

    override fun onDestroy() {
        chime.stop()
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        // Covers a fresh install (no broadcast has arrived yet) and returning from permission settings.
        dawnly.scope.launch { dawnly.watchScheduler.reconcile() }
        dawnly.scope.launch { ReminderScheduler.sync(this@MainActivity) }
        com.dawnly.app.notify.TimerController.sync(this)
    }
}
