package com.secureguard.enterprise.services

import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import javax.inject.Inject
import javax.inject.Singleton

/**
 * WebSocket-Service — OkHttp-WebSocket für den API-Node-Manager.
 *
 * Knoten "websocket" sendet Suchanfragen über eine WebSocket-Verbindung
 * und empfängt Antworten asynchron über den `incoming`-Flow.
 */
@Singleton
class WebSocketService @Inject constructor() {

    private val client = OkHttpClient.Builder().build()
    private var webSocket: WebSocket? = null

    private val _incoming = MutableSharedFlow<String>(extraBufferCapacity = 32)
    val incoming: SharedFlow<String> = _incoming.asSharedFlow()

    fun connect(url: String): Boolean {
        return try {
            val request = Request.Builder().url(url).build()
            webSocket = client.newWebSocket(request, object : WebSocketListener() {
                override fun onMessage(webSocket: WebSocket, text: String) {
                    _incoming.tryEmit(text)
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: okhttp3.Response?) {
                    Log.w(TAG, "WebSocket failure", t)
                }
            })
            true
        } catch (e: Exception) {
            Log.w(TAG, "WebSocket connect failed", e)
            false
        }
    }

    fun sendMessage(payload: Map<String, Any>): Boolean {
        return try {
            val json = com.google.gson.Gson().toJson(payload)
            webSocket?.send(json) ?: false
        } catch (e: Exception) {
            false
        }
    }

    fun disconnect() {
        webSocket?.close(1000, "Normal closure")
        webSocket = null
    }

    companion object { private const val TAG = "WebSocketService" }

    /** Suche (neues SearchResult-Interface). */
    suspend fun searchAssetResult(asset: Asset): SearchResult {
        val d = searchAsset(asset) ?: return SearchResult.notFound(DetectionSource.URBAN)
        return SearchResult.success(d, DetectionSource.URBAN, accuracy = 0.80f,
            metadata = mapOf("transport" to "websocket"))
    }
}
