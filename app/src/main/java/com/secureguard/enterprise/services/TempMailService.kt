package com.secureguard.enterprise.services

import com.secureguard.enterprise.data.model.Asset
import com.secureguard.enterprise.data.model.Detection
import com.secureguard.enterprise.data.model.DetectionSource
import com.secureguard.enterprise.data.model.SearchResult
import com.secureguard.enterprise.mcp.EmailProvider
import com.secureguard.enterprise.mcp.DefaultEmailProvider
import com.secureguard.enterprise.mcp.OTPDetector
import com.secureguard.enterprise.mcp.ProviderEmail
import com.secureguard.enterprise.mcp.ProviderInbox
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * TempMailService — provider-agnostischer Wrapper für temporäre
 * E-Mail-Inboxen und OTP-Empfang.
 *
 * Standard-Provider ist FreeCustom.Email (REST + 1s-Poll-Loop). Über die
 * System-Property `secureguard.mcp.provider` ist konfigurierbar:
 *   freecustom | courier | mailagent | apify-otp
 *
 * Drei Backup-Strategien (2026-Stand):
 *   1. `createAndWaitForOTP(timeoutMs)` — One-Shot, bevorzugt
 *   2. `waitForOTP(timeoutMs)`          — 2-Schritt (Inbox + Wait)
 *   3. `extractMagicLink(...)`          — falls bevorzugt
 *
 * `OTPDetector` macht provider-agnostische Heuristik (numeric, alphanum,
 * Schlüsselwörter, Magic-Link-Priorität).
 */
@Singleton
class TempMailService @Inject constructor() {

    // ── Provider zur Laufzeit umschaltbar (via System-Property lesbar) ──
    private val provider: EmailProvider
        get() = DefaultEmailProvider.select()

    // ── State für die UI ──
    private val _currentInbox = MutableStateFlow<InboxResult?>(null)
    val currentInbox: StateFlow<InboxResult?> = _currentInbox.asStateFlow()

    private val _lastOTP = MutableStateFlow<OTPResult?>(null)
    val lastOTP: StateFlow<OTPResult?> = _lastOTP.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    /** Welcher Provider aktuell genutzt wird (für UI-Anzeige). */
    val providerName: String get() = provider.name

    // ── E-Mail / Inbox ──
    suspend fun createInbox(): InboxResult? {
        _isProcessing.value = true
        return try {
            val p: ProviderInbox? = provider.createInbox()
            if (p != null) {
                val result = InboxResult(
                    success = true,
                    email   = p.email,
                    token   = p.token,
                    inboxId = p.inboxId,
                    providerName = providerName
                )
                _currentInbox.value = result
                _isProcessing.value = false
                result
            } else {
                _isProcessing.value = false
                null
            }
        } catch (e: Exception) {
            _isProcessing.value = false
            null
        }
    }

    // ── One-Shot-Pfad (MailAgent-/Courier-Style) ──
    suspend fun createAndWaitForOTP(timeoutMs: Long = 45000): OTPResult? {
        _isProcessing.value = true
        return try {
            val email: ProviderEmail? = provider.createAndWaitForOTP(timeoutMs)
            val result = email.let { mapEmailToOTPResult(it, providerName) }
            _lastOTP.value = result
            _isProcessing.value = false
            result
        } catch (e: Exception) {
            _isProcessing.value = false
            OTPResult(success = false, error = e.message ?: "Exception")
        }
    }

    // ── 2-Schritt-Pfad ──
    suspend fun waitForOTP(timeoutMs: Long = 45000): OTPResult? {
        val inbox = _currentInbox.value ?: return null
        _isProcessing.value = true
        return try {
            val email = provider.waitForEmail(inbox.inboxId, inbox.token, timeoutMs)
            val result = mapEmailToOTPResult(email, providerName)
            _lastOTP.value = result
            _isProcessing.value = false
            result
        } catch (e: Exception) {
            _isProcessing.value = false
            null
        }
    }

    suspend fun getLastOTP(): OTPResult? = _lastOTP.value

    fun clearInbox() {
        _currentInbox.value = null
        _lastOTP.value = null
        _isProcessing.value = false
    }

    /** Provider-agnostische OTP/MagicLink-Extraktion aus Rohtext. */
    fun extractOTP(emailBody: String): String? =
        OTPDetector.extract(subject = "", body = emailBody).let { (it as? OTPDetector.Extracted.OTP)?.code }

    fun extractMagicLink(emailBody: String): String? =
        OTPDetector.extract(subject = "", body = emailBody).let { (it as? OTPDetector.Extracted.MagicLink)?.url }

    /** Erweitert: priorisiert MagicLink > OTP > None, mit confidence. */
    fun extract(subject: String, body: String): OTPDetector.Extracted = OTPDetector.extract(subject, body)

    /** Automatisierter Standard-Flow (für UI-Button / Agent-Calls). */
    suspend fun autoRegisterAndGetOTP(serviceName: String, timeoutMs: Long = 45000): OTPResult? =
        createAndWaitForOTP(timeoutMs)

    // ── Mapping Provider-Response → interne Result-Klasse ──
    private fun mapEmailToOTPResult(email: ProviderEmail?, providerName: String): OTPResult {
        if (email == null) return OTPResult(success = false, error = "Timeout oder keine Mail empfangen", providerName = providerName)

        val ext = OTPDetector.extract(subject = email.subject, body = email.body)
        return when (ext) {
            is OTPDetector.Extracted.OTP -> {
                OTPResult(
                    success      = true,
                    otp          = ext.code,
                    email        = email.subject.let { "" } .let { "" } + "", // subject-as-email placeholder
                    from         = email.from,
                    subject      = email.subject,
                    magicLink    = null,
                    providerName = providerName
                )
            }
            is OTPDetector.Extracted.MagicLink -> {
                OTPResult(
                    success      = true,
                    otp          = null,
                    from         = email.from,
                    subject      = email.subject,
                    magicLink    = ext.url,
                    providerName = providerName
                )
            }
            OTPDetector.Extracted.None -> {
                OTPResult(
                    success      = false,
                    error        = "Kein OTP/MagicLink im E-Mail-Body erkannt",
                    from         = email.from,
                    subject      = email.subject,
                    providerName = providerName
                )
            }
        }
    }

    /** Rückwärtskompatibles Such-Interface — Temp-Mail-Kanal ist Pilot-TODO. */
    suspend fun searchAsset(asset: Asset): Detection? = null

    /** Suche (neues SearchResult-Interface — erhält E-Mail-OTP-Schritte als Detection). */
    suspend fun searchAssetResult(asset: Asset): SearchResult {
        val d = searchAsset(asset) ?: return SearchResult.notFound(
            DetectionSource.URBAN,
            metadata = mapOf("reason" to "temp_mail_no_otp"))
        return SearchResult.success(d, DetectionSource.URBAN, accuracy = 0.95f,
            metadata = mapOf("transport" to "temp_mail"))
    }
}

data class InboxResult(
    val success: Boolean,
    val email: String = "",
    val token: String = "",
    val inboxId: String = "",
    val providerName: String = "",
    val error: String? = null
)

data class OTPResult(
    val success: Boolean,
    val otp: String? = null,
    val email: String = "",
    val magicLink: String? = null,
    val from: String = "",
    val subject: String = "",
    val providerName: String = "",
    val error: String? = null
)
