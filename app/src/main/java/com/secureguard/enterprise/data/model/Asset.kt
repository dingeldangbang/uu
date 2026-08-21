package com.secureguard.enterprise.data.model

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

@Entity(tableName = "assets")
data class Asset(
    @PrimaryKey val id: String,
    val name: String,
    val shortName: String = name,
    val mac: String = "",
    val category: AssetCategory = AssetCategory.GENERIC,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val location: String? = null,
    val batteryPercent: Int = 0,
    val rssi: Int = -100,
    val status: AssetStatus = AssetStatus.UNKNOWN,
    val lastSeen: Long? = null,
    val tags: List<String> = emptyList(),
    val owner: String = "",
    val externalAllowed: Boolean = false
) {
    /** Verschlüsselte Notizen — bewusst NICHT persistiert (Room-@Ignore im Body). */
    @Ignore
    val encryptedNotes: ByteArray? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Asset) return false
        return id == other.id
    }
    override fun hashCode(): Int = id.hashCode()
}

enum class AssetCategory {
    SENSOR, GATEWAY, VEHICLE, PERSON, DEVICE, GENERIC
}

enum class AssetStatus {
    ONLINE, OFFLINE, ALERT, MAINTENANCE, SEARCHING, UNKNOWN
}
