package com.example.funlife.game.platformer

import com.example.funlife.game.platformer.catalog.PlatformerAnimClip
import com.example.funlife.game.platformer.catalog.PlatformerRemoteAnimCache
import kotlin.math.max

/**
 * 横版近战攻击：仅 catalog 远程角色且 manifest 含 attack clip（不含行走小鸡）。
 */
object PlatformerCombat {

    const val ATTACK_ANIM_FPS = 18f
    private const val ATTACK_COOLDOWN_SEC = 0.42f
    /** 攻击动画中判定有效的进度区间（0~1）。 */
    private const val HIT_WINDOW_START = 0.22f
    private const val HIT_WINDOW_END = 0.58f

    fun canAttack(characterId: PlatformerCharacterId): Boolean {
        if (characterId == PlatformerCharacterId.CHICK_PRO_MAX) return false
        val cfg = PlatformerRemoteAnimCache.config(characterId) ?: return false
        return PlatformerAnimClip.ATTACK in cfg.clips
    }

    fun attackClipFor(player: PlatformerPlayer, characterId: PlatformerCharacterId): PlatformerAnimClip? {
        if (player.attackAnimSecLeft <= 0f) return null
        val cfg = PlatformerRemoteAnimCache.config(characterId) ?: return null
        return when {
            player.attackJumpVariant && PlatformerAnimClip.JUMP_ATTACK in cfg.clips ->
                PlatformerAnimClip.JUMP_ATTACK
            PlatformerAnimClip.ATTACK in cfg.clips -> PlatformerAnimClip.ATTACK
            else -> null
        }
    }

    fun attackElapsedSec(player: PlatformerPlayer): Float {
        val total = player.attackAnimTotalSec.coerceAtLeast(0.001f)
        return (total - player.attackAnimSecLeft).coerceIn(0f, total)
    }

    fun attackProgress(player: PlatformerPlayer): Float {
        val total = player.attackAnimTotalSec.coerceAtLeast(0.001f)
        return attackElapsedSec(player) / total
    }

    fun isInHitWindow(player: PlatformerPlayer): Boolean {
        if (player.attackAnimSecLeft <= 0f) return false
        val p = attackProgress(player)
        return p in HIT_WINDOW_START..HIT_WINDOW_END
    }

    fun tryBeginAttack(
        player: PlatformerPlayer,
        characterId: PlatformerCharacterId,
        airborne: Boolean,
    ): PlatformerPlayer {
        if (!canAttack(characterId)) return player
        if (player.rangedAnimSecLeft > 0f) return player
        if (player.attackAnimSecLeft > 0f || player.attackCooldownSecLeft > 0f) return player
        val cfg = PlatformerRemoteAnimCache.config(characterId) ?: return player
        val jumpVariant = airborne && PlatformerAnimClip.JUMP_ATTACK in cfg.clips
        val clip = when {
            jumpVariant -> PlatformerAnimClip.JUMP_ATTACK
            PlatformerAnimClip.ATTACK in cfg.clips -> PlatformerAnimClip.ATTACK
            else -> return player
        }
        val frames = PlatformerRemoteAnimCache.clipManifestFrameCount(characterId, clip)
        val duration = frames / ATTACK_ANIM_FPS
        PlatformerRemoteAnimCache.requestSheetPlaybackAsync(characterId, clip)
        return player.copy(
            attackAnimSecLeft = duration,
            attackAnimTotalSec = duration,
            attackJumpVariant = jumpVariant,
            attackCooldownSecLeft = 0f,
        )
    }

    fun tickAttackTimers(player: PlatformerPlayer, dt: Float): PlatformerPlayer {
        var p = player
        if (p.attackCooldownSecLeft > 0f) {
            p = p.copy(attackCooldownSecLeft = max(0f, p.attackCooldownSecLeft - dt))
        }
        if (p.attackAnimSecLeft <= 0f) return p
        val left = max(0f, p.attackAnimSecLeft - dt)
        p = if (left <= 0f) {
            p.copy(
                attackAnimSecLeft = 0f,
                attackAnimTotalSec = 0f,
                attackJumpVariant = false,
                attackCooldownSecLeft = ATTACK_COOLDOWN_SEC,
            )
        } else {
            p.copy(attackAnimSecLeft = left)
        }
        return p
    }

    fun meleeHitRect(
        px: Float,
        py: Float,
        pw: Float,
        ph: Float,
        facingRight: Boolean,
        tilePx: Int,
    ): FloatArray {
        val tile = tilePx.toFloat()
        val reach = tile * 1.08f
        val hitH = ph * 0.74f
        val centerY = py + ph * 0.44f
        val top = centerY - hitH * 0.5f
        val bottom = centerY + hitH * 0.5f
        return if (facingRight) {
            floatArrayOf(px + pw * 0.42f, top, px + pw * 0.42f + reach, bottom)
        } else {
            floatArrayOf(px + pw * 0.58f - reach, top, px + pw * 0.58f, bottom)
        }
    }

    fun rectsOverlap(a: FloatArray, bx: Float, by: Float, bw: Float, bh: Float): Boolean =
        a[0] < bx + bw && a[2] > bx && a[1] < by + bh && a[3] > by

    fun applyMeleeHits(
        enemies: List<PlatformerEnemy>,
        player: PlatformerPlayer,
        px: Float,
        py: Float,
        pw: Float,
        ph: Float,
        tilePx: Int,
    ): Pair<List<PlatformerEnemy>, List<PlatformerHitSpark>> {
        if (!isInHitWindow(player)) return enemies to emptyList()
        val hitBox = meleeHitRect(px, py, pw, ph, player.facingRight, tilePx)
        val sparks = mutableListOf<PlatformerHitSpark>()
        val updated = enemies.map { enemy ->
            if (!enemy.alive) return@map enemy
            val ew = PlatformerEnemySystem.width(enemy.type, tilePx, enemy.catalogId)
            val eh = PlatformerEnemySystem.height(enemy.type, tilePx, enemy.catalogId)
            if (rectsOverlap(hitBox, enemy.x, enemy.y, ew, eh)) {
                sparks += PlatformerHitSpark(
                    x = enemy.x + ew * 0.5f,
                    y = enemy.y + eh * 0.35f,
                )
                enemy.copy(alive = false)
            } else {
                enemy
            }
        }
        return updated to sparks
    }

    fun attackCooldownFraction(player: PlatformerPlayer): Float {
        if (player.attackAnimSecLeft > 0f) return 0f
        return (player.attackCooldownSecLeft / ATTACK_COOLDOWN_SEC).coerceIn(0f, 1f)
    }
}

data class PlatformerHitSpark(
    val x: Float,
    val y: Float,
    val ageSec: Float = 0f,
) {
    companion object {
        const val LIFETIME_SEC = 0.22f
    }
}
