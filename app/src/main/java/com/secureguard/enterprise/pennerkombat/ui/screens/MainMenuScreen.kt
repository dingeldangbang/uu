package com.secureguard.enterprise.pennerkombat.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.secureguard.enterprise.pennerkombat.ui.components.PennerButton

@Composable
fun MainMenuScreen(
    onPlayArcade: () -> Unit,
    onPlayVersus: () -> Unit,
    onStory: () -> Unit,
    onTrophies: () -> Unit,
    onOptions: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF1A0000), Color.Black, Color(0xFF0A0A0A))
                )
            )
    ) {
        // Background pattern
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // Title
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "🩸 PENNER",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFFF1744),
                    letterSpacing = 6.sp
                )
                Text(
                    text = "KOMBAT",
                    fontSize = 52.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 8.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .background(Color(0xFFFF1744), RoundedCornerShape(2.dp))
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "BAHNHOF EDITION • 2026",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 2.sp
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "\"Hier unten gibt es keine Regeln. Nur Überlebende.\"",
                    color = Color.Gray,
                    fontSize = 13.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }

            // Menu buttons
            Column(
                modifier = Modifier.fillMaxWidth(0.85f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PennerButton(text = "🎮 ARCADE (vs KI)", onClick = onPlayArcade, modifier = Modifier.fillMaxWidth())
                PennerButton(text = "⚔️ VERSUS (1vs1)", onClick = onPlayVersus, modifier = Modifier.fillMaxWidth(), color = Color(0xFF6200EA))
                PennerButton(text = "📖 STORY MODE - 8 KAPITEL", onClick = onStory, modifier = Modifier.fillMaxWidth(), color = Color(0xFF00695C))
                PennerButton(text = "🏆 TROPHÄEN (56)", onClick = onTrophies, modifier = Modifier.fillMaxWidth(), color = Color(0xFF4E342E))
                PennerButton(text = "⚙️ OPTIONEN", onClick = onOptions, modifier = Modifier.fillMaxWidth(), color = Color(0xFF37474F))
            }

            // Footer
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("9 KÄMPFER", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text("•", color = Color.Gray, fontSize = 10.sp)
                    Text("360° ARENA", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text("•", color = Color.Gray, fontSize = 10.sp)
                    Text("FATALITIES", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "BUILD v1.0.0 • UNITY 2023.3 LTS / URP PORT • ANDROID 10+ • ARM64 • VULKAN",
                    color = Color.Gray.copy(alpha = 0.5f),
                    fontSize = 9.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // Blood splats decoration
        Box(modifier = Modifier.align(Alignment.TopEnd).padding(40.dp).size(80.dp).background(Color(0xFFFF1744).copy(alpha = 0.15f), RoundedCornerShape(50)))
        Box(modifier = Modifier.align(Alignment.BottomStart).padding(20.dp).size(120.dp).background(Color(0xFFFF1744).copy(alpha = 0.1f), RoundedCornerShape(50)))
    }
}
