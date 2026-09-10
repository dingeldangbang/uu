package de.dingeldangbang.agentmobile.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import de.dingeldangbang.agentmobile.AgentApplication
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentMainScreen(viewModel: MainViewModel, onSettings: () -> Unit) {
    val state by viewModel.state.collectAsState()
    Scaffold(
        modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing),
        topBar = {
            TopAppBar(
                title = { Text("Agent Mobile") },
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Einstellungen")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                "Offline-first Agent mit optionaler HTTPS-Cloud-Anbindung",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = state.prompt,
                onValueChange = viewModel::updatePrompt,
                label = { Text("Frage oder Befehl") },
                minLines = 4,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = viewModel::submit,
                enabled = !state.isLoading && state.prompt.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.isLoading) CircularProgressIndicator()
                else Text("Ausführen")
            }
            state.source.takeIf(String::isNotBlank)?.let {
                Spacer(Modifier.height(12.dp))
                Text("Quelle: $it", style = MaterialTheme.typography.labelMedium)
            }
            state.error?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }
            if (state.result.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(state.result, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentSettingsScreen(application: AgentApplication, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember(application) { application.credentials }
    var endpoint by remember { mutableStateOf(store.load()?.endpoint ?: "https://") }
    var token by remember { mutableStateOf(store.load()?.token ?: "") }
    var status by remember { mutableStateOf("") }
    var modelInstalled by remember { mutableStateOf(application.localEngine.modelInstalled()) }

    val modelPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            status = "Installiere Modell ..."
            runCatching { application.localEngine.installModel(uri) }
                .onSuccess {
                    modelInstalled = true
                    status = "Lokales Modell installiert."
                }
                .onFailure { status = it.message ?: "Modell konnte nicht installiert werden." }
        }
    }

    Scaffold(
        modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing),
        topBar = { TopAppBar(title = { Text("Einstellungen") }) },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Cloud-Adapter", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = endpoint,
                onValueChange = { endpoint = it },
                label = { Text("HTTPS-Basis-URL") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = token,
                onValueChange = { token = it },
                label = { Text("Bearer-Token") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    runCatching { store.save(endpoint, token) }
                        .onSuccess { status = "Zugangsdaten verschlüsselt gespeichert." }
                        .onFailure { status = it.message ?: "Speichern fehlgeschlagen." }
                }) { Text("Speichern") }
                OutlinedButton(onClick = {
                    scope.launch { status = application.cloudClient.test().message }
                }) { Text("Verbindung testen") }
            }
            OutlinedButton(onClick = {
                store.clear()
                endpoint = "https://"
                token = ""
                status = "Zugangsdaten gelöscht."
            }) { Text("Cloud-Zugang löschen") }

            Spacer(Modifier.height(12.dp))
            Text("Lokales Modell", style = MaterialTheme.typography.titleMedium)
            Text(if (modelInstalled) "Modellstatus: installiert" else "Modellstatus: nicht installiert")
            OutlinedButton(onClick = {
                modelPicker.launch(arrayOf("application/octet-stream", "application/zip", "*/*"))
            }) { Text(".litertlm-Modell importieren") }
            Text(
                "Das Modell wird nicht in Git eingecheckt und bleibt im privaten App-Speicher.",
                style = MaterialTheme.typography.bodySmall,
            )

            if (status.isNotBlank()) Text(status)
            Spacer(Modifier.height(8.dp))
            Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Zurück") }
        }
    }
}
