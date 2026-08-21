package com.secureguard.enterprise.presentation.ui.assets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.secureguard.enterprise.data.model.AssetStatus
import com.secureguard.enterprise.presentation.components.ActionButton
import com.secureguard.enterprise.util.rememberToast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetDetailScreen(
    assetId: String,
    viewModel: AssetDetailViewModel = hiltViewModel()
) {
    val asset by viewModel.asset.collectAsState()
    val detections by viewModel.detections.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val searchResult by viewModel.searchResult.collectAsState()
    val actionResult by viewModel.actionResult.collectAsState()
    val nav = rememberNavController()
    val toast = rememberToast()
    var menuOpen by remember { mutableStateOf(false) }
    var deleteOpen by remember { mutableStateOf(false) }

    LaunchedEffect(assetId) { viewModel.loadAsset(assetId) }

    val current = asset
    if (current == null) {
        Scaffold(
            topBar = { TopAppBar(title = { Text("Lade…") }) }
        ) { padding ->
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
        }
        return@Scaffold
    }

    if (deleteOpen) {
        AlertDialog(
            onDismissRequest = { deleteOpen = false },
            title = { Text("Asset löschen?") },
            text  = { Text("Möchtest du \"${current.name}\" wirklich aus der DB entfernen?") },
            confirmButton = {
                TextButton(onClick = {
                    deleteOpen = false
                    nav.popBackStack()
                }) { Text("Löschen", color = Color(0xFFE53935)) }
            },
            dismissButton = {
                TextButton(onClick = { deleteOpen = false }) { Text("Abbrechen") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(current.shortName) },
                navigationIcon = {
                    IconButton(onClick = { nav.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Zurück")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshTelemetry() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Aktualisieren")
                    }
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menü")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Löschen") },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                            onClick = {
                                menuOpen = false
                                deleteOpen = true
                            }
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = when (current.status) {
                            AssetStatus.ONLINE      -> Color(0xFF2E7D32).copy(alpha = 0.1f)
                            AssetStatus.MAINTENANCE -> Color(0xFFFFB300).copy(alpha = 0.1f)
                            AssetStatus.OFFLINE     -> Color(0xFFE53935).copy(alpha = 0.1f)
                            else                    -> Color.Gray.copy(alpha = 0.1f)
                        }
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
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
                                            color = when (current.status) {
                                                AssetStatus.ONLINE      -> Color(0xFF2E7D32)
                                                AssetStatus.MAINTENANCE -> Color(0xFFFFB300)
                                                AssetStatus.OFFLINE     -> Color(0xFFE53935)
                                                else                    -> Color.Gray
                                            },
                                            shape = CircleShape
                                        )
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    current.status.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = when (current.status) {
                                        AssetStatus.ONLINE      -> Color(0xFF2E7D32)
                                        AssetStatus.MAINTENANCE -> Color(0xFFFFB300)
                                        AssetStatus.OFFLINE     -> Color(0xFFE53935)
                                        else                    -> Color.Gray
                                    }
                                )
                            }
                            Text(
                                "📍 ${current.latitude ?: "unbekannt"}, ${current.longitude ?: "unbekannt"}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                "📶 ${current.rssi} dBm",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "⏱ Letzte Aktualisierung: ${
                                current.lastSeen?.let {
                                    SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(it))
                                } ?: "Nie"
                            }",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Map-Preview
            item {
                Card(modifier = Modifier.fillMaxWidth().height(150.dp)) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📍 Karte (öffnen via Karte-Tab)",
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (current.latitude != null && current.longitude != null) {
                                Text(
                                    "${"%.5f".format(current.latitude)}, ${"%.5f".format(current.longitude)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Telemetrie (echte Felder)
            item {
                Text("📊 Telemetrie", style = MaterialTheme.typography.titleMedium)
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            RealTelemetryItem("🔋 Batterie", "${current.batteryPercent}%")
                            RealTelemetryItem("📶 Signal",   "${current.rssi} dBm")
                            RealTelemetryItem("📁 Kategorie", current.category.name)
                            RealTelemetryItem("🏷️ Status",  current.status.name)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            RealTelemetryItem("🔑 ID",     current.id.take(8))
                            RealTelemetryItem("📡 MAC",    current.mac.takeLast(8))
                        }
                    }
                }
            }

            // Asset-Tags
            if (current.tags.isNotEmpty() || current.location != null || current.owner.isNotBlank()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            current.location?.let {
                                AssistChip(
                                    onClick = { nav.navigate("map") },
                                    label = { Text("📍 $it") }
                                )
                            }
                            current.owner.takeIf { it.isNotBlank() }?.let {
                                AssistChip(
                                    onClick = { toast("Besitzer: $it", false) },
                                    label = { Text("👤 $it") }
                                )
                            }
                            current.tags.take(3).forEach { t ->
                                AssistChip(
                                    onClick = { toast("Tag: #$t", false) },
                                    label = { Text("#$t") },
                                    colors = AssistChipDefaults.assistChipColors(
                                        labelColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Aktionen
            item {
                Text("🎯 Aktionen", style = MaterialTheme.typography.titleMedium)
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ActionButton(
                                modifier = Modifier.weight(1f),
                                icon  = Icons.Default.Warning,
                                label = "🔔 Alarm",
                                onClick = { viewModel.executeAction(ActionType.ALARM) },
                                enabled = current.status == AssetStatus.ONLINE
                            )
                            ActionButton(
                                modifier = Modifier.weight(1f),
                                icon  = Icons.Default.Lightbulb,
                                label = "💡 Blinken",
                                onClick = { viewModel.executeAction(ActionType.LIGHT) },
                                enabled = current.status == AssetStatus.ONLINE
                            )
                            ActionButton(
                                modifier = Modifier.weight(1f),
                                icon  = Icons.Default.PowerSettingsNew,
                                label = "🔇 Motor",
                                onClick = { viewModel.executeAction(ActionType.MOTOR_OFF) },
                                enabled = current.status == AssetStatus.ONLINE
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ActionButton(
                                modifier = Modifier.weight(1f),
                                icon  = Icons.Default.BatteryAlert,
                                label = "🔋 Batterie",
                                onClick = { viewModel.executeAction(ActionType.BATTERY) },
                                enabled = current.status == AssetStatus.ONLINE
                            )
                            ActionButton(
                                modifier = Modifier.weight(1f),
                                icon  = Icons.Default.Message,
                                label = "📝 Nachricht",
                                onClick = { viewModel.executeAction(ActionType.MESSAGE) },
                                enabled = current.status == AssetStatus.ONLINE
                            )
                            ActionButton(
                                modifier = Modifier.weight(1f),
                                icon  = Icons.Default.LocationOn,
                                label = "📍 Position",
                                onClick = { viewModel.executeAction(ActionType.POSITION) },
                                enabled = current.status == AssetStatus.ONLINE
                            )
                        }
                        if (actionResult != null && actionResult !=
                            com.secureguard.enterprise.data.model.ActionResult.Processing) {
                            Text(
                                text = if (actionResult!!.success) "✅ ${actionResult!!.message}"
                                       else "❌ ${actionResult!!.message}",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (actionResult!!.success) Color(0xFF2E7D32) else Color(0xFFE53935),
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }
            }

            // Suchoptionen
            item {
                Text("🔍 Suchoptionen", style = MaterialTheme.typography.titleMedium)
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            androidx.compose.material3.Button(
                                modifier = Modifier.weight(1f),
                                onClick = { viewModel.startSearch() },
                                enabled = !isSearching
                            ) {
                                if (isSearching) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                                } else {
                                    Text("🔄 Suche starten")
                                }
                            }
                            androidx.compose.material3.Button(
                                modifier = Modifier.weight(1f),
                                onClick = { /* externe Quellen */ },
                                enabled = current.externalAllowed
                            ) { Text("🌍 Extern") }
                            androidx.compose.material3.Button(
                                modifier = Modifier.weight(1f),
                                onClick = { nav.navigate("optical") }
                            ) { Text("👁️ Kamera") }
                        }
                        if (searchResult != null) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = if (searchResult!!.found)
                                    "✅ Gefunden! RSSI: ${searchResult!!.detection?.rssi} dBm"
                                else "❌ Nicht gefunden",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            // Historie
            item {
                Text("📋 Historie (${detections.size})",
                    style = MaterialTheme.typography.titleMedium)
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (detections.isEmpty()) {
                            Text(
                                "Keine Historieneinträge",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            detections.take(10).forEach { d ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                                            .format(Date(d.timestamp)),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        "${d.sourceType.name} | 📶 ${d.rssi} dBm",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    if (d.latitude != null && d.longitude != null) {
                                        Text(
                                            "📍 ${"%.4f".format(d.latitude)}, ${"%.4f".format(d.longitude)}",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RealTelemetryItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.bodySmall,
             color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
