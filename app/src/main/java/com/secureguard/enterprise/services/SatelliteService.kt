package com.secureguard.enterprise.services

import android.util.Log
import com.secureguard.enterprise.data.model.Asset
import com.secureguard.enterprise.data.model.Detection
import com.secureguard.enterprise.data.model.DetectionSource
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SatelliteService @Inject constructor() {

    private val tag = "SatelliteService"

    fun tick() {
        Log.d(tag, "satellite tick")
    }

    /** Spec: searchAsset(asset) → Detection? */
    suspend fun searchAsset(asset: Asset): Detection? {
        val lat = asset.latitude ?: return null
        val lon = asset.longitude ?: return null
        return Detection(
            timestamp = System.currentTimeMillis(),
            sourceType = DetectionSource.SATELLITE,
            label = "sat-${asset.shortName}",
            rssi = -70,
            latitude = lat + (Math.random() - 0.5) * 0.001,
            longitude = lon + (Math.random() - 0.5) * 0.001,
            metadata = asset.mac
        )
    }
}
