package com.secureguard.enterprise.presentation.ui.actions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.secureguard.enterprise.data.model.AssetStatus
import com.secureguard.enterprise.presentation.components.ActionButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionsScreen(
    viewModel: ActionsViewModel = hiltViewModel()
) {
    val assets by viewModel.assets.collectAsState()
    val selectedAsset by viewModel.selectedAsset.collectAsState()
    val commandLog by viewModel.commandLog.collectAsState()
    val isExecuting by viewModel.isExecuting.collectAsState()
    val nav = rememberNavController()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("⚡ Aktionen") },
                navigationIcon = {
                    IconButton(onClick = { nav.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Zurück")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.clearLog() }) {
                        Icon(Icons.Default.Clear, contentDescription = "Log löschen")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Asset-Auswahl Header
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("🎯 Asset auswählen", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            selectedAsset?.shortName ?: "Bitte zuerst Asset auswählen",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            assets.take(3).forEach { a ->
                                androidx.compose.material3.AssistChip(
                                    onClick = { viewModel.selectAsset(a) },
                                    label = { Text(a.shortName) },
                                    leadingIcon = {
                                        Icon(Icons.Default.Warning, contentDescription = null)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            if (selectedAsset != null) {
                val current = selectedAsset!!
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = when (current.status) {
                                AssetStatus.ONLINE -> Color(0xFF2E7D32).copy(alpha = 0.05f)
                                else               -> Color(0xFFE53935).copy(alpha = 0.05f)
                            }
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "🚀 Aktionen für ${current.shortName}",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = "STATUS: ${current.status.name}  |  📶 ${current.rssi} dBm",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Divider(modifier = Modifier.padding(vertical = 8.dp))

                            // Hauptaktionen
                            Row(modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ActionButton(
                                    modifier = Modifier.weight(1f),
                                    icon = Icons.Default.Warning,
                                    label = "🔔 Alarm",
                                    onClick = { viewModel.executeAction(ActionType.ALARM) },
                                    enabled = current.status == AssetStatus.ONLINE && !isExecuting
                                )
                                ActionButton(
                                    modifier = Modifier.weight(1f),
                                    icon = Icons.Default.Lightbulb,
                                    label = "💡 Blinken",
                                    onClick = { viewModel.executeAction(ActionType.LIGHT) },
                                    enabled = current.status == AssetStatus.ONLINE && !isExecuting
                                )
                            }
                            Row(modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ActionButton(
                                    modifier = Modifier.weight(1f),
                                    icon = Icons.Default.PowerSettingsNew,
                                    label = "🔇 Motor aus",
                                    onClick = { viewModel.executeAction(ActionType.MOTOR_OFF) },
                                    enabled = current.status == AssetStatus.ONLINE && !isExecuting
                                )
                                ActionButton(
                                    modifier = Modifier.weight(1f),
                                    icon = Icons.Default.BatteryAlert,
                                    label = "🔋 Batterie",
                                    onClick = { viewModel.executeAction(ActionType.BATTERY) },
                                    enabled = current.status == AssetStatus.ONLINE && !isExecuting
                                )
                            }
                            Row(modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ActionButton(
                                    modifier = Modifier.weight(1f),
                                    icon = Icons.Default.Message,
                                    label = "📝 Nachricht",
                                    onClick = { viewModel.executeAction(ActionType.MESSAGE) },
                                    enabled = current.status == AssetStatus.ONLINE && !isExecuting
                                )
                                ActionButton(
                                    modifier = Modifier.weight(1f),
                                    icon = Icons.Default.LocationOn,
                                    label = "📍 Position",
                                    onClick = { viewModel.executeAction(ActionType.POSITION) },
                                    enabled = current.status == AssetStatus.ONLINE && !isExecuting
                                )
                            }
                            Divider(modifier = Modifier.padding(vertical = 8.dp))

                            // Zusätzliche Aktionen
                            Row(modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ActionButton(
                                    modifier = Modifier.weight(1f),
                                    icon = Icons.Default.Refresh,
                                    label = "🔄 Neustarten",
                                    onClick = { viewModel.executeAction(ActionType.RESTART) },
                                    enabled = current.status == AssetStatus.ONLINE && !isExecuting
                                )
                                ActionButton(
                                    modifier = Modifier.weight(1f),
                                    icon = Icons.Default.Storage,
                                    label = "📊 Telemetrie",
                                    onClick = { viewModel.executeAction(ActionType.TELEMETRY) },
                                    enabled = current.status == AssetStatus.ONLINE && !isExecuting
                                )
                            }

                            // Setttings
                            Divider(modifier = Modifier.padding(vertical = 8.dp))
                            Text("⚙️ Einstellungen", style = MaterialTheme.typography.titleSmall)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = true, onCheckedChange = {})
                                Text("Recover/Resend aktivieren")
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = false, onCheckedChange = {})
                                Text("Steuerlog aufzeichnen")
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = false, onCheckedChange = {})
                                Text("Automatische Benachrichtigung")
                            }

                            if (isExecuting) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Aktion wird ausgeführt...",
                                         style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }

                // Command Log
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("📋 Command Log", style = MaterialTheme.typography.titleSmall)
                                TextButton(onClick = { viewModel.clearLog() }) {
                                    Text("Log löschen")
                                }
                            }
                            if (commandLog.isEmpty()) {
                                Text(
                                    "Keine Einträge",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                commandLog.takeLast(10).forEach { entry ->
                                    Text(
                                        text = entry,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    "Bitte wähle ein Asset aus",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
