package com.secureguard.enterprise.data.database

import androidx.room.TypeConverter
import com.secureguard.enterprise.data.model.AlertSeverity
import com.secureguard.enterprise.data.model.AlertType
import com.secureguard.enterprise.data.model.AssetCategory
import com.secureguard.enterprise.data.model.AssetStatus
import com.secureguard.enterprise.data.model.DetectionSource
import java.util.Date

class Converters {

    @TypeConverter
    fun fromCategory(value: AssetCategory?): String? = value?.name

    @TypeConverter
    fun toCategory(value: String?): AssetCategory? =
        value?.let { runCatching { AssetCategory.valueOf(it) }.getOrNull() }

    @TypeConverter
    fun fromStatus(value: AssetStatus?): String? = value?.name

    @TypeConverter
    fun toStatus(value: String?): AssetStatus? =
        value?.let { runCatching { AssetStatus.valueOf(it) }.getOrNull() }

    @TypeConverter
    fun fromSeverity(value: AlertSeverity?): String? = value?.name

    @TypeConverter
    fun toSeverity(value: String?): AlertSeverity? =
        value?.let { runCatching { AlertSeverity.valueOf(it) }.getOrNull() }

    @TypeConverter
    fun fromAlertType(value: AlertType?): String? = value?.name

    @TypeConverter
    fun toAlertType(value: String?): AlertType? =
        value?.let { runCatching { AlertType.valueOf(it) }.getOrNull() }

    @TypeConverter
    fun fromSource(value: DetectionSource?): String? = value?.name

    @TypeConverter
    fun toSource(value: String?): DetectionSource? =
        value?.let { runCatching { DetectionSource.valueOf(it) }.getOrNull() }

    @TypeConverter
    fun fromDate(value: Long?): Date? = value?.let { Date(it) }

    @TypeConverter
    fun toDate(date: Date?): Long? = date?.time

    @TypeConverter
    fun fromTagList(value: List<String>?): String =
        value.orEmpty().joinToString("|")

    @TypeConverter
    fun toTagList(value: String?): List<String> =
        value?.takeIf { it.isNotEmpty() }?.split('|').orEmpty()
}
