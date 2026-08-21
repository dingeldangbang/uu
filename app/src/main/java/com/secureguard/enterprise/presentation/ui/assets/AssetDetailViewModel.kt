package com.secureguard.enterprise.presentation.ui.assets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.secureguard.enterprise.data.model.ActionResult
import com.secureguard.enterprise.data.model.Alert
import com.secureguard.enterprise.data.model.AlertSeverity
import com.secureguard.enterprise.data.model.AlertType
import com.secureguard.enterprise.data.model.Asset
import com.secureguard.enterprise.data.model.AssetStatus
import com.secureguard.enterprise.data.model.Detection
import com.secureguard.enterprise.data.model.SearchResult
import com.secureguard.enterprise.data.repository.SecureGuardRepository
import com.secureguard.enterprise.services.CrowdService
import com.secureguard.enterprise.services.LoraService
import com.secureguard.enterprise.services.NotificationService
import com.secureguard.enterprise.services.OpticalService
import com.secureguard.enterprise.services.SatelliteService
import com.secureguard.enterprise.services.TelemetryService
import com.secureguard.enterprise.services.UrbanService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AssetDetailViewModel @Inject constructor(
    private val repository: SecureGuardRepository,
    private val telemetryService: TelemetryService,
    private val loraService: LoraService,
    private val opticalService: OpticalService,
    private val urbanService: UrbanService,
    private val crowdService: CrowdService,
    private val satelliteService: SatelliteService,
    private val notificationService: NotificationService
) : ViewModel() {

    private val _asset = MutableStateFlow<Asset?>(null)
    val asset: StateFlow<Asset?> = _asset.asStateFlow()

    private val _detections = MutableStateFlow<List<Detection>>(emptyList())
    val detections: StateFlow<List<Detection>> = _detections.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _searchResult = MutableStateFlow<SearchResult?>(null)
    val searchResult: StateFlow<SearchResult?> = _searchResult.asStateFlow()

    private val _actionResult = MutableStateFlow<ActionResult?>(null)
    val actionResult: StateFlow<ActionResult?> = _actionResult.asStateFlow()

    /** Spec: loadAsset(id). */
    fun loadAsset(assetId: String) {
        viewModelScope.launch {
            // 1. Versuch: per MAC (häufigster Identifier aus QR-Scan)
            var found = repository.getAssetByMac(assetId)
            // 2. Versuch: per Asset-ID
            if (found == null) {
                val snapshot = repository.assetSnapshot()
                found = snapshot.firstOrNull { it.id == assetId }
            }
            if (found != null) {
                _asset.value = found
                loadDetections(found.mac.ifBlank { found.id })
            }
        }
    }

    private fun loadDetections(mac: String) {
        viewModelScope.launch {
            repository.getDetections(mac).collect { list ->
                _detections.value = list
            }
        }
    }

    // ─────── Aktionen ───────

    fun executeAction(actionType: ActionType) {
        viewModelScope.launch {
            _actionResult.value = ActionResult.Processing
            val asset = _asset.value ?: return@launch
            val result = when (actionType) {
                ActionType.ALARM     -> performSend(asset, "ALARM",     "Alarm ausgelöst",     "Keine Verbindung")
                ActionType.LIGHT     -> performSend(asset, "LIGHT",     "Lichter blinken",     "Keine Verbindung")
                ActionType.MOTOR_OFF -> performSend(asset, "MOTOR_OFF", "Motor ausgeschaltet", "Keine Verbindung")
                ActionType.BATTERY   -> performSend(asset, "BATTERY",   "Batterie getrennt",   "Keine Verbindung")
                ActionType.MESSAGE   -> performSend(asset, "MESSAGE",   "Nachricht gesendet",  "Keine Verbindung")
                ActionType.POSITION  -> performSend(asset, "POSITION",  "Position angefordert","Keine Verbindung")
                ActionType.RESTART   -> performSend(asset, "RESTART",   "Neustart ausgelöst",  "Keine Verbindung")
                ActionType.TELEMETRY -> performSend(asset, "TELEMETRY", "Telemetrie gelesen",  "Keine Verbindung")
            }
            _actionResult.value = result
            if (result.success) {
                repository.insertAlert(
                    Alert(
                        timestamp = System.currentTimeMillis(),
                        type = AlertType.SECURITY,
                        severity = AlertSeverity.INFO,
                        title = "${actionType.name} ausgeführt",
                        message = result.message,
                        assetId = asset.id
                    )
                )
                notificationService.sendActionNotification(asset, actionType, true)
            } else {
                repository.insertAlert(
                    Alert(
                        timestamp = System.currentTimeMillis(),
                        type = AlertType.CRITICAL,
                        severity = AlertSeverity.WARNING,
                        title = "${actionType.name} fehlgeschlagen",
                        message = result.message,
                        assetId = asset.id
                    )
                )
                notificationService.sendActionNotification(asset, actionType, false)
            }
        }
    }

    private suspend fun performSend(
        asset: Asset, cmd: String, okMsg: String, errMsg: String
    ): ActionResult {
        if (asset.mac.isBlank()) return ActionResult(false, "Keine MAC-Adresse")
        return if (telemetryService.sendCommand(asset.mac, cmd)) {
            ActionResult(true, okMsg)
        } else {
            ActionResult(false, errMsg)
        }
    }

    // ─────── Suche ───────

    fun startSearch() {
        viewModelScope.launch {
            _isSearching.value = true
            val asset = _asset.value ?: return@launch

            val candidates = listOfNotNull(
                telemetryService.searchAsset(asset),
                loraService.searchAsset(asset),
                opticalService.searchAsset(asset),
                urbanService.searchAsset(asset),
                crowdService.searchAsset(asset),
                satelliteService.searchAsset(asset)
            )
            val best = candidates.minByOrNull { it.rssi }
            _searchResult.value = if (best != null) {
                repository.insertDetection(best)
                SearchResult(found = true, detection = best)
            } else {
                SearchResult(found = false)
            }
            _isSearching.value = false
        }
    }

    fun refreshTelemetry() {
        viewModelScope.launch {
            val asset = _asset.value ?: return@launch
            val telemetry = telemetryService.getLatestTelemetry(asset.mac)
            val macForUpdate = asset.mac.ifBlank { asset.id }
            if (telemetry != null) {
                repository.updateAssetStatus(
                    mac = macForUpdate,
                    status = AssetStatus.ONLINE,
                    timestamp = System.currentTimeMillis(),
                    lat = telemetry.latitude,
                    lon = telemetry.longitude
                )
                loadAsset(asset.id)
            } else {
                // Fallback: Position über FLP-Live-Cache ableiten
                val now = System.currentTimeMillis()
                repository.updateAssetStatus(
                    mac = macForUpdate,
                    status = AssetStatus.ONLINE,
                    timestamp = now
                )
                loadAsset(asset.id)
            }
        }
    }
}

enum class ActionType { ALARM, LIGHT, MOTOR_OFF, BATTERY, MESSAGE, POSITION, RESTART, TELEMETRY }
