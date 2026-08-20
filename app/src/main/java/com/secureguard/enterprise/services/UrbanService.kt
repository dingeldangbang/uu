package com.secureguard.enterprise.services

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Build
import android.telephony.CellInfo
import android.telephony.CellInfoGsm
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.secureguard.enterprise.data.model.Asset
import com.secureguard.enterprise.data.model.Detection
import com.secureguard.enterprise.data.model.DetectionSource
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class WifiResult(
    val ssid: String,
    val bssid: String,
    val rssi: Int,
    val frequency: Int
)

data class CellResult(
    val networkType: String,
    val rssi: Int,
    val cellId: Long?,
    val lac: Int?
)

/**
 * UrbanService — Echte WifiManager + TelephonyManager-Scans.
 *
 * - `scanWifiNetworks()` startet einen aktiven WLAN-Scan und liefert
 *   alle sichtbaren Netzwerke (SSID, BSSID, RSSI, Frequenz)
 * - `scanCellTowers()` liest Zell-Informationen (LTE/5G/GSM) mit RSSI
 * - `searchAsset(asset)` vergleicht Asset-MAC/RSSI mit der Umgebung
 *
 * Permissions (bereits im Manifest):
 *   ACCESS_WIFI_STATE, CHANGE_WIFI_STATE, READ_PHONE_STATE
 *   + Location (für WLAN-Scan ab Android 13)
 */
@Singleton
class UrbanService @Inject constructor(
    @ApplicationContext private val ctx: Context
) {
    private val wifiManager: WifiManager? = runCatching {
        ctx.getSystemService(WifiManager::class.java)
    }.getOrNull()

    private val telephonyManager: TelephonyManager? = runCatching {
        ctx.getSystemService(TelephonyManager::class.java)
    }.getOrNull()

    private var lastWifiScan: List<WifiResult> = emptyList()
    private var lastCellScan: List<CellResult> = emptyList()

    /** Startet einen WLAN-Scan (asynchron; Ergebnis via getLastWifiScan). */
    @SuppressLint("MissingPermission")
    fun startWifiScan(): Boolean {
        val wm = wifiManager ?: return false
        if (!hasWifiPermission()) return false

        return try {
            val started = wm.startScan()
            if (started) {
                lastWifiScan = wm.scanResults
                    .map { WifiResult(it.ssid, it.bssid, it.level, it.frequency) }
                    .sortedByDescending { it.rssi }
                Log.i(TAG, "WLAN-Scan: ${lastWifiScan.size} Netze")
            }
            started
        } catch (e: Exception) {
            Log.w(TAG, "WLAN-Scan fehlgeschlagen", e)
            false
        }
    }

    fun getLastWifiScan(): List<WifiResult> {
        // Immer aktualisieren, falls der Scan schon fertig ist
        wifiManager?.let { wm ->
            if (hasWifiPermission()) {
                lastWifiScan = wm.scanResults
                    .map { WifiResult(it.ssid, it.bssid, it.level, it.frequency) }
                    .sortedByDescending { it.rssi }
            }
        }
        return lastWifiScan
    }

    /** Liest Zell-Informationen mit RSSI (LTE/5G/GSM). */
    @SuppressLint("MissingPermission")
    fun scanCellTowers(): List<CellResult> {
        val tm = telephonyManager ?: return emptyList()
        if (!hasPhonePermission()) return emptyList()

        return try {
            val cells = tm.allCellInfo ?: emptyList()
            lastCellScan = cells.mapNotNull { cell -> cellToResult(cell) }
            Log.i(TAG, "Zell-Scan: ${lastCellScan.size} Zellen")
            lastCellScan
        } catch (e: Exception) {
            Log.w(TAG, "Zell-Scan fehlgeschlagen", e)
            emptyList()
        }
    }

    private fun cellToResult(cell: CellInfo): CellResult? {
        return when (cell) {
            is CellInfoLte -> CellResult(
                networkType = "LTE",
                rssi = cell.cellSignalStrength?.rssi ?: 0,
                cellId = cell.cellIdentity?.ci,
                lac = cell.cellIdentity?.tac
            )
            is CellInfoNr -> CellResult(
                networkType = "5G",
                rssi = cell.cellSignalStrength?.ssRsrp ?: 0,
                cellId = cell.cellIdentity?.nci,
                lac = null
            )
            is CellInfoGsm -> CellResult(
                networkType = "GSM",
                rssi = cell.cellSignalStrength?.rssi ?: 0,
                cellId = cell.cellIdentity?.cid,
                lac = cell.cellIdentity?.lac
            )
            else -> null
        }
    }

    /**
     * Spec: searchAsset(asset) → Detection? 
     * Sucht die Asset-MAC in den sichtbaren WLAN-Netzen bzw. vergleicht
     * mit gescannten Zellen. Liefert eine Detection, wenn der Tracker
     * in der Umgebung gesehen wurde.
     */
    suspend fun searchAsset(asset: Asset): Detection? {
        val wifis = getLastWifiScan()

        // 1) MAC-Match gegen BSSID (wenn Asset eine MAC hat)
        val macClean = asset.mac.replace(":", "").replace("-", "").uppercase()
        if (macClean.isNotBlank()) {
            val match = wifis.firstOrNull {
                it.bssid.replace(":", "").replace("-", "").uppercase() == macClean
            }
            if (match != null) {
                return Detection(
                    timestamp = System.currentTimeMillis(),
                    sourceType = DetectionSource.WIFI,
                    label = "wifi:${asset.shortName}",
                    rssi = match.rssi,
                    latitude = asset.latitude,
                    longitude = asset.longitude,
                    metadata = "bssid-match ssid=${match.ssid}"
                )
            }
        }

        // 2) Zell-Scan (wenn verfügbar)
        if (hasPhonePermission()) {
            val cells = scanCellTowers()
            if (cells.isNotEmpty()) {
                val strongest = cells.maxByOrNull { it.rssi }
                if (strongest != null) {
                    return Detection(
                        timestamp = System.currentTimeMillis(),
                        sourceType = DetectionSource.WIFI,
                        label = "cell:${strongest.networkType}",
                        rssi = strongest.rssi,
                        latitude = asset.latitude,
                        longitude = asset.longitude,
                        metadata = "cell-id=${strongest.cellId} lac=${strongest.lac}"
                    )
                }
            }
        }

        return null
    }

    /**
     * WiFi-Scan-Berechtigung.
     *
     * Wichtig für Android 11 (API 30): `WifiManager.getScanResults()` liefert
     * Ergebnisse NUR, wenn zusätzlich eine Standortberechtigung (FINE oder
     * COARSE) erteilt wurde. Ab Android 12/13 kommt NEARBY_WIFI_DEVICES dazu.
     */
    private fun hasWifiPermission(): Boolean {
        val wifiState = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_WIFI_STATE) ==
                PackageManager.PERMISSION_GRANTED
        val location = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        return wifiState && location
    }

    private fun hasPhonePermission(): Boolean {
        val perm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.READ_BASIC_PHONE_STATE
        else Manifest.permission.READ_PHONE_STATE
        return ContextCompat.checkSelfPermission(ctx, perm) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private const val TAG = "UrbanService"
    }
}
