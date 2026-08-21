package com.secureguard.enterprise.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "alerts")
data class Alert(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val type: AlertType = AlertType.SECURITY,
    val severity: AlertSeverity = AlertSeverity.INFO,
    val title: String = "",
    val message: String = "",
    val assetId: String? = null,
    val acknowledged: Boolean = false,
    val acknowledgedBy: String? = null
) {
    fun asDate(): Date = Date(timestamp)
}

enum class AlertSeverity { INFO, WARNING, CRITICAL }

enum class AlertType { SECURITY, CRITICAL, INFO, ACTION, MAINTENANCE }
