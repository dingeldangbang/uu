package com.secureguard.enterprise.services

import android.content.Context
import com.secureguard.enterprise.data.model.Asset
import com.secureguard.enterprise.data.model.Detection
import com.secureguard.enterprise.data.model.DetectionSource
import com.secureguard.enterprise.data.model.SearchResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * MeshService — generischer Mesh-Netzwerk-Scan-Knoten.
 *
 * Stuhlhalter ohne konkrete Meshtastic-Binding. Liefert eine
 * MeshNode-Liste (statisch konfiguriert) und simuliert eine
 * Beacon-Broadcast-Anfragen-Pipeline.
 *
 * Für Produktion: rextech.espressif.mesh.MeshManager oder
 * com.geeksville.mesh.MeshService (Meshtastic Android) integrieren.
 */
@Singleton
class MeshService @Inject constructor(
    @ApplicationContext private val ctx: Context
) {
    companion object {
        private const val BEACON_INTERVAL_MS = 500L
        private const val SCAN_TIMEOUT_MS = 5000L
    }

    /** Stuhlhalter-Liste von Mesh-Knoten. */
    private val knownNodes = listOf(
        MeshNode(id = "node_wohnung",  name = "Wohnung",  lat = 52.5200, lon = 13.4050, rssi = -55),
        MeshNode(id = "node_buero",    name = "Büro",     lat = 52.5074, lon = 13.3904, rssi = -72),
        MeshNode(id = "node_keller",   name = "Keller",   lat = 52.5347, lon = 13.4033, rssi = -85)
    )

    /** Search-Operation: abgefragt wird, ob ein Knoten das Asset-MAC vor kurzem gesehen hat. */
    suspend fun searchAsset(asset: Asset): SearchResult = withContext(Dispatchers.IO) {
        val started = System.currentTimeMillis()
        try {
            // Query-Simulation: Knoten werden iterativ befragt
            var bestNode: MeshNode? = null
            for (node in knownNodes) {
                // Stuhlhalter: Annahme dass Knoten immer das Asset an ihrem Standort sieht
                bestNode = node
                delay(BEACON_INTERVAL_MS)

                // 90% Timeout-Sicherheit
                if (System.currentTimeMillis() - started > SCAN_TIMEOUT_MS) break
            }

            val duration = System.currentTimeMillis() - started
            if (bestNode == null) {
                return@withContext SearchResult.notFound(
                    DetectionSource.URBAN,
                    durationMs = duration,
                    metadata = mapOf("reason" to "no_beacon")
                )
            }
            val detection = Detection(
                timestamp = System.currentTimeMillis(),
                sourceType = DetectionSource.URBAN,
                label = "mesh:${bestNode.id}",
                rssi = bestNode.rssi,
                latitude = bestNode.lat,
                longitude = bestNode.lon,
                metadata = bestNode.name,
                assetMac = asset.mac,
                nodeId = bestNode.id,
                isVerified = true,
                triangulationPoints = 1
            )
            SearchResult.success(
                detection = detection,
                source = DetectionSource.URBAN,
                accuracy = 0.70f,
                durationMs = duration,
                metadata = mapOf(
                    "node" to bestNode.id,
                    "node_name" to bestNode.name,
                    "nodes_queried" to knownNodes.size
                )
            )
        } catch (e: Exception) {
            SearchResult.error(
                DetectionSource.URBAN,
                e.message ?: "Mesh-Lookup fehlgeschlagen",
                durationMs = System.currentTimeMillis() - started
            )
        }
    }

    /** Paxcounter-Integration (ESP-Submodul) — Stuhlhalter. */
    suspend fun searchWithPaxcounter(asset: Asset): SearchResult =
        SearchResult.notFound(DetectionSource.URBAN,
            metadata = mapOf("reason" to "paxcounter_non_implemented"))

    /** ESP32-Direkt-Verbindung — Stuhlhalter. */
    suspend fun searchWithESP32(asset: Asset, esp32Id: String): SearchResult =
        SearchResult.notFound(DetectionSource.URBAN,
            metadata = mapOf("esp32" to esp32Id))
}

data class MeshNode(
    val id: String,
    val name: String,
    val lat: Double,
    val lon: Double,
    val rssi: Int
)
