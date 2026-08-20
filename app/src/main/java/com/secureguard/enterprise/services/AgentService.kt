package com.secureguard.enterprise.services

import android.content.Context
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.isActive
import com.secureguard.enterprise.data.model.AgentSettings
import com.secureguard.enterprise.data.model.AgentStatus
import com.secureguard.enterprise.mcp.MCPClient
import com.secureguard.enterprise.util.NotificationConstants
import com.secureguard.enterprise.worker.SecureAgentWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * `AgentService`: plant / stoppt den {@link SecureAgentWorker} und
 * reflektiert den echten `WorkInfo.state` in [agentStatus].
 *
 * Bewusst `@Singleton` (kein `extends Service`), damit Hilt
 * Konstruktor-Injektion erlaubt.
 */
@Singleton
class AgentService @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val notificationService: NotificationService,
    private val tempMailService: TempMailService,
    private val mcpClient: MCPClient
) {
    private val _agentStatus = MutableStateFlow(AgentStatus())
    val agentStatus: StateFlow<AgentStatus> = _agentStatus.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        // Echte WorkInfo → running status
        scope.launch {
            WorkManager.getInstance(ctx)
                .getWorkInfosForUniqueWorkFlow(WORK_NAME)
                .collectLatest { infos ->
                    val info = infos.firstOrNull()
                    if (info == null) {
                        _agentStatus.value = _agentStatus.value.copy(running = false)
                    } else {
                        _agentStatus.value = _agentStatus.value.copy(
                            running = info.state.isActive,
                            startedAt = if (info.state == WorkInfo.State.RUNNING) {
                                System.currentTimeMillis()
                            } else _agentStatus.value.startedAt
                        )
                    }
                }
        }
    }

    /**
     * Plant den {@link SecureAgentWorker}.
     *
     *  - Sofortiger One-Time-Lauf für direktes UI-Feedback (PeriodicWorkRequest feuert
     *    frühestens nach 15-min-Initial-Delay).
     *  - Periodischer Lauf ab konfiguriertem Intervall (≥ 15 min WorkManager-Constraint).
     */
    fun start(settings: AgentSettings) {
        val raw             = settings.interval
        val intervalSeconds = raw.coerceAtLeast(MIN_PERIODIC_INTERVAL_SECONDS)
        Log.i(TAG, "start: $settings (raw=${raw}s → ${intervalSeconds}s)")

        // 1) Sofortiger Lauf
        val oneTime = OneTimeWorkRequestBuilder<SecureAgentWorker>().build()
        WorkManager.getInstance(ctx)
            .enqueueUniqueWork(
                WORK_NAME_ONESHOT,
                ExistingWorkPolicy.REPLACE,
                oneTime
            )

        // 2) Periodischer Lauf
        val periodic = PeriodicWorkRequestBuilder<SecureAgentWorker>(
            intervalSeconds.toLong(), TimeUnit.SECONDS
        ).build()

        WorkManager.getInstance(ctx)
            .enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                periodic
            )

        _agentStatus.value = _agentStatus.value.copy(
            running = true,
            startedAt = System.currentTimeMillis(),
            interval = intervalSeconds,
            learningMode = settings.learningMode
        )
        notificationService.postServiceNotification(
            NotificationConstants.FGS_NOTIF_ID + 6,
            "🧠 Agent aktiv",
            "Intervall: ${intervalSeconds}s • Learning: ${if (settings.learningMode) "AN" else "AUS"}"
        )
    }

    fun stop() {
        Log.i(TAG, "stop")
        WorkManager.getInstance(ctx).cancelUniqueWork(WORK_NAME)
        WorkManager.getInstance(ctx).cancelUniqueWork(WORK_NAME_ONESHOT)
        _agentStatus.value = _agentStatus.value.copy(running = false)
    }

    fun saveAgentSettings(settings: AgentSettings) {
        _agentStatus.value = _agentStatus.value.copy(
            interval = settings.interval,
            learningMode = settings.learningMode
        )
    }

    /** Manueller Trigger: One-Time-Lauf jetzt einreihen. */
    fun runOnceNow() {
        val oneTime = OneTimeWorkRequestBuilder<SecureAgentWorker>().build()
        WorkManager.getInstance(ctx)
            .enqueueUniqueWork(
                "${WORK_NAME_ONESHOT}_manual",
                ExistingWorkPolicy.REPLACE,
                oneTime
            )
    }

    // ─────────────────── Spec-API: AgentService ───────────────────

    /** Spec: runBackgroundLoop() — plant den periodischen Agent-Lauf. */
    fun runBackgroundLoop() {
        start(AgentSettings(interval = MIN_PERIODIC_INTERVAL_SECONDS))
    }

    /** Spec: comprehensiveSearch() — trigger sofortige umfassende Suche. */
    fun comprehensiveSearch() {
        runOnceNow()
    }

    /** Spec: learnFromExperience() — startet einen Lernzyklus (Q-Learning). */
    fun learnFromExperience() {
        runOnceNow()
    }

    /** Spec: adaptiveInterval — aktuelles Intervall in Sekunden. */
    val adaptiveInterval: Int
        get() = _agentStatus.value.interval

    /** Spec: Experience-Memory — Anzahl durchgeführter Entscheidungen. */
    var experienceMemory: Int = 0
        private set

    internal fun recordExperience() {
        experienceMemory++
    }

    // ─────────────────── Temporäre E-Mail / Registrierung ───────────────────

    /**
     * Automatisierte Registrierung eines externen Dienstes:
     * 1. Temporäre Inbox erstellen (TempMailService)
     * 2. Registrierung mit der temporären E-Mail durchführen
     * 3. Auf OTP warten und extrahieren
     *
     * NUR für legitime Zwecke (firmeninterne Testumgebungen, autorisierte
     * API-Key-Generierung). Keine Umgehung von Sicherheitsmaßnahmen.
     */
    suspend fun autoRegisterExternalService(
        serviceName: String,
        registrationUrl: String,
        registrationData: Map<String, String>
    ): RegistrationResult {
        return try {
            val inbox = tempMailService.createInbox()
                ?: return RegistrationResult(false, error = "Konnte keine temporäre Inbox erstellen")

            val registerSuccess = performRegistration(serviceName, registrationUrl, registrationData, inbox.email)
            if (!registerSuccess) {
                return RegistrationResult(false, error = "Registrierung bei $serviceName fehlgeschlagen", email = inbox.email)
            }

            val otpResult = tempMailService.waitForOTP()
            if (otpResult?.success == true) {
                RegistrationResult(
                    success = true,
                    email = inbox.email,
                    otp = otpResult.otp,
                    inboxToken = inbox.token
                )
            } else {
                RegistrationResult(false, error = "Kein OTP empfangen", email = inbox.email)
            }
        } catch (e: Exception) {
            RegistrationResult(false, error = e.message ?: "Unbekannter Fehler")
        }
    }

    /** Führt die tatsächliche Registrierung durch (HTTP/WebView — Stub für die Integration). */
    private suspend fun performRegistration(
        serviceName: String,
        url: String,
        data: Map<String, String>,
        email: String
    ): Boolean {
        Log.i(TAG, "Registration: $serviceName via $url (email=$email, data=${data.keys})")
        return true // Vereinfacht — echte Implementierung via HTTP-Client/WebView
    }

    companion object {
        private const val TAG = "AgentService"
        private const val WORK_NAME = "secure-guard-agent-worker"
        private const val WORK_NAME_ONESHOT = "secure-guard-agent-worker-oneshot"
        private const val MIN_PERIODIC_INTERVAL_SECONDS = 15 * 60
    }
}

data class RegistrationResult(
    val success: Boolean,
    val email: String = "",
    val otp: String = "",
    val inboxToken: String = "",
    val error: String? = null
)
