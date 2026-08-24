package com.secureguard.enterprise.pennerkombat.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.secureguard.enterprise.pennerkombat.engine.GameManager
import com.secureguard.enterprise.pennerkombat.model.*
import com.secureguard.enterprise.pennerkombat.ui.components.HealthBar
import com.secureguard.enterprise.pennerkombat.ui.components.PowerBar
import kotlinx.coroutines.delay
import kotlin.math.*

@Composable
fun ArenaScreen(
    p1Fighter: Fighter,
    p2Fighter: Fighter,
    gameMode: GameMode,
    onExit: () -> Unit,
    difficulty: Int = 2
) {
    val gameManager = remember { GameManager() }
    val p1State by gameManager.player1.collectAsState()
    val p2State by gameManager.player2.collectAsState()
    val matchState by gameManager.matchState.collectAsState()
    val arenaState by gameManager.arenaState.collectAsState()
    val effects by gameManager.effects.collectAsState()

    var isP2AI by remember { mutableStateOf(gameMode != GameMode.VERSUS) }
    var joystickOffset by remember { mutableStateOf(Offset.Zero) }
    var showPause by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        gameManager.setP2AI(isP2AI)
        gameManager.difficulty = difficulty
        gameManager.initMatch(p1Fighter, p2Fighter, gameMode)
    }

    // Game loop
    LaunchedEffect(matchState.isRoundActive) {
        var lastTime = System.currentTimeMillis()
        while (true) {
            val now = System.currentTimeMillis()
            val delta = (now - lastTime) / 1000f
            lastTime = now
            if (!showPause && matchState.isRoundActive) {
                gameManager.update(delta.coerceAtMost(0.05f), now)
            }
            delay(16) // ~60fps
        }
    }

    // Auto next round
    LaunchedEffect(matchState.isRoundActive, matchState.p1Wins, matchState.p2Wins) {
        if (!matchState.isRoundActive && matchState.result != MatchResult.ONGOING) {
            val needed = matchState.bestOf / 2 + 1
            if (matchState.p1Wins < needed && matchState.p2Wins < needed) {
                delay(2000)
                gameManager.startNextRound()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A))
    ) {
        // Arena background canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Ground
            drawRect(
                color = Color(0xFF1A1A1A),
                topLeft = Offset(0f, size.height * 0.65f),
                size = Size(size.width, size.height * 0.35f)
            )
            // Grid lines
            for (i in 0..10) {
                val x = size.width * i / 10f
                drawLine(
                    color = Color.White.copy(alpha = 0.05f),
                    start = Offset(x, size.height * 0.65f),
                    end = Offset(x, size.height),
                    strokeWidth = 1f
                )
            }
            // Neon signs
            drawRect(
                color = Color(0xFF6200EA).copy(alpha = 0.15f),
                topLeft = Offset(size.width * 0.1f, size.height * 0.1f),
                size = Size(80f, 20f)
            )
            drawRect(
                color = Color(0xFFFF1744).copy(alpha = 0.15f),
                topLeft = Offset(size.width * 0.7f, size.height * 0.15f),
                size = Size(100f, 25f)
            )
            // Beer crates as boxes
            drawRect(
                color = Color(0xFF8B4513).copy(alpha = 0.6f),
                topLeft = Offset(size.width * 0.15f, size.height * 0.6f),
                size = Size(40f, 30f)
            )
            drawRect(
                color = Color(0xFF8B4513).copy(alpha = 0.6f),
                topLeft = Offset(size.width * 0.75f, size.height * 0.6f),
                size = Size(40f, 30f)
            )
        }

        // Fighters canvas
        if (p1State != null && p2State != null) {
            FightersCanvas(
                p1 = p1State!!,
                p2 = p2State!!,
                effects = effects,
                modifier = Modifier.fillMaxSize()
            )
        }

        // HUD Top
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            // Round and timer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // P1 HP
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(p1State?.fighter?.emoji ?: "", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(p1Fighter.displayName, color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp, maxLines = 1)
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    HealthBar(
                        current = p1State?.currentHP ?: 0f,
                        max = p1Fighter.maxHP,
                        color = Color(0xFF00E5FF),
                        modifier = Modifier.fillMaxWidth(),
                        isLeft = true
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    PowerBar(power = p1State?.powerMeter ?: 0f, modifier = Modifier.fillMaxWidth(0.7f))
                    if ((p1State?.comboCount ?: 0) > 1) {
                        Text(
                            "${p1State?.comboCount} HITS! COMBO!",
                            color = Color.Yellow,
                            fontWeight = FontWeight.Black,
                            fontSize = 10.sp
                        )
                    }
                }

                // Center Timer
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 8.dp)) {
                    Text(
                        text = "${matchState.timer.toInt()}",
                        color = if (matchState.timer < 10) Color.Red else Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 28.sp
                    )
                    Row {
                        repeat(matchState.bestOf) { i ->
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .padding(2.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            i < matchState.p1Wins -> Color(0xFF00E5FF)
                                            i < matchState.p1Wins + matchState.p2Wins && i >= matchState.p1Wins -> Color(0xFFFF1744)
                                            else -> Color.Gray.copy(alpha = 0.3f)
                                        }
                                    )
                            )
                        }
                    }
                    Text("R ${matchState.round}", color = Color.Gray, fontSize = 10.sp)
                }

                // P2 HP
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(p2Fighter.displayName, color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp, maxLines = 1)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(p2State?.fighter?.emoji ?: "", fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    HealthBar(
                        current = p2State?.currentHP ?: 0f,
                        max = p2Fighter.maxHP,
                        color = Color(0xFFFF1744),
                        modifier = Modifier.fillMaxWidth(),
                        isLeft = false
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    PowerBar(power = p2State?.powerMeter ?: 0f, modifier = Modifier.fillMaxWidth(0.7f).align(Alignment.End))
                    if ((p2State?.comboCount ?: 0) > 1) {
                        Text(
                            "${p2State?.comboCount} HITS! COMBO!",
                            color = Color.Yellow,
                            fontWeight = FontWeight.Black,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            // Mops alert
            if (arenaState.mopsTriggered) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .background(Color.Yellow.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                        .border(1.dp, Color.Yellow, RoundedCornerShape(4.dp))
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🐶 MOPS ALARM! ARENA WACKELT! 🐶", color = Color.Yellow, fontWeight = FontWeight.Black, fontSize = 12.sp)
                }
            }
        }

        // Controls bottom
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            // Joystick
            Box(
                modifier = Modifier.size(130.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(2.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragEnd = {
                                    joystickOffset = Offset.Zero
                                    gameManager.movePlayer(1, 0f)
                                },
                                onDragCancel = {
                                    joystickOffset = Offset.Zero
                                    gameManager.movePlayer(1, 0f)
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    joystickOffset += dragAmount
                                    val maxDist = 45f
                                    val dist = sqrt(joystickOffset.x * joystickOffset.x + joystickOffset.y * joystickOffset.y)
                                    if (dist > maxDist) {
                                        val ratio = maxDist / dist
                                        joystickOffset = Offset(joystickOffset.x * ratio, joystickOffset.y * ratio)
                                    }
                                    val dirX = (joystickOffset.x / maxDist).coerceIn(-1f, 1f)
                                    gameManager.movePlayer(1, dirX)
                                    if (joystickOffset.y < -30) {
                                        gameManager.jump(1)
                                    }
                                }
                            )
                        }
                ) {
                    drawCircle(color = Color.White.copy(alpha = 0.15f), radius = 45f)
                    drawCircle(
                        color = Color(0xFF00E5FF),
                        radius = 18f,
                        center = Offset(center.x + joystickOffset.x, center.y + joystickOffset.y)
                    )
                }
                Text("🕹️", modifier = Modifier.align(Alignment.TopCenter).offset(y = (-6).dp), fontSize = 10.sp)
            }

            // Action buttons
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.End) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ActionButton(text = "🛡️\nBLOCK", color = Color(0xFF37474F), onDown = { gameManager.block(1, true) }, onUp = { gameManager.block(1, false) })
                    ActionButton(text = "⭐\nSPEC1", color = Color(0xFF6A1B9A), onClick = { gameManager.special1(1, System.currentTimeMillis()) })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ActionButton(text = "👊\nLIGHT", color = Color(0xFF1565C0), onClick = { gameManager.lightAttack(1, System.currentTimeMillis()) })
                    ActionButton(text = "💥\nHEAVY", color = Color(0xFFFF1744), onClick = { gameManager.heavyAttack(1, System.currentTimeMillis()) })
                    ActionButton(text = "💀\nSPEC2", color = Color(0xFFBF360C), onClick = { gameManager.special2(1, System.currentTimeMillis()) })
                }
            }
        }

        // Pause button top right
        IconButton(
            onClick = { showPause = true },
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 80.dp, end = 8.dp)
        ) {
            Text("⏸️", fontSize = 20.sp)
        }

        // KO / Round End overlay
        if (!matchState.isRoundActive) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val needed = matchState.bestOf / 2 + 1
                    val matchEnded = matchState.p1Wins >= needed || matchState.p2Wins >= needed
                    if (matchEnded) {
                        Text(
                            text = if (matchState.p1Wins > matchState.p2Wins) "🏆 ${p1Fighter.displayName} GEWINNT!" else "💀 ${p2Fighter.displayName} GEWINNT!",
                            color = Color.Yellow,
                            fontWeight = FontWeight.Black,
                            fontSize = 24.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (matchState.result == MatchResult.PERFECT) "PERFECT!" else if (matchState.showFatality) "FATALITY!" else "K.O.",
                            color = Color(0xFFFF1744),
                            fontWeight = FontWeight.Black,
                            fontSize = 48.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(onClick = {
                                gameManager.initMatch(p1Fighter, p2Fighter, gameMode)
                            }) { Text("REMATCH") }
                            Button(onClick = onExit, colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)) { Text("MENÜ") }
                        }
                    } else {
                        Text(
                            text = when (matchState.result) {
                                MatchResult.PLAYER1_WIN -> "${p1Fighter.displayName} GEWINNT RUNDE ${matchState.round}"
                                MatchResult.PLAYER2_WIN -> "${p2Fighter.displayName} GEWINNT RUNDE ${matchState.round}"
                                MatchResult.PERFECT -> "PERFECT! ${matchState.winner?.displayName}"
                                else -> "UNENTSCHIEDEN"
                            },
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        CircularProgressIndicator(color = Color(0xFFFF1744))
                    }
                }
            }
        }

        // Pause dialog
        if (showPause) {
            AlertDialog(
                onDismissRequest = { showPause = false },
                title = { Text("⏸️ PAUSE", fontWeight = FontWeight.Black) },
                text = {
                    Column {
                        Text("Spiel pausiert")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("P1: ${p1Fighter.displayName} HP ${p1State?.currentHP?.toInt()}/${p1Fighter.maxHP.toInt()}")
                        Text("P2: ${p2Fighter.displayName} HP ${p2State?.currentHP?.toInt()}/${p2Fighter.maxHP.toInt()}")
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showPause = false }) { Text("WEITER") }
                },
                dismissButton = {
                    TextButton(onClick = { showPause = false; onExit() }) { Text("AUFGEBEN") }
                }
            )
        }
    }
}

