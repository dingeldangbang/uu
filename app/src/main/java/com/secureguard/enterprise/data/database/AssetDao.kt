package com.secureguard.enterprise.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.secureguard.enterprise.data.model.Asset
import com.secureguard.enterprise.data.model.AssetStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface AssetDao {

    @Query("SELECT * FROM assets ORDER BY name")
    fun observeAll(): Flow<List<Asset>>

    @Query("SELECT * FROM assets WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<Asset?>

    @Query("SELECT * FROM assets WHERE mac = :mac LIMIT 1")
    suspend fun findByMac(mac: String): Asset?

    @Query("SELECT * FROM assets WHERE status = :status")
    fun observeByStatus(status: AssetStatus): Flow<List<Asset>>

    @Query("SELECT * FROM assets WHERE mac = :mac")
    fun observeByMac(mac: String): Flow<Asset?>

    @Query("SELECT COUNT(*) FROM assets")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(asset: Asset)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(assets: List<Asset>)

    @Update
    suspend fun update(asset: Asset)

    @Query("DELETE FROM assets WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE assets SET status = :status, lastSeen = :ts WHERE id = :id")
    suspend fun setStatus(id: String, status: AssetStatus, ts: Long)

    @Query("""
        UPDATE assets
           SET status = :status,
               lastSeen = :ts,
               latitude = :lat,
               longitude = :lon,
               batteryPercent = batteryPercent
         WHERE mac = :mac
    """)
    suspend fun updateAssetStatus(mac: String, status: AssetStatus, ts: Long, lat: Double?, lon: Double?)

    @Query("DELETE FROM assets WHERE mac = :mac")
    suspend fun deleteByMac(mac: String)
}
