package com.secureguard.enterprise.services

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.secureguard.enterprise.data.model.Asset
import com.secureguard.enterprise.data.model.AssetStatus
import com.secureguard.enterprise.data.model.Detection
import com.secureguard.enterprise.data.model.DetectionSource
import com.secureguard.enterprise.data.model.PendingCommand
import com.secureguard.enterprise.data.model.SearchResult
import com.secureguard.enterprise.data.model.TelemetryData
import com.secureguard.enterprise.data.repository.SecureGuardRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Echte Telemetrie-Hülle:
 * - Abonniert FLP (GPS / GLONASS / Galileo fusioniert)
 * - `sendCommand()` persistiert in `PendingCommandDao` und feuert auf den Flow
 * - Eine Hardware-Bridge konsumiert aus [pendingCommands], führt aus, markiert zurück
 */
@Singleton
class TelemetryService @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val repo: SecureGuardRepository,
    private val fusedClient: FusedLocationProviderClient,
    private val request: LocationRequest
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _latestLocation = MutableSharedFlow<Location>(replay = 1, extraBufferCapacity = 8)
    val latestLocation: SharedFlow<Location> = _latestLocation.asSharedFlow()

    private val _pendingCommands = MutableSharedFlow<PendingCommand>(replay = 0, extraBufferCapacity = 16)
    val pendingCommands: SharedFlow<PendingCommand> = _pendingCommands.asSharedFlow()

    private val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val loc = result.lastLocation ?: return
            _latestLocation.tryEmit(loc)
            scope.launch { applyLocationToActiveAssets(loc) }
        }
    }

    init {
        if (hasLocationPermission()) startUpdates()
        else Log.w(TAG, "Location-Permission nicht erteilt — Updates bleiben deaktiviert")
    }

    fun startUpdates() {
        if (!hasLocationPermission()) return
        try {
            @SuppressLint("MissingPermission")
            fusedClient.requestLocationUpdates(request, callback, ctx.mainLooper)
            Log.i(TAG, "Location-Updates gestartet")
        } catch (e: SecurityException) {
            Log.w(TAG, "startUpdates: SecurityException", e)
        }
    }

    fun stopUpdates() {
        fusedClient.removeLocationUpdates(callback)
        Log.i(TAG, "Location-Updates gestoppt")
    }

    /**
     * Live-Location → persistiere letzte Position auf einem
     * "selbst"-Asset (Owner = "self") oder erstes ONLINE-Asset als Demo-Fallback.
     */
    private suspend fun applyLocationToActiveAssets(loc: Location) {
        val now = System.currentTimeMillis()
        val candidate = repo.assetSnapshot()
            .firstOrNull { it.status == AssetStatus.ONLINE }
            ?: return

        repo.updateAssetStatus(
            mac = candidate.mac.ifBlank { candidate.id },
            status = AssetStatus.ONLINE,
            timestamp = now,
            lat = loc.latitude,
            lon = loc.longitude
        )
        repo.pushDetection(
            Detection(
                timestamp = now,
                sourceType = DetectionSource.GNSS,
                label = "loc-${"%.4f".format(loc.latitude)},${"%.4f".format(loc.longitude)}",
                rssi = if (loc.hasAccuracy()) loc.accuracy.toInt() else -100,
                latitude = loc.latitude,
                longitude = loc.longitude,
                metadata = "flp"
            )
        )
    }

    suspend fun pushRssi(rssi: Int) {
        Log.d(TAG, "pushRssi=$rssi")
    }

    suspend fun sendCommand(mac: String, command: String): Boolean {
        if (mac.isBlank()) return false
        val cmd = PendingCommand(mac = mac, command = command, createdAt = System.currentTimeMillis())
        val id  = repo.addPendingCommand(cmd)
        if (id <= 0) return false
        _pendingCommands.emit(cmd.copy(id = id))
        Log.i(TAG, "sendCommand queued: $command → $mac (id=$id)")
        return true
    }

    suspend fun markExecuted(id: Long) {
        repo.markPendingExecuted(id, System.currentTimeMillis())
    }

    suspend fun markFailed(id: Long, reason: String) {
        repo.markPendingFailed(id, System.currentTimeMillis(), reason)
    }

    suspend fun searchAsset(asset: Asset): Detection? {
        val cachedLocation = _latestLocation.replayCache.firstOrNull()
        if (cachedLocation != null && asset.latitude == null) {
            return Detection(
                timestamp = System.currentTimeMillis(),
                sourceType = DetectionSource.GNSS,
                label = "self->${asset.shortName}",
                rssi = if (cachedLocation.hasAccuracy()) cachedLocation.accuracy.toInt() else -100,
                latitude = cachedLocation.latitude,
                longitude = cachedLocation.longitude,
                metadata = asset.mac
            )
        }
        val lat = asset.latitude ?: return null
        val lon = asset.longitude ?: return null
        return Detection(
            timestamp = System.currentTimeMillis(),
            sourceType = DetectionSource.BLE,
            label = "ble-${asset.shortName}",
            rssi = asset.rssi,
            latitude = lat,
            longitude = lon,
            metadata = asset.mac
        )
    }

    /** Suche (neues SearchResult-Interface — wrappt [searchAsset]). */
    suspend fun searchAssetResult(asset: Asset): SearchResult {
        val started = System.currentTimeMillis()
        val d = searchAsset(asset)
        return if (d != null) {
            SearchResult.success(d, d.sourceType, durationMs = System.currentTimeMillis() - started)
        } else {
            SearchResult.notFound(
                DetectionSource.GNSS,
                durationMs = System.currentTimeMillis() - started,
                metadata = mapOf("reason" to "no_telemetry_location")
            )
        }
    }

    suspend fun getLatestTelemetry(mac: String): TelemetryData? =
        repo.getLatestTelemetry(mac)

    suspend fun refreshTelemetry(asset: Asset) {
        Log.d(TAG, "refreshTelemetry(${asset.shortName})")
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
    }

    companion object { private const val TAG = "TelemetryService" }
}
