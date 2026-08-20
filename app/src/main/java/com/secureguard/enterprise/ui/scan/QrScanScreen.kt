package com.secureguard.enterprise.ui.scan

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.compose.rememberNavController
import com.secureguard.enterprise.util.DeviceCompat
import com.secureguard.enterprise.util.rememberToast
import kotlinx.coroutines.flow.collectLatest
import java.net.URLEncoder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrScanScreen(
    vm: ScanViewModel = hiltViewModel(),
) {
    val nav = rememberNavController()
    val ctx = LocalContext.current
    val toast = rememberToast()
    val available = remember { vm.available() }
    val triggered by vm.triggered.collectAsState(initial = false)

    var manualPayload by remember { mutableStateOf("") }
    var lastScan by remember { mutableStateOf<String?>(null) }
    var usingManual by remember { mutableStateOf(!available) }

    // Android 13+ Notification-Permission (auf Android 11/CT45P gibt es
    // keinen POST_NOTIFICATIONS-Dialog — Notifications werden immer angezeigt)
    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* ignore – zeigt nur den Dialog */ }

    LaunchedEffect(Unit) {
        if (DeviceCompat.isAndroid13Plus &&
            !ctx.permGranted(Manifest.permission.POST_NOTIFICATIONS)) {
            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Honeywell claim/release an den Lifecycle binden.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, usingManual) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    if (!usingManual) vm.claim()
                }
                Lifecycle.Event.ON_PAUSE  -> vm.release()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            vm.release()
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Live-Scans konsumieren
    LaunchedEffect(usingManual) {
        if (!usingManual) {
            vm.scans.collectLatest { payload ->
                lastScan = payload
                val encoded = URLEncoder.encode(payload, "UTF-8")
                nav.navigate("add_asset?payload=$encoded") {
                    popUpTo("scan") { inclusive = false }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("📷 QR / Barcode-Scan") },
                navigationIcon = {
                    IconButton(onClick = { nav.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Zurück")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (available) Icons.Default.CameraAlt else Icons.Default.Edit,
                            contentDescription = null,
                            tint = if (available) Color(0xFF2E7D32) else Color(0xFFFFB300)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (available)
                                "Honeywell Scanner: aktiv (CT45P)"
                            else
                                "Honeywell nicht verfügbar – manuelle Eingabe",
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (available)
                            "Hardware-Trigger oder SW-Button drücken, um zu scannen."
                        else
                            "Tippe unten den Payload ein – z.B. secureguard://asset?id=…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Hardware-Trigger-Status live
            AssistChip(
                onClick = { /* decorative */ },
                label = {
                    Text(if (triggered) "Hardware-Trigger: aktiv" else "Hardware-Trigger: bereit")
                },
                leadingIcon = {
                    Icon(Icons.Default.FlashlightOn, contentDescription = null)
                }
            )

            // Visueller Scan-Fokus-Rahmen (Hilfe für die Trigger-Position)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .border(3.dp, Color(0xFFFFB300), RoundedCornerShape(16.dp))
                ) {
                    // Eck-Markierungen
                    Box(Modifier.align(Alignment.TopStart).size(24.dp)
                        .border(4.dp, Color(0xFFFFB300), RoundedCornerShape(topStart = 16.dp)))
                    Box(Modifier.align(Alignment.TopEnd).size(24.dp)
                        .border(4.dp, Color(0xFFFFB300), RoundedCornerShape(topEnd = 16.dp)))
                    Box(Modifier.align(Alignment.BottomStart).size(24.dp)
                        .border(4.dp, Color(0xFFFFB300), RoundedCornerShape(bottomStart = 16.dp)))
                    Box(Modifier.align(Alignment.BottomEnd).size(24.dp)
                        .border(4.dp, Color(0xFFFFB300), RoundedCornerShape(bottomEnd = 16.dp)))
                }
                Text(
                    "Scan-Bereich — Hardware-Trigger drücken",
                    modifier = Modifier.align(Alignment.BottomCenter).padding(top = 8.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (available && !usingManual) {
                Button(
                    onClick = { vm.softScan() },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("🔫 Software-Scan auslösen") }

                Button(
                    onClick = { usingManual = true; vm.release() },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("✏️ Stattdessen manuell eingeben") }
            } else {
                OutlinedTextField(
                    value = manualPayload,
                    onValueChange = { manualPayload = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Payload (JSON / secureguard://asset?id=… oder id\\nname\\nmac)") },
                    supportingText = { Text("Browse-Linie wachsend pflichtfrei dokumentiert.") }
                )
                Button(
                    onClick = {
                        if (manualPayload.isBlank()) {
                            toast("Bitte Payload eingeben", false)
                        } else {
                            val p = vm.parse(manualPayload)
                            if (p.id.isBlank() && p.name.isBlank() && p.mac.isBlank()) {
                                toast("Ungültiges Format", true)
                            } else {
                                val encoded = URLEncoder.encode(manualPayload, "UTF-8")
                                nav.navigate("add_asset?payload=$encoded")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("OK → AddAsset") }

                if (available) {
                    Button(
                        onClick = {
                            usingManual = false
                            // Erneute Claim im nächsten ON_RESUME-Effekt:
                            vm.claim()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("📷 Zurück zum Hardware-Scanner") }
                }
            }

            // zuletzt gelesen
            lastScan?.let { code ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(4.dp),
                    colors = androidx.compose.material3.CardDefaults.cardColors(
                        containerColor = Color(0xFF2E7D32).copy(alpha = 0.08f)
                    )
                ) {
                    Text(
                        "✅ Letzter Scan: $code",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Hint unten
            Spacer(Modifier.weight(1f))
            Text(
                "CT45P-Hardware-Buttons: Standardisiert mittlere Trigger oder Lanyard.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun Context.permGranted(name: String) =
    ContextCompat.checkSelfPermission(this, name) == PackageManager.PERMISSION_GRANTED
