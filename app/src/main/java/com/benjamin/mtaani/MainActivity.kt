package com.benjamin.mtaani

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.benjamin.mtaani.navigation.AppNavHost
import com.benjamin.mtaani.ui.theme.MtaaniTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MtaaniTheme {
                AppNavHost()
            }
        }
    }
}
