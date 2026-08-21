package com.secureguard.enterprise.presentation.ui.nodes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.secureguard.enterprise.agent.ApiNodeManager
import com.secureguard.enterprise.agent.NodeStatus
import com.secureguard.enterprise.services.AuditService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NodeStatusViewModel @Inject constructor(
    private val manager: ApiNodeManager,
    private val auditService: AuditService
) : ViewModel() {

    val nodeStatus: StateFlow<Map<String, NodeStatus>> = manager.nodeStatus
    val auditLog: StateFlow<List<String>> = auditService.log

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            manager.refreshHealth()
            _isLoading.value = false
        }
    }

    fun toggleNode(nodeId: String) {
        manager.toggleNode(nodeId)
    }
}
