package com.dawnly.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.dawnly.app.ui.HelloScreen
import com.dawnly.app.ui.theme.DawnlyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            DawnlyTheme {
                HelloScreen()
            }
        }
    }
}
