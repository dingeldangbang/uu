package com.secureguard.enterprise.services

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult as LeScanResult
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.secureguard.enterprise.data.model.Asset
import com.secureguard.enterprise.data.model.Detection
import com.secureguard.enterprise.data.model.DetectionSource
import com.secureguard.enterprise.data.model.SearchResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Date
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * BLEService — BLE-Scan über Android Bluetooth LE API.
 *
 * Nutzt NICHT die Nordic-Bibliothek (niedrigere Dependency).
 * Stattdessen Direct-Callback auf BluetoothLeScanner.
 *
 * Android-11-Pflicht: ACCESS_FINE_LOCATION (BLE-Scan-Results filter GPS).
 */
@Singleton
class BLEService @Inject constructor(
    @ApplicationContext private val ctx: Context
) {
    companion object {
        private const val SCAN_TIMEOUT_MS = 8000L
        private const val FILTER_UUID_NTAG = "0000FEAA-0000-1000-8000-00805F9B34FB"
    }

    private val results = ConcurrentHashMap<String, LeScanResult>()

    private val btManager: BluetoothManager? =
        ctx.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val adapter: BluetoothAdapter? = btManager?.adapter

    /** Darf der Gerätename gelesen werden? (ab API 31 BLUETOOTH_CONNECT-pflichtig) */
    private fun canReadDeviceName(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                ctx, Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else true

    /** Gerätename defensiv lesen — ohne CONNECT wirft `device.name` SecurityException. */
    private fun safeDeviceName(device: android.bluetooth.BluetoothDevice?): String? =
        if (device == null || !canReadDeviceName()) null
        else runCatching { device.name }.getOrNull()

    /** BLE-Permission vorhanden (Android 12+ eigene Scan-Permission). */
    fun hasPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                ctx, Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                ctx, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    /** Suche Asset per BLE. */
    suspend fun searchAsset(asset: Asset): SearchResult = withContext(Dispatchers.IO) {
        val started = System.currentTimeMillis()
        if (!hasPermission()) {
            return@withContext SearchResult.error(DetectionSource.BLE, "BLE-Permission fehlt")
        }
        if (adapter == null || !adapter.isEnabled) {
            return@withContext SearchResult.error(DetectionSource.BLE, "BLE deaktiviert")
        }

        val scanner = adapter.bluetoothLeScanner
            ?: return@withContext SearchResult.error(DetectionSource.BLE, "Scanner nicht verfügbar")

        try {
            val found = withTimeoutOrNull(SCAN_TIMEOUT_MS) {
                suspendCancellableCoroutine<SearchResult> { cont ->
                    results.clear()
                    val callback = object : ScanCallback() {
                        override fun onScanResult(type: Int, result: LeScanResult) {
                            val mac = result.device?.address ?: return
                            if (mac.equals(asset.mac, ignoreCase = true) ||
                                mac.replace(":", "").equals(
                                    asset.mac.replace(":", "").replace("-", ""),
                                    ignoreCase = true
                                )
                            ) {
                                results[mac] = result
                                try { scanner.stopScan(this) } catch (_: Exception) {}
                                if (cont.isActive) cont.resume(parseToSuccess(asset, result, started))
                            }
                        }

                        override fun onScanFailed(errorCode: Int) {
                            if (cont.isActive) {
                                cont.resume(
                                    SearchResult.error(
                                        DetectionSource.BLE,
                                        "Scan failed code=$errorCode",
                                        durationMs = System.currentTimeMillis() - started
                                    )
                                )
                            }
                        }
                    }
                    cont.invokeOnCancellation {
                        try { scanner.stopScan(callback) } catch (_: Exception) {}
                    }
                    try {
                        scanner.startScan(callback)
                    } catch (e: SecurityException) {
                        cont.resume(SearchResult.error(DetectionSource.BLE, "SecurityException: ${e.message}",
                            durationMs = System.currentTimeMillis() - started))
                        return@suspendCancellableCoroutine
                    }
                }
            } ?: run {
                // Timeout: Scan gestoppt (invokeOnCancellation) — bestes Teilergebnis verwenden
                results.values.firstOrNull()
                    ?.let { parseToSuccess(asset, it, started) }
                    ?: SearchResult.notFound(
                        DetectionSource.BLE,
                        durationMs = System.currentTimeMillis() - started
                    )
            }
            found
        } catch (e: Exception) {
            SearchResult.error(DetectionSource.BLE, e.message ?: "Unbekannter Fehler",
                durationMs = System.currentTimeMillis() - started)
        }
    }

    private fun parseToSuccess(asset: Asset, result: LeScanResult, started: Long): SearchResult {
        val scanRecord = result.scanRecord ?: return SearchResult.notFound(
            DetectionSource.BLE,
            durationMs = System.currentTimeMillis() - started
        )
        val foundMac = result.device?.address ?: asset.mac
            val detection = Detection(
                timestamp = System.currentTimeMillis(),
                sourceType = DetectionSource.BLE,
                label = scanRecord.deviceName ?: safeDeviceName(result.device) ?: "BLE:${foundMac}",
            rssi = result.rssi,
            latitude = null,
            longitude = null,
            metadata = foundMac,
            assetMac = asset.mac,
            nodeId = foundMac,
            isVerified = false,
            triangulationPoints = 1
        )
        return SearchResult.success(
            detection = detection,
            source = DetectionSource.BLE,
            accuracy = accuracyFromRssi(result.rssi),
            durationMs = System.currentTimeMillis() - started,
            metadata = mapOf(
                "mac" to foundMac,
                "name" to (safeDeviceName(result.device) ?: "?"),
                "rssi" to result.rssi
            )
        )
    }

    private fun accuracyFromRssi(rssi: Int): Float = when {
        rssi > -50 -> 0.95f
        rssi > -70 -> 0.75f
        rssi > -90 -> 0.50f
        else -> 0.30f
    }
}

private object ContextCompat {
    fun checkSelfPermission(c: Context, p: String): Int =
        c.checkSelfPermission(p)
}
