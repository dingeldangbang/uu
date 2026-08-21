package com.secureguard.enterprise.presentation

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
import androidx.compose.ui.platform.LocalContext
import android.util.Log
import com.secureguard.enterprise.presentation.navigation.SecureGuardNavHost
import com.secureguard.enterprise.presentation.theme.SecureGuardTheme
import com.secureguard.enterprise.util.PermissionStatus
import dagger.hilt.android.AndroidEntryPoint

/**
 * **Android 11+ Runtime-Permissions** werden hier einmalig beim ersten
 * Composition abgefragt. Welche das sind, definiert zentral
 * [PermissionStatus.startupPermissions]:
 *
 *  · Alle Versionen:        FINE/COARSE_LOCATION
 *  · Android 11/12 (CT45P): + READ_PHONE_STATE
 *  · Android 12+ (API 31):  + BLUETOOTH_SCAN, BLUETOOTH_CONNECT
 *  · Android 13+ (API 33):  + POST_NOTIFICATIONS, NEARBY_WIFI_DEVICES
 *
 * Was diese Activity **NICHT** tut:
 * - Kein BETRIEBSVEREINBARUNG-Loading, kein Compliance-/Acceptance-Dialog.
 * - `ACCESS_BACKGROUND_LOCATION` wird weder deklariert noch abgefragt
 *   (Datenschutz-minimal; Tracking läuft im Vordergrund).
 * - `CAMERA` wird **nicht** vorab abgefragt, sondern kontextbezogen von
 *   `OpticalScanScreen` / `QrScanScreen` (Permission-in-Context).
 * - Normale Permissions (WIFI_STATE, CHANGE_WIFI_STATE, READ_BASIC_PHONE_STATE)
 *   werden nicht angefragt — sie sind beim Install bereits gewährt.
 *
 * Abgelehnte Berechtigungen werden protokolliert; den dauerhaften Status
 * zeigt das Panel „Berechtigungen" in den Settings.
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

private const val TAG = "Permissions"

@Composable
private fun RequestRequiredPermissions() {
    val ctx = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val denied = result.filterValues { !it }.keys
        if (denied.isEmpty()) {
            Log.i(TAG, "alle Start-Berechtigungen erteilt")
        } else {
            // Kein harter Abbruch: die Services prüfen zur Laufzeit erneut und
            // liefern SearchResult.error(...). Settings zeigt den Status an.
            Log.w(TAG, "abgelehnt: ${denied.joinToString { PermissionStatus.shortName(it) }}")
        }
    }

    LaunchedEffect(Unit) {
        val missing = PermissionStatus.startupPermissions()
            .filterNot { PermissionStatus.isGranted(ctx, it) }
        if (missing.isNotEmpty()) launcher.launch(missing.toTypedArray())
    }
}
