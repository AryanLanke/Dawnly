package com.dawnly.app.notify

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dawnly.app.R
import com.dawnly.app.ui.glass.FlowingBackground
import com.dawnly.app.ui.glass.PrimaryButton
import com.dawnly.app.ui.theme.DawnlyTheme
import com.dawnly.app.ui.theme.clockSaysNight

/** "Still up?" over the lock screen, with one big "I'm up". Closes once answered or ringing. */
class StillUpActivity : ComponentActivity() {
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
        if (!StillUpCheck.isWaiting(this)) {
            finish()
            return
        }
        setContent {
            DawnlyTheme(darkTheme = clockSaysNight()) {
                val asking by StillUpCheck.isAsking.collectAsStateWithLifecycle()
                LaunchedEffect(asking) { if (!asking && !StillUpCheck.isWaiting(this@StillUpActivity)) finish() }
                Box(Modifier.fillMaxSize()) {
                    FlowingBackground()
                    Column(
                        Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(
                            Modifier.padding(top = 120.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(
                                stringResource(R.string.still_up_title),
                                style = MaterialTheme.typography.displaySmall,
                                textAlign = TextAlign.Center,
                            )
                            Text(
                                stringResource(R.string.still_up_screen_text, StillUpPlan.ANSWER_TIME.toMinutes().toInt()),
                                style = MaterialTheme.typography.bodyLarge,
                                color = DawnlyTheme.colors.textSoft,
                                textAlign = TextAlign.Center,
                            )
                        }
                        PrimaryButton(
                            stringResource(R.string.still_up_action),
                            {
                                StillUpCheck.answer(this@StillUpActivity)
                                finish()
                            },
                            Modifier.fillMaxWidth().widthIn(max = 420.dp).padding(bottom = 24.dp),
                        )
                    }
                }
            }
        }
    }
}
