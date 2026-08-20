package com.secureguard.enterprise.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Persistierte Q-Learning-Konfiguration des selbstlernenden Agenten.
 * Room-Entity (Singleton-Row mit id=1), von [com.secureguard.enterprise.worker.SecureAgentWorker]
 * gelesen und nach jedem Trainingszyklus aktualisiert.
 */
@Entity(tableName = "agent_config")
data class AgentConfig(
    @PrimaryKey val id: Int = 1,
    val explorationRate: Float = 0.15f,
    val discountFactor: Float = 0.9f,
    val learningRate: Float = 0.1f,
    val totalDecisions: Int = 0,
    val qTable: Map<String, Float> = emptyMap(),
    val lastTrainingEpoch: Long = 0L
)

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
