package com.secureguard.enterprise.presentation.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.secureguard.enterprise.data.model.AgentSettings
import com.secureguard.enterprise.data.model.Asset
import com.secureguard.enterprise.data.model.AssetStatus
import com.secureguard.enterprise.data.model.DashboardUiState
import com.secureguard.enterprise.data.repository.SecureGuardRepository
import com.secureguard.enterprise.services.AgentService
import com.secureguard.enterprise.services.DeviceBatteryProvider
import com.secureguard.enterprise.services.TelemetryService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: SecureGuardRepository,
    private val agentService: AgentService,
    private val battery: DeviceBatteryProvider,
    private val telemetry: TelemetryService
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val _assets = MutableStateFlow<List<Asset>>(emptyList())
    val assets: StateFlow<List<Asset>> = _assets.asStateFlow()

    private val _agentStatus = MutableStateFlow(false)
    val agentStatus: StateFlow<Boolean> = _agentStatus.asStateFlow()

    init {
        loadAssets()
        monitorAgentStatus()
        monitorBattery()
        ensureAgentStarted()
    }

    private fun loadAssets() {
        viewModelScope.launch {
            repository.getWhitelistedAssets().collect { assetList ->
                _assets.value = assetList
                updateStats(assetList)
                _uiState.update { it.copy(lastSyncTime = formatClock()) }
            }
        }
    }

    private fun monitorAgentStatus() {
        viewModelScope.launch {
            agentService.agentStatus.collect { status ->
                _agentStatus.value = status.running
                _uiState.update {
                    it.copy(agentRunning = status.running, lastSyncTime = formatClock())
                }
            }
        }
    }

    private fun monitorBattery() {
        viewModelScope.launch {
            battery.level.collect { level ->
                _uiState.update { it.copy(batteryLevel = level) }
            }
        }
        viewModelScope.launch {
            battery.plugged.collect { isPlugged ->
                _uiState.update { it.copy(batteryPlugged = isPlugged) }
            }
        }
    }

    private fun ensureAgentStarted() {
        // Stelle sicher, dass in jedem Fall eine Standard-Konfig vorhanden ist;
        // start-Aufruf ist idempotent dank UPDATE-Policy.
        val settings = AgentSettings(
            interval = 30,
            dynamicPriority = true,
            learningMode = true,
            offlineOnly = true,
            externalSources = false
        )
        agentService.start(settings)
        telemetry.startUpdates()
    }

    private fun updateStats(assetList: List<Asset>) {
        val total       = assetList.size
        val online      = assetList.count { it.status == AssetStatus.ONLINE }
        val maintenance = assetList.count { it.status == AssetStatus.MAINTENANCE }
        val offline     = assetList.count { it.status == AssetStatus.OFFLINE }
        val searching   = assetList.count { it.status == AssetStatus.SEARCHING }

        _uiState.update { state ->
            state.copy(
                totalAssets      = total,
                onlineAssets     = online,
                offlineAssets    = offline,
                maintenanceAssets = maintenance,
                activeSearches   = searching,
                alertCount       = maintenance + offline
            )
        }
    }

    fun refresh() {
        viewModelScope.launch { loadAssets() }
    }

    fun toggleAgent() {
        if (_agentStatus.value) agentService.stop()
        else agentService.start(defaultSettings())
    }

    private fun defaultSettings() = AgentSettings(
        interval = 30,
        dynamicPriority = true,
        learningMode = true,
        offlineOnly = true,
        externalSources = false
    )

    private fun formatClock() = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

    override fun onCleared() {
        agentService.stop()
        telemetry.stopUpdates()
        super.onCleared()
    }
}
