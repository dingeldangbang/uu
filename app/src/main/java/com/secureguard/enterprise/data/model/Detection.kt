package com.secureguard.enterprise.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "detections")
data class Detection(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val sourceType: DetectionSource = DetectionSource.BLE,
    val label: String = "",
    val confidence: Float = 1f,
    val rssi: Int = -100,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val metadata: String = "",
    // ── API-Node-Manager Erweiterung ──
    val assetMac: String = "",
    val nodeId: String = "",
    val isVerified: Boolean = false,
    val triangulationPoints: Int = 0
)

enum class DetectionSource {
    OPTICAL, ACOUSTIC, RF, GNSS, BLE, WIFI, LORA, SATELLITE, CROWD, URBAN, NFC
}

data class TelemetryData(
    val mac: String,
    val timestamp: Long,
    val batteryPercent: Int,
    val signal: Int,
    val latitude: Double,
    val longitude: Double
)
