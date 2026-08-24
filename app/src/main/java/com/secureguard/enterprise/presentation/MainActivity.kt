package com.secureguard.enterprise.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.secureguard.enterprise.pennerkombat.navigation.PennerNavHost
import com.secureguard.enterprise.pennerkombat.ui.theme.PennerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { PennerApp() }
    }
}

@Composable
fun PennerApp() {
    PennerTheme(darkTheme = true) {
        Surface(modifier = Modifier.fillMaxSize()) {
            PennerNavHost()
        }
    }
}
