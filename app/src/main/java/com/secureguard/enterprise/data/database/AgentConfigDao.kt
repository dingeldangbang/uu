package com.secureguard.enterprise.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.secureguard.enterprise.data.model.AgentConfig
import kotlinx.coroutines.flow.Flow

@Dao
interface AgentConfigDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(config: AgentConfig)

    @Query("SELECT * FROM agent_config WHERE id = 1 LIMIT 1")
    suspend fun get(): AgentConfig?

    @Query("SELECT * FROM agent_config WHERE id = 1 LIMIT 1")
    fun observe(): Flow<AgentConfig?>
}
