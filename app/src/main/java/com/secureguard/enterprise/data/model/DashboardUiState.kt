package com.secureguard.enterprise.data.model

/** Bestellung der Datei entfernt — bleibt als Kompatibilitäts-Stub. */
data class DashboardUiState(
    val totalAssets: Int = 0,
    val onlineAssets: Int = 0,
    val offlineAssets: Int = 0,
    val maintenanceAssets: Int = 0,
    val activeSearches: Int = 0,
    val alertCount: Int = 0,
    val agentRunning: Boolean = false,
    /** Geräte-Akku (echt via DeviceBatteryProvider). */
    val batteryLevel: Int = 50,
    /** Akku am Ladegerät? */
    val batteryPlugged: Boolean = false,
    val lastSyncTime: String = "--:--"
)

/** Surrogate für Action-/Search-Resultate aus den Detail-ViewModels. */
data class ActionResult(
    val success: Boolean,
    val message: String
) {
    companion object {
        val Processing = ActionResult(false, "Wird ausgeführt...")
    }
}

data class SearchResult(
    val found: Boolean,
    val detection: Detection? = null
)
