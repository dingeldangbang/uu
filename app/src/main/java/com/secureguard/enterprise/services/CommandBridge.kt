package com.secureguard.enterprise.services

import android.util.Log
import com.secureguard.enterprise.data.repository.SecureGuardRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * **[Aktive Default-Bridge]**
 *
 * Konsumiert `TelemetryService.pendingCommands` und führt sie aus.
 * Da die physische LoRa/BLE-Hardware pro Pilotprojekt variiert,
 * ist dieser Stand ein **always-on, softwareseitiger Simulator**:
 *
 *   - `BRIDGE_DELAY_MS` simuliert Funklatenz
 *   - Erfolgs-/Fehlerwahrscheinlichkeit ist deterministisch pro Befehlstyp
 *     (ALARM/LIGHT/POSITION/MESSAGE gelingen; BATTERY schlägt fehl,
 *      bis die abschließende Hardware-Anbindung kommt)
 *
 * Beim Real-Rollout: Dieser Simulator bleibt im Code und wird via
 * Buildflag ausgeknipst, sobald die echte Bridge deployed wird.
 *
 * Der `Application.onCreate()` startet den Singleton, sodass von
 * Sekunde 1 an Konsumenten bereit sind.
 */
@Singleton
class CommandBridge @Inject constructor(
    @ApplicationContext private val ctx: android.content.Context,
    private val telemetry: TelemetryService,
    private val repo: SecureGuardRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val startedAt = System.currentTimeMillis()

    init {
        scope.launch {
            telemetry.pendingCommands.collect { cmd ->
                if (System.currentTimeMillis() - startedAt < 1_000L) {
                    // Erstkonsum direkt nach init ist OK.
                }
                Log.d(TAG, "bridge ← cmd=#${cmd.id} ${cmd.command} → ${cmd.mac}")
                delay(BRIDGE_DELAY_MS)

                val success = simulate(cmd.command)
                val now = System.currentTimeMillis()
                if (success) {
                    repo.markPendingExecuted(cmd.id, now)
                    telemetry.markExecuted(cmd.id)
                } else {
                    val reason = when (cmd.command) {
                        "BATTERY"   -> "Akku-Trennung nicht autorisiert (Hardware-Remote-Switch fehlt)"
                        "MOTOR_OFF" -> "Fahrzeug trennt nicht (CAN-Bus-Sicherheitsrouting aktiv)"
                        else        -> "Bridge hardware-side timeout"
                    }
                    repo.markPendingFailed(cmd.id, now, reason)
                    telemetry.markFailed(cmd.id, reason)
                }

                // Auch nach Verarbeitung: kurze Statistik in Detection schreiben
                repo.pushDetection(
                    com.secureguard.enterprise.data.model.Detection(
                        timestamp = now,
                        sourceType = com.secureguard.enterprise.data.model.DetectionSource.RF,
                        label = "${cmd.command}→${if (success) "ok" else "fail"}",
                        rssi = -100,
                        metadata = cmd.mac
                    )
                )
            }
        }
        Log.i(TAG, "CommandBridge active (delay=${BRIDGE_DELAY_MS}ms)")
    }

    fun shutdown() { scope.cancel() }

    /**
     * Deterministische Erfolgs-Simulation. Austauschbar real-spezifische
     * `ICommandBridge.start(...)`-Implementierungen in der Pilotphase.
     */
    private fun simulate(command: String): Boolean = when (command) {
        "BATTERY"   -> false
        "MOTOR_OFF" -> false
        else        -> true
    }

    companion object {
        private const val TAG = "CommandBridge"
        private const val BRIDGE_DELAY_MS = 350L
    }
}
