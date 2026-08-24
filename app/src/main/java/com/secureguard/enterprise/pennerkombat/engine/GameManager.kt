package com.secureguard.enterprise.pennerkombat.engine

import com.secureguard.enterprise.pennerkombat.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class GameManager {

    private val fighterController = FighterController()
    private val aiController = AIController()

    private val _player1 = MutableStateFlow<FighterInMatch?>(null)
    val player1: StateFlow<FighterInMatch?> = _player1

    private val _player2 = MutableStateFlow<FighterInMatch?>(null)
    val player2: StateFlow<FighterInMatch?> = _player2

    private val _matchState = MutableStateFlow(MatchState())
    val matchState: StateFlow<MatchState> = _matchState

    private val _arenaState = MutableStateFlow(ArenaState())
    val arenaState: StateFlow<ArenaState> = _arenaState

    private val _playerStats = MutableStateFlow(PlayerStats())
    val playerStats: StateFlow<PlayerStats> = _playerStats

    private val _effects = MutableStateFlow<List<GameEffect>>(emptyList())
    val effects: StateFlow<List<GameEffect>> = _effects

    var gameMode: GameMode = GameMode.VERSUS
    var difficulty: Int = 2 // 0-5

    fun initMatch(p1Fighter: Fighter, p2Fighter: Fighter, mode: GameMode = GameMode.VERSUS) {
        gameMode = mode
        val p1 = FighterInMatch(
            fighter = p1Fighter,
            currentHP = p1Fighter.maxHP,
            positionX = -3f,
            facingRight = true
        )
        val p2 = FighterInMatch(
            fighter = p2Fighter,
            currentHP = p2Fighter.maxHP,
            positionX = 3f,
            facingRight = false
        )
        _player1.value = p1
        _player2.value = p2
        _matchState.value = MatchState(
            round = 1,
            bestOf = 3,
            p1Wins = 0,
            p2Wins = 0,
            timer = 99f,
            result = MatchResult.ONGOING,
            isRoundActive = true
        )
        _arenaState.value = ArenaState()
        _effects.value = emptyList()
    }

    fun update(delta: Float, currentTime: Long) {
        val p1 = _player1.value ?: return
        val p2 = _player2.value ?: return
        val match = _matchState.value

        if (!match.isRoundActive) return

        // Update fighters
        fighterController.updateFighter(p1, delta, currentTime)
        fighterController.updateFighter(p2, delta, currentTime)

        // AI for P2 if not versus human (for now AI always unless versus flag)
        if (gameMode != GameMode.VERSUS || true) { // AI enabled for demo, can toggle
            // Only AI if second player is AI-controlled (we keep flag)
            if (_isP2AI) {
                aiController.update(p2, p1, fighterController, delta, difficulty, currentTime)
            }
        }

        // Timer
        if (match.timer > 0) {
            match.timer -= delta
            if (match.timer <= 0) {
                match.timer = 0f
                endRoundByTime()
            }
        }

        // Check death
        if (p1.currentHP <= 0 || p2.currentHP <= 0) {
            endRoundByKO()
        }

        // Effects update
        _effects.value = _effects.value.mapNotNull { it.update(delta) }

        // HitStop / SlowMo
        if (match.hitStop > 0) {
            match.hitStop -= delta
        }

        _matchState.value = match.copy()
        _player1.value = p1
        _player2.value = p2
    }

    private var _isP2AI: Boolean = true
    fun setP2AI(isAI: Boolean) { _isP2AI = isAI }

    fun movePlayer(playerNum: Int, dirX: Float) {
        val f = if (playerNum == 1) _player1.value else _player2.value ?: return
        fighterController.move(f, dirX)
    }

    fun jump(playerNum: Int) {
        val f = if (playerNum == 1) _player1.value else _player2.value ?: return
        fighterController.jump(f)
    }

    fun block(playerNum: Int, blocking: Boolean) {
        val f = if (playerNum == 1) _player1.value else _player2.value ?: return
        fighterController.block(f, blocking)
    }

    fun lightAttack(playerNum: Int, currentTime: Long) {
        val attacker = if (playerNum == 1) _player1.value else _player2.value ?: return
        val defender = if (playerNum == 1) _player2.value else _player1.value ?: return
        val hit = fighterController.lightAttack(attacker, defender, currentTime)
        if (hit) onHit(attacker, defender, false)
    }

    fun heavyAttack(playerNum: Int, currentTime: Long) {
        val attacker = if (playerNum == 1) _player1.value else _player2.value ?: return
        val defender = if (playerNum == 1) _player2.value else _player1.value ?: return
        val hit = fighterController.heavyAttack(attacker, defender, currentTime)
        if (hit) onHit(attacker, defender, true)
    }

    fun special1(playerNum: Int, currentTime: Long) {
        val attacker = if (playerNum == 1) _player1.value else _player2.value ?: return
        val defender = if (playerNum == 1) _player2.value else _player1.value ?: return
        val hit = fighterController.special1(attacker, defender, currentTime)
        if (hit) onHit(attacker, defender, true, isSpecial = true)
    }

    fun special2(playerNum: Int, currentTime: Long) {
        val attacker = if (playerNum == 1) _player1.value else _player2.value ?: return
        val defender = if (playerNum == 1) _player2.value else _player1.value ?: return
        val hit = fighterController.special2(attacker, defender, currentTime)
        if (hit) onHit(attacker, defender, true, isSpecial = true)
    }

    private fun onHit(attacker: FighterInMatch, defender: FighterInMatch, heavy: Boolean, isSpecial: Boolean = false) {
        // Hit effect
        val effect = GameEffect(
            x = defender.positionX,
            y = defender.positionY + 1f,
            type = if (isSpecial) EffectType.SPECIAL_HIT else if (heavy) EffectType.HEAVY_HIT else EffectType.LIGHT_HIT,
            timer = 0.4f
        )
        _effects.value = _effects.value + effect

        // Hit stop
        val match = _matchState.value
        match.hitStop = if (heavy) 0.08f else 0.04f
        if (isSpecial) match.hitStop = 0.12f

        // Combo check for max
        if (attacker.comboCount > _playerStats.value.maxCombo) {
            _playerStats.value = _playerStats.value.copy(maxCombo = attacker.comboCount)
        }

        // Check for mops trigger random
        if (attacker.comboCount >= 5 && !_arenaState.value.mopsTriggered) {
            if (kotlin.random.Random.nextFloat() < 0.15f) {
                _arenaState.value = _arenaState.value.copy(mopsTriggered = true)
                _effects.value = _effects.value + GameEffect(
                    x = 0f, y = 2f, type = EffectType.MOPS, timer = 1.5f
                )
            }
        }
    }

    private fun endRoundByKO() {
        val p1 = _player1.value ?: return
        val p2 = _player2.value ?: return
        val match = _matchState.value

        val p1Won = p2.currentHP <= 0 && p1.currentHP > 0
        val p2Won = p1.currentHP <= 0 && p2.currentHP > 0

        if (p1Won) {
            match.p1Wins++
            if (p1.currentHP >= p1.fighter.maxHP * 0.99f) {
                match.result = MatchResult.PERFECT
                _playerStats.value = _playerStats.value.copy(perfects = _playerStats.value.perfects + 1)
            } else {
                match.result = MatchResult.PLAYER1_WIN
            }
            match.winner = p1.fighter
        } else if (p2Won) {
            match.p2Wins++
            match.result = MatchResult.PLAYER2_WIN
            match.winner = p2.fighter
        } else {
            match.result = MatchResult.DRAW
        }

        match.isRoundActive = false
        match.showFatality = p1Won || p2Won

        _matchState.value = match.copy()

        // Check match end
        checkMatchEnd()
    }

    private fun endRoundByTime() {
        val p1 = _player1.value ?: return
        val p2 = _player2.value ?: return
        val match = _matchState.value

        if (p1.currentHP > p2.currentHP) {
            match.p1Wins++
            match.result = MatchResult.PLAYER1_WIN
            match.winner = p1.fighter
        } else if (p2.currentHP > p1.currentHP) {
            match.p2Wins++
            match.result = MatchResult.PLAYER2_WIN
            match.winner = p2.fighter
        } else {
            match.result = MatchResult.DRAW
        }
        match.isRoundActive = false
        _matchState.value = match.copy()
        checkMatchEnd()
    }

    private fun checkMatchEnd() {
        val match = _matchState.value
        val needed = match.bestOf / 2 + 1
        if (match.p1Wins >= needed || match.p2Wins >= needed) {
            // Match ended
            _playerStats.value = _playerStats.value.copy(
                totalFights = _playerStats.value.totalFights + 1,
                wins = _playerStats.value.wins + if (match.p1Wins > match.p2Wins) 1 else 0,
                losses = _playerStats.value.losses + if (match.p2Wins > match.p1Wins) 1 else 0
            )
        } else {
            // Next round after delay handled in UI
        }
    }

    fun startNextRound() {
        val p1 = _player1.value ?: return
        val p2 = _player2.value ?: return
        p1.currentHP = p1.fighter.maxHP
        p1.positionX = -3f
        p1.positionY = 0f
        p1.state = FighterState.IDLE
        p1.comboCount = 0
        p1.stunTimer = 0f
        p1.attackCooldown = 0f
        p1.velocityX = 0f
        p1.velocityY = 0f
        p1.facingRight = true

        p2.currentHP = p2.fighter.maxHP
        p2.positionX = 3f
        p2.positionY = 0f
        p2.state = FighterState.IDLE
        p2.comboCount = 0
        p2.stunTimer = 0f
        p2.attackCooldown = 0f
        p2.velocityX = 0f
        p2.velocityY = 0f
        p2.facingRight = false

        val match = _matchState.value
        match.round++
        match.timer = 99f
        match.result = MatchResult.ONGOING
        match.isRoundActive = true
        match.showFatality = false
        match.winner = null
        match.hitStop = 0f

        _matchState.value = match.copy()
        _player1.value = p1
        _player2.value = p2
    }

    fun resetMatch() {
        _player1.value?.let {
            it.currentHP = it.fighter.maxHP
            it.positionX = -3f
            it.positionY = 0f
            it.state = FighterState.IDLE
        }
        _player2.value?.let {
            it.currentHP = it.fighter.maxHP
            it.positionX = 3f
            it.positionY = 0f
            it.state = FighterState.IDLE
        }
        _matchState.value = MatchState()
    }
}

enum class EffectType {
    LIGHT_HIT, HEAVY_HIT, SPECIAL_HIT, BLOCK, MOPS, BLOOD, SHOCKWAVE
}

data class GameEffect(
    val x: Float,
    val y: Float,
    val type: EffectType,
    var timer: Float,
    val maxTimer: Float = 0.4f
) {
    fun update(delta: Float): GameEffect? {
        timer -= delta
        return if (timer <= 0) null else this
    }
    val progress: Float get() = 1f - timer / maxTimer
}
