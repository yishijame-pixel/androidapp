package com.example.funlife.game.platformer

import kotlin.math.abs
import kotlin.math.max

/**
 * 行走小鸡专属：马里奥式弹跳篮球远程（抛物线 + 地面反弹 + 旋转）。
 */
object PlatformerChickBasketball {

    const val COOLDOWN_SEC = 0.62f
    /** 出手帧约占 attack 动画的 48%（与丢球动作对齐）。 */
    private const val SPAWN_FRAME_FRAC = 0.48f
    private const val LAUNCH_SPEED_X = 300f
    private const val LAUNCH_SPEED_Y = -255f
    private const val GRAVITY = 1520f
    private const val BOUNCE_RESTITUTION_Y = 0.68f
    private const val BOUNCE_RESTITUTION_X = 0.72f
    private const val MIN_BOUNCE_VY = 95f
    private const val MAX_BOUNCES = 8
    private const val SPIN_FACTOR = 0.028f
    /** 相对格宽的碰撞/显示半径。 */
    private const val BALL_RADIUS_TILE_FRAC = 0.28f

    fun canThrow(characterId: PlatformerCharacterId): Boolean =
        characterId == PlatformerCharacterId.CHICK_PRO_MAX

    fun buttonLabel(): String = "球"

    fun tryBeginThrow(player: PlatformerPlayer): PlatformerPlayer {
        if (player.rangedAnimSecLeft > 0f || player.rangedCooldownSecLeft > 0f) return player
        if (player.attackAnimSecLeft > 0f) return player
        PlatformerPlayerSprites.prefetchAttackSheet()
        val duration = PlatformerPlayerSprites.basketballThrowDurationSec()
        return player.copy(
            rangedAnimSecLeft = duration,
            rangedAnimTotalSec = duration,
            rangedClip = PlatformerAnimClipRef.BASKETBALL,
            rangedProjectileSpawned = false,
            rangedJumpVariant = false,
            rangedRunVariant = false,
            rangedCooldownSecLeft = 0f,
        )
    }

    private fun spawnProgressWindow(): ClosedFloatingPointRange<Float> {
        val frameCount = PlatformerPlayerSprites.attackSheetFrameCount().coerceAtLeast(1)
        val releaseFrame = (frameCount * SPAWN_FRAME_FRAC).toInt().coerceIn(0, frameCount - 1)
        val center = releaseFrame.toFloat() / frameCount
        return (center - 0.025f)..(center + 0.035f)
    }

    fun trySpawn(
        player: PlatformerPlayer,
        px: Float,
        py: Float,
        pw: Float,
        ph: Float,
        tilePx: Int,
        nextId: Int,
    ): PlatformerRangedCombat.SpawnResult? {
        if (player.rangedClip != PlatformerAnimClipRef.BASKETBALL) return null
        if (player.rangedAnimSecLeft <= 0f || player.rangedProjectileSpawned) return null
        val progress = 1f - player.rangedAnimSecLeft / player.rangedAnimTotalSec.coerceAtLeast(0.001f)
        if (progress !in spawnProgressWindow()) return null
        val tile = tilePx.toFloat()
        val radius = tile * BALL_RADIUS_TILE_FRAC
        val facingRight = player.facingRight
        val originX = if (facingRight) px + pw * 0.72f else px + pw * 0.28f
        val originY = py + ph * 0.38f
        val scale = tile / PLATFORMER_TILE_PX
        val vx = if (facingRight) LAUNCH_SPEED_X * scale else -LAUNCH_SPEED_X * scale
        val vy = LAUNCH_SPEED_Y * scale
        val projectile = PlatformerProjectile(
            id = nextId,
            trapId = PlatformerProjectile.PLAYER_TRAP_ID,
            x = originX - radius,
            y = originY - radius,
            vx = vx,
            vy = vy,
            source = PlatformerProjectileSource.PLAYER,
            shotKind = PlatformerPlayerShotKind.BASKETBALL,
            spinRad = 0f,
            bouncesLeft = MAX_BOUNCES,
            radius = radius,
        )
        return PlatformerRangedCombat.SpawnResult(
            player = player.copy(rangedProjectileSpawned = true),
            projectile = projectile,
        )
    }

