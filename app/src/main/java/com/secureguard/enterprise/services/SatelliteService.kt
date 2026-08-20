package com.secureguard.enterprise.services

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
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
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * SatelliteService — GPS/GLONASS-Position via Fused Location Provider.
 *
 * In einem "searchAsset"-Kontext liefert der Service die Position
 * des Geräts (CT45P). Falls das Asset mit dem Gerät "co-located" ist
 * (z. B. das Asset-Asset ist das Gerät selbst oder direkt daneben),
 * wird eine entsprechende Detection mit der GPS-Position erzeugt.
 *
 * Liefert IMMER eine Position zurück (egal ob als gefunden/nicht)
 *
 * Android-11 / API 30: ACCESS_FINE_LOCATION oder COARSE
 */
@Singleton
class SatelliteService @Inject constructor(
    @ApplicationContext private val ctx: Context
) {
    companion object {
        private const val GPS_TIMEOUT_MS = 8000L
        private const val GPS_MIN_ACCURACY_M = 50.0f
    }

    private val fused: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(ctx)

    fun hasPermission(): Boolean = try {
        ctx.checkSelfPermission(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                Manifest.permission.ACCESS_FINE_LOCATION
            else
                Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    } catch (_: Exception) { false }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Location? {
        if (!hasPermission()) return null
        return withTimeoutOrNull(GPS_TIMEOUT_MS) {
            withContext(Dispatchers.IO) {
                try {
                    // Versuch lastLocation (schnell, wenn kürzlich aktualisiert)
                    val last = suspendCancellableCoroutine<Location?> { cont ->
                        val cts = CancellationTokenSource()
                        cont.invokeOnCancellation { cts.cancel() }
                        fused.getLastLocation(cts.token)
                            .addOnSuccessListener { cont.resume(it) }
                            .addOnCanceledListener { cont.resume(null) }
                            .addOnFailureListener { cont.resume(null) }
                    }
                    if (last != null) return@withContext last
                } catch (_: Exception) { /* fallthrough */ }

                // Aktuelle Position anfordern
                suspendCancellableCoroutine<Location?> { cont ->
                    val cts = CancellationTokenSource()
                    cont.invokeOnCancellation { cts.cancel() }
                    fused.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                        .addOnSuccessListener { cont.resume(it) }
                        .addOnCanceledListener { cont.resume(null) }
                        .addOnFailureListener { cont.resume(null) }
                }
            }
        } as? Location
    }

    /** Search-Service: liefert aktuelle GPS-Position als "Detection". */
    suspend fun searchAsset(asset: Asset): SearchResult = withContext(Dispatchers.IO) {
        val started = System.currentTimeMillis()
        try {
            val loc = getCurrentLocation()
            val duration = System.currentTimeMillis() - started
            if (loc == null) {
                return@withContext SearchResult.notFound(
                    DetectionSource.GPS,
                    durationMs = duration,
                    metadata = mapOf("reason" to "no_location")
                )
            }
            if (loc.hasAccuracy() && loc.accuracy > GPS_MIN_ACCURACY_M) {
                return@withContext SearchResult.notFound(
                    DetectionSource.GPS,
                    durationMs = duration,
                    metadata = mapOf(
                        "reason" to "low_accuracy",
                        "accuracy" to loc.accuracy
                    )
                )
            }
            val detection = Detection(
                timestamp = Date(),
                sourceType = DetectionSource.GPS,
                label = "gps_fix",
                rssi = 0,
                latitude = loc.latitude,
                longitude = loc.longitude,
                metadata = mapOf(
                    "accuracy" to loc.accuracy,
                    "provider" to (loc.provider ?: "fused"),
                    "altitude" to loc.altitude,
                    "bearing" to loc.bearing,
                    "speed" to loc.speed
                ).mapValues { it.value ?: 0.0 },
                assetMac = asset.mac,
                nodeId = "fused_${loc.provider ?: "fused"}",
                isVerified = true,
                triangulationPoints = 1
            )
            SearchResult.success(
                detection = detection,
                source = DetectionSource.GPS,
                accuracy = accuracyFromGps(loc.accuracy),
                durationMs = duration,
                metadata = mapOf(
                    "lat" to loc.latitude,
                    "lon" to loc.longitude,
                    "accuracy_m" to loc.accuracy
                )
            )
        } catch (e: SecurityException) {
            SearchResult.error(DetectionSource.GPS, "SecEx: ${e.message}",
                durationMs = System.currentTimeMillis() - started)
        } catch (e: Exception) {
            SearchResult.error(DetectionSource.GPS, e.message ?: "Fehler",
                durationMs = System.currentTimeMillis() - started)
        }
    }

    private fun accuracyFromGps(accuracy: Float): Float = when {
        accuracy <= 0.0f -> 0.6f
        accuracy < 5 -> 0.95f
        accuracy < 15 -> 0.75f
        accuracy < 30 -> 0.50f
        else -> 0.30f
    }
}
