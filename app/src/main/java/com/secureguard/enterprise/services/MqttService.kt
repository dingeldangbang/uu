package com.secureguard.enterprise.services

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallback
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import javax.inject.Inject
import javax.inject.Singleton

/**
 * MQTT-Service — Paho MQTT-Client für den API-Node-Manager.
 *
 * Knoten "mqtt" im ApiNodeManager nutzt publish/subscribe:
 *   - `publish(topic, payload)` → Anfrage ins Mesh
 *   - `messages`-Flow → eingehende Antworten
 *
 * Broker-URL konfigurierbar über `MQTT_BROKER_URL`-Property
 * (Default: tcp://localhost:1883 → deaktiviert, bis konfiguriert).
 */
@Singleton
class MqttService @Inject constructor(
    @ApplicationContext private val ctx: Context
) {
    private var client: MqttClient? = null
    private var connected = false

    private val _messages = MutableSharedFlow<Pair<String, String>>(extraBufferCapacity = 32)
    val messages: SharedFlow<Pair<String, String>> = _messages.asSharedFlow()

    val brokerUrl: String
        get() = System.getProperty("secureguard.mqtt.broker", "tcp://localhost:1883")

    fun connect(): Boolean {
        if (connected) return true
        return try {
            client = MqttClient(brokerUrl, "secureguard-${System.currentTimeMillis()}", MemoryPersistence())
            val opts = MqttConnectOptions().apply {
                isCleanSession = true
                connectionTimeout = 10
                keepAliveInterval = 30
            }
            client?.connect(opts)
            client?.setCallback(object : MqttCallback {
                override fun connectionLost(cause: Throwable?) {
                    connected = false
                    Log.w(TAG, "MQTT connection lost", cause)
                }

                override fun messageArrived(topic: String?, message: MqttMessage?) {
                    val payload = message?.toString() ?: ""
                    _messages.tryEmit((topic ?: "") to payload)
                }

                override fun deliveryComplete(token: IMqttDeliveryToken?) {
                    // ok
                }
            })
            connected = true
            Log.i(TAG, "MQTT connected: $brokerUrl")
            true
        } catch (e: Exception) {
            Log.w(TAG, "MQTT connect failed", e)
            false
        }
    }

    fun publish(topic: String, payload: String): Boolean {
        if (!connected) connect()
        return try {
            client?.publish(topic, MqttMessage(payload.toByteArray())) != null
        } catch (e: Exception) {
            Log.w(TAG, "MQTT publish failed", e)
            false
        }
    }

    fun subscribe(topic: String): Boolean {
        if (!connected) connect()
        return try {
            client?.subscribe(topic) != null
        } catch (e: Exception) {
            Log.w(TAG, "MQTT subscribe failed", e)
            false
        }
    }

    fun disconnect() {
        try {
            client?.disconnect()
        } catch (_: Exception) { }
        client = null
        connected = false
    }

    companion object { private const val TAG = "MqttService" }

    /** Suche (neues SearchResult-Interface). */
    suspend fun searchAssetResult(asset: Asset): SearchResult {
        val d = searchAsset(asset) ?: return SearchResult.notFound(DetectionSource.URBAN)
        return SearchResult.success(d, DetectionSource.URBAN, accuracy = 0.80f,
            metadata = mapOf("transport" to "mqtt"))
    }
}
