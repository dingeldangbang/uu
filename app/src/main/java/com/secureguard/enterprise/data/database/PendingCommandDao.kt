package com.secureguard.enterprise.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.secureguard.enterprise.data.model.PendingCommand
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingCommandDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(cmd: PendingCommand): Long

    @Update
    suspend fun update(cmd: PendingCommand)

    @Query("SELECT * FROM pending_commands WHERE executedAt IS NULL ORDER BY createdAt ASC LIMIT :limit")
    fun observePending(limit: Int = 50): Flow<List<PendingCommand>>

    @Query("SELECT * FROM pending_commands WHERE mac = :mac AND executedAt IS NULL ORDER BY createdAt DESC")
    fun observePendingForMac(mac: String): Flow<List<PendingCommand>>

    @Query("UPDATE pending_commands SET retried = retried + 1, lastAttemptAt = :ts WHERE id = :id")
    suspend fun markRetry(id: Long, ts: Long)

    @Query("UPDATE pending_commands SET executedAt = :ts WHERE id = :id")
    suspend fun markExecuted(id: Long, ts: Long)

    @Query("UPDATE pending_commands SET lastAttemptAt = :ts, errorReason = :reason WHERE id = :id")
    suspend fun markFailed(id: Long, ts: Long, reason: String)

    @Query("DELETE FROM pending_commands WHERE executedAt IS NOT NULL AND executedAt < :ts")
    suspend fun purgeExecutedOlderThan(ts: Long)

    @Query("DELETE FROM pending_commands WHERE mac = :mac AND executedAt IS NULL")
    suspend fun cancelPendingForMac(mac: String)
}
