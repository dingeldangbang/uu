package com.secureguard.enterprise.pennerkombat.model

import androidx.compose.ui.graphics.Color

enum class FighterArchetype {
    BRAWLER, TANK, SPEED, TRICKSTER, ZONER
}

data class SpecialMove(
    val id: String,
    val name: String,
    val description: String,
    val damage: Float,
    val cooldown: Float,
    val icon: String
)

data class Fighter(
    val id: String,
    val displayName: String,
    val nickName: String,
    val description: String,
    val archetype: FighterArchetype,
    val maxHP: Float,
    val moveSpeed: Float,
    val lightDamage: Float,
    val heavyDamage: Float,
    val defense: Float,
    val special1: SpecialMove,
    val special2: SpecialMove,
    val fatality: String,
    val brutality: String,
    val colorPrimary: Color,
    val colorSecondary: Color,
    val emoji: String,
    val story: String,
    val tier: Int // 1-5
)

data class Trophy(
    val id: String,
    val name: String,
    val description: String,
    val icon: String,
    val rarity: String, // Bronze, Silber, Gold, Platin
    var unlocked: Boolean = false
)

data class StoryChapter(
    val id: Int,
    val title: String,
    val description: String,
    val opponentId: String,
    val dialog: List<String>,
    val reward: String,
    var completed: Boolean = false,
    val isBoss: Boolean = false
)