@Composable
fun FightersCanvas(
    p1: FighterInMatch,
    p2: FighterInMatch,
    effects: List<com.secureguard.enterprise.pennerkombat.engine.GameEffect>,
    modifier: Modifier = Modifier
) {
    val config = LocalConfiguration.current
    val infiniteTransition = rememberInfiniteTransition(label = "fighter_anim")
    val bounce by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(animation = tween(300, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
        label = "bounce"
    )

    Canvas(modifier = modifier) {
        val centerY = size.height * 0.62f
        val scale = size.width / 20f

        fun drawFighter(f: FighterInMatch, isP1: Boolean) {
            val x = size.width / 2 + f.positionX * scale
            val y = centerY - f.positionY * scale * 2 - if (f.state == FighterInMatch::class.java.let { f.isGrounded } ) bounce else 0f

            // Shadow
            drawOval(
                color = Color.Black.copy(alpha = 0.5f),
                topLeft = Offset(x - 25f, centerY + 10f),
                size = Size(50f, 12f)
            )

            // Body color based on state
            val bodyColor = when (f.state) {
                FighterState.HIT_REACT -> Color.Red
                FighterState.BLOCKING -> Color(0xFF78909C)
                FighterState.LIGHT_ATTACK, FighterState.HEAVY_ATTACK -> f.fighter.colorPrimary
                FighterState.SPECIAL1, FighterState.SPECIAL2 -> Color.Yellow
                FighterState.DEAD -> Color.Gray
                else -> f.fighter.colorPrimary
            }

            // Fighter as capsule + emoji
            // Head
            drawCircle(
                color = bodyColor,
                radius = 22f,
                center = Offset(x, y - 40f)
            )
            // Body
            drawRect(
                color = bodyColor,
                topLeft = Offset(x - 18f, y - 30f),
                size = Size(36f, 50f)
            )
            // Arms - attack extension
            val armLength = if (f.state == FighterState.LIGHT_ATTACK || f.state == FighterState.HEAVY_ATTACK) 45f else 25f
            val armDir = if (f.facingRight) 1f else -1f
            drawRect(
                color = f.fighter.colorSecondary,
                topLeft = Offset(x + armDir * 10f, y - 25f),
                size = Size(armLength * armDir, 10f)
            )
            // Legs
            drawRect(
                color = Color.Black.copy(alpha = 0.8f),
                topLeft = Offset(x - 15f, y + 20f),
                size = Size(12f, 30f)
            )
            drawRect(
                color = Color.Black.copy(alpha = 0.8f),
                topLeft = Offset(x + 3f, y + 20f),
                size = Size(12f, 30f)
            )

            // Facing indicator
            val eyeOffset = if (f.facingRight) 6f else -6f
            drawCircle(
                color = Color.White,
                radius = 4f,
                center = Offset(x + eyeOffset, y - 42f)
            )

            // Blocking shield
            if (f.isBlocking) {
                drawRect(
                    color = Color.Cyan.copy(alpha = 0.4f),
                    topLeft = Offset(x - 25f + if (f.facingRight) 20f else -10f, y - 35f),
                    size = Size(15f, 60f)
                )
            }
        }

        drawFighter(p1, true)
        drawFighter(p2, false)

        // Effects
        effects.forEach { eff ->
            val ex = size.width / 2 + eff.x * scale
            val ey = centerY - eff.y * scale * 2
            val alpha = 1f - eff.progress
            val col = when (eff.type) {
                com.secureguard.enterprise.pennerkombat.engine.EffectType.LIGHT_HIT -> Color.Yellow
                com.secureguard.enterprise.pennerkombat.engine.EffectType.HEAVY_HIT -> Color.Red
                com.secureguard.enterprise.pennerkombat.engine.EffectType.SPECIAL_HIT -> Color.Magenta
                com.secureguard.enterprise.pennerkombat.engine.EffectType.BLOCK -> Color.Cyan
                com.secureguard.enterprise.pennerkombat.engine.EffectType.MOPS -> Color.Yellow
                else -> Color.White
            }
            drawCircle(
                color = col.copy(alpha = alpha),
                radius = (30f * (1f + eff.progress)),
                center = Offset(ex, ey)
            )
            if (eff.type == com.secureguard.enterprise.pennerkombat.engine.EffectType.MOPS) {
                // Extra shockwave
                drawCircle(
                    color = Color.Yellow.copy(alpha = alpha * 0.5f),
                    radius = 60f * (1f + eff.progress),
                    center = Offset(ex, ey),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
                )
            }
        }

        // Distance marker
        val dist = abs(p1.positionX - p2.positionX)
        if (dist < 2.6f) {
            // Attack range indicator
            val midX = size.width / 2 + (p1.positionX + p2.positionX) / 2 * scale
            drawLine(
                color = Color.Red.copy(alpha = 0.6f),
                start = Offset(midX, centerY - 80f),
                end = Offset(midX, centerY + 20f),
                strokeWidth = 2f
            )
        }
    }

    // Overlay emoji labels
    Box(modifier = Modifier.fillMaxSize()) {
        // P1 label floating
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(
                    x = (p1.positionX * 18).dp,
                    y = (-p1.positionY * 20 - 80).dp
                )
        ) {
            Text(p1.fighter.emoji, fontSize = 24.sp)
        }
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(
                    x = (p2.positionX * 18).dp,
                    y = (-p2.positionY * 20 - 80).dp
                )
        ) {
            Text(p2.fighter.emoji, fontSize = 24.sp)
        }
    }
}

@Composable
fun ActionButton(
    text: String,
    color: Color,
    onClick: (() -> Unit)? = null,
    onDown: (() -> Unit)? = null,
    onUp: (() -> Unit)? = null
) {
    var pressed by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(if (pressed) color.copy(alpha = 0.8f) else color.copy(alpha = 0.6f))
            .border(2.dp, Color.White.copy(alpha = 0.3f), CircleShape)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        pressed = true
                        onDown?.invoke() ?: onClick?.invoke()
                    },
                    onDragEnd = {
                        pressed = false
                        onUp?.invoke()
                    },
                    onDragCancel = {
                        pressed = false
                        onUp?.invoke()
                    },
                    onDrag = { _, _ -> }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color.White, fontWeight = FontWeight.Black, fontSize = 10.sp, lineHeight = 10.sp)
    }
}
