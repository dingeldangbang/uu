package com.secureguard.enterprise.pennerkombat.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.secureguard.enterprise.pennerkombat.model.FighterDatabase

@Composable
fun TrophyScreen(onBack: () -> Unit) {
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
                Text("🏆 TROPHÄEN - 56 STÜCK", color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp)
                Spacer(modifier = Modifier.width(60.dp))
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                TrophyStat("BRONZE", "12", Color(0xFFCD7F32))
                TrophyStat("SILBER", "18", Color(0xFFC0C0C0))
                TrophyStat("GOLD", "20", Color(0xFFFFD700))
                TrophyStat("PLATIN", "6", Color(0xFFE5E4E2))
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(FighterDatabase.trophies) { trophy ->
                    Card(
                        modifier = Modifier.fillMaxWidth().border(1.dp, when (trophy.rarity) {
                            "Bronze" -> Color(0xFFCD7F32)
                            "Silber" -> Color(0xFFC0C0C0)
                            "Gold" -> Color(0xFFFFD700)
                            else -> Color(0xFFE5E4E2)
                        }.copy(alpha = 0.5f), RoundedCornerShape(6.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(trophy.icon, fontSize = 28.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(trophy.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text(trophy.description, color = Color.Gray, fontSize = 10.sp, maxLines = 2)
                                Spacer(modifier = Modifier.height(2.dp))
                                Box(
                                    modifier = Modifier
                                        .background(
                                            when (trophy.rarity) {
                                                "Bronze" -> Color(0xFFCD7F32)
                                                "Silber" -> Color(0xFFC0C0C0)
                                                "Gold" -> Color(0xFFFFD700)
                                                else -> Color(0xFFE5E4E2)
                                            }.copy(alpha = 0.2f), RoundedCornerShape(2.dp)
                                        )
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text(trophy.rarity.uppercase(), color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
                // Fill up to 56 with dummy
                items(44) { idx ->
                    Card(
                        modifier = Modifier.fillMaxWidth().border(1.dp, Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(6.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF111111)),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("🔒", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Geheim #${idx + 13}", color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                Text("???", color = Color.Gray.copy(alpha = 0.5f), fontSize = 9.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TrophyStat(label: String, count: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(count, color = color, fontWeight = FontWeight.Black, fontSize = 20.sp)
        Text(label, color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}
