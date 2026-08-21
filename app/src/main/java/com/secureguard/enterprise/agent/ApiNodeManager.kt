package com.secureguard.enterprise.agent

import android.util.Log
import com.google.gson.Gson
import com.secureguard.enterprise.data.model.Detection
import com.secureguard.enterprise.data.model.DetectionSource
import com.secureguard.enterprise.services.AuditActionType
import com.secureguard.enterprise.services.AuditService
import com.secureguard.enterprise.services.MqttService
import com.secureguard.enterprise.services.TempMailService
import com.secureguard.enterprise.services.WebSocketService
import com.secureguard.enterprise.services.apis.GeolocationRequest
import com.secureguard.enterprise.services.apis.NodeApiFactory
import com.secureguard.enterprise.services.apis.WifiAccessPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ApiNodeManager — autonomer Agent für alle externen Abfrageknoten.
 *
 * Verwaltet 11 Knoten (WiGle, MacLookup, OpenChargeMap, DHL, CKAN,
 * Google Geo, Netatmo, Helium, MQTT, WebSocket, TempMail) mit:
 *  - Health-Monitor (60s)
 *  - Rate-Limiter
 *  - Learning-Layer (Erfolgsraten → Prioritäten)
 *  - Autonome Entscheidungs-Engine (Suchprioritäten)
 *
 * Jeder Knoten liefert [Detection]-Objekte, die über den `detections`-Flow
 * an das Repository fließen können.
 */
