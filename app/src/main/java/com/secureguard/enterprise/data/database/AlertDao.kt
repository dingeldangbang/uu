package com.secureguard.enterprise.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.secureguard.enterprise.data.model.Alert
import kotlinx.coroutines.flow.Flow

@Dao
interface AlertDao {

    @Query("SELECT * FROM alerts ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecent(limit: Int = 50): Flow<List<Alert>>

    @Query("SELECT * FROM alerts WHERE acknowledged = 0 ORDER BY timestamp DESC")
    fun observeOpen(): Flow<List<Alert>>

    @Query("SELECT COUNT(*) FROM alerts WHERE acknowledged = 0")
    suspend fun openCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(alert: Alert): Long

    @Update
    suspend fun update(alert: Alert)

    @Query("UPDATE alerts SET acknowledged = 1, acknowledgedBy = :by WHERE id = :id")
    suspend fun acknowledge(id: Long, by: String)

    @Query("DELETE FROM alerts WHERE timestamp < :thresholdMs")
    suspend fun purgeOlderThan(thresholdMs: Long)
}
