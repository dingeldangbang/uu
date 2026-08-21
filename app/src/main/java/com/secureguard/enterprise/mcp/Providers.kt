package com.secureguard.enterprise.mcp

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import java.util.concurrent.TimeUnit

/**
 * FreeCustom.Email REST-Provider (Default).
 * Unterstützt: `create_inbox`, `wait_for_email` (Polling, 1s-Intervall).
 */
class FreeCustomEmailProvider(
    private val baseUrl: String = "https://api.freecustom.email"
) : EmailProvider {
    override val name = "FreeCustom.Email"
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    private val gson = Gson()

    override suspend fun createInbox(): ProviderInbox? = withContext(Dispatchers.IO) {
        try {
            val json = HttpClient.call(
                client, "POST", "$baseUrl/v1/inboxes/create", null, null
            ) ?: return@withContext null
            val obj = gson.fromJson(json, Map::class.java)
            ProviderInbox(
                email   = obj["address"]?.toString() ?: return@withContext null,
                token   = obj["token"]?.toString() ?: return@withContext null,
                inboxId = obj["id"]?.toString() ?: return@withContext null
            )
        } catch (e: Exception) { null }
    }

    override suspend fun waitForEmail(inboxId: String, token: String?, timeoutMs: Long): ProviderEmail? = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < timeoutMs) {
            try {
                val body = HttpClient.call(
                    client, "GET", "$baseUrl/v1/inboxes/$inboxId/emails", null, null
                ) ?: continue
                if (body == "[]") {
                    kotlinx.coroutines.delay(1_000)
                    continue
                }
                val arr = gson.fromJson(body, Array::class.java)
                if (arr.isNotEmpty()) {
                    val obj = arr[0] as Map<*, *>
                    return@withContext ProviderEmail(
                        id         = obj["id"]?.toString().orEmpty(),
                        from       = obj["from"]?.toString().orEmpty(),
                        subject    = obj["subject"]?.toString().orEmpty(),
                        body       = obj["body"]?.toString().orEmpty(),
                        receivedAt = System.currentTimeMillis()
                    )
                }
            } catch (e: Exception) { /* retry */ }
            kotlinx.coroutines.delay(1_000)
        }
        null
    }

    override suspend fun createAndWaitForOTP(timeoutMs: Long): ProviderEmail? {
        val inbox = createInbox() ?: return null
        return waitForEmail(inbox.inboxId, inbox.token, timeoutMs)
    }
}

/** Courier-Provider (Stub; echte Anbindung folgt bei Provider-Key). */
class CourierProvider(
    private val baseUrl: String = "https://api.courier.com/"
) : EmailProvider {
    override val name = "Courier"
    override suspend fun createInbox(): ProviderInbox? = null
    override suspend fun waitForEmail(inboxId: String, token: String?, timeoutMs: Long): ProviderEmail? = null
    override suspend fun createAndWaitForOTP(timeoutMs: Long): ProviderEmail? {
        val inbox = createInbox() ?: return null
        return waitForEmail(inbox.inboxId, inbox.token, timeoutMs)
    }
}

/** MailAgent-Provider (Stub; echte Anbindung folgt bei Provider-Key). */
class MailAgentProvider(
    private val baseUrl: String = "https://api.mailagent.io/"
) : EmailProvider {
    override val name = "MailAgent"
    override suspend fun createInbox(): ProviderInbox? = null
    override suspend fun waitForEmail(inboxId: String, token: String?, timeoutMs: Long): ProviderEmail? = null
    override suspend fun createAndWaitForOTP(timeoutMs: Long): ProviderEmail? = null
}

/** TempMail-OTP-MCP-Provider (Stub; Apify-Actor aktivieren, wenn Key verfügbar). */
class ApifyOtpProvider(
    private val baseUrl: String = "https://api.apify.com/v2/"
) : EmailProvider {
    override val name = "Apify OTP"
    override suspend fun createInbox(): ProviderInbox? = null
    override suspend fun waitForEmail(inboxId: String, token: String?, timeoutMs: Long): ProviderEmail? = null
    override suspend fun createAndWaitForOTP(timeoutMs: Long): ProviderEmail? = null
}

/** Hilfsfunktion für HTTP-Calls. */
internal object HttpClient {
    fun call(
        client: OkHttpClient,
        method: String,
        url: String,
        body: String?,
        bearer: String?
    ): String? {
        return try {
            val req = Request.Builder()
                .url(url)
                .method(method, if (body != null) RequestBody.create(null, body) else null)
            if (bearer != null) req.addHeader("Authorization", "Bearer $bearer")
            client.newCall(req.build()).execute().use { resp ->
                if (resp.isSuccessful) resp.body?.string() else null
            }
        } catch (e: Exception) { null }
    }
}
