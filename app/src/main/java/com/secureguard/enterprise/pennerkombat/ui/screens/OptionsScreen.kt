package com.secureguard.enterprise.pennerkombat.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun OptionsScreen(
    onBack: () -> Unit,
    difficulty: Int,
    onDifficultyChange: (Int) -> Unit
) {
    var soundEnabled by remember { mutableStateOf(true) }
    var vibrationEnabled by remember { mutableStateOf(true) }
    var bloodEnabled by remember { mutableStateOf(true) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A))
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onBack) { Text("← MENÜ", color = Color.White) }
                Text("⚙️ OPTIONEN", color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
                Spacer(modifier = Modifier.width(60.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("SCHWIERIGKEIT", color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        listOf("LEICHT", "NORMAL", "HART", "BRUTAL", "IRRE", "PENNER").forEachIndexed { idx, label ->
                            val selected = difficulty == idx
                            Button(
                                onClick = { onDifficultyChange(idx) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selected) Color(0xFFFF1744) else Color(0xFF2A2A2A),
                                    contentColor = Color.White
                                ),
                                modifier = Modifier.weight(1f).padding(horizontal = 2.dp),
                                contentPadding = PaddingValues(4.dp),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(label, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Text(
                        text = when (difficulty) {
                            0 -> "Für Anfänger. KI reagiert langsam."
                            1 -> "Ausgeglichen. Für Gelegenheits-Penner."
                            2 -> "Standard. So ist die Straße."
                            3 -> "KI blockt 70%, kontert hart."
                            4 -> "KI liest deine Inputs. Viel Glück."
                            5 -> "PENNER MODUS - Du wirst sterben. Oft."
                            else -> ""
                        },
                        color = Color.Gray,
                        fontSize = 11.sp
                    )

                    Divider(color = Color.Gray.copy(alpha = 0.2f))

                    OptionToggle("🔊 SOUND", "SFX + Musik", soundEnabled) { soundEnabled = it }
                    OptionToggle("📳 VIBRATION", "Bei Hits vibrieren", vibrationEnabled) { vibrationEnabled = it }
                    OptionToggle("🩸 BLUT", "Bluteffekte & Fatalities", bloodEnabled) { bloodEnabled = it }

                    Divider(color = Color.Gray.copy(alpha = 0.2f))

                    Text("BUILD INFO", color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        InfoRow("Engine", "Custom Kotlin Compose Engine (Unity Port)")
                        InfoRow("Version", "1.0.0 - Bahnhof Edition")
                        InfoRow("Graphics", "URP Style - 60 FPS - Vulkan")
                        InfoRow("Audio", "31 Tracks - Beat-Sync")
                        InfoRow("Physik", "Ragdoll + Knockback + 360° Movement")
                        InfoRow("Netcode", "QR + WebSocket (Versus)")
                        InfoRow("API Level", "Android 10+ (API 29+) ARM64")
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                "© 2026 Penner Kombat - Alle Rechte vorbehalten. Keine echten Penner wurden verletzt. Vielleicht.",
                color = Color.Gray.copy(alpha = 0.5f),
                fontSize = 9.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun OptionToggle(title: String, desc: String, enabled: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Text(desc, color = Color.Gray, fontSize = 10.sp)
        }
        Switch(
            checked = enabled,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFFF1744), checkedTrackColor = Color(0xFFFF1744).copy(alpha = 0.5f))
        )
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color.Gray, fontSize = 10.sp)
        Text(value, color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
    }
}
