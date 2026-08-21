package com.secureguard.enterprise.services

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

enum class AuditActionType {
    SEARCH_COMPLETED,
    SEARCH_FAILED,
    COMMAND_SENT,
    NODE_STATUS_CHANGED,
    AGENT_CYCLE,
    NODE_PRIORITY_ADAPTED
}

/**
 * AuditService — zentrales Aktions-Log des API-Node-Managers.
 *
 * Jeder Knoten-Zugriff, jede Suche und jede autonome Entscheidung wird
 * mit Zeitstempel protokolliert (in-memory StateFlow; in einem echten
 * Produktivsystem via Room persistierbar).
 */
@Singleton
class AuditService @Inject constructor() {

    private val _log = MutableStateFlow<List<String>>(emptyList())
    val log: StateFlow<List<String>> = _log.asStateFlow()

    fun logAction(type: AuditActionType, actor: String, target: String, detail: String = "") {
        val ts = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val entry = "[$ts] ${type.name} | $actor → $target | $detail"
        _log.value = (_log.value + entry).takeLast(200)
        Log.i(TAG, entry)
    }

    fun clear() {
        _log.value = emptyList()
    }

    companion object { private const val TAG = "AuditService" }
}
