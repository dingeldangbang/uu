package de.dingeldangbang.agentmobile

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import de.dingeldangbang.agentmobile.ui.AgentMainScreen
import de.dingeldangbang.agentmobile.ui.AgentSettingsScreen
import de.dingeldangbang.agentmobile.ui.MainViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels {
        viewModelFactory {
            initializer {
                MainViewModel(
                    application = application as AgentApplication,
                    repository = (application as AgentApplication).repository,
                )
            }
        }
    }

    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent { AgentMobileRoot(viewModel) }
    }
}

@Composable
private fun AgentMobileRoot(viewModel: MainViewModel) {
    var settings by rememberSaveable { mutableStateOf(false) }
    MaterialTheme {
        if (settings) {
            AgentSettingsScreen(
                application = viewModel.application,
                onBack = { settings = false },
            )
        } else {
            AgentMainScreen(viewModel = viewModel, onSettings = { settings = true })
        }
    }
}
