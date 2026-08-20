package com.secureguard.enterprise.presentation.ui.actions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.secureguard.enterprise.data.model.Asset
import com.secureguard.enterprise.data.repository.SecureGuardRepository
import com.secureguard.enterprise.services.TelemetryService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class ActionsViewModel @Inject constructor(
    private val repository: SecureGuardRepository,
    private val telemetryService: TelemetryService
) : ViewModel() {

    private val _assets = MutableStateFlow<List<Asset>>(emptyList())
    val assets: StateFlow<List<Asset>> = _assets.asStateFlow()

    private val _selectedAsset = MutableStateFlow<Asset?>(null)
    val selectedAsset: StateFlow<Asset?> = _selectedAsset.asStateFlow()

    private val _commandLog = MutableStateFlow<List<String>>(emptyList())
    val commandLog: StateFlow<List<String>> = _commandLog.asStateFlow()

    private val _isExecuting = MutableStateFlow(false)
    val isExecuting: StateFlow<Boolean> = _isExecuting.asStateFlow()

    init { loadAssets() }

    private fun loadAssets() {
        viewModelScope.launch {
            repository.getWhitelistedAssets().collect { list ->
                _assets.value = list
                if (_selectedAsset.value == null && list.isNotEmpty()) {
                    _selectedAsset.value = list.first()
                }
            }
        }
    }

    fun selectAsset(asset: Asset) { _selectedAsset.value = asset }

    fun executeAction(actionType: ActionType) {
        viewModelScope.launch {
            val asset = _selectedAsset.value ?: return@launch
            _isExecuting.value = true
            val ts   = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            val ok   = telemetryService.sendCommand(asset.mac, actionType.cmd)
            val mark = if (ok) "✓" else "✗"
            _commandLog.value = _commandLog.value + "$ts → ${actionType.label} $mark"
            _isExecuting.value = false
        }
    }

    fun clearLog() { _commandLog.value = emptyList() }
}

enum class ActionType(val cmd: String, val label: String) {
    ALARM("ALARM",     "ALARM ausgelöst"),
    LIGHT("LIGHT",     "Lichter blinken"),
    MOTOR_OFF("MOTOR_OFF","Motor ausgeschaltet"),
    BATTERY("BATTERY", "Batterie getrennt"),
    MESSAGE("MESSAGE", "Nachricht gesendet"),
    POSITION("POSITION","Position angefordert"),
    RESTART("RESTART", "Neustart ausgelöst"),
    TELEMETRY("TELEMETRY","Telemetrie gelesen")
}
