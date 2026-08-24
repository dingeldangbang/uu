package com.secureguard.enterprise.pennerkombat.engine

import com.secureguard.enterprise.pennerkombat.model.Fighter
import com.secureguard.enterprise.pennerkombat.model.FighterInMatch
import com.secureguard.enterprise.pennerkombat.model.FighterState
import kotlin.math.abs
import kotlin.random.Random

class FighterController {

    companion object {
        const val GRAVITY = 26f
        const val JUMP_FORCE = 9.5f
        const val KNOCKBACK_FORCE = 8f
        const val COMBO_WINDOW_MS = 2000L
        const val ATTACK_RANGE = 2.5f
        const val LIGHT_COOLDOWN = 0.4f
        const val HEAVY_COOLDOWN = 0.8f
        const val BLOCK_DAMAGE_REDUCTION = 0.22f
        const val STUN_DURATION = 0.2f
    }

    fun updateFighter(fighter: FighterInMatch, delta: Float, currentTime: Long) {
        if (fighter.attackCooldown > 0) fighter.attackCooldown -= delta
        if (fighter.special1Cooldown > 0) fighter.special1Cooldown -= delta
        if (fighter.special2Cooldown > 0) fighter.special2Cooldown -= delta
        if (fighter.stunTimer > 0) {
            fighter.stunTimer -= delta
            return
        }

        // Physics
        if (!fighter.isGrounded) {
            fighter.velocityY -= GRAVITY * delta
            fighter.positionY += fighter.velocityY * delta
            if (fighter.positionY <= 0f) {
                fighter.positionY = 0f
                fighter.isGrounded = true
                fighter.velocityY = 0f
                if (fighter.state == FighterState.JUMPING) fighter.state = FighterState.IDLE
            }
        }

        fighter.positionX += fighter.velocityX * delta
        fighter.velocityX *= 0.85f // friction
        if (abs(fighter.velocityX) < 0.1f) fighter.velocityX = 0f

        // Clamp arena
        fighter.positionX = fighter.positionX.coerceIn(-8f, 8f)

        // Reset attack state
        if (fighter.state == FighterState.LIGHT_ATTACK || fighter.state == FighterState.HEAVY_ATTACK) {
            if (fighter.attackCooldown <= 0.1f) {
                fighter.state = FighterState.IDLE
            }
        }
    }

    fun move(fighter: FighterInMatch, dirX: Float) {
        if (fighter.stunTimer > 0) return
        if (fighter.state == FighterState.DEAD || fighter.state == FighterState.KNOCKED) return
        if (fighter.isBlocking) {
            fighter.velocityX = dirX * fighter.fighter.moveSpeed * 0.35f
        } else {
            fighter.velocityX = dirX * fighter.fighter.moveSpeed
        }
        if (dirX != 0f) {
            fighter.facingRight = dirX > 0
            if (fighter.state == FighterState.IDLE) fighter.state = FighterState.WALKING
        } else {
            if (fighter.state == FighterState.WALKING) fighter.state = FighterState.IDLE
        }
    }

    fun jump(fighter: FighterInMatch) {
        if (!fighter.isGrounded) return
        if (fighter.stunTimer > 0) return
        fighter.velocityY = JUMP_FORCE
        fighter.isGrounded = false
        fighter.state = FighterState.JUMPING
    }

    fun block(fighter: FighterInMatch, blocking: Boolean) {
        if (fighter.stunTimer > 0) return
        fighter.isBlocking = blocking
        if (blocking) fighter.state = FighterState.BLOCKING else if (fighter.state == FighterState.BLOCKING) fighter.state = FighterState.IDLE
    }

    fun lightAttack(attacker: FighterInMatch, defender: FighterInMatch, currentTime: Long): Boolean {
        if (attacker.attackCooldown > 0) return false
        if (attacker.stunTimer > 0) return false
        if (attacker.isBlocking) return false
        attacker.state = FighterState.LIGHT_ATTACK
        attacker.attackCooldown = LIGHT_COOLDOWN
        return tryHit(attacker, defender, attacker.fighter.lightDamage, false, currentTime)
    }

