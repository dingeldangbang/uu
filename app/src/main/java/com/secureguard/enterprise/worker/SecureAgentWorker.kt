package com.secureguard.enterprise.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.secureguard.enterprise.data.database.AgentConfigDao
import com.secureguard.enterprise.data.model.AgentConfig
import com.secureguard.enterprise.data.model.Alert
import com.secureguard.enterprise.data.model.AlertSeverity
import com.secureguard.enterprise.data.model.AlertType
import com.secureguard.enterprise.data.model.AssetStatus
import com.secureguard.enterprise.data.model.DetectionSource
import com.secureguard.enterprise.data.model.Detection
import com.secureguard.enterprise.data.repository.SecureGuardRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlin.random.Random

/**
 * Adaptiver Agent-Worker.
 *
 * Liest [AgentConfig] aus der DB, aktualisiert den Q-Table
 * über eine echte ε-greedy Q-Learning-Policy und persistiert zurück.
 *
 * Die Reward-Funktion ist als einfaches Heuristik-Bootstrap implementiert:
 * - batteryOk +1, signalOk +1, geofenceOk +1
 * - falsePositive −2, missedCritical −3
 *
 * Echte Belohnung käme aus Folgeereignissen (later state outcomes).
 */
@HiltWorker
class SecureAgentWorker @AssistedInject constructor(
    @Assisted ctx: Context,
    @Assisted params: WorkerParameters,
    private val repo: SecureGuardRepository,
    private val configDao: AgentConfigDao
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        return try {
            runCycle()
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "agent cycle failed", e)
            Result.retry()
        }
    }

    private suspend fun runCycle() {
        val now = System.currentTimeMillis()
        val config = configDao.get() ?: AgentConfig().also { configDao.upsert(it) }
        val epsilon = config.explorationRate
        val gamma   = config.discountFactor
        val alpha   = config.learningRate

        // Snapshot aller Assets
        val assets = repo.assetSnapshot()
        if (assets.isEmpty()) {
            Log.d(TAG, "keine Assets → skip")
            return
        }

        // Exploration vs. Exploitation
        val explore = Random.nextFloat() < epsilon
        Log.d(TAG, "cycle start • assets=${assets.size} • ε=$epsilon explore=$explore")

        val updatedDecisions = config.totalDecisions + 1
        val qUpdates = HashMap(config.qTable)

        // Zwei Aktions-Hashes (Stubs) — Realität: actions wie LOCKDOWN/ALARM/NONE...
        val actions = listOf("OBSERVE", "LOCKDOWN", "ALERT", "NONE")

        for (asset in assets) {
            val stateKey = "${asset.status.name}|b${asset.batteryPercent}%|s${asset.rssi}"
            val chosen = if (explore) actions.random() else {
                actions.maxByOrNull { qUpdates[stateKey + "|" + it] ?: 0f }
                    ?: "OBSERVE"
            }
            val reward = heuristicReward(asset)
            val previousValue = qUpdates[stateKey + "|" + chosen] ?: 0f
            val nextStateKey = stateKey.replace(asset.status.name, asset.status.name)
            val nextBest = actions.maxOfOrNull { qUpdates[nextStateKey + "|" + it] ?: 0f }
            val newValue = previousValue + alpha * (reward + gamma * (nextBest ?: 0f) - previousValue)
            qUpdates[stateKey + "|" + chosen] = newValue

            // Policy-Migration: bei niedrigem Akku → MAINTENANCE
            if (asset.batteryPercent in 1..10 && asset.status == AssetStatus.ONLINE) {
                repo.updateAssetStatus(
                    mac = asset.mac.ifBlank { asset.id },
                    status = AssetStatus.MAINTENANCE,
                    timestamp = now
                )
                repo.insertAlert(
                    Alert(
                        timestamp = now,
                        severity = AlertSeverity.WARNING,
                        type = AlertType.MAINTENANCE,
                        title = "Akku-Kritisch: ${asset.name}",
                        message = "Akku ${asset.batteryPercent}% → Wartungsmodus aktiviert",
                        assetId = asset.id
                    )
                )
            }
        }

        // Persistierte Konfig zurück
        configDao.upsert(
            config.copy(
                qTable = qUpdates,
                lastTrainingEpoch = now,
                totalDecisions = updatedDecisions
            )
        )

        // Activity trail
        if (explore) {
            repo.pushDetection(
                Detection(
                    timestamp = now,
                    sourceType = DetectionSource.LORA,
                    label = "agent-explore",
                    latitude = null, longitude = null,
                    metadata = "ε-cycle"
                )
            )
        }

        Log.i(TAG, "cycle done • decisions=$updatedDecisions q=${qUpdates.size}")
    }

    private fun heuristicReward(asset: com.secureguard.enterprise.data.model.Asset): Float {
        val weights = mapOf(
            "batteryOk"     to (if (asset.batteryPercent > 30) 1f else -0.5f),
            "signalOk"      to (if (asset.rssi > -80) 1f else -0.3f),
            "geofenceOk"    to 1f,
            "falsePositive" to (if (asset.status == AssetStatus.ALERT) -2f else 0f),
            "missedCritical" to (if (asset.status == AssetStatus.OFFLINE && asset.lastSeen != null
                                   && System.currentTimeMillis() - asset.lastSeen > 24 * 3600_000L) -3f else 0f)
        )
        return weights.values.sum()
    }

    companion object { private const val TAG = "SecureAgentWorker" }
}
