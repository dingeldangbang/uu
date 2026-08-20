package com.secureguard.enterprise.mcp

import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * MCP-Client (Model Context Protocol) für externe E-Mail-Dienste.
 *
 * Verbindet sich per WebSocket zu einem MCP-Server und ruft Tools auf:
 *  - create_inbox    → neue temporäre Inbox
 *  - wait_for_otp    → auf OTP warten
 *  - extract_magic_link → Magic-Link extrahieren
 *
 * NUR für legitime Zwecke: automatische Registrierung in firmeninternen
 * Testumgebungen / autorisierte API-Key-Generierung.
 */
@Singleton
class MCPClient @Inject constructor() {

    companion object {
        private const val MCP_SERVER_URL = "wss://mcp.freecustom.email"
        private const val TIMEOUT_MS = 45000L // 45 Sekunden
    }

    private val gson = Gson()
    private val client = OkHttpClient.Builder().build()

    private var webSocket: WebSocket? = null
    private var requestId = 0

    private val pendingRequests = ConcurrentHashMap<Int, (JsonObject) -> Unit>()

    private val _events = MutableSharedFlow<MCPEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<MCPEvent> = _events.asSharedFlow()

    // ── Verbindung ──
    fun connect() {
        if (webSocket != null) return
        val request = Request.Builder()
            .url(MCP_SERVER_URL)
            .addHeader("Origin", "secureguard://enterprise")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: okhttp3.Response) {
                _events.tryEmit(MCPEvent.Connected)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val json = gson.fromJson(text, JsonObject::class.java)
                    val id = json.get("id")?.asInt ?: return
                    pendingRequests.remove(id)?.invoke(json)
                } catch (e: Exception) {
                    _events.tryEmit(MCPEvent.Error(e.message ?: "Parsing error"))
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                _events.tryEmit(MCPEvent.Disconnected)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: okhttp3.Response?) {
                _events.tryEmit(MCPEvent.Error(t.message ?: "Connection failed"))
            }
        })
    }

    fun disconnect() {
        webSocket?.close(1000, "Normal closure")
        webSocket = null
    }

    // ── Tools ──
    suspend fun createInbox(): InboxResult? {
        val id = ++requestId
        val request = JsonObject().apply {
            addProperty("jsonrpc", "2.0")
            addProperty("id", id)
            addProperty("method", "tools/call")
            add("params", JsonObject().apply {
                addProperty("name", "create_inbox")
                add("arguments", JsonObject())
            })
        }

        return sendRequest(request, id) { response ->
            val result = response.getAsJsonObject("result")
            val content = result.getAsJsonArray("content")
            val text = content[0].asJsonObject.get("text").asString
            val data = gson.fromJson(text, InboxData::class.java)
            InboxResult(success = true, email = data.email, token = data.token, inboxId = data.inboxId)
        }
    }

    suspend fun waitForOTP(inboxToken: String, timeoutMs: Long = TIMEOUT_MS): OTPResult? {
        val id = ++requestId
        val request = JsonObject().apply {
            addProperty("jsonrpc", "2.0")
            addProperty("id", id)
            addProperty("method", "tools/call")
            add("params", JsonObject().apply {
                addProperty("name", "wait_for_otp")
                add("arguments", JsonObject().apply {
                    addProperty("token", inboxToken)
                    addProperty("timeout", timeoutMs)
                })
            })
        }

        return sendRequest(request, id) { response ->
            val result = response.getAsJsonObject("result")
            val content = result.getAsJsonArray("content")
            val text = content[0].asJsonObject.get("text").asString
            val data = gson.fromJson(text, OTPData::class.java)
            if (data.otp != null) {
                OTPResult(success = true, otp = data.otp, email = data.email ?: "", from = data.from ?: "", subject = data.subject ?: "")
            } else {
                OTPResult(success = false, error = "Timeout oder keine OTP gefunden")
            }
        }
    }

    suspend fun extractMagicLink(inboxToken: String): MagicLinkResult? {
        val id = ++requestId
        val request = JsonObject().apply {
            addProperty("jsonrpc", "2.0")
            addProperty("id", id)
            addProperty("method", "tools/call")
            add("params", JsonObject().apply {
                addProperty("name", "extract_magic_link")
                add("arguments", JsonObject().apply { addProperty("token", inboxToken) })
            })
        }

        return sendRequest(request, id) { response ->
            val result = response.getAsJsonObject("result")
            val content = result.getAsJsonArray("content")
            val text = content[0].asJsonObject.get("text").asString
            val data = gson.fromJson(text, MagicLinkData::class.java)
            if (data.magicLink != null) {
                MagicLinkResult(success = true, magicLink = data.magicLink, email = data.email ?: "")
            } else {
                MagicLinkResult(success = false, error = "Kein Magic Link gefunden")
            }
        }
    }

    // ── Hilfsfunktion ──
    private suspend fun <T> sendRequest(
        request: JsonObject,
        id: Int,
        onResponse: (JsonObject) -> T?
    ): T? {
        return suspendCancellableCoroutine { continuation ->
            pendingRequests[id] = { response ->
                try {
                    continuation.resume(onResponse(response))
                } catch (e: Exception) {
                    continuation.resume(null)
                }
            }

            val sent = webSocket?.send(request.toString()) ?: false
            if (!sent) {
                pendingRequests.remove(id)
                continuation.resume(null)
            }

            continuation.invokeOnCancellation {
                pendingRequests.remove(id)
            }
        }
    }

    // ── DTOs ──
    data class InboxData(val email: String, val token: String, val inboxId: String)
    data class OTPData(val otp: String?, val email: String?, val from: String?, val subject: String?)
    data class MagicLinkData(val magicLink: String?, val email: String?)
}

sealed class MCPEvent {
    object Connected : MCPEvent()
    object Disconnected : MCPEvent()
    data class Error(val message: String) : MCPEvent()
    data class InboxCreated(val email: String, val token: String) : MCPEvent()
    data class OTPReceived(val otp: String, val email: String) : MCPEvent()
}

data class InboxResult(
    val success: Boolean,
    val email: String = "",
    val token: String = "",
    val inboxId: String = "",
    val error: String? = null
)

data class OTPResult(
    val success: Boolean,
    val otp: String = "",
    val email: String = "",
    val from: String = "",
    val subject: String = "",
    val error: String? = null
)

data class MagicLinkResult(
    val success: Boolean,
    val magicLink: String = "",
    val email: String = "",
    val error: String? = null
)
