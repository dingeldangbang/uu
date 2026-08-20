package com.secureguard.enterprise.presentation.ui.addasset

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.secureguard.enterprise.data.model.AssetCategory
import com.secureguard.enterprise.util.rememberToast
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAssetScreen(
    scannedPayload: String? = null,
    viewModel: AddAssetViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val nav = rememberNavController()
    val toast = rememberToast()
    val scope = rememberCoroutineScope()

    LaunchedEffect(scannedPayload) {
        if (!scannedPayload.isNullOrBlank()) {
            viewModel.setScannedPayload(ScannedPayload.of(scannedPayload))
        } else {
            viewModel.setScannedPayload(null)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("➕ Asset hinzufügen") },
                navigationIcon = {
                    IconButton(onClick = { nav.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Zurück")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        scope.launch {
                            val res = viewModel.save()
                            res.fold(
                                onSuccess = {
                                    toast("Asset gespeichert: ${it.name}", true)
                                    nav.popBackStack()
                                },
                                onFailure = { toast("Fehler: ${it.message}", true) }
                            )
                        }
                    }) {
                        Icon(Icons.Default.Save, contentDescription = "Speichern")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.message.isNotBlank()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "ℹ️ ${state.message}",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            OutlinedTextField(
                value = state.id,
                onValueChange = { },
                label = { Text("ID") },
                modifier = Modifier.fillMaxWidth(),
                enabled = false,
                supportingText = { Text("Aus QR-Code oder manuell ergänzen") }
            )

            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::setName,
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = state.mac,
                onValueChange = viewModel::setMac,
                label = { Text("MAC-Adresse") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = state.location,
                onValueChange = { viewModel.setLocation(it) },
                label = { Text("Ort/Location") },
                modifier = Modifier.fillMaxWidth()
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.latitude?.toString().orEmpty(),
                    onValueChange = { viewModel.setLat(it) },
                    label = { Text("Lat") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = state.longitude?.toString().orEmpty(),
                    onValueChange = { viewModel.setLon(it) },
                    label = { Text("Lon") },
                    modifier = Modifier.weight(1f)
                )
            }

            Text("Kategorie", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                AssetCategory.values().forEach { c ->
                    FilterChip(
                        selected = state.category == c,
                        onClick = { viewModel.setCategory(c) },
                        label = { Text(c.name) }
                    )
                }
            }

            Text("Akku: ${state.batteryPercent}%", style = MaterialTheme.typography.titleSmall)
            Slider(
                value = state.batteryPercent.toFloat(),
                onValueChange = { viewModel.setBattery(it.toInt()) },
                valueRange = 0f..100f,
                steps = 19
            )

            Button(
                onClick = {
                    scope.launch {
                        val res = viewModel.save()
                        res.fold(
                            onSuccess = {
                                toast("Asset gespeichert: ${it.name}", true)
                                nav.popBackStack()
                            },
                            onFailure = { toast("Fehler: ${it.message}", true) }
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = state.name.isNotBlank() && state.id.isNotBlank()
            ) {
                Text("Asset speichern", color = Color.White)
            }
        }
    }
}
