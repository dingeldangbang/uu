package com.secureguard.enterprise.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.secureguard.enterprise.data.model.AgentConfig
import com.secureguard.enterprise.data.model.Alert
import com.secureguard.enterprise.data.model.Asset
import com.secureguard.enterprise.data.model.Detection
import com.secureguard.enterprise.data.model.PendingCommand

@Database(
    entities = [
        Asset::class,
        Detection::class,
        Alert::class,
        AgentConfig::class,
        PendingCommand::class
    ],
    version = 4,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class SecureGuardDatabase : RoomDatabase() {
    abstract fun assetDao(): AssetDao
    abstract fun detectionDao(): DetectionDao
    abstract fun alertDao(): AlertDao
    abstract fun agentConfigDao(): AgentConfigDao
    abstract fun pendingCommandDao(): PendingCommandDao

    companion object { const val DB_NAME = "secureguard.db" }
}
