package com.secureguard.enterprise.pennerkombat.model

enum class GameMode {
    ARCADE, VERSUS, STORY, TRAINING
}

enum class FighterState {
    IDLE, WALKING, LIGHT_ATTACK, HEAVY_ATTACK, SPECIAL1, SPECIAL2, BLOCKING, HIT_REACT, KNOCKED, DEAD, JUMPING, DASHING
}

enum class MatchResult {
    ONGOING, PLAYER1_WIN, PLAYER2_WIN, DRAW, PERFECT
}

data class FighterInMatch(
    val fighter: Fighter,
    var currentHP: Float = fighter.maxHP,
    var positionX: Float = 0f,
    var positionY: Float = 0f,
    var state: FighterState = FighterState.IDLE,
    var isBlocking: Boolean = false,
    var isGrounded: Boolean = true,
    var comboCount: Int = 0,
    var lastHitTime: Long = 0L,
    var stunTimer: Float = 0f,
    var attackCooldown: Float = 0f,
    var special1Cooldown: Float = 0f,
    var special2Cooldown: Float = 0f,
    var facingRight: Boolean = true,
    var velocityX: Float = 0f,
    var velocityY: Float = 0f,
    var powerMeter: Float = 0f // 0-100 für EX Moves
)

data class ArenaState(
    val name: String = "Bahnhofsvorplatz",
    val description: String = "Nasser Asphalt, Neon flackert, Bierkästen stapeln sich",
    val interactiveObjects: List<ArenaObject> = listOf(
        ArenaObject("Bierkasten", "🍺", -4f, true),
        ArenaObject("Gasflasche", "🧯", 4f, true),
        ArenaObject("Wäscheleine", "👕", 0f, false),
        ArenaObject("Neon Schild", "💡", 2f, false)
    ),
    var mopsTriggered: Boolean = false
)

data class ArenaObject(
    val name: String,
    val emoji: String,
    val positionX: Float,
    val breakable: Boolean,
    var broken: Boolean = false
)

data class MatchState(
    var round: Int = 1,
    var bestOf: Int = 3,
    var p1Wins: Int = 0,
    var p2Wins: Int = 0,
    var timer: Float = 99f,
    var result: MatchResult = MatchResult.ONGOING,
    var isRoundActive: Boolean = true,
    var showFatality: Boolean = false,
    var winner: Fighter? = null,
    var slowMotion: Boolean = false,
    var hitStop: Float = 0f
)

data class PlayerStats(
    var totalFights: Int = 0,
    var wins: Int = 0,
    var losses: Int = 0,
    var maxCombo: Int = 0,
    var fatalities: Int = 0,
    var perfects: Int = 0,
    var unlockedFighters: MutableSet<String> = mutableSetOf("le_binde", "mell", "rolf"),
    var unlockedTrophies: MutableSet<String> = mutableSetOf(),
    var storyProgress: Int = 0
)
