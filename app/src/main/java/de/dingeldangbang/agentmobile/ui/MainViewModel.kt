package de.dingeldangbang.agentmobile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.dingeldangbang.agentmobile.AgentApplication
import de.dingeldangbang.agentmobile.agent.AgentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


data class MainUiState(
    val prompt: String = "",
    val result: String = "",
    val source: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
)

class MainViewModel(
    val application: AgentApplication,
    private val repository: AgentRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(MainUiState())
    val state: StateFlow<MainUiState> = _state.asStateFlow()

    fun updatePrompt(value: String) = _state.update { it.copy(prompt = value, error = null) }

    fun submit() {
        val prompt = _state.value.prompt.trim()
        if (prompt.isBlank() || _state.value.isLoading) return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, result = "", source = "", error = null) }
            runCatching { repository.handle(prompt) }
                .onSuccess { result ->
                    _state.update { it.copy(result = result.text, source = result.source, isLoading = false) }
                }
                .onFailure { error ->
                    _state.update { it.copy(error = error.message ?: "Unbekannter Fehler", isLoading = false) }
                }
        }
    }
}
