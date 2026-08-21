package com.secureguard.enterprise.data.repository

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persistente App-Settings via SharedPreferences.
 * Stellt je Toggle einen StateFlow zur Verfügung.
 */
@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext ctx: Context
) {
    private val prefs: SharedPreferences =
        ctx.getSharedPreferences("secureguard.settings", Context.MODE_PRIVATE)

    private val _states = MutableStateFlow(load())
    val states: StateFlow<SettingsState> = _states.asStateFlow()

    fun set(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
        _states.value = _states.value.copy(entries = _states.value.entries + (key to value))
    }

    fun resetRetention() {
        prefs.edit().putInt(KEY_RETENTION_DAYS, 30).apply()
        _states.value = _states.value.copy(retentionDays = 30)
    }

    fun setRetention(days: Int) {
        prefs.edit().putInt(KEY_RETENTION_DAYS, days).apply()
        _states.value = _states.value.copy(retentionDays = days)
    }

    fun resetDb() {
        // Aufrufer (DB-Drop) wird den Flow aktualisieren.
        prefs.edit().putBoolean(KEY_DB_RESET_PENDING, true).apply()
        _states.value = _states.value.copy(dbResetPending = true)
    }

    fun clearDbResetFlag() {
        prefs.edit().putBoolean(KEY_DB_RESET_PENDING, false).apply()
        _states.value = _states.value.copy(dbResetPending = false)
    }

    private fun load(): SettingsState {
        val toggles = mapOf(
            KEY_LORA      to prefs.getBoolean(KEY_LORA, true),
            KEY_TELEMETRY to prefs.getBoolean(KEY_TELEMETRY, true),
            KEY_OPTICAL   to prefs.getBoolean(KEY_OPTICAL, false),
            KEY_CROWD     to prefs.getBoolean(KEY_CROWD, true),
            KEY_DARK      to prefs.getBoolean(KEY_DARK, true),
            KEY_RECOVER_LOG to prefs.getBoolean(KEY_RECOVER_LOG, true),
            KEY_AUTONOTIFY to prefs.getBoolean(KEY_AUTONOTIFY, false)
        )
        return SettingsState(
            entries = toggles,
            retentionDays = prefs.getInt(KEY_RETENTION_DAYS, 30),
            dbResetPending = prefs.getBoolean(KEY_DB_RESET_PENDING, false)
        )
    }

    companion object {
        const val KEY_LORA        = "lora_enabled"
        const val KEY_TELEMETRY   = "telemetry_enabled"
        const val KEY_OPTICAL     = "optical_enabled"
        const val KEY_CROWD       = "crowd_enabled"
        const val KEY_DARK        = "dark_mode"
        const val KEY_RECOVER_LOG = "recover_log"
        const val KEY_AUTONOTIFY  = "auto_notify"
        const val KEY_RETENTION_DAYS = "retention_days"
        const val KEY_DB_RESET_PENDING = "db_reset_pending"
    }
}

data class SettingsState(
    val entries: Map<String, Boolean>,
    val retentionDays: Int,
    val dbResetPending: Boolean
) {
    fun isEnabled(key: String, default: Boolean = true): Boolean =
        entries[key] ?: default
}