    fun cooldownFraction(player: PlatformerPlayer): Float {
        if (player.rangedAnimSecLeft > 0f) return 0f
        return (player.rangedCooldownSecLeft / COOLDOWN_SEC).coerceIn(0f, 1f)
    }

    fun tickProjectiles(
        world: PlatformerWorld,
        projectiles: List<PlatformerProjectile>,
        dt: Float,
    ): List<PlatformerProjectile> {
        val tile = world.tileF
        val scale = tile / PLATFORMER_TILE_PX
        val gravity = GRAVITY * scale
        return projectiles.mapNotNull { proj ->
            if (!proj.alive || proj.shotKind != PlatformerPlayerShotKind.BASKETBALL) return@mapNotNull proj
            tickOne(world, proj, dt, tile, gravity)
        }
    }

    private fun tickOne(
        world: PlatformerWorld,
        proj: PlatformerProjectile,
        dt: Float,
        tile: Float,
        gravity: Float,
    ): PlatformerProjectile? {
        var x = proj.x
        var y = proj.y
        var vx = proj.vx
        var vy = proj.vy + gravity * dt
        var bounces = proj.bouncesLeft
        val spin = proj.spinRad + vx * SPIN_FACTOR * dt

        val r = proj.radius.coerceAtLeast(tile * BALL_RADIUS_TILE_FRAC * 0.85f)
        val diameter = r * 2f

        x += vx * dt
        y += vy * dt

        if (vy >= 0f) {
            val footCenterX = x + r
            val footY = y + diameter
            val tx = (footCenterX / tile).toInt().coerceIn(0, world.width - 1)
            val ty = (footY / tile).toInt().coerceIn(0, world.height - 1)
            val tileTop = ty * tile
            if (footY >= tileTop && isBounceSolid(world, tx, ty) &&
                footY <= tileTop + tile * 0.55f
            ) {
                y = tileTop - diameter
                vy = -max(abs(vy) * BOUNCE_RESTITUTION_Y, MIN_BOUNCE_VY)
                bounces--
            }
        }

        if (vy < 0f) {
            val headY = y
            val tx = ((x + r) / tile).toInt().coerceIn(0, world.width - 1)
            val ty = (headY / tile).toInt().coerceIn(0, world.height - 1)
            val tileBottom = (ty + 1) * tile
            if (headY <= tileBottom && isBounceSolid(world, tx, ty)) {
                y = tileBottom
                vy = abs(vy) * BOUNCE_RESTITUTION_Y
                bounces--
            }
        }

        val centerY = y + r
        if (vx > 0f) {
            val frontX = x + diameter
            val tx = (frontX / tile).toInt().coerceIn(0, world.width - 1)
            val ty = (centerY / tile).toInt().coerceIn(0, world.height - 1)
            if (isBounceSolid(world, tx, ty) && frontX >= tx * tile) {
                x = tx * tile - diameter
                vx = -abs(vx) * BOUNCE_RESTITUTION_X
                bounces--
            }
        } else if (vx < 0f) {
            val frontX = x
            val tx = (frontX / tile).toInt().coerceIn(0, world.width - 1)
            val ty = (centerY / tile).toInt().coerceIn(0, world.height - 1)
            if (isBounceSolid(world, tx, ty) && frontX <= (tx + 1) * tile) {
                x = (tx + 1) * tile
                vx = abs(vx) * BOUNCE_RESTITUTION_X
                bounces--
            }
        }

        if (bounces < 0 || x < -tile * 4f || x > world.width * tile + tile * 4f || y > world.height * tile + tile * 2f) {
            return null
        }
        if (y < -tile * 6f) return null

        return proj.copy(
            x = x,
            y = y,
            vx = vx,
            vy = vy,
            spinRad = spin,
            bouncesLeft = bounces,
        )
    }

    private fun isBounceSolid(world: PlatformerWorld, tx: Int, ty: Int): Boolean {
        return when (world.cellAt(tx, ty)) {
            PlatformerCell.SOLID,
            PlatformerCell.CRATE,
            PlatformerCell.PLATFORM,
            -> true
            else -> false
        }
    }

    fun projectileHitRadius(projectile: PlatformerProjectile, tilePx: Int): Float =
        projectile.radius.coerceAtLeast(tilePx * BALL_RADIUS_TILE_FRAC * 0.85f)
}
