package com.secureguard.enterprise.services

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Build
import com.secureguard.enterprise.data.model.Asset
import com.secureguard.enterprise.data.model.Detection
import com.secureguard.enterprise.data.model.DetectionSource
import com.secureguard.enterprise.data.model.SearchResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * WiFiService — WLAN-Scan zur Lokalisierung.
 *
 * Erfordert:
 *   - Android 11: ACCESS_FINE_LOCATION (oder COARSE) für Scan-Ergebnisse
 *   - Android 12+: NEARBY_WIFI_DEVICES als Alternative
 *
 * Findet ein Asset anhand der BSSID. Liefern echte Scan-Ergebnisse
 * vom System-WifiManager, keine Dummy-Werte.
 */
@Singleton
class WiFiService @Inject constructor(
    @ApplicationContext private val ctx: Context
) {
    companion object {
        private const val SCAN_TIMEOUT_MS = 6000L
        private const val POLL_INTERVAL_MS = 1000L
    }

    private val wm: WifiManager? =
        ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager

    fun lastScan(): List<ScanResult> = try {
        if (hasLocationPermission()) (wm?.scanResults ?: emptyList())
        else emptyList()
    } catch (_: SecurityException) { emptyList() }

    private fun hasLocationPermission(): Boolean = try {
        val perm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // API 31+: NEARBY_WIFI_DEVICES ist die WLAN-Berechtigung,
            // LOCATION wird weiterhin mitbenötigt für Scan-Results.
            ctx.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED ||
            ctx.checkSelfPermission(Manifest.permission.NEARBY_WIFI_DEVICES) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            ctx.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        }
        perm
    } catch (_: Exception) { false }

    suspend fun startScan(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (!hasLocationPermission()) return@withContext false
                wm?.startScan() ?: false
            } catch (_: SecurityException) { false }
        }
    }

    /** Suche Asset anhand BSSID/MAC. */
    suspend fun searchAsset(asset: Asset): SearchResult = withContext(Dispatchers.IO) {
        val started = System.currentTimeMillis()
        if (asset.mac.isBlank()) {
            return@withContext SearchResult.error(
                DetectionSource.WIFI,
                "Asset-MAC leer",
                durationMs = System.currentTimeMillis() - started
            )
        }
        if (wm == null) {
            return@withContext SearchResult.error(
                DetectionSource.WIFI,
                "WifiManager nicht verfügbar",
                durationMs = System.currentTimeMillis() - started
            )
        }

        // Scan starten + Ergebnisse pollen (Android: Resultate erst nach Poll verfügbar)
        try {
            val targetNorm = asset.mac.replace(":", "").replace("-", "").uppercase()
            // Vorhandene Ergebnisse prüfen
            val existing = try { wm.scanResults?.toList() ?: emptyList() } catch (_: Exception) { emptyList() }
            val match = existing.firstOrNull {
                it.BSSID.replace(":", "").replace("-", "").uppercase() == targetNorm
            }

            if (match != null) {
                return@withContext buildResult(asset, match, started, existing.size)
            }

            // Neuen Scan triggern + pollen
            try { wm.startScan() } catch (_: SecurityException) { /* ignore */ }
            val deadline = started + SCAN_TIMEOUT_MS
            while (System.currentTimeMillis() < deadline) {
                delay(POLL_INTERVAL_MS)
                val scanned = try { wm.scanResults?.toList() ?: emptyList() } catch (_: Exception) { emptyList() }
                val found = scanned.firstOrNull {
                    it.BSSID.replace(":", "").replace("-", "").uppercase() == targetNorm
                }
                if (found != null) {
                    return@withContext buildResult(asset, found, started, scanned.size)
                }
            }

            SearchResult.notFound(
                DetectionSource.WIFI,
                durationMs = System.currentTimeMillis() - started,
                metadata = mapOf("target_bssid_norm" to targetNorm)
            )
        } catch (e: SecurityException) {
            SearchResult.error(DetectionSource.WIFI, "SecurityException: ${e.message}",
                durationMs = System.currentTimeMillis() - started)
        } catch (e: Exception) {
            SearchResult.error(DetectionSource.WIFI, e.message ?: "Unbekannter Fehler",
                durationMs = System.currentTimeMillis() - started)
        }
    }

    private fun buildResult(
        asset: Asset,
        scan: ScanResult,
        started: Long,
        totalScanned: Int
    ): SearchResult {
        val detection = Detection(
            timestamp = Date(),
            sourceType = DetectionSource.WIFI,
            label = scan.SSID ?: "WiFi:${scan.BSSID}",
            rssi = scan.level,
            latitude = null,
            longitude = null,
            metadata = scan.capabilities ?: "",
            assetMac = asset.mac,
            nodeId = scan.BSSID,
            isVerified = false,
            triangulationPoints = 1
        )
        return SearchResult.success(
            detection = detection,
            source = DetectionSource.WIFI,
            accuracy = accuracyFromRssi(scan.level),
            durationMs = System.currentTimeMillis() - started,
            metadata = mapOf(
                "bssid" to scan.BSSID,
                "ssid" to (scan.SSID ?: ""),
                "frequency" to scan.frequency,
                "networks_scanned" to totalScanned
            )
        )
    }

    private fun accuracyFromRssi(rssi: Int): Float = when {
        rssi > -40 -> 0.95f
        rssi > -60 -> 0.75f
        rssi > -80 -> 0.50f
        else -> 0.30f
    }
}
