package com.secureguard.enterprise.services

import android.util.Log
import com.secureguard.enterprise.data.model.Asset
import com.secureguard.enterprise.data.model.Detection
import com.secureguard.enterprise.data.model.DetectionSource
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * LoraService — LoRa/LoRaWAN-Kommunikation.
 *
 * - `searchAsset(asset)` liefert eine LoRa-basierte Detection (Simulation der
 *   Frame-Telemetrie, real ersetzbar durch Hardware-SDK)
 * - `detections`-Flow: Live-Strom aller eingehenden LoRa-Pakete
 *
 * Platzhalter für echte Integration:
 *   - Nordic nRF52/SX126x via SPI
 *   - RN2483 / RN2903 AT-Modul
 *   - TheThingsNetwork MQTT-Bridge
 */
@Singleton
class LoraService @Inject constructor() {

    private val _detections = MutableSharedFlow<Detection>(replay = 0, extraBufferCapacity = 16)
    val detections: SharedFlow<Detection> = _detections.asSharedFlow()

    suspend fun poll() {
        Log.d(TAG, "LoRa poll")
    }

    /** Spec: searchAsset(asset) → Detection? */
    suspend fun searchAsset(asset: Asset): Detection? {
        val lat = asset.latitude ?: return null
        val lon = asset.longitude ?: return null
        val detection = Detection(
            timestamp = System.currentTimeMillis(),
            sourceType = DetectionSource.LORA,
            label = "lora-${asset.shortName}",
            rssi = (asset.rssi - 5).coerceAtLeast(-120),
            latitude = lat,
            longitude = lon,
            metadata = asset.mac
        )
        _detections.tryEmit(detection)
        return detection
    }

    companion object { private const val TAG = "LoraService" }
}
