package com.dawnly.app

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
        setContent {
            DawnlyRoot(main, home, settings)
        }
    }

    override fun onResume() {
        super.onResume()
        // Covers a fresh install (no broadcast has arrived yet) and returning from permission settings.
        dawnly.scope.launch { dawnly.watchScheduler.reconcile() }
    }
}
