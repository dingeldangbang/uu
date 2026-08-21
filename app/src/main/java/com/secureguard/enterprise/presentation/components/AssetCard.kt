package com.secureguard.enterprise.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.secureguard.enterprise.data.model.Asset
import com.secureguard.enterprise.data.model.AssetStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AssetCard(
    asset: Asset,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusDot(asset.status)
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = asset.name.ifBlank { asset.shortName },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(2.dp))
                val lastSeenText = asset.lastSeen?.let {
                    "last " + SimpleDateFormat("HH:mm", Locale.GERMANY).format(Date(it))
                } ?: "nie gesehen"
                val locationPart = asset.location?.let { " • 📍 $it" } ?: ""
                Text(
                    text = "${asset.category.name}$locationPart • $lastSeenText",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            BatteryIcon(asset.batteryPercent)
        }
    }
}

@Composable
private fun StatusDot(status: AssetStatus) {
    val color = when (status) {
        AssetStatus.ONLINE      -> Color(0xFF2E7D32)
        AssetStatus.OFFLINE     -> Color(0xFF616161)
        AssetStatus.ALERT       -> Color(0xFFE53935)
        AssetStatus.MAINTENANCE -> Color(0xFFFFB300)
        AssetStatus.SEARCHING   -> Color(0xFF1565C0)
        AssetStatus.UNKNOWN     -> Color(0xFF9E9E9E)
    }
    Box(
        modifier = Modifier
            .size(12.dp)
            .background(color, CircleShape)
    )
}

@Composable
private fun BatteryIcon(percent: Int) {
    val (icon, tint) = when {
        percent < 25 -> Icons.Default.BatteryAlert to Color(0xFFE53935)
        percent < 75 -> Icons.Default.BatteryStd   to Color(0xFFFFB300)
        else         -> Icons.Default.BatteryFull  to Color(0xFF2E7D32)
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = "$percent% battery",
             tint = tint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.size(4.dp))
        Text("$percent%", style = MaterialTheme.typography.labelMedium)
    }
}
