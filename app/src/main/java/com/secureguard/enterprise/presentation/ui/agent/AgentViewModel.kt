package com.secureguard.enterprise.presentation.ui.agent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.secureguard.enterprise.data.model.AgentSettings
import com.secureguard.enterprise.data.model.PendingCommand
import com.secureguard.enterprise.data.repository.SecureGuardRepository
import com.secureguard.enterprise.services.AgentService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AgentViewModel @Inject constructor(
    private val agentService: AgentService,
    private val repo: SecureGuardRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AgentUiState())
    val uiState: StateFlow<AgentUiState> = _uiState.asStateFlow()

    /** Live-Anzahl offener Pending-Commands (Bridge-Konsument in Aktion). */
    val pending: StateFlow<List<PendingCommand>> = repo.observePendingCommands(50)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init { monitorAgentStatus() }

    private fun monitorAgentStatus() {
        viewModelScope.launch {
            agentService.agentStatus.collect { status ->
                _uiState.update { state ->
                    state.copy(
                        agentRunning = status.running,
                        runtime      = calculateRuntime(status.startedAt),
                        progress     = if (status.running) 85f else 0f
                    )
                }
            }
        }
    }

    private fun calculateRuntime(startedAt: Long): String {
        if (startedAt <= 0) return "0h 0m"
        val diff = System.currentTimeMillis() - startedAt
        val totalMin = diff / 60_000
        val days = totalMin / (60 * 24)
        val hours = (totalMin / 60) % 24
        val mins = totalMin % 60
        return when {
            days > 0  -> "${days}d ${hours}h ${mins}m"
            hours > 0 -> "${hours}h ${mins}m"
            else      -> "${mins}m"
        }
    }

    fun setDuration(duration: String)   = _uiState.update { it.copy(duration = duration) }
    fun setCustomDays(days: Int)        = _uiState.update { it.copy(customDays = days) }
    fun applyCustomDuration() {
        val days = _uiState.value.customDays
        if (days > 0) _uiState.update { it.copy(duration = "custom") }
    }
    fun setInterval(interval: Int)      = _uiState.update { it.copy(interval = interval) }
    fun setCustomInterval(interval: Int)= _uiState.update { it.copy(customInterval = interval) }
    fun applyCustomInterval() {
        val interval = _uiState.value.customInterval
        if (interval > 0) _uiState.update { it.copy(interval = interval) }
    }
    fun setPriority(priority: String)   = _uiState.update { it.copy(priority = priority) }
    fun setDynamicPriority(enabled: Boolean) =
        _uiState.update { it.copy(dynamicPriority = enabled) }
    fun setLearningMode(enabled: Boolean) =
        _uiState.update { it.copy(learningMode = enabled) }

    fun saveSettings() {
        val s = _uiState.value
        val settings = AgentSettings(
            interval        = s.interval,
            dynamicPriority = s.dynamicPriority,
            learningMode    = s.learningMode,
            offlineOnly     = true,
            externalSources = false
        )
        if (s.agentRunning) agentService.stop()
        agentService.start(settings)
    }

    /** Sofortiger Einmallauf (UI-Taste "Jetzt trainieren"). */
    fun runOnceNow() {
        agentService.runOnceNow()
    }
}

data class AgentUiState(
    val agentRunning: Boolean = false,
    val runtime: String = "0h 0m",
    val progress: Float = 0f,
    val duration: String = "unlimited",
    val customDays: Int = 0,
    val interval: Int = 30,
    val customInterval: Int = 30,
    val priority: String = "high",
    val dynamicPriority: Boolean = true,
    val learningMode: Boolean = true
)
