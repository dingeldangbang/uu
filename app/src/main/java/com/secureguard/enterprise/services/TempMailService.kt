package com.secureguard.enterprise.services

import com.google.gson.Gson
import com.secureguard.enterprise.mcp.InboxResult
import com.secureguard.enterprise.mcp.OTPResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * TempMailService — temporäre E-Mail-Inboxen für automatische Registrierung
 * und OTP-Empfang (legitime Test-/QA-Zwecke).
 *
 * REST-basiert gegen den konfigurierten Provider:
 *   POST /v1/inboxes/create      → neue Inbox
 *   GET  /v1/inboxes/{id}/emails → E-Mails abrufen
 *
 * Zusätzlich wird `MCPClient` als Alternative für WebSocket-basierte
 * Provider bereitgehalten.
 */
@Singleton
class TempMailService @Inject constructor() {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    companion object {
        private const val API_BASE = "https://api.freecustom.email"
    }

    // ── State für die UI ──
    private val _currentInbox = MutableStateFlow<InboxResult?>(null)
    val currentInbox: StateFlow<InboxResult?> = _currentInbox.asStateFlow()

    private val _lastOTP = MutableStateFlow<OTPResult?>(null)
    val lastOTP: StateFlow<OTPResult?> = _lastOTP.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    // ── Inbox erstellen ──
    suspend fun createInbox(): InboxResult? {
        _isProcessing.value = true
        return try {
            val request = Request.Builder()
                .url("$API_BASE/v1/inboxes/create")
                .post(RequestBody.create(null, ""))
                .build()

            val inbox = withContext(Dispatchers.IO) {
                client.newCall(request).execute().use { resp ->
                    gson.fromJson(resp.body?.string() ?: "{}", Inbox::class.java)
                }
            }
            val result = InboxResult(
                success = inbox.id.isNotBlank(),
                email = inbox.address,
                inboxId = inbox.id
            )
            if (result.success) _currentInbox.value = result
            _isProcessing.value = false
            result
        } catch (e: Exception) {
            _isProcessing.value = false
            null
        }
    }

    // ── Auf E-Mail warten ──
    suspend fun waitForEmail(inboxId: String, timeoutMs: Long = 45000): Email? {
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < timeoutMs) {
            val emails = getEmails(inboxId)
            if (emails.isNotEmpty()) return emails.first()
            delay(1000)
        }
        return null
    }

    // ── OTP abrufen (für UI) ──
    suspend fun waitForOTP(timeoutMs: Long = 45000): OTPResult? {
        val inbox = _currentInbox.value ?: return null
        _isProcessing.value = true
        return try {
            val email = waitForEmail(inbox.inboxId, timeoutMs)
            val otp = email?.body?.let { extractOTP(it) }
            val result = if (otp != null) {
                OTPResult(
                    success = true,
                    otp = otp,
                    email = inbox.email,
                    from = email?.from ?: "",
                    subject = email?.subject ?: ""
                )
            } else {
                OTPResult(success = false, error = "Kein OTP empfangen (Timeout?)")
            }
            _lastOTP.value = result
            _isProcessing.value = false
            result
        } catch (e: Exception) {
            _isProcessing.value = false
            null
        }
    }

    // ── E-Mails abrufen ──
    suspend fun getEmails(inboxId: String): List<Email> {
        return try {
            val request = Request.Builder()
                .url("$API_BASE/v1/inboxes/$inboxId/emails")
                .get()
                .build()

            withContext(Dispatchers.IO) {
                client.newCall(request).execute().use { resp ->
                    val body = resp.body?.string() ?: "[]"
                    gson.fromJson(body, Array<Email>::class.java).toList()
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ── OTP / Magic-Link Extraktion ──
    fun extractOTP(emailBody: String): String? =
        Regex("\\b\\d{4,6}\\b").find(emailBody)?.value

    fun extractMagicLink(emailBody: String): String? =
        Regex("https?://[\\w./?=&-]+").find(emailBody)?.value

    fun clearInbox() {
        _currentInbox.value = null
        _lastOTP.value = null
        _isProcessing.value = false
    }

    /** Automatisierter Flow: Inbox → warte auf OTP. */
    suspend fun autoRegisterAndGetOTP(serviceName: String, timeoutMs: Long = 45000): OTPResult? {
        val inbox = createInbox() ?: return null
        // Der Agent verwendet inbox.email für die Registrierung
        return waitForOTP(timeoutMs)
    }
}

data class Inbox(
    val id: String = "",
    val address: String = "",
    val createdAt: String = ""
)

data class Email(
    val id: String = "",
    val from: String = "",
    val subject: String = "",
    val body: String = "",
    val receivedAt: String = ""
)
