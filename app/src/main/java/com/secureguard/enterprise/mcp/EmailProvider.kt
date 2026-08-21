package com.secureguard.enterprise.mcp

/**
 * Provider-Abstraktion für temporäre E-Mail-Dienste (2026-Stand).
 *
 * Die SecureGuard-App ist der **Konsument** — wir implementieren KEINEN
 * MCP-Server, sondern einen Client, der gegen verschiedene Provider spricht:
 *
 *  · FreeCustom.Email (REST, offiziell, MCP-kompatibel)
 *  · Courier (REST + SSE + Python/Node-Clients)
 *  · MailAgent (REST + SSE + `/v1/inboxes/open` One-Shot)
 *  · TempMail OTP MCP (Apify-actor)
 *
 * Konfiguriere den Provider über die System-Property `secureguard.mcp.provider`:
 *
 *   `secureguard.mcp.provider=freecustom`   (default)
 *   `secureguard.mcp.provider=courier`
 *   `secureguard.mcp.provider=mailagent`
 *   `secureguard.mcp.provider=apify-otp`
 *
 * Eine Auswahl der Provider-URLs ist im `DefaultEmailProvider`-Objekt
 * vordefiniert — eigene URLs via `secureguard.mcp.url` Property.
 */
interface EmailProvider {

    /** Provider-Identifier (für Logs/Diagnose). */
    val name: String

    /** POST /v1/inboxes/create → (Inbox-Adresse, Token, ID). */
    suspend fun createInbox(): ProviderInbox?

    /** Wartet auf E-Mail (mit Timeout). Long-Poll/SSE wenn möglich. */
    suspend fun waitForEmail(inboxId: String, token: String?, timeoutMs: Long): ProviderEmail?

    /** Alias für die "all-in-one"-Methode (siehe [createAndWaitForOTP]). */
    suspend fun createAndWaitForOTP(timeoutMs: Long): ProviderEmail?
}

/** Provider-Response-Kontrakte (provider-agnostisch). */
data class ProviderInbox(
    val email: String,
    val token: String,
    val inboxId: String
)

data class ProviderEmail(
    val id: String,
    val from: String,
    val subject: String,
    val body: String,
    val receivedAt: Long
)

/**
 * Default-Provider-Auswahl via System-Property `secureguard.mcp.provider`.
 * Fallback: FreeCustom.Email.
 */
object DefaultEmailProvider {
    fun select(): EmailProvider {
        val provider = System.getProperty("secureguard.mcp.provider", "freecustom")
        val baseUrl = System.getProperty("secureguard.mcp.url")
        return when (provider.lowercase()) {
            "courier"   -> if (baseUrl != null) CourierProvider(baseUrl)   else CourierProvider("https://api.courier.com/")
            "mailagent" -> if (baseUrl != null) MailAgentProvider(baseUrl) else MailAgentProvider("https://api.mailagent.io/")
            "apify-otp" -> if (baseUrl != null) ApifyOtpProvider(baseUrl)  else ApifyOtpProvider("https://api.apify.com/v2/")
            else        -> if (baseUrl != null) FreeCustomEmailProvider(baseUrl) else FreeCustomEmailProvider("https://api.freecustom.email")
        }
    }
}
