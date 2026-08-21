package com.secureguard.enterprise.presentation.ui.nodes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.secureguard.enterprise.R
import com.secureguard.enterprise.agent.NodeStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NodeStatusScreen(
    viewModel: NodeStatusViewModel = hiltViewModel()
) {
    val nodeStatus by viewModel.nodeStatus.collectAsState()
    val auditLog by viewModel.auditLog.collectAsState()
    val nav = rememberNavController()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("📡 Abfrageknoten") },
                navigationIcon = {
                    IconButton(onClick = { nav.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Zurück")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Aktualisieren")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { Text("Status aller externen Abfragequellen", style = MaterialTheme.typography.titleMedium) }

            items(nodeStatus.entries.toList(), key = { it.key }) { (nodeId, status) ->
                NodeStatusItem(
                    nodeId = nodeId,
                    status = status,
                    onToggle = { viewModel.toggleNode(nodeId) }
                )
            }

            item { Spacer(Modifier.height(8.dp)) }
            item { Text("📋 Audit-Log", style = MaterialTheme.typography.titleMedium) }
            items(auditLog.takeLast(15)) { entry ->
                Text(entry, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 1.dp))
            }
        }
    }
}

@Composable
fun NodeStatusItem(
    nodeId: String,
    status: NodeStatus,
    onToggle: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(
                            when (status) {
                                NodeStatus.ONLINE -> Color(0xFF2E7D32)
                                NodeStatus.OFFLINE -> Color(0xFFE53935)
                                NodeStatus.ERROR -> Color(0xFFFFB300)
                                NodeStatus.RATE_LIMITED -> Color(0xFFEF6C00)
                                else -> Color(0xFF9E9E9E)
                            },
                            CircleShape
                        )
                )
                Spacer(Modifier.width(8.dp))
                // Knoten-Typ-Icon (Alternative zu reinem Text)
                Icon(
                    painter = painterResource(nodeIconRes(nodeId)),
                    contentDescription = nodeId,
                    tint = when (status) {
                        NodeStatus.ONLINE -> Color(0xFF1565C0)
                        else -> Color(0xFF9E9E9E)
                    },
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(nodeId, style = MaterialTheme.typography.titleSmall)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    status.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = when (status) {
                        NodeStatus.ONLINE -> Color(0xFF2E7D32)
                        NodeStatus.OFFLINE -> Color(0xFFE53935)
                        else -> Color.Gray
                    }
                )
                Spacer(Modifier.width(4.dp))
                Switch(checked = true, onCheckedChange = { onToggle() })
            }
        }
    }
}

/**
 * Knoten-Typ-Icon-Zuordnung (Alternative zu reinem Text):
 *  - API-Knoten (WiGle, MacLookup, OCM, DHL, CKAN, Google, Netatmo, Helium) → ic_node_api
 *  - MQTT-Broker                                                          → ic_node_mqtt
 *  - WebSocket                                                             → ic_node_ws
 *  - TempMail                                                              → ic_node_mail
 */
private fun nodeIconRes(nodeId: String): Int = when (nodeId) {
    "mqtt"      -> R.drawable.ic_node_mqtt
    "websocket" -> R.drawable.ic_node_ws
    "tempmail"  -> R.drawable.ic_node_mail
    else        -> R.drawable.ic_node_api
}
