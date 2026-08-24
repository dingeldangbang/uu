package com.secureguard.enterprise.pennerkombat.engine

import com.secureguard.enterprise.pennerkombat.model.FighterInMatch
import com.secureguard.enterprise.pennerkombat.model.FighterState
import kotlin.math.abs
import kotlin.random.Random

class AIController {

    private var decisionTimer = 0f
    private var strafeDir = 1f
    private var blockTimer = 0f

    fun update(
        ai: FighterInMatch,
        player: FighterInMatch,
        controller: FighterController,
        delta: Float,
        difficulty: Int,
        currentTime: Long
    ) {
        if (ai.state == FighterState.DEAD) return
        if (ai.stunTimer > 0) return

        decisionTimer -= delta
        if (blockTimer > 0) blockTimer -= delta

        val distance = abs(ai.positionX - player.positionX)
        val reactionDelay = when (difficulty) {
            0 -> 0.6f
            1 -> 0.4f
            2 -> 0.25f
            3 -> 0.15f
            4 -> 0.08f
            5 -> 0.03f
            else -> 0.2f
        }

        if (decisionTimer > 0) {
            // Continue current movement
            return
        }

        decisionTimer = reactionDelay + Random.nextFloat() * 0.3f

        // Difficulty based behavior
        val aggression = when (difficulty) {
            0 -> 0.3f
            1 -> 0.45f
            2 -> 0.6f
            3 -> 0.75f
            4 -> 0.85f
            5 -> 0.95f
            else -> 0.6f
        }

        // If player attacking and close, try block
        if (player.state == FighterState.LIGHT_ATTACK || player.state == FighterState.HEAVY_ATTACK) {
            if (distance < 3f && Random.nextFloat() < (0.4f + difficulty * 0.1f)) {
                controller.block(ai, true)
                blockTimer = 0.3f + Random.nextFloat() * 0.4f
                return
            }
        }

        if (blockTimer <= 0) {
            controller.block(ai, false)
        }

        when {
            distance > 3.5f -> {
                // Move closer
                val dir = if (player.positionX > ai.positionX) 1f else -1f
                controller.move(ai, dir)
                // Occasional dash
                if (Random.nextFloat() < 0.1f && ai.isGrounded) {
                    controller.jump(ai)
                }
            }
            distance < 1.2f -> {
                // Too close, maybe back off or attack
                if (Random.nextFloat() < aggression) {
                    // Attack
                    if (Random.nextFloat() < 0.5f) {
                        controller.lightAttack(ai, player, currentTime)
                    } else {
                        controller.heavyAttack(ai, player, currentTime)
                    }
                } else {
                    val dir = if (player.positionX > ai.positionX) -1f else 1f
                    controller.move(ai, dir)
                }
            }
            else -> {
                // Optimal range - attack
                val roll = Random.nextFloat()
                when {
                    roll < 0.35f -> controller.lightAttack(ai, player, currentTime)
                    roll < 0.65f -> controller.heavyAttack(ai, player, currentTime)
                    roll < 0.80f -> {
                        if (ai.special1Cooldown <= 0) controller.special1(ai, player, currentTime)
                        else controller.lightAttack(ai, player, currentTime)
                    }
                    roll < 0.90f -> {
                        if (ai.special2Cooldown <= 0) controller.special2(ai, player, currentTime)
                        else controller.heavyAttack(ai, player, currentTime)
                    }
                    else -> {
                        // Strafe
                        strafeDir = if (Random.nextFloat() < 0.5f) 1f else -1f
                        controller.move(ai, strafeDir * 0.5f)
                    }
                }
            }
        }

        // Jump logic
        if (Random.nextFloat() < 0.05f && ai.isGrounded) {
            controller.jump(ai)
        }
    }
}