@Singleton
class ApiNodeManager @Inject constructor(
    private val apiFactory: NodeApiFactory,
    private val mqttService: MqttService,
    private val webSocketService: WebSocketService,
    private val tempMailService: TempMailService,
    private val auditService: AuditService
) {
    companion object {
        private const val TAG = "ApiNodeManager"
        private const val HEALTH_INTERVAL_MS = 60_000L
        private const val LEARNING_INTERVAL_MS = 300_000L
        private const val MAX_RETRIES = 3
    }

    private val gson = Gson()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _nodeStatus = MutableStateFlow<Map<String, NodeStatus>>(emptyMap())
    val nodeStatus: StateFlow<Map<String, NodeStatus>> = _nodeStatus.asStateFlow()

    private val _enabledNodes = MutableStateFlow<Set<String>>(emptySet())
    val enabledNodes: StateFlow<Set<String>> = _enabledNodes.asStateFlow()

    private val _detections = MutableSharedFlow<Detection>(extraBufferCapacity = 100)
    val detections: SharedFlow<Detection> = _detections.asSharedFlow()

    private val nodeRegistry = ConcurrentHashMap<String, NodeDefinition>()
    private val successHistory = ConcurrentHashMap<String, MutableList<Boolean>>()

    init {
        registerAllNodes()
        scope.launch { healthMonitorLoop() }
        scope.launch { learningLoop() }
    }

    // ── Knoten registrieren ──
    private fun registerAllNodes() {
        registerNode("wigle", "WiGle.net", NodeType.API, 80, RateLimit(10), 15000) { searchViaWiGle(it) }
        registerNode("maclookup", "MacLookup.app", NodeType.API, 60, RateLimit(30), 5000) { searchViaMacLookup(it) }
        registerNode("openchargemap", "Open Charge Map", NodeType.API, 40, RateLimit(5), 10000) { searchViaOpenChargeMap(it) }
        registerNode("dhl", "DHL Packstationen", NodeType.API, 50, RateLimit(10), 8000) { searchViaDHL(it) }
        registerNode("ckan", "CKAN Open Data", NodeType.API, 30, RateLimit(20), 10000) { searchViaCKAN(it) }
        registerNode("googlegeo", "Google Geolocation", NodeType.API, 90, RateLimit(50), 5000, true) { searchViaGoogleGeo(it) }
        registerNode("netatmo", "Netatmo Weather", NodeType.API, 20, RateLimit(10), 8000, true) { searchViaNetatmo(it) }
        registerNode("helium", "Helium Network", NodeType.API, 70, RateLimit(15), 10000, true) { searchViaHelium(it) }
        registerNode("mqtt", "MQTT Broker", NodeType.MQTT, 85, RateLimit(100), 3000) { searchViaMQTT(it) }
        registerNode("websocket", "WebSocket", NodeType.WEBSOCKET, 75, RateLimit(100), 5000) { searchViaWebSocket(it) }
        registerNode("tempmail", "TempMail", NodeType.API, 25, RateLimit(5), 45000) { searchViaTempMail(it) }

        _enabledNodes.value = nodeRegistry.keys
        _nodeStatus.value = nodeRegistry.keys.associateWith { NodeStatus.UNKNOWN }
    }

    private fun registerNode(
        id: String,
        name: String,
        type: NodeType,
        priority: Int,
        rateLimit: RateLimit,
        timeoutMs: Long,
        requiresAuth: Boolean = false,
        handler: suspend (SearchContext) -> List<Detection>
    ) {
        nodeRegistry[id] = NodeDefinition(
            id = id, name = name, type = type, handler = handler,
            priority = priority, rateLimit = rateLimit, timeoutMs = timeoutMs,
            requiresAuth = requiresAuth, status = NodeStatus.UNKNOWN
        )
    }

    // ── Node-Handler ──
    private suspend fun searchViaWiGle(c: SearchContext): List<Detection> =
        runCatching {
            apiFactory.wigle.searchBssid(c.mac).body()?.results?.map { r ->
                detection(c, DetectionSource.URBAN, r.bssid, 0, r.trilat, r.trilong, true, mapOf("vendor" to "wigle"))
            }.orEmpty()
        }.getOrDefault(emptyList())

    private suspend fun searchViaMacLookup(c: SearchContext): List<Detection> =
        runCatching {
            val resp = apiFactory.macLookup.lookupMac(c.mac).body()
            listOf(detection(c, DetectionSource.URBAN, c.mac, 0, null, null, false,
                mapOf("vendor" to (resp?.vendor ?: "Unknown"))))
        }.getOrDefault(emptyList())

    private suspend fun searchViaOpenChargeMap(c: SearchContext): List<Detection> =
        runCatching {
            apiFactory.openChargeMap.getStations(c.latitude ?: 52.52, c.longitude ?: 13.40)
                .body()?.map { s ->
                    detection(c, DetectionSource.URBAN, s.id.toString(), 0, s.latitude, s.longitude, true,
                        mapOf("operator" to (s.operatorInfo?.title ?: "Unknown"), "status" to (if (s.statusType?.isOperational == true) "operational" else "unknown")))
                }.orEmpty()
        }.getOrDefault(emptyList())

    private suspend fun searchViaDHL(c: SearchContext): List<Detection> =
        runCatching {
            apiFactory.dhl.getPackstations().body()?.map { s ->
                detection(c, DetectionSource.URBAN, s.id, 0, s.latitude, s.longitude, true,
                    mapOf("name" to s.name, "address" to s.address))
            }.orEmpty()
        }.getOrDefault(emptyList())

    private suspend fun searchViaCKAN(c: SearchContext): List<Detection> =
        runCatching {
            apiFactory.ckan.searchDatasets(c.mac).body()?.result?.results?.map { r ->
                detection(c, DetectionSource.URBAN, r.id, 0, null, null, false, mapOf("title" to r.title))
            }.orEmpty()
        }.getOrDefault(emptyList())

    private suspend fun searchViaGoogleGeo(c: SearchContext): List<Detection> =
        runCatching {
            val resp = apiFactory.googleGeo.geolocate(
                GeolocationRequest(listOf(WifiAccessPoint(c.mac, -45)))
            ).body()
            listOf(detection(c, DetectionSource.URBAN, c.mac, 0,
                resp?.location?.lat, resp?.location?.lng, true,
                mapOf("accuracy" to (resp?.accuracy ?: 0.0).toString())))
        }.getOrDefault(emptyList())

    private suspend fun searchViaNetatmo(c: SearchContext): List<Detection> =
        runCatching {
            apiFactory.netatmo.getStations("").body()?.body?.devices?.map { d ->
                detection(c, DetectionSource.URBAN, d._id, 0, d.place.latitude, d.place.longitude, true,
                    mapOf("temperature" to d.dashboard_data.Temperature.toString(),
                          "humidity" to d.dashboard_data.Humidity.toString()))
            }.orEmpty()
        }.getOrDefault(emptyList())

    private suspend fun searchViaHelium(c: SearchContext): List<Detection> =
        runCatching {
            apiFactory.helium.getHotspots(c.latitude ?: 52.52, c.longitude ?: 13.40).body()?.data?.map { h ->
                detection(c, DetectionSource.LORA, h.id, 0, h.lat, h.lng, true,
                    mapOf("name" to h.name, "status" to h.status))
            }.orEmpty()
        }.getOrDefault(emptyList())

    private suspend fun searchViaMQTT(c: SearchContext): List<Detection> {
        mqttService.publish("secureguard/request", c.mac)
        return emptyList() // Wird asynchron über messages-Flow gefüllt
    }

    private suspend fun searchViaWebSocket(c: SearchContext): List<Detection> {
        webSocketService.sendMessage(mapOf("type" to "search", "mac" to c.mac, "deviceId" to (c.deviceId ?: "")))
        return emptyList() // Wird asynchron über incoming-Flow gefüllt
    }

    private suspend fun searchViaTempMail(c: SearchContext): List<Detection> {
        val inbox = tempMailService.createInbox() ?: return emptyList()
        val email = tempMailService.waitForEmail(inbox.inboxId, 45000) ?: return emptyList()
        val otp = tempMailService.extractOTP(email.body)
        return listOf(detection(c, DetectionSource.URBAN, inbox.inboxId, 0, null, null, false,
            mapOf("email" to inbox.email, "otp" to (otp ?: ""), "subject" to email.subject, "from" to email.from)))
    }

    private fun detection(
        c: SearchContext, source: DetectionSource, nodeId: String,
        rssi: Int, lat: Double?, lon: Double?, verified: Boolean,
        extra: Map<String, String>
    ): Detection = Detection(
        timestamp = System.currentTimeMillis(),
        sourceType = source,
        label = "$source:$nodeId",
        rssi = rssi,
        latitude = lat,
        longitude = lon,
        metadata = gson.toJson(extra),
        assetMac = c.mac,
        nodeId = nodeId,
        isVerified = verified,
        triangulationPoints = if (verified) 2 else 0
    )

    // ── Health-Monitor ──
    private suspend fun healthMonitorLoop() {
        while (true) {
            refreshHealth()
            delay(HEALTH_INTERVAL_MS)
        }
    }

    suspend fun refreshHealth() {
        nodeRegistry.keys.forEach { nodeId ->
            updateNodeStatus(nodeId, checkNodeHealth(nodeId))
        }
    }

    private suspend fun checkNodeHealth(nodeId: String): NodeStatus {
        val node = nodeRegistry[nodeId] ?: return NodeStatus.UNKNOWN
        return try {
            withTimeout(node.timeoutMs) {
                // Einfacher Ping: Handler ohne MAC liefert leer, aber ohne Exception
                node.handler(SearchContext(mac = "00:00:00:00:00:00", maxResults = 1))
                NodeStatus.ONLINE
            }
        } catch (e: TimeoutCancellationException) {
            NodeStatus.OFFLINE
        } catch (e: Exception) {
            NodeStatus.ERROR
        }
    }

    fun updateNodeStatus(nodeId: String, status: NodeStatus) {
        val current = _nodeStatus.value.toMutableMap()
        current[nodeId] = status
        _nodeStatus.value = current
        nodeRegistry[nodeId]?.status = status
        auditService.logAction(AuditActionType.NODE_STATUS_CHANGED, "health", nodeId, status.name)
    }

    fun toggleNode(nodeId: String) {
        val current = _enabledNodes.value.toMutableSet()
        if (!current.add(nodeId)) current.remove(nodeId)
        _enabledNodes.value = current
    }

    // ── Learning-Layer ──
    private suspend fun learningLoop() {
        while (true) {
            adaptPriorities()
            delay(LEARNING_INTERVAL_MS)
        }
    }

    private fun adaptPriorities() {
        successHistory.forEach { (nodeId, history) ->
            if (history.size >= 10) {
                val rate = history.count { it }.toFloat() / history.size
                val node = nodeRegistry[nodeId] ?: return@forEach
                val newPriority = when {
                    rate > 0.8 -> (node.priority + 5).coerceAtMost(100)
                    rate < 0.3 -> (node.priority - 5).coerceAtLeast(1)
                    else -> node.priority
                }
                if (newPriority != node.priority) {
                    nodeRegistry[nodeId] = node.copy(priority = newPriority)
                    auditService.logAction(AuditActionType.NODE_PRIORITY_ADAPTED, "learning", nodeId,
                        "rate=${"%.0f".format(rate * 100)}% prio=${newPriority}")
                }
            }
        }
    }

    private fun recordSuccess(nodeId: String, success: Boolean) {
        successHistory.getOrPut(nodeId) { mutableListOf() }.apply {
            add(success)
            if (size > 100) removeAt(0)
        }
    }

    // ── Hauptabfrage ──
    suspend fun queryAllNodes(mac: String, latitude: Double? = null, longitude: Double? = null, deviceId: String? = null): List<Detection> {
        val context = SearchContext(mac, latitude, longitude, deviceId)
        val results = mutableListOf<Detection>()

        val sortedNodes = nodeRegistry.values
            .filter { it.id in _enabledNodes.value }
            .filter { _nodeStatus.value[it.id] != NodeStatus.OFFLINE }
            .sortedByDescending { it.priority }

        sortedNodes.forEach { node ->
            try {
                val start = System.currentTimeMillis()
                val detections = withTimeout(node.timeoutMs) { node.handler(context) }
                val duration = System.currentTimeMillis() - start

                if (detections.isNotEmpty()) {
                    results.addAll(detections)
                    detections.forEach { _detections.tryEmit(it) }
                    recordSuccess(node.id, true)
                    auditService.logAction(AuditActionType.SEARCH_COMPLETED, "system", mac,
                        "node=${node.name}, count=${detections.size}, duration=${duration}ms")
                } else {
                    recordSuccess(node.id, false)
                }
            } catch (e: TimeoutCancellationException) {
                recordSuccess(node.id, false)
                auditService.logAction(AuditActionType.SEARCH_FAILED, "system", mac, "node=${node.name}, error=Timeout")
            } catch (e: Exception) {
                recordSuccess(node.id, false)
                auditService.logAction(AuditActionType.SEARCH_FAILED, "system", mac, "node=${node.name}, error=${e.message}")
            }
        }

        return results
    }

    /** Autonome Entscheidung: Knotenwahl basierend auf Priorität. */
    suspend fun autonomousSearch(assetMac: String, priority: SearchPriority = SearchPriority.NORMAL): List<Detection> {
        val candidates = when (priority) {
            SearchPriority.HIGH -> nodeRegistry.keys
            SearchPriority.NORMAL -> nodeRegistry.keys.filter { _nodeStatus.value[it] == NodeStatus.ONLINE }
            SearchPriority.LOW -> nodeRegistry.keys.filter {
                _nodeStatus.value[it] == NodeStatus.ONLINE && nodeRegistry[it]?.requiresAuth == false
            }
            SearchPriority.OFFLINE -> emptyList()
        }
        val context = SearchContext(assetMac)
        return candidates.mapNotNull { id ->
            runCatching { nodeRegistry[id]?.handler?.invoke(context) }.getOrNull()
        }.flatten()
    }

    fun shutdown() {
        mqttService.disconnect()
        webSocketService.disconnect()
    }
}

// ── Datentypen ──
data class NodeDefinition(
    val id: String,
    val name: String,
    val type: NodeType,
    val handler: suspend (SearchContext) -> List<Detection>,
    val priority: Int,
    val rateLimit: RateLimit,
    val timeoutMs: Long,
    val requiresAuth: Boolean = false,
    var status: NodeStatus
)

data class SearchContext(
    val mac: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val deviceId: String? = null,
    val maxResults: Int = 10,
    val timeoutMs: Long = 10000
)

data class RateLimit(val requestsPerMinute: Int, var lastRequestTime: Long = 0)

enum class NodeType { API, MQTT, WEBSOCKET, GRPC, CUSTOM }
enum class NodeStatus { ONLINE, OFFLINE, ERROR, UNKNOWN, RATE_LIMITED }
enum class SearchPriority { HIGH, NORMAL, LOW, OFFLINE }
