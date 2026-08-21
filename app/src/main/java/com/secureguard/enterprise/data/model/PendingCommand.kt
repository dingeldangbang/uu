package com.secureguard.enterprise.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Pending-Command-Queue für externe LoRa/BLE-Bridges.
 *
 * `TelemetryService.sendCommand(mac, command)` persistiert einen Eintrag
 * und produziert eine `PendingCommand`-Emission. Eine echte Hardware-Bridge
 * nimmt den Eintrag vom Flow, führt ihn aus, und setzt `executedAt`.
 */
@Entity(tableName = "pending_commands")
data class PendingCommand(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mac: String,
    val command: String,
    val createdAt: Long,
    val retried: Int = 0,
    val lastAttemptAt: Long? = null,
    val executedAt: Long? = null,
    val errorReason: String? = null
) {
    fun isPending(): Boolean = executedAt == null
}
