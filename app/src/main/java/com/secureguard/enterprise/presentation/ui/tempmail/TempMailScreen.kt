package com.secureguard.enterprise.presentation.ui.tempmail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController

/**
 * TempMailScreen — Dashboard für temporäre E-Mail-Inboxen und OTP-Empfang.
 *
 * Verwendung (legitim):
 *  - Automatische Registrierung in firmeninternen Testumgebungen
 *  - API-Key-Generierung für autorisierte Dienste
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TempMailScreen(
    viewModel: TempMailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentInbox by viewModel.currentInbox.collectAsState()
    val lastOTP by viewModel.lastOTP.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val nav = rememberNavController()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("📧 Temporäre E-Mail") },
                navigationIcon = {
                    IconButton(onClick = { nav.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Zurück")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.clearInbox() }) {
                        Icon(Icons.Default.Delete, contentDescription = "Leeren")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Aktuelle Inbox
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (currentInbox != null)
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("📨 Aktuelle Inbox", style = MaterialTheme.typography.titleMedium)
                        if (currentInbox != null) {
                            Text("📧 ${currentInbox?.email}", style = MaterialTheme.typography.bodyLarge)
                            Text("🔑 Token: ${currentInbox?.token?.take(20) ?: "—"}...", style = MaterialTheme.typography.bodySmall)
                            Text("📋 ID: ${currentInbox?.inboxId}", style = MaterialTheme.typography.bodySmall)
                        } else {
                            Text("Keine Inbox erstellt", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }

            // Aktionen
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.createInbox() },
                        modifier = Modifier.weight(1f),
                        enabled = !isProcessing
                    ) {
                        if (isProcessing) CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        else Text("📬 Neue Inbox")
                    }
                    Button(
                        onClick = { viewModel.waitForOTP() },
                        modifier = Modifier.weight(1f),
                        enabled = currentInbox != null && !isProcessing
                    ) {
                        Text("⏳ OTP abrufen")
                    }
                }
            }

            // OTP-Ergebnis
            if (lastOTP != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (lastOTP?.success == true)
                                Color(0xFF2E7D32).copy(alpha = 0.1f)
                            else Color(0xFFE53935).copy(alpha = 0.1f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                if (lastOTP?.success == true) "✅ OTP empfangen!" else "❌ Kein OTP",
                                style = MaterialTheme.typography.titleMedium
                            )
                            if (lastOTP?.success == true) {
                                Text("🔑 OTP: ${lastOTP?.otp}", style = MaterialTheme.typography.titleLarge)
                                Text("📧 Von: ${lastOTP?.from}", style = MaterialTheme.typography.bodySmall)
                                Text("📋 Betreff: ${lastOTP?.subject}", style = MaterialTheme.typography.bodySmall)
                                Text("📧 Empfänger: ${lastOTP?.email}", style = MaterialTheme.typography.bodySmall)
                            } else {
                                Text(lastOTP?.error ?: "Fehler", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }

            // Status-Log
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("📋 Log", style = MaterialTheme.typography.titleMedium)
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        if (uiState.logEntries.isEmpty()) {
                            Text("Keine Einträge", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            uiState.logEntries.takeLast(10).forEach { entry ->
                                Text(entry, style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(vertical = 2.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
