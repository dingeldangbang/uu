package com.secureguard.enterprise.data.repository

import androidx.room.withTransaction
import com.secureguard.enterprise.data.database.AlertDao
import com.secureguard.enterprise.data.database.AssetDao
import com.secureguard.enterprise.data.database.DetectionDao
import com.secureguard.enterprise.data.database.PendingCommandDao
import com.secureguard.enterprise.data.database.SecureGuardDatabase
import com.secureguard.enterprise.data.model.Alert
import com.secureguard.enterprise.data.model.Asset
import com.secureguard.enterprise.data.model.AssetStatus
import com.secureguard.enterprise.data.model.Detection
import com.secureguard.enterprise.data.model.PendingCommand
import com.secureguard.enterprise.data.model.TelemetryData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecureGuardRepository @Inject constructor(
    private val db: SecureGuardDatabase,
    private val assetDao: AssetDao,
    private val detectionDao: DetectionDao,
    private val alertDao: AlertDao,
    private val pendingCommandDao: PendingCommandDao
) {

    // ─────────────────── Assets ───────────────────

    /** Spec: getAllAssets() -> Flow<List<Asset>> */
    fun getAllAssets(): Flow<List<Asset>> = assetDao.observeAll()

    /** Spec: getWhitelistedAssets() -> Flow<List<Asset>> */
    fun getWhitelistedAssets(): Flow<List<Asset>> = assetDao.observeAll()

    fun observeAssets(): Flow<List<Asset>> = assetDao.observeAll()
    fun observeAssetById(id: String): Flow<Asset?> = assetDao.observeById(id)

    /** Spec: getAssetByMac(mac) -> Asset? */
    suspend fun getAssetByMac(mac: String): Asset? = assetDao.findByMac(mac)

    /** Spec: insertAsset(asset) */
    suspend fun insertAsset(asset: Asset) = assetDao.upsert(asset)

    suspend fun upsertAsset(asset: Asset) = assetDao.upsert(asset)
    suspend fun upsertAssets(assets: List<Asset>) = assetDao.upsertAll(assets)

    /** Spec: updateAsset(asset) */
    suspend fun updateAsset(asset: Asset) = assetDao.update(asset)

    /** Spec: deleteAsset(id) */
    suspend fun deleteAsset(id: String) = assetDao.deleteById(id)

    suspend fun setAssetStatus(id: String, status: AssetStatus, ts: Long = System.currentTimeMillis()) =
        assetDao.setStatus(id, status, ts)

    /** Spec: updateAssetStatus(mac, status, ts, lat, lon) */
    suspend fun updateAssetStatus(
        mac: String,
        status: AssetStatus,
        timestamp: Long,
        lat: Double? = null,
        lon: Double? = null
    ) = assetDao.updateAssetStatus(mac, status, timestamp, lat, lon)

    // ─────────────────── Detections ───────────────────

    fun observeRecentDetections(limit: Int = 100): Flow<List<Detection>> =
        detectionDao.observeRecent(limit)

    /** Spec: getDetections(mac) -> Flow<List<Detection>> */
    fun getDetections(mac: String): Flow<List<Detection>> = detectionDao.observeByMac(mac)

    suspend fun pushDetection(detection: Detection): Long = detectionDao.insert(detection)

    /** Spec: insertDetection(detection) -> Detection? */
    suspend fun insertDetection(detection: Detection): Detection? {
        val id = detectionDao.insert(detection)
        return detection.copy(id = id)
    }

    // ─────────────────── Alerts ───────────────────

    fun observeOpenAlerts(): Flow<List<Alert>> = alertDao.observeOpen()

    /** Spec: getAllAlerts() -> Flow<List<Alert>> */
    fun getAllAlerts(): Flow<List<Alert>> = alertDao.observeRecent(200)

    /** Spec: getUnresolvedAlerts() -> Flow<List<Alert>> */
    fun getUnresolvedAlerts(): Flow<List<Alert>> = alertDao.observeOpen()

    /** Spec: insertAlert(alert) */
    suspend fun insertAlert(alert: Alert) { alertDao.insert(alert) }
    suspend fun pushAlert(alert: Alert): Long = alertDao.insert(alert)

    /** Spec: updateAlert(alert) */
    suspend fun updateAlert(alert: Alert) = alertDao.update(alert)

    suspend fun acknowledge(id: Long, by: String) = alertDao.acknowledge(id, by)

    // ─────────────────── Agent / Config ───────────────────

    suspend fun processAgentDecision(
        assetId: String,
        newStatus: String,
        alertTitle: String? = null,
        msg: String? = null
    ) {
        db.withTransaction {
            assetDao.setStatus(assetId, AssetStatus.valueOf(newStatus), System.currentTimeMillis())
            if (alertTitle != null && msg != null) {
                alertDao.insert(
                    Alert(
                        timestamp = System.currentTimeMillis(),
                        severity = com.secureguard.enterprise.data.model.AlertSeverity.WARNING,
                        title = alertTitle,
                        message = msg,
                        assetId = assetId
                    )
                )
            }
        }
    }

    suspend fun openAlertCount(): Int = alertDao.openCount()
    suspend fun assetCount(): Int = assetDao.count()
    suspend fun assetSnapshot(): List<Asset> = assetDao.observeAll().first()

    /** GC über N Tage. */
    suspend fun purgeOld(thresholdDays: Int = 30) {
        val cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(thresholdDays.toLong())
        db.withTransaction {
            detectionDao.purgeOlderThan(cutoff)
            alertDao.purgeOlderThan(cutoff)
        }
    }

    fun retentionDaysOrDefault(): Int = 30

    suspend fun getLatestTelemetry(mac: String): TelemetryData? {
        val a = assetDao.findByMac(mac) ?: return null
        val lat = a.latitude ?: return null
        val lon = a.longitude ?: return null
        return TelemetryData(
            mac = mac,
            timestamp = a.lastSeen ?: System.currentTimeMillis(),
            batteryPercent = a.batteryPercent,
            signal = a.rssi,
            latitude = lat,
            longitude = lon
        )
    }

    // ─────────────────── Pending-Commands ───────────────────

    suspend fun addPendingCommand(cmd: PendingCommand): Long = pendingCommandDao.insert(cmd)

    fun observePendingCommands(limit: Int = 50): Flow<List<PendingCommand>> =
        pendingCommandDao.observePending(limit)

    suspend fun markPendingExecuted(id: Long, ts: Long) = pendingCommandDao.markExecuted(id, ts)

    suspend fun markPendingFailed(id: Long, ts: Long, reason: String) =
        pendingCommandDao.markFailed(id, ts, reason)

    suspend fun cancelPendingForMac(mac: String) = pendingCommandDao.cancelPendingForMac(mac)
}
