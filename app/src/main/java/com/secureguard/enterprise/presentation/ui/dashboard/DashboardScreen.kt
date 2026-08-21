package com.secureguard.enterprise.presentation.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.secureguard.enterprise.presentation.components.AssetCard
import com.secureguard.enterprise.presentation.components.StatCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val assets by viewModel.assets.collectAsState()
    val agentRunning by viewModel.agentStatus.collectAsState()
    val nav = rememberNavController()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🛡️ SecureGuard Pro") },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Aktualisieren")
                    }
                    BadgedBox(badge = { Badge { Text(uiState.alertCount.toString()) } }) {
                        IconButton(onClick = { nav.navigate("alerts") }) {
                            Icon(Icons.Default.Notifications, contentDescription = "Alarme")
                        }
                    }
                    IconButton(onClick = { nav.navigate("settings") }) {
                        Icon(Icons.Default.Settings, contentDescription = "Einstellungen")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.toggleAgent() },
                containerColor = if (agentRunning) Color(0xFF2E7D32) else Color(0xFFE53935)
            ) {
                Icon(
                    imageVector = if (agentRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = if (agentRunning) "Agent stoppen" else "Agent starten"
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status-Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(
                                    color = if (agentRunning) Color(0xFF2E7D32) else Color(0xFFE53935),
                                    shape = CircleShape
                                )
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (agentRunning) "AKTIV" else "INAKTIV",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (agentRunning) Color(0xFF2E7D32) else Color(0xFFE53935)
                        )
                    }
                    Text("📶 ${uiState.onlineAssets}/${uiState.totalAssets} Assets",
                        style = MaterialTheme.typography.bodyMedium)
                    Text("🔋 ${uiState.batteryLevel}%",
                        style = MaterialTheme.typography.bodyMedium)
                    Text("⏱ ${uiState.lastSyncTime}",
                        style = MaterialTheme.typography.bodySmall)
                }
            }

            // 3 Statistik-Karten
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        value = "${uiState.onlineAssets}/${uiState.totalAssets}",
                        label = "Assets",
                        icon = Icons.Default.Devices,
                        color = MaterialTheme.colorScheme.primary
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        value = "${uiState.activeSearches}",
                        label = "Suchen",
                        icon = Icons.Default.Search,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        value = "${uiState.alertCount}",
                        label = "Alarme",
                        icon = Icons.Default.Warning,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            // Liste-Überschrift
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🎯 Geschützte Assets",
                        style = MaterialTheme.typography.titleMedium)
                    androidx.compose.material3.TextButton(
                        onClick = { nav.navigate("assets") }
                    ) { Text("Alle anzeigen →") }
                }
            }

            items(assets.take(5), key = { it.id }) { asset ->
                AssetCard(
                    asset = asset,
                    onClick = { nav.navigate("asset_detail/${asset.id}") }
                )
            }

            // Asset hinzufügen
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { nav.navigate("add_asset") }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("➕ Asset hinzufügen")
                        Spacer(Modifier.width(16.dp))
                        Button(onClick = { nav.navigate("scan") }) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("QR-Scan")
                        }
                    }
                }
            }

            // Agent-Footer
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "📊 Agent: ${if (agentRunning) "Aktiv" else "Inaktiv"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "🔄 Sync: ${uiState.lastSyncTime}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
