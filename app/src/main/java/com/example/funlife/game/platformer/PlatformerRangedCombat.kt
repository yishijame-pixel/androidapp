package com.example.funlife.game.platformer

import com.example.funlife.game.platformer.catalog.PlatformerAnimClip
import com.example.funlife.game.platformer.catalog.PlatformerRemoteAnimCache
import kotlin.math.max

/** 横版远程攻击：射击 / 投掷 / 行走小鸡篮球。 */
object PlatformerRangedCombat {

    const val RANGED_ANIM_FPS = 16f
    private const val RANGED_COOLDOWN_SEC = 0.52f
    private const val THROW_COOLDOWN_SEC = 0.58f
    private const val SPAWN_PROGRESS = 0.42f
    private const val SPAWN_PROGRESS_END = 0.52f
    private const val BULLET_SPEED = 400f
    private const val KUNAI_SPEED = 350f
    private const val KUNAI_VY = -45f

    fun canRangedAttack(characterId: PlatformerCharacterId): Boolean {
        if (PlatformerChickBasketball.canThrow(characterId)) return true
        val cfg = PlatformerRemoteAnimCache.config(characterId) ?: return false
        val clips = cfg.clips
        return PlatformerAnimClip.SHOOT in clips ||
            PlatformerAnimClip.THROW in clips ||
            PlatformerAnimClip.RUN_SHOOT in clips ||
            PlatformerAnimClip.JUMP_SHOOT in clips
    }

    fun rangedButtonLabel(characterId: PlatformerCharacterId): String {
        if (PlatformerChickBasketball.canThrow(characterId)) return PlatformerChickBasketball.buttonLabel()
        val cfg = PlatformerRemoteAnimCache.config(characterId) ?: return "射"
        return if (PlatformerAnimClip.THROW in cfg.clips &&
            PlatformerAnimClip.SHOOT !in cfg.clips &&
            PlatformerAnimClip.RUN_SHOOT !in cfg.clips
        ) {
            "投"
        } else {
            "射"
        }
    }

    fun pickRangedClip(
        cfg: com.example.funlife.game.platformer.catalog.PlatformerAnimConfig,
        player: PlatformerPlayer,
        locomoting: Boolean,
    ): PlatformerAnimClip {
        val clips = cfg.clips
        return when {
            player.rangedJumpVariant && PlatformerAnimClip.JUMP_SHOOT in clips ->
                PlatformerAnimClip.JUMP_SHOOT
            player.rangedRunVariant && locomoting && PlatformerAnimClip.RUN_SHOOT in clips ->
                PlatformerAnimClip.RUN_SHOOT
            !player.grounded && PlatformerAnimClip.JUMP_SHOOT in clips ->
                PlatformerAnimClip.JUMP_SHOOT
            locomoting && PlatformerAnimClip.RUN_SHOOT in clips ->
                PlatformerAnimClip.RUN_SHOOT
            PlatformerAnimClip.SHOOT in clips -> PlatformerAnimClip.SHOOT
            PlatformerAnimClip.THROW in clips -> PlatformerAnimClip.THROW
            else -> clips.first()
        }
    }

    fun rangedClipFor(player: PlatformerPlayer, characterId: PlatformerCharacterId): PlatformerAnimClip? {
        if (player.rangedAnimSecLeft <= 0f) return null
        val cfg = PlatformerRemoteAnimCache.config(characterId) ?: return null
        return pickRangedClip(cfg, player, locomoting = false)
    }

    fun rangedElapsedSec(player: PlatformerPlayer): Float {
        val total = player.rangedAnimTotalSec.coerceAtLeast(0.001f)
        return (total - player.rangedAnimSecLeft).coerceIn(0f, total)
    }

    fun rangedProgress(player: PlatformerPlayer): Float {
        val total = player.rangedAnimTotalSec.coerceAtLeast(0.001f)
        return rangedElapsedSec(player) / total
    }