    fun heavyAttack(attacker: FighterInMatch, defender: FighterInMatch, currentTime: Long): Boolean {
        if (attacker.attackCooldown > 0) return false
        if (attacker.stunTimer > 0) return false
        if (attacker.isBlocking) return false
        attacker.state = FighterState.HEAVY_ATTACK
        attacker.attackCooldown = HEAVY_COOLDOWN
        return tryHit(attacker, defender, attacker.fighter.heavyDamage, true, currentTime)
    }

    fun special1(attacker: FighterInMatch, defender: FighterInMatch, currentTime: Long): Boolean {
        if (attacker.special1Cooldown > 0) return false
        if (attacker.stunTimer > 0) return false
        attacker.state = FighterState.SPECIAL1
        attacker.special1Cooldown = attacker.fighter.special1.cooldown
        attacker.powerMeter = (attacker.powerMeter - 25f).coerceAtLeast(0f)
        val success = tryHit(attacker, defender, attacker.fighter.special1.damage, true, currentTime, knockbackMult = 1.5f)
        attacker.state = FighterState.IDLE
        return success
    }

    fun special2(attacker: FighterInMatch, defender: FighterInMatch, currentTime: Long): Boolean {
        if (attacker.special2Cooldown > 0) return false
        if (attacker.stunTimer > 0) return false
        attacker.state = FighterState.SPECIAL2
        attacker.special2Cooldown = attacker.fighter.special2.cooldown
        attacker.powerMeter = (attacker.powerMeter - 35f).coerceAtLeast(0f)
        val success = tryHit(attacker, defender, attacker.fighter.special2.damage, true, currentTime, knockbackMult = 1.8f)
        attacker.state = FighterState.IDLE
        return success
    }

    private fun tryHit(
        attacker: FighterInMatch,
        defender: FighterInMatch,
        baseDamage: Float,
        heavy: Boolean,
        currentTime: Long,
        knockbackMult: Float = 1f
    ): Boolean {
        val distance = abs(attacker.positionX - defender.positionX)
        val verticalDist = abs(attacker.positionY - defender.positionY)
        if (distance > ATTACK_RANGE) return false
        if (verticalDist > 2f) return false

        // Facing check
        val facingCorrect = if (attacker.facingRight) defender.positionX > attacker.positionX else defender.positionX < attacker.positionX
        if (!facingCorrect && distance > 1.2f) return false

        applyDamage(defender, attacker, baseDamage, heavy, currentTime, knockbackMult)
        return true
    }

    fun applyDamage(
        defender: FighterInMatch,
        attacker: FighterInMatch,
        baseDamage: Float,
        heavy: Boolean,
        currentTime: Long,
        knockbackMult: Float = 1f
    ) {
        var damage = baseDamage
        if (defender.isBlocking) {
            damage *= BLOCK_DAMAGE_REDUCTION
            defender.stunTimer = 0.15f
        } else {
            // Combo
            if (currentTime - attacker.lastHitTime < COMBO_WINDOW_MS) {
                attacker.comboCount++
            } else {
                attacker.comboCount = 1
            }
            attacker.lastHitTime = currentTime
            attacker.powerMeter = (attacker.powerMeter + if (heavy) 8f else 4f).coerceAtMost(100f)

            val knockDir = if (defender.positionX > attacker.positionX) 1f else -1f
            defender.velocityX = knockDir * KNOCKBACK_FORCE * knockbackMult
            defender.velocityY = 3f * knockbackMult
            defender.isGrounded = false
            defender.stunTimer = STUN_DURATION + if (heavy) 0.15f else 0f
            defender.state = FighterState.HIT_REACT

            // Random crit
            if (Random.nextFloat() < 0.08f) {
                damage *= 1.5f
            }
        }

        // Defense
        damage *= (1f - defender.fighter.defense * 0.5f)

        defender.currentHP -= damage
        if (defender.currentHP <= 0f) {
            defender.currentHP = 0f
            defender.state = FighterState.DEAD
        }
    }
}
