package com.secureguard.enterprise.presentation.ui.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.compose.rememberNavController
import com.secureguard.enterprise.data.repository.SettingsRepository
import com.secureguard.enterprise.util.PermissionStatus
import com.secureguard.enterprise.util.rememberToast
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    vm: SettingsViewModel = hiltViewModel()
) {
    val toast = rememberToast()
    val state by vm.state.collectAsState()
    val scope = rememberCoroutineScope()
    val nav = rememberNavController()
    var retentionInput by remember { mutableStateOf(state.retentionDays.toString()) }

    LaunchedEffect(state.retentionDays) {
        retentionInput = state.retentionDays.toString()
    }

    fun toggle(key: String, on: Boolean, label: String) {
        vm.toggle(key)
        toast("${label}: ${if (!on) "AN" else "AUS"}", false)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("⚙️ Settings", style = MaterialTheme.typography.headlineMedium)

        HorizontalDivider()

        ToggleRow("📡 LoRa / BLE",
                  state.isEnabled(SettingsRepository.KEY_LORA))    { toggle(SettingsRepository.KEY_LORA, it, "LoRa") }
        ToggleRow("📈 Telemetrie",
                  state.isEnabled(SettingsRepository.KEY_TELEMETRY)) { toggle(SettingsRepository.KEY_TELEMETRY, it, "Telemetrie") }
        ToggleRow("👁️ Optische Erkennung",
                  state.isEnabled(SettingsRepository.KEY_OPTICAL))  { toggle(SettingsRepository.KEY_OPTICAL, it, "Optik") }
        ToggleRow("🌍 Crowdsourcing",
                  state.isEnabled(SettingsRepository.KEY_CROWD))    { toggle(SettingsRepository.KEY_CROWD, it, "Crowdsourcing") }
        ToggleRow("🌙 Dark Mode",
                  state.isEnabled(SettingsRepository.KEY_DARK))     { toggle(SettingsRepository.KEY_DARK, it, "Dark Mode") }

        HorizontalDivider()
        Text("Aufbewahrung", style = MaterialTheme.typography.titleSmall)

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Tage: ", style = MaterialTheme.typography.bodyMedium)
            androidx.compose.material3.OutlinedTextField(
                value = retentionInput,
                onValueChange = { retentionInput = it.filter(Char::isDigit).take(3) },
                modifier = Modifier.fillMaxWidth(0.4f),
                singleLine = true
            )
            Spacer(Modifier.weight(1f))
            Button(onClick = {
                scope.launch {
                    val d = retentionInput.toIntOrNull() ?: 30
                    vm.setRetention(d)
                    toast("Retention: $d Tage gespeichert", true)
                }
            }) { Text("Speichern") }
        }

        HorizontalDivider()
        PermissionPanel()

        HorizontalDivider()
        Text("Agent & Externe Dienste", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { nav.navigate("nodes") },
            modifier = Modifier.fillMaxWidth()
        ) { Text("📡 Abfrageknoten (API-Node-Manager)") }
        Spacer(Modifier.height(4.dp))
        Button(
            onClick = { nav.navigate("tempmail") },
            modifier = Modifier.fillMaxWidth()
        ) { Text("📧 Temporäre E-Mail (OTP)") }

        HorizontalDivider()
        Button(onClick = { vm.requestDbReset(); toast("DB wird beim nächsten Start zurückgesetzt", true) },
               modifier = Modifier.fillMaxWidth()) {
            Text("DB zurücksetzen")
        }
        Button(onClick = {
            scope.launch {
                vm.clearLogs()
                toast("Alte Erkennungen & Alerts gelöscht", true)
            }
        },
               modifier = Modifier.fillMaxWidth()) {
            Text("Alte Logs löschen")
        }

        Spacer(Modifier.height(16.dp))
        Text("Version 1.0.0 • Build 1",
             style = MaterialTheme.typography.bodySmall,
             color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ToggleRow(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = value, onCheckedChange = onChange)
    }
}

/**
 * Status-Panel „Berechtigungen".
 *
 * Zweck: kein Suchkanal soll stumm sterben, weil eine Berechtigung fehlt.
 * Fehlende Rechte lassen sich direkt erneut anfragen; bei dauerhafter
 * Ablehnung führt der zweite Button in die System-App-Einstellungen.
 */
@Composable
private fun PermissionPanel() {
    val ctx = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var refresh by remember { mutableStateOf(0) }

    // Nach Rückkehr aus den System-Einstellungen neu auswerten.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refresh++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { refresh++ }

    val channels = remember(refresh) {
        PermissionStatus.allChannels().map { ch ->
            Triple(ch, PermissionStatus.isSatisfied(ctx, ch), PermissionStatus.missing(ctx, ch))
        }
    }
    val missingAll = channels.flatMap { it.third }.distinct()

    Text("Berechtigungen", style = MaterialTheme.typography.titleSmall)

    channels.forEach { (channel, ok, missing) ->
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(if (ok) "✅" else "⚠️")
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(channel.label, style = MaterialTheme.typography.bodyMedium)
                if (!ok) {
                    Text(
                        "fehlt: " + missing.joinToString { PermissionStatus.shortName(it) },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }

    if (missingAll.isEmpty()) {
        Text(
            "Alle Kanäle sind einsatzbereit.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    } else {
        Spacer(Modifier.height(4.dp))
        Button(
            onClick = { launcher.launch(missingAll.toTypedArray()) },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Fehlende Berechtigungen anfragen") }
        Button(
            onClick = {
                ctx.startActivity(
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", ctx.packageName, null)
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("App-Einstellungen öffnen") }
    }
}
