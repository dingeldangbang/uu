package com.secureguard.enterprise.presentation.ui.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import androidx.preference.PreferenceManager
import com.secureguard.enterprise.R
import com.secureguard.enterprise.data.model.Asset
import com.secureguard.enterprise.data.model.AssetStatus
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: MapViewModel = hiltViewModel()
) {
    val assets by viewModel.assets.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val lastUpdate by viewModel.lastUpdate.collectAsState()
    val nav = rememberNavController()
    val ctx = LocalContext.current

    var mapView by remember { mutableStateOf<MapView?>(null) }

    DisposableEffect(Unit) {
        Configuration.getInstance().load(
            ctx,
            PreferenceManager.getDefaultSharedPreferences(ctx)
        )
        Configuration.getInstance().userAgentValue = "SecureGuard"
        onDispose {
            mapView?.onDetach()
            mapView = null
        }
    }

    val onZoomIn: () -> Unit = { mapView?.controller?.zoomIn() }
    val onZoomOut: () -> Unit = { mapView?.controller?.zoomOut() }
    val onCenter: () -> Unit = {
        mapView?.let { mv ->
            val valid = assets.filter { it.latitude != null && it.longitude != null }
            if (valid.isNotEmpty()) {
                val avgLat = valid.mapNotNull { it.latitude }.average()
                val avgLon = valid.mapNotNull { it.longitude }.average()
                mv.controller.animateTo(GeoPoint(avgLat, avgLon))
            }
        }
    }
    val onRefresh: () -> Unit = { viewModel.refresh() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🗺️ Karte") },
                navigationIcon = {
                    IconButton(onClick = { nav.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Zurück")
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Aktualisieren")
                    }
                    IconButton(onClick = onZoomIn) {
                        Icon(Icons.Default.ZoomIn, contentDescription = "Vergrößern")
                    }
                    IconButton(onClick = onZoomOut) {
                        Icon(Icons.Default.ZoomOut, contentDescription = "Verkleinern")
                    }
                    IconButton(onClick = onCenter) {
                        Icon(Icons.Default.CenterFocusStrong, contentDescription = "Zentrieren")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            AndroidView(
                factory = { c ->
                    MapView(c).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setBuiltInZoomControls(false)
                        setMultiTouchControls(true)
                        controller.setZoom(15.0)
                        controller.setCenter(GeoPoint(52.52, 13.40))
                        mapView = this
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { mv ->
                    mv.overlays.clear()
                    assets.forEach { asset ->
                        if (asset.latitude != null && asset.longitude != null) {
                            val marker = Marker(mv).apply {
                                position = GeoPoint(asset.latitude, asset.longitude)
                                title = asset.shortName
                                subDescription = "📶 ${asset.rssi} dBm | 🔋 ${asset.batteryPercent}%"
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                icon = ctx.resources.getDrawable(
                                    when (asset.status) {
                                        AssetStatus.ONLINE      -> R.drawable.marker_green
                                        AssetStatus.OFFLINE     -> R.drawable.marker_red
                                        AssetStatus.MAINTENANCE -> R.drawable.marker_yellow
                                        AssetStatus.ALERT       -> R.drawable.marker_red
                                        else                    -> R.drawable.marker_gray
                                    },
                                    null
                                )
                            }
                            mv.overlays.add(marker)
                        }
                    }
                    mv.invalidate()
                }
            )

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize().padding(16.dp),
                    contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            androidx.compose.material3.Card(
                modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Legende:", style = MaterialTheme.typography.bodySmall)
                    Row {
                        Box(modifier = Modifier.size(12.dp).background(Color(0xFF2E7D32), CircleShape))
                        Spacer(Modifier.width(8.dp))
                        Text("Online", style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.width(16.dp))
                        Box(modifier = Modifier.size(12.dp).background(Color(0xFFE53935), CircleShape))
                        Spacer(Modifier.width(8.dp))
                        Text("Offline", style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.width(16.dp))
                        Box(modifier = Modifier.size(12.dp).background(Color(0xFFFFB300), CircleShape))
                        Spacer(Modifier.width(8.dp))
                        Text("Wartung", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            val visibleCount = assets.count { it.latitude != null && it.longitude != null }
            Text(
                text = "⏱ Letzte Aktualisierung: $lastUpdate | 📍 $visibleCount Assets sichtbar",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp)
            )
        }
    }
}
