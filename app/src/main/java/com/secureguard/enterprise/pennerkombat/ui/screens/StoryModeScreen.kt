package com.secureguard.enterprise.pennerkombat.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.secureguard.enterprise.pennerkombat.model.Fighter
import com.secureguard.enterprise.pennerkombat.model.FighterDatabase
import com.secureguard.enterprise.pennerkombat.model.StoryChapter
import com.secureguard.enterprise.pennerkombat.ui.components.PennerButton

@Composable
fun StoryModeScreen(
    onStartChapter: (StoryChapter, Fighter, Fighter) -> Unit,
    onBack: () -> Unit
) {
    var selectedChapter by remember { mutableStateOf<StoryChapter?>(null) }
    var playerFighter by remember { mutableStateOf(FighterDatabase.fighters[0]) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF1A1A1A), Color.Black)
                )
            )
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onBack) { Text("← MENÜ", color = Color.White) }
                Text("📖 STORY MODUS - 8 KAPITEL", color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp)
                Spacer(modifier = Modifier.width(60.dp))
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                "Vom Niemand zum Straßenkönig. 4 Enden. 8 Bosse. Keine Gnade.",
                color = Color.Gray,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(FighterDatabase.storyChapters) { chapter ->
                    StoryChapterCard(
                        chapter = chapter,
                        isSelected = selectedChapter?.id == chapter.id,
                        onSelect = { selectedChapter = chapter }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            selectedChapter?.let { ch ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (ch.isBoss) "👹 BOSS" else "📄 KAPITEL ${ch.id}",
                                color = if (ch.isBoss) Color.Red else Color.Gray,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                modifier = Modifier.background(Color.Black, RoundedCornerShape(2.dp)).padding(4.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(ch.title, color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(ch.description, color = Color.Gray, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("DIALOG:", color = Color.Yellow, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        ch.dialog.forEach { line ->
                            Text("„$line\"", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp, modifier = Modifier.padding(start = 8.dp, top = 2.dp))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("BELOHNUNG: ${ch.reward}", color = Color(0xFF00E676), fontSize = 11.sp, fontWeight = FontWeight.Bold)

                        Spacer(modifier = Modifier.height(12.dp))

                        // Player fighter picker small
                        Text("DEIN KÄMPFER:", color = Color.Gray, fontSize = 10.sp)
                        Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FighterDatabase.fighters.take(5).forEach { f ->
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(if (playerFighter.id == f.id) Color(0xFFFF1744) else Color(0xFF2A2A2A), RoundedCornerShape(4.dp))
                                        .border(1.dp, if (playerFighter.id == f.id) Color.White else Color.Transparent, RoundedCornerShape(4.dp))
                                        .padding(2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    TextButton(onClick = { playerFighter = f }, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(0.dp)) {
                                        Text(f.emoji, fontSize = 18.sp)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        val opponent = FighterDatabase.getById(ch.opponentId)
                        PennerButton(
                            text = "⚔️ KÄMPFE GEGEN ${opponent.displayName.uppercase()}",
                            onClick = { onStartChapter(ch, playerFighter, opponent) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StoryChapterCard(chapter: StoryChapter, isSelected: Boolean, onSelect: () -> Unit) {
    val opponent = FighterDatabase.getById(chapter.opponentId)
    Card(
        onClick = onSelect,
        modifier = Modifier.fillMaxWidth().border(if (isSelected) 2.dp else 1.dp, if (isSelected) Color(0xFFFF1744) else Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(6.dp)),
        colors = CardDefaults.cardColors(containerColor = if (isSelected) Color(0xFF2A0A0A) else Color(0xFF1A1A1A)),
        shape = RoundedCornerShape(6.dp)
    ) {
        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(opponent.colorPrimary.copy(alpha = 0.3f), RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(opponent.emoji, fontSize = 24.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("KAPITEL ${chapter.id}", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    if (chapter.isBoss) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("BOSS", color = Color.Red, fontSize = 9.sp, fontWeight = FontWeight.Black, modifier = Modifier.background(Color.Red.copy(alpha = 0.2f), RoundedCornerShape(2.dp)).padding(horizontal = 4.dp, vertical = 1.dp))
                    }
                }
                Text(chapter.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("Gegner: ${opponent.displayName} - ${opponent.nickName}", color = Color.Gray, fontSize = 10.sp)
            }
            if (chapter.completed) {
                Text("✅", fontSize = 18.sp)
            } else {
                Text("🔒", fontSize = 14.sp, color = Color.Gray)
            }
        }
    }
}
