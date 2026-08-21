package com.secureguard.enterprise.presentation.ui.assets

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.secureguard.enterprise.data.model.AssetStatus
import com.secureguard.enterprise.presentation.components.AssetCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetListScreen(
    viewModel: AssetListViewModel = hiltViewModel()
) {
    val filteredAssets by viewModel.filteredAssets.collectAsState()
    val search by viewModel.searchQuery.collectAsState()
    val selectedStatus by viewModel.selectedStatus.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val nav = rememberNavController()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("📦 Assets (${uiState.total})") },
                navigationIcon = {
                    IconButton(onClick = { nav.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Zurück")
                    }
                },
                actions = {
                    IconButton(onClick = { nav.navigate("scan") }) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = "QR-Scan")
                    }
                    IconButton(onClick = { nav.navigate("add_asset") }) {
                        Icon(Icons.Default.Add, contentDescription = "Hinzufügen")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)
        ) {
            OutlinedTextField(
                value = search,
                onValueChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("🔍 Asset suchen...") },
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (search.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearFilters() }) {
                            Icon(Icons.Default.Clear, contentDescription = "Löschen")
                        }
                    }
                }
            )

            ScrollableTabRow(
                selectedTabIndex = when (selectedStatus) {
                    null                       -> 0
                    AssetStatus.ONLINE         -> 1
                    AssetStatus.OFFLINE        -> 2
                    AssetStatus.MAINTENANCE    -> 3
                    else                       -> 0
                },
                modifier = Modifier.padding(vertical = 8.dp),
                edgePadding = 0.dp
            ) {
                val labels = listOf("Alle", "Online", "Offline", "Wartung")
                labels.forEachIndexed { i, label ->
                    val status = when (i) {
                        1 -> AssetStatus.ONLINE
                        2 -> AssetStatus.OFFLINE
                        3 -> AssetStatus.MAINTENANCE
                        else -> null
                    }
                    Tab(
                        selected = selectedStatus == status,
                        onClick = { viewModel.setStatusFilter(status) },
                        text = { Text(label) }
                    )
                }
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                if (filteredAssets.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxSize().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    "Keine Assets gefunden",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "Füge ein Asset hinzu oder ändere die Filter",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(filteredAssets, key = { it.id }) { asset ->
                        AssetCard(
                            asset = asset,
                            onClick = { nav.navigate("asset_detail/${asset.id}") }
                        )
                    }
                }
            }

            Divider()
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Text("📊 ${uiState.total} Assets",  style = MaterialTheme.typography.bodySmall)
                Text("🟢 ${uiState.online} Online",  style = MaterialTheme.typography.bodySmall)
                Text("🔴 ${uiState.offline} Offline",style = MaterialTheme.typography.bodySmall)
                Text("🟡 ${uiState.maintenance} Wartung", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun rememberNavControllerWrapper() = rememberNavController()