    fun tryBeginRanged(
        player: PlatformerPlayer,
        characterId: PlatformerCharacterId,
        airborne: Boolean,
        locomoting: Boolean,
    ): PlatformerPlayer {
        if (PlatformerChickBasketball.canThrow(characterId)) {
            return PlatformerChickBasketball.tryBeginThrow(player)
        }
        if (!canRangedAttack(characterId)) return player
        if (player.attackAnimSecLeft > 0f) return player
        if (player.rangedAnimSecLeft > 0f || player.rangedCooldownSecLeft > 0f) return player
        val cfg = PlatformerRemoteAnimCache.config(characterId) ?: return player
        val jumpVariant = airborne && PlatformerAnimClip.JUMP_SHOOT in cfg.clips
        val runVariant = !airborne && locomoting && PlatformerAnimClip.RUN_SHOOT in cfg.clips
        val clip = when {
            jumpVariant -> PlatformerAnimClip.JUMP_SHOOT
            runVariant -> PlatformerAnimClip.RUN_SHOOT
            !airborne && PlatformerAnimClip.SHOOT in cfg.clips -> PlatformerAnimClip.SHOOT
            PlatformerAnimClip.THROW in cfg.clips -> PlatformerAnimClip.THROW
            PlatformerAnimClip.SHOOT in cfg.clips -> PlatformerAnimClip.SHOOT
            else -> return player
        }
        val frames = PlatformerRemoteAnimCache.clipManifestFrameCount(characterId, clip)
        val duration = frames / RANGED_ANIM_FPS
        PlatformerRemoteAnimCache.requestSheetPlaybackAsync(characterId, clip)
        return player.copy(
            rangedAnimSecLeft = duration,
            rangedAnimTotalSec = duration,
            rangedJumpVariant = jumpVariant,
            rangedRunVariant = runVariant,
            rangedClip = PlatformerAnimClipRef.from(clip),
            rangedProjectileSpawned = false,
            rangedCooldownSecLeft = 0f,
        )
    }

    fun tickRangedTimers(player: PlatformerPlayer, dt: Float): PlatformerPlayer {
        var p = player
        if (p.rangedCooldownSecLeft > 0f) {
            p = p.copy(rangedCooldownSecLeft = max(0f, p.rangedCooldownSecLeft - dt))
        }
        if (p.rangedAnimSecLeft <= 0f) return p
        val left = max(0f, p.rangedAnimSecLeft - dt)
        return if (left <= 0f) {
            val cooldown = when (p.rangedClip) {
                PlatformerAnimClipRef.THROW -> THROW_COOLDOWN_SEC
                PlatformerAnimClipRef.BASKETBALL -> PlatformerChickBasketball.COOLDOWN_SEC
                else -> RANGED_COOLDOWN_SEC
            }
            p.copy(
                rangedAnimSecLeft = 0f,
                rangedAnimTotalSec = 0f,
                rangedJumpVariant = false,
                rangedRunVariant = false,
                rangedClip = null,
                rangedProjectileSpawned = false,
                rangedCooldownSecLeft = cooldown,
            )
        } else {
            p.copy(rangedAnimSecLeft = left)
        }
    }

    data class SpawnResult(val player: PlatformerPlayer, val projectile: PlatformerProjectile)

    fun trySpawnProjectile(
        player: PlatformerPlayer,
        characterId: PlatformerCharacterId,
        px: Float,
        py: Float,
        pw: Float,
        ph: Float,
        tilePx: Int,
        nextId: Int,
    ): SpawnResult? {
        if (PlatformerChickBasketball.canThrow(characterId)) {
            return PlatformerChickBasketball.trySpawn(player, px, py, pw, ph, tilePx, nextId)
        }
        if (player.rangedAnimSecLeft <= 0f || player.rangedProjectileSpawned) return null
        val progress = rangedProgress(player)
        if (progress !in SPAWN_PROGRESS..SPAWN_PROGRESS_END) return null
        val clip = player.rangedClip?.toAnimClip() ?: return null
        val tile = tilePx.toFloat()
        val facingRight = player.facingRight
        val originX = if (facingRight) px + pw * 0.72f else px + pw * 0.28f
        val originY = py + ph * 0.38f
        val shotKind = when (clip) {
            PlatformerAnimClip.THROW -> PlatformerPlayerShotKind.KUNAI
            else -> PlatformerPlayerShotKind.BULLET
        }
        val vx = if (facingRight) {
            if (shotKind == PlatformerPlayerShotKind.KUNAI) KUNAI_SPEED else BULLET_SPEED
        } else {
            if (shotKind == PlatformerPlayerShotKind.KUNAI) -KUNAI_SPEED else -BULLET_SPEED
        }
        val vy = if (shotKind == PlatformerPlayerShotKind.KUNAI) KUNAI_VY else 0f
        val projectile = PlatformerProjectile(
            id = nextId,
            trapId = PlatformerProjectile.PLAYER_TRAP_ID,
            x = originX,
            y = originY,
            vx = vx,
            vy = vy,
            source = PlatformerProjectileSource.PLAYER,
            shotKind = shotKind,
        )
        return SpawnResult(
            player = player.copy(rangedProjectileSpawned = true),
            projectile = projectile,
        )
    }

