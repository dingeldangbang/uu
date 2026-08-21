package com.secureguard.enterprise

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.secureguard.enterprise.data.repository.SecureGuardRepository
import com.secureguard.enterprise.data.repository.SeedDataInitializer
import com.secureguard.enterprise.services.AgentService
import com.secureguard.enterprise.services.CommandBridge
import com.secureguard.enterprise.services.DeviceBatteryProvider
import com.secureguard.enterprise.services.HoneywellScanner
import com.secureguard.enterprise.services.TelemetryService
import com.secureguard.enterprise.util.NotificationConstants
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Application-Root. Initialisiert:
 * - Hilt-Container
 * - WorkManager mit HiltWorkerFactory
 * - Notification-Channels (API 26+)
 * - Eager-Singleton-Boots (CommandBridge, BatteryProvider, …)
 *
 * Was diese Application **NICHT** tut:
 * - Lädt **keine** BETRIEBSVEREINBARUNG.md (Blueprint-Dokument; bleibt
 *   ausschließlich als Referenz im Repo).
 * - Erzwingt **keinen** Acceptance-Dialog.
 * - Persistiert **keine** Compliance-Toggles.
 *
 * Die DSGVO/BDSG-Bedingungen aus dem Dokument sind als TOMs in den
 * Modulen implementiert (Room-AES, Location-Coarse-Mode, Retention-Worker);
 * die Vorlage selbst ist hingegen "blueprint" und nirgendwo ins UI
 * gebunden. Sobald der Pilot produktiv geht: ein zusätzlicher
 * `ComplianceGate` ist problemlos nachzurüsten.
 */
@HiltAndroidApp
class SecureGuardApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var commandBridge: CommandBridge           // aktiviert PendingCommandConsumer
    @Inject lateinit var batteryProvider: DeviceBatteryProvider  // registriert BroadcastReceiver
    @Inject lateinit var honeywellScanner: HoneywellScanner      // ab `claim()` aktiv (UI-getrieben)
    @Inject lateinit var telemetryService: TelemetryService      // startet FLP bei Permission
    @Inject lateinit var agentService: AgentService              // initialisiert WorkManager
    @Inject lateinit var seedData: SeedDataInitializer           // füllt Beispiel-Daten beim ersten Boot
    @Inject lateinit var apiNodeManager: ApiNodeManager          // 11 Abfrageknoten (WiGle, DHL, ...)
    @Inject lateinit var tempMailService: TempMailService        // temporäre E-Mail / OTP
    @Inject lateinit var repository: SecureGuardRepository       // Room-Zugriff für Node-Detections

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        // Defensive: alle @Singletons einmal initialisiert; die Reihenfolge legt
        // nur fest, wann `init {}` Blöcke feuern.
        ensureSingletonsStarted()
        LogBootState()
        // Seed-Daten nur beim ersten Boot (assetCount() == 0)
        appScope.launch {
            try {
                seedData.seedIfEmpty()
            } catch (e: Exception) {
                android.util.Log.e("SecureGuardApp", "seed failed", e)
            }
        }

        // ApiNodeManager-Detections → Room persistieren (Knoten-Funde erscheinen in History)
        appScope.launch {
            apiNodeManager.detections.collect { detection ->
                runCatching { repository.pushDetection(detection) }
            }
        }
    }

    private fun ensureSingletonsStarted() {
        // Honig (read-once) — Hilt's @Singleton sorgt für die Instanziierung;
        // dieser Block existiert nur, damit die Reihenfolge dokumentiert ist.
        listOf(
            "CommandBridge"        to commandBridge,
            "DeviceBatteryProvider" to batteryProvider,
            "HoneywellScanner"     to honeywellScanner,
            "TelemetryService"     to telemetryService,
            "AgentService"         to agentService
        ).forEach { (name, ref) ->
            android.util.Log.i("SecureGuardApp", "$name → ${ref.javaClass.simpleName}")
        }
    }

    private fun LogBootState() {
        android.util.Log.i(
            "SecureGuardApp",
            "boot complete · ${com.secureguard.enterprise.util.DeviceCompat.deviceSummary()} · " +
            "targetSdk=34 · BETRIEBSVEREINBARUNG=blueprint (NOT bound to UI)"
        )
    }

    override fun onTerminate() {
        commandBridge.shutdown()
        super.onTerminate()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(NotificationManager::class.java) ?: return

        nm.createNotificationChannel(
            NotificationChannel(
                NotificationConstants.CHANNEL_ALERTS,
                getString(R.string.notif_channel_alerts),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.notif_channel_alerts_desc)
                enableLights(true)
                enableVibration(true)
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(
                NotificationConstants.CHANNEL_SERVICES,
                getString(R.string.notif_channel_services),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notif_channel_services_desc)
                setShowBadge(false)
            }
        )
    }
}
