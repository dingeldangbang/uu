package com.secureguard.enterprise.agent

data class NodeConfig(
    val enabled: Boolean = true,
    val timeoutMs: Long = 10000,
    val maxRetries: Int = 3,
    val priority: Int = 50,
    val rateLimitPerMinute: Int = 10,
    val fallbackNodes: List<String> = emptyList(),
    val requiresAuth: Boolean = false,
    val apiKey: String? = null
)

object DefaultNodeConfigs {
    val WIGLE = NodeConfig(priority = 80, rateLimitPerMinute = 10, timeoutMs = 15000)
    val MACLOOKUP = NodeConfig(priority = 60, rateLimitPerMinute = 30, timeoutMs = 5000)
    val OPEN_CHARGE_MAP = NodeConfig(priority = 40, rateLimitPerMinute = 5, timeoutMs = 10000)
    val DHL = NodeConfig(priority = 50, rateLimitPerMinute = 10, timeoutMs = 8000)
    val CKAN = NodeConfig(priority = 30, rateLimitPerMinute = 20, timeoutMs = 10000)
    val GOOGLE_GEO = NodeConfig(priority = 90, rateLimitPerMinute = 50, timeoutMs = 5000, requiresAuth = true)
    val NETATMO = NodeConfig(priority = 20, rateLimitPerMinute = 10, timeoutMs = 8000, requiresAuth = true)
    val HELIUM = NodeConfig(priority = 70, rateLimitPerMinute = 15, timeoutMs = 10000, requiresAuth = true)
    val MQTT = NodeConfig(priority = 85, rateLimitPerMinute = 100, timeoutMs = 3000)
    val WEBSOCKET = NodeConfig(priority = 75, rateLimitPerMinute = 100, timeoutMs = 5000)
    val TEMPMAIL = NodeConfig(priority = 25, rateLimitPerMinute = 5, timeoutMs = 45000)
}
