package com.secureguard.enterprise.presentation

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.secureguard.enterprise.presentation.navigation.SecureGuardNavHost
import com.secureguard.enterprise.presentation.theme.SecureGuardTheme
import com.secureguard.enterprise.util.DeviceCompat
import dagger.hilt.android.AndroidEntryPoint

/**
 * **Android 11+ Runtime-Permissions** werden hier einmalig beim ersten
 * Composition abgefragt — automatisch versionsabhängig:
 *
 *  · Android 11 (CT45P, API 30):
 *      FINE/COARSE_LOCATION · CAMERA · READ_PHONE_STATE
 *      (WIFI_STATE/CHANGE_WIFI_STATE sind normal — auto-granted)
 *  · Android 12+ (API 31):   + BLUETOOTH_SCAN, BLUETOOTH_CONNECT
 *  · Android 13+ (API 33):   + POST_NOTIFICATIONS, NEARBY_WIFI_DEVICES,
 *                            READ_BASIC_PHONE_STATE statt READ_PHONE_STATE
 *
 * Was diese Activity **NICHT** tut:
 * - Es wird **kein** BETRIEBSVEREINBARUNG-Loading durchgeführt.
 * - Es gibt **keinen** Compliance-/Acceptance-Dialog.
 * - `ACCESS_BACKGROUND_LOCATION` wird bewusst NICHT abgefragt
 *   (Datenschutz-minimal; Tracking läuft im Vordergrund).
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SecureGuardApp() }
    }
}

@Composable
fun SecureGuardApp() {
    SecureGuardTheme(darkTheme = true) {
        Surface(modifier = Modifier.fillMaxSize()) {
            RequestRequiredPermissions()
            SecureGuardNavHost()
        }
    }
}

@Composable
private fun RequestRequiredPermissions() {
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* Resultate werden nicht gerendert, TelemetryService prüft runtime erneut. */ }

    LaunchedEffect(Unit) {
        val perms = mutableListOf<String>()

        // ── Alle Android-Versionen (inkl. Android 11 / CT45P) ──
        perms += Manifest.permission.ACCESS_FINE_LOCATION
        perms += Manifest.permission.ACCESS_COARSE_LOCATION
        perms += Manifest.permission.CAMERA
        perms += Manifest.permission.ACCESS_WIFI_STATE   // normal — wird sofort granted
        perms += Manifest.permission.CHANGE_WIFI_STATE   // normal — wird sofort granted

        // ── Android 13+ (API 33+) ──
        if (DeviceCompat.isAndroid13Plus) {
            perms += Manifest.permission.POST_NOTIFICATIONS
            perms += Manifest.permission.NEARBY_WIFI_DEVICES
            perms += Manifest.permission.READ_BASIC_PHONE_STATE
        } else {
            // ── Android 11/12 (CT45P): klassische Phone-State-Berechtigung ──
            perms += Manifest.permission.READ_PHONE_STATE
        }

        // ── Android 12+ (API 31+): neues BLE-Modell ──
        if (DeviceCompat.isAndroid12Plus) {
            perms += Manifest.permission.BLUETOOTH_SCAN
            perms += Manifest.permission.BLUETOOTH_CONNECT
        }

        launcher.launch(perms.distinct().toTypedArray())
    }
}
