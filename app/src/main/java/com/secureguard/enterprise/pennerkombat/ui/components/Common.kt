package com.secureguard.enterprise.pennerkombat.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PennerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = Color(0xFFFF1744)
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .height(56.dp)
            .border(2.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(4.dp)),
        shape = RoundedCornerShape(4.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            contentColor = Color.White,
            disabledContainerColor = Color.Gray,
            disabledContentColor = Color.LightGray
        )
    ) {
        Text(
            text = text.uppercase(),
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp,
            fontSize = 16.sp
        )
    }
}

@Composable
fun HealthBar(
    current: Float,
    max: Float,
    color: Color,
    modifier: Modifier = Modifier,
    isLeft: Boolean = true
) {
    val progress = (current / max).coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .height(24.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(Color.Black)
            .border(2.dp, Color.White, RoundedCornerShape(2.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(color, color.copy(alpha = 0.6f))
                    )
                )
        )
        // Damage ticks
        Row(Modifier.fillMaxSize()) {
            repeat(10) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .border(0.5.dp, Color.Black.copy(alpha = 0.5f))
                )
            }
        }
    }
}

@Composable
fun PowerBar(
    power: Float,
    modifier: Modifier = Modifier
) {
    val progress = (power / 100f).coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .height(12.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color.Black.copy(alpha = 0.7f))
            .border(1.dp, Color.Yellow, RoundedCornerShape(6.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(Color.Yellow, Color(0xFFFF6D00))
                    )
                )
        )
    }
}

@Composable
fun GlitchText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.White
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontWeight = FontWeight.Black,
        letterSpacing = 3.sp,
        style = MaterialTheme.typography.displayMedium
    )
}
