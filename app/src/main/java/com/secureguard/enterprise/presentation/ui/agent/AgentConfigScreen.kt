package com.secureguard.enterprise.presentation.ui.agent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentConfigScreen(
    viewModel: AgentViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val nav = rememberNavController()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🤖 Agent Konfiguration") },
                navigationIcon = {
                    IconButton(onClick = { nav.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Zurück")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.saveSettings() }) {
                        Icon(Icons.Default.Save, contentDescription = "Speichern")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (uiState.agentRunning)
                            Color(0xFF2E7D32).copy(alpha = 0.1f)
                        else Color(0xFFE53935).copy(alpha = 0.1f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .background(
                                            if (uiState.agentRunning) Color(0xFF2E7D32)
                                            else Color(0xFFE53935),
                                            CircleShape
                                        )
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    if (uiState.agentRunning) "AKTIV" else "INAKTIV",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (uiState.agentRunning)
                                        Color(0xFF2E7D32) else Color(0xFFE53935)
                                )
                            }
                            Text("⏱ Laufzeit: ${uiState.runtime}",
                                 style = MaterialTheme.typography.bodySmall)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("📊 Gesamtdauer: ${uiState.progress.toInt()}%",
                             style = MaterialTheme.typography.bodySmall)
                        LinearProgressIndicator(
                            progress = uiState.progress / 100f,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Pending-Commands — zeigt live, dass die Bridge läuft
            item {
                val pendingList by viewModel.pending.collectAsState()
                val pendingCount = pendingList.size
                Card(modifier = Modifier.fillMaxWidth(),
                     colors = CardDefaults.cardColors(
                         containerColor = if (pendingCount > 0)
                             Color(0xFFFFB300).copy(alpha = 0.1f)
                         else Color(0xFF2E7D32).copy(alpha = 0.05f)
                     )) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "📋 CommandBridge",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                if (pendingCount > 0) "$pendingCount pending" else "idle",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (pendingCount > 0) Color(0xFFFFB300) else Color(0xFF2E7D32)
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            if (pendingCount > 0)
                                "Bridge wartet auf Abarbeitung; LATENZ ≈ 350 ms."
                            else
                                "Bridge läuft im Hintergrund — Befehle werden sofort verarbeitet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (pendingCount > 0) {
                            Spacer(Modifier.height(8.dp))
                            pendingList.take(3).forEach { c ->
                                Text(
                                    "  • ${c.command} → ${c.mac.takeLast(8)}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }

            // Dauer
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("📅 Gesamtdauer", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = uiState.duration == "1h",
                                onClick = { viewModel.setDuration("1h") },
                                label = { Text("1 Std.") }
                            )
                            FilterChip(
                                selected = uiState.duration == "6h",
                                onClick = { viewModel.setDuration("6h") },
                                label = { Text("6 Std.") }
                            )
                            FilterChip(
                                selected = uiState.duration == "24h",
                                onClick = { viewModel.setDuration("24h") },
                                label = { Text("24 Std.") }
                            )
                            FilterChip(
                                selected = uiState.duration == "1w",
                                onClick = { viewModel.setDuration("1w") },
                                label = { Text("1 Woche") }
                            )
                            FilterChip(
                                selected = uiState.duration == "unlimited",
                                onClick = { viewModel.setDuration("unlimited") },
                                label = { Text("∞") }
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = uiState.customDays.toString(),
                                onValueChange = {
                                    viewModel.setCustomDays(it.toIntOrNull() ?: 0)
                                },
                                label = { Text("Tage") },
                                modifier = Modifier.weight(1f)
                            )
                            Button(onClick = { viewModel.applyCustomDuration() }) {
                                Text("✔️ Speichern")
                            }
                        }
                    }
                }
            }

            // Intervall
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("⏱ Abfrageintervall", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = uiState.interval == 30,
                                onClick = { viewModel.setInterval(30) },
                                label = { Text("30 Sek.") }
                            )
                            FilterChip(
                                selected = uiState.interval == 60,
                                onClick = { viewModel.setInterval(60) },
                                label = { Text("1 Min.") }
                            )
                            FilterChip(
                                selected = uiState.interval == 300,
                                onClick = { viewModel.setInterval(300) },
                                label = { Text("5 Min.") }
                            )
                            FilterChip(
                                selected = uiState.interval == 900,
                                onClick = { viewModel.setInterval(900) },
                                label = { Text("15 Min.") }
                            )
                            FilterChip(
                                selected = uiState.interval == 3600,
                                onClick = { viewModel.setInterval(3600) },
                                label = { Text("1 Std.") }
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = uiState.customInterval.toString(),
                                onValueChange = {
                                    viewModel.setCustomInterval(it.toIntOrNull() ?: 30)
                                },
                                label = { Text("Sekunden") },
                                modifier = Modifier.weight(1f)
                            )
                            Button(onClick = { viewModel.applyCustomInterval() }) {
                                Text("✔️ Speichern")
                            }
                        }
                    }
                }
            }

            // Priorisierung
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("🎯 Priorisierung", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = uiState.priority == "high",
                                onClick = { viewModel.setPriority("high") },
                                label = { Text("Hoch") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFE53935).copy(alpha = 0.2f)
                                )
                            )
                            FilterChip(
                                selected = uiState.priority == "medium",
                                onClick = { viewModel.setPriority("medium") },
                                label = { Text("Mittel") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFFFB300).copy(alpha = 0.2f)
                                )
                            )
                            FilterChip(
                                selected = uiState.priority == "low",
                                onClick = { viewModel.setPriority("low") },
                                label = { Text("Niedrig") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF2E7D32).copy(alpha = 0.2f)
                                )
                            )
                        }
                        Row(
                            modifier = Modifier.padding(top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = uiState.dynamicPriority,
                                onCheckedChange = { viewModel.setDynamicPriority(it) }
                            )
                            Text("⚡ Dynamische Anpassung", style = MaterialTheme.typography.bodyMedium)
                        }
                        Row(
                            modifier = Modifier.padding(top = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = uiState.learningMode,
                                onCheckedChange = { viewModel.setLearningMode(it) }
                            )
                            Text("🔄 Lernmodus (rekursive Verbesserung)",
                                 style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = { viewModel.saveSettings() },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    Text("💾 Konfiguration speichern")
                }
            }
        }
    }
}