    fun applyPlayerProjectileHits(
        enemies: List<PlatformerEnemy>,
        projectiles: List<PlatformerProjectile>,
        tilePx: Int,
    ): Triple<List<PlatformerEnemy>, List<PlatformerProjectile>, List<PlatformerHitSpark>> {
        var updatedEnemies = enemies
        val sparks = mutableListOf<PlatformerHitSpark>()
        val updatedProjectiles = projectiles.map { proj ->
            if (proj.source != PlatformerProjectileSource.PLAYER || !proj.alive) return@map proj
            var hit = false
            updatedEnemies = updatedEnemies.map { enemy ->
                if (hit || !enemy.alive) return@map enemy
                val ew = PlatformerEnemySystem.width(enemy.type, tilePx, enemy.catalogId)
                val eh = PlatformerEnemySystem.height(enemy.type, tilePx, enemy.catalogId)
                if (projectileHitsEnemy(proj, enemy.x, enemy.y, ew, eh, tilePx)) {
                    hit = true
                    sparks += PlatformerHitSpark(
                        x = enemy.x + ew * 0.5f,
                        y = enemy.y + eh * 0.35f,
                    )
                    enemy.copy(alive = false)
                } else {
                    enemy
                }
            }
            if (hit) proj.copy(alive = false) else proj
        }.filter { it.alive }
        return Triple(updatedEnemies, updatedProjectiles, sparks)
    }

    private fun projectileHitsEnemy(
        projectile: PlatformerProjectile,
        ex: Float,
        ey: Float,
        ew: Float,
        eh: Float,
        tilePx: Int,
    ): Boolean {
        val r = when (projectile.shotKind) {
            PlatformerPlayerShotKind.BASKETBALL ->
                PlatformerChickBasketball.projectileHitRadius(projectile, tilePx)
            else -> tilePx * 0.18f
        }
        val cx = projectile.x + r
        val cy = projectile.y + r
        val insetX = ew * 0.12f
        val insetY = eh * 0.12f
        return cx + r > ex + insetX &&
            cx - r < ex + ew - insetX &&
            cy + r > ey + insetY &&
            cy - r < ey + eh - insetY
    }

    fun rangedCooldownFraction(
        player: PlatformerPlayer,
        characterId: PlatformerCharacterId = PlatformerCharacterId.CHICK_PRO_MAX,
    ): Float {
        if (player.rangedAnimSecLeft > 0f) return 0f
        if (PlatformerChickBasketball.canThrow(characterId)) {
            return PlatformerChickBasketball.cooldownFraction(player)
        }
        val total = if (player.rangedClip == PlatformerAnimClipRef.THROW) {
            THROW_COOLDOWN_SEC
        } else {
            RANGED_COOLDOWN_SEC
        }
        return (player.rangedCooldownSecLeft / total).coerceIn(0f, 1f)
    }

    fun prefetchClips(characterId: PlatformerCharacterId) {
        if (!canRangedAttack(characterId)) return
        val cfg = PlatformerRemoteAnimCache.config(characterId) ?: return
        listOf(
            PlatformerAnimClip.SHOOT,
            PlatformerAnimClip.RUN_SHOOT,
            PlatformerAnimClip.JUMP_SHOOT,
            PlatformerAnimClip.THROW,
        ).filter { it in cfg.clips }.forEach { clip ->
            PlatformerRemoteAnimCache.requestSheetPlaybackAsync(characterId, clip)
        }
    }
}

enum class PlatformerProjectileSource { TRAP, PLAYER }

enum class PlatformerPlayerShotKind { BULLET, KUNAI, BASKETBALL }
