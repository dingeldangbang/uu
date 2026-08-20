package com.secureguard.enterprise.data.model

/** Konfiguration für den selbstlernenden Agent (SettingsDialog). */
data class AgentSettings(
    val interval: Int = 30,
    val dynamicPriority: Boolean = true,
    val learningMode: Boolean = true,
    val offlineOnly: Boolean = true,
    val externalSources: Boolean = false
)

/** Live-Status eines laufenden AgentService. */
data class AgentStatus(
    val running: Boolean = false,
    val startedAt: Long = 0L,
    val interval: Int = 30,
    val learningMode: Boolean = true
)
