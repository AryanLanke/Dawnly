package com.dawnly.app.notify

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.dawnly.app.R
import com.dawnly.app.data.reminder.ReminderSounds
import com.dawnly.app.data.sound.AlertSound
import com.dawnly.app.dawnly
import com.dawnly.app.ui.glass.FlowingBackground
import com.dawnly.app.ui.glass.GlassButton
import com.dawnly.app.ui.glass.PrimaryButton
import com.dawnly.app.ui.theme.DawnlyTheme
import com.dawnly.app.ui.theme.clockSaysNight
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class TestLabActivity : ComponentActivity() {
    private var message by mutableStateOf(R.string.test_lab_detail)
    private var busy by mutableStateOf(false)
    private var test: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DawnlyTheme(darkTheme = clockSaysNight()) {
                Box(Modifier.fillMaxSize()) {
                    FlowingBackground()
                    Column(
                        Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Text(stringResource(R.string.test_lab_title), style = MaterialTheme.typography.headlineMedium)
                        Text(stringResource(R.string.test_lab_detail), style = MaterialTheme.typography.bodyMedium)
                        Text(stringResource(message), style = MaterialTheme.typography.bodyLarge)
                        if (!busy) {
                            GlassButton(stringResource(R.string.test_alarm_sound), { ring(AlertSound.PhoneDefault) })
                            GlassButton(stringResource(R.string.test_reminder_sound), { ring(ReminderSounds.DEFAULT) })
                            GlassButton(stringResource(R.string.test_music), { music(offline = false) })
                            GlassButton(stringResource(R.string.test_offline), { music(offline = true) })
                            Text(stringResource(R.string.test_offline_detail), style = MaterialTheme.typography.bodySmall)
                        }
                        PrimaryButton(stringResource(R.string.test_stop), ::stopTest)
                        GlassButton(stringResource(R.string.test_back), { finish() })
                    }
                }
            }
        }
    }

    private fun ring(sound: AlertSound) {
        if (busy) return
        if (ReminderRingService.ringing.value != null) {
            message = R.string.test_busy
            return
        }
        busy = true
        test = lifecycleScope.launch {
            try {
                if (ReminderRingService.test(this@TestLabActivity, sound)) {
                    message = R.string.test_ring_running
                    delay(ReminderRingService.TEST_TIMEOUT_MS + 500L)
                    message = R.string.test_finished
                } else {
                    message = R.string.test_ring_failed
                }
            } finally {
                busy = false
            }
        }
    }

    private fun music(offline: Boolean) {
        if (busy) return
        if (ReminderRingService.ringing.value != null) {
            message = R.string.test_busy
            return
        }
        busy = true
        message = R.string.test_starting
        test = lifecycleScope.launch {
            try {
                var result = MusicResult.Off
                WakeMusic.test(this@TestLabActivity, dawnly.settings.wakeMusic.first(), offline) {
                    result = it
                    message = it.message
                }
                when (result) {
                    MusicResult.Offline, MusicResult.NotInstalled, MusicResult.NoPlayback, MusicResult.Unavailable -> {
                        if (ReminderRingService.test(this@TestLabActivity, AlertSound.PhoneDefault)) {
                            delay(ReminderRingService.TEST_TIMEOUT_MS + 500L)
                        } else {
                            message = R.string.test_ring_failed
                        }
                    }
                    MusicResult.Started -> message = R.string.test_finished
                    else -> Unit
                }
            } finally {
                busy = false
            }
        }
    }

    private fun stopTest() {
        test?.cancel()
        test = null
        ReminderRingService.stopTest(this)
        message = R.string.test_stopped
    }

    override fun onDestroy() {
        stopTest()
        super.onDestroy()
    }
}

private val MusicResult.message: Int
    get() = when (this) {
        MusicResult.Started -> R.string.test_started
        MusicResult.Off -> R.string.test_off
        MusicResult.Offline -> R.string.test_no_network
        MusicResult.NotInstalled -> R.string.test_no_app
        MusicResult.AlreadyPlaying -> R.string.test_already_playing
        MusicResult.CannotDismiss -> R.string.test_no_permission
        MusicResult.Busy -> R.string.test_busy
        MusicResult.NoPlayback -> R.string.test_no_playback
        MusicResult.Unavailable -> R.string.test_unavailable
    }
