package com.secureguard.enterprise.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.secureguard.enterprise.data.model.Detection
import kotlinx.coroutines.flow.Flow

@Dao
interface DetectionDao {

    @Query("SELECT * FROM detections ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecent(limit: Int = 100): Flow<List<Detection>>

    @Query("SELECT * FROM detections WHERE metadata = :mac OR assetMac = :mac ORDER BY timestamp DESC LIMIT :limit")
    fun observeByMac(mac: String, limit: Int = 50): Flow<List<Detection>>

    @Query("SELECT COUNT(*) FROM detections")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(detection: Detection): Long

    @Query("DELETE FROM detections WHERE timestamp < :thresholdMs")
    suspend fun purgeOlderThan(thresholdMs: Long)

    @Query("SELECT COUNT(*) FROM detections WHERE timestamp > :sinceMs")
    suspend fun countSince(sinceMs: Long): Int
}
