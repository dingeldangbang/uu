package com.secureguard.enterprise.ui.optical

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.secureguard.enterprise.data.model.DetectionSource
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * OpticalScanScreen — Live-Kameravorschau mit ML-Kit-Objekterkennung.
 *
 * Zeigt den CameraX-Preview + die letzten erkannten Objekte in Echtzeit.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpticalScanScreen(
    vm: OpticalScanViewModel = hiltViewModel()
) {
    val nav = rememberNavController()
    val ctx = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val lastDetection by vm.lastDetection.collectAsStateWithLifecycle()
    val isAnalyzing by vm.isAnalyzing.collectAsStateWithLifecycle()

    var hasCamera by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }

    val camLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCamera = granted }

    LaunchedEffect(Unit) {
        if (!hasCamera) camLauncher.launch(Manifest.permission.CAMERA)
    }

    val previewView = remember { PreviewView(ctx) }

    // CameraX binden/stoppen
    DisposableEffect(lifecycleOwner, hasCamera) {
        if (hasCamera) vm.startCamera(lifecycleOwner, previewView)
        onDispose { vm.stopCamera() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("👁️ Optische Erkennung") },
                navigationIcon = {
                    IconButton(onClick = { nav.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Zurück")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Kameravorschau
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                AndroidView(
                    factory = { previewView },
                    modifier = Modifier.fillMaxSize()
                )
                // Status-Overlay
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = if (isAnalyzing)
                        androidx.compose.ui.graphics.Color(0xCC2E7D32)
                    else
                        androidx.compose.ui.graphics.Color(0xCCE53935)
                ) {
                    Text(
                        text = if (isAnalyzing) "● ML-Kit aktiv" else "○ Kamera aus",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = androidx.compose.ui.graphics.Color.White
                    )
                }

                // Fokus-Rahmen — Zielbereich für die Objekterkennung
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(200.dp)
                        .border(3.dp, androidx.compose.ui.graphics.Color(0xCCFFFFFF), RoundedCornerShape(20.dp))
                )
            }

            // Letzte Detektion
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Letzte Erkennung",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(8.dp))
                    if (lastDetection == null) {
                        Text(
                            "Noch kein Objekt erkannt. Richte die Kamera auf ein Objekt.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        val d = lastDetection!!
                        Text(
                            "🏷️ ${d.label}",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "Konfidenz: ${"%.0f".format(d.confidence * 100)}%",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            "Quelle: ${d.sourceType.name} • ${
                                SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(d.timestamp))
                            }",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}
