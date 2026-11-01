package com.dawnly.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.dawnly.app.ui.feasibility.FeasibilityScreen
import com.dawnly.app.ui.theme.DawnlyTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            DawnlyTheme {
                FeasibilityScreen()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Covers a fresh install (no broadcast has arrived yet) and returning from permission settings.
        dawnly.scope.launch { dawnly.watchScheduler.reconcile() }
    }
}
