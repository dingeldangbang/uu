package com.secureguard.enterprise.pennerkombat.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.secureguard.enterprise.pennerkombat.model.Fighter
import com.secureguard.enterprise.pennerkombat.model.FighterDatabase
import com.secureguard.enterprise.pennerkombat.ui.components.PennerButton

@Composable
fun CharacterSelectScreen(
    mode: String, // "arcade" or "versus"
    onFight: (Fighter, Fighter) -> Unit,
    onBack: () -> Unit
) {
    var p1Selected by remember { mutableStateOf<Fighter?>(FighterDatabase.fighters[0]) }
    var p2Selected by remember { mutableStateOf<Fighter?>(FighterDatabase.fighters[1]) }
    var selectingP1 by remember { mutableStateOf(true) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A))
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onBack) { Text("← ZURÜCK", color = Color.White) }
                Text(
                    text = if (mode == "versus") "VERSUS - WÄHLE KÄMPFER" else "ARCADE - WÄHLE DEINEN KÄMPFER",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.width(80.dp))
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Selected preview
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                FighterPreviewCard(fighter = p1Selected, label = "P1", isSelecting = selectingP1, onClick = { selectingP1 = true })
                Text("VS", color = Color(0xFFFF1744), fontWeight = FontWeight.Black, fontSize = 28.sp, modifier = Modifier.align(Alignment.CenterVertically))
                FighterPreviewCard(fighter = p2Selected, label = if (mode == "versus") "P2" else "KI GEGNER", isSelecting = !selectingP1, onClick = { selectingP1 = false })
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Fighter grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(FighterDatabase.fighters) { fighter ->
                    FighterSlot(
                        fighter = fighter,
                        isSelectedP1 = p1Selected?.id == fighter.id,
                        isSelectedP2 = p2Selected?.id == fighter.id,
                        onClick = {
                            if (selectingP1) {
                                p1Selected = fighter
                                if (mode == "arcade") selectingP1 = false // auto switch to P2 in arcade
                            } else {
                                p2Selected = fighter
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Details of currently selecting
            val currentFighter = if (selectingP1) p1Selected else p2Selected
            currentFighter?.let { f ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row {
                            Text(f.emoji, fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(f.displayName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("- ${f.nickName}", color = f.colorPrimary, fontSize = 12.sp)
                            Spacer(modifier = Modifier.weight(1f))
                            Text(f.archetype.name, color = Color.Gray, fontSize = 10.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(f.description, color = Color.Gray, fontSize = 11.sp, maxLines = 2)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            StatMini("HP", f.maxHP.toInt().toString())
                            StatMini("SPD", f.moveSpeed.toString())
                            StatMini("DMG", "${f.lightDamage.toInt()}/${f.heavyDamage.toInt()}")
                            StatMini("DEF", "${(f.defense*100).toInt()}%")
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Badge(text = "${f.special1.icon} ${f.special1.name}", color = f.colorPrimary)
                            Badge(text = "${f.special2.icon} ${f.special2.name}", color = f.colorSecondary)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            PennerButton(
                text = "🔥 KAMPF STARTEN - ${p1Selected?.displayName} VS ${p2Selected?.displayName}",
                onClick = {
                    if (p1Selected != null && p2Selected != null) {
                        onFight(p1Selected!!, p2Selected!!)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = p1Selected != null && p2Selected != null
            )
        }
    }
}

@Composable
fun FighterPreviewCard(fighter: Fighter?, label: String, isSelecting: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(140.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelecting) Color(0xFFFF1744).copy(alpha = 0.2f) else Color(0xFF1A1A1A))
            .border(
                width = if (isSelecting) 2.dp else 1.dp,
                color = if (isSelecting) Color(0xFFFF1744) else Color.Gray.copy(alpha = 0.3f),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Text(label, color = if (isSelecting) Color(0xFFFF1744) else Color.Gray, fontWeight = FontWeight.Black, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(fighter?.emoji ?: "❓", fontSize = 48.sp)
        Text(fighter?.displayName ?: "WÄHLEN", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text(fighter?.nickName ?: "", color = Color.Gray, fontSize = 10.sp)
    }
}

@Composable
fun FighterSlot(fighter: Fighter, isSelectedP1: Boolean, isSelectedP2: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .aspectRatio(0.8f)
            .clip(RoundedCornerShape(6.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(fighter.colorPrimary.copy(alpha = 0.8f), fighter.colorSecondary.copy(alpha = 0.6f))
                )
            )
            .border(
                width = when {
                    isSelectedP1 && isSelectedP2 -> 3.dp
                    isSelectedP1 || isSelectedP2 -> 2.dp
                    else -> 1.dp
                },
                color = when {
                    isSelectedP1 && isSelectedP2 -> Color.Yellow
                    isSelectedP1 -> Color(0xFF00E5FF)
                    isSelectedP2 -> Color(0xFFFF1744)
                    else -> Color.White.copy(alpha = 0.2f)
                },
                shape = RoundedCornerShape(6.dp)
            )
            .clickable { onClick() }
            .padding(6.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(fighter.tier.toString(), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(2.dp)).padding(2.dp))
                if (isSelectedP1) Text("P1", color = Color.Cyan, fontSize = 10.sp, fontWeight = FontWeight.Black)
                if (isSelectedP2) Text("P2", color = Color.Red, fontSize = 10.sp, fontWeight = FontWeight.Black)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(fighter.emoji, fontSize = 32.sp)
                Text(fighter.displayName, color = Color.White, fontWeight = FontWeight.Black, fontSize = 11.sp, maxLines = 1)
            }
            Text(fighter.archetype.name.take(4), color = Color.White.copy(alpha = 0.7f), fontSize = 8.sp)
        }
    }
}

@Composable
fun StatMini(label: String, value: String) {
    Column {
        Text(label, color = Color.Gray, fontSize = 8.sp)
        Text(value, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun Badge(text: String, color: Color) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
            .border(1.dp, color, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(text, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}
