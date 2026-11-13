package com.dawnly.app.notify

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dawnly.app.R
import com.dawnly.app.ui.format.clockFace
import com.dawnly.app.ui.glass.FlowingBackground
import com.dawnly.app.ui.glass.GlassButton
import com.dawnly.app.ui.glass.PrimaryButton
import com.dawnly.app.ui.theme.DawnlyTheme
import com.dawnly.app.ui.theme.clockSaysNight
import java.time.Instant
import kotlinx.coroutines.delay

/**
 * The screen of a reminder that rings like an alarm ([ReminderRingService]): its name, why and the
 * time, with Stop and Snooze. Shows over the lock screen and turns the screen on. Closes itself
 * once the ringing ends, however it ended.
 */
class ReminderRingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        // Nothing ringing (opened late from an old notification): nothing to show.
        if (ReminderRingService.ringing.value == null) {
            finish()
            return
        }
        setContent {
            DawnlyTheme(darkTheme = clockSaysNight()) {
                val ringing by ReminderRingService.ringing.collectAsStateWithLifecycle()
                LaunchedEffect(ringing) { if (ringing == null) finish() }
                // Back doesn't silence it by accident; only Stop or Snooze does.
                BackHandler {}
                Box(Modifier.fillMaxSize()) {
                    FlowingBackground()
                    ringing?.let {
                        RingScreen(
                            it,
                            onStop = { ReminderRingService.stop(this@ReminderRingActivity) },
                            onSnooze = { ReminderRingService.snooze(this@ReminderRingActivity) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RingScreen(ringing: Ringing, onStop: () -> Unit, onSnooze: () -> Unit) {
    val now by produceState(Instant.now()) {
        while (true) {
            delay(1_000L)
            value = Instant.now()
        }
    }
    Column(
        Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.padding(top = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            val face = clockFace(now)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(face.digits, style = MaterialTheme.typography.displayLarge)
                if (face.marker != null) {
                    Text(
                        face.marker,
                        style = MaterialTheme.typography.titleMedium,
                        color = DawnlyTheme.colors.textSoft,
                        modifier = Modifier.padding(start = 4.dp, bottom = 9.dp),
                    )
                }
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(ringing.name, style = MaterialTheme.typography.displaySmall, textAlign = TextAlign.Center)
            if (ringing.why.isNotBlank()) {
                Text(
                    ringing.why,
                    style = MaterialTheme.typography.bodyLarge,
                    color = DawnlyTheme.colors.textSoft,
                    textAlign = TextAlign.Center,
                )
            }
        }
        Column(
            Modifier.fillMaxWidth().widthIn(max = 420.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PrimaryButton(stringResource(R.string.ring_stop), onStop, Modifier.fillMaxWidth())
            if (ringing.id != ReminderRingService.TEST_ID) {
                GlassButton(
                    stringResource(R.string.ring_snooze, ReminderRingService.SNOOZE_MINUTES),
                    onSnooze,
                    Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
