package com.example.funlife.game.platformer

import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/** 空中跟随玩家、下蛋的天敌小鸡。 */
data class PlatformerSkyChick(
    val x: Float,
    val y: Float,
    val animPhase: Float = 0f,
    val eggCooldownSec: Float = INITIAL_EGG_DELAY_SEC,
    val dropFlashSec: Float = 0f,
    val facingRight: Boolean = Random.nextBoolean(),
    val turnCooldownSec: Float = randomTurnCooldown(),
) {
    companion object {
        const val INITIAL_EGG_DELAY_SEC = 2.2f

        fun randomTurnCooldown(): Float = 1.6f + Random.nextFloat() * 2.8f
    }
}

data class PlatformerSkyEgg(
    val id: Int,
    val x: Float,
    val y: Float,
    val vy: Float = 0f,
    val alive: Boolean = true,
)

object PlatformerSkyChickSystem {

    const val SHEET_COLS = 8
    const val SHEET_ROWS = 2
    /** 格宽倍数（显示宽）。 */
    const val CHICK_WIDTH_TILES = 1.35f
    const val HEIGHT_ABOVE_PLAYER_TILES = 11f
    private const val MIN_CLEARANCE_ABOVE_PLAYER_TILES = 9f
    private const val FOLLOW_X_LERP = 6.5f
    private const val FOLLOW_Y_LERP = 4.2f
    private const val EGG_INTERVAL_SEC = 2.6f
    private const val DROP_FLASH_SEC = 0.38f
    private const val EGG_GRAVITY = 1180f
    private const val ALIGN_TOLERANCE_TILES = 2.8f

    fun createInitial(world: PlatformerWorld): PlatformerSkyChick {
        val tile = world.tileF
        val pw = PlatformerPhysics.playerW(world.tilePx)
        val ph = PlatformerPhysics.playerH(world.tilePx)
        val px = world.player.x
        val py = world.player.y
        val w = chickWidth(world.tilePx)
        val h = chickHeight(world.tilePx)
        return PlatformerSkyChick(
            x = px + pw * 0.5f - w * 0.5f,
            y = py - tile * HEIGHT_ABOVE_PLAYER_TILES,
            facingRight = Random.nextBoolean(),
            eggCooldownSec = PlatformerSkyChick.INITIAL_EGG_DELAY_SEC,
            turnCooldownSec = PlatformerSkyChick.randomTurnCooldown(),
        ).let { adjustY(it, py, ph, tile, h) }
    }

    fun chickWidth(tilePx: Int): Float = tilePx * CHICK_WIDTH_TILES
    fun chickHeight(tilePx: Int): Float = tilePx * CHICK_WIDTH_TILES

    fun frameIndex(chick: PlatformerSkyChick): Int {
        if (chick.dropFlashSec > 0f) return 8
        return 1 + (chick.animPhase.toInt() % 7)
    }

    data class TickResult(
        val skyChick: PlatformerSkyChick?,
        val skyEggs: List<PlatformerSkyEgg>,
        val lethalSkyEggHit: Boolean,
    )

    fun tick(
        world: PlatformerWorld,
        playerX: Float,
        playerY: Float,
        playerW: Float,
        playerH: Float,
        dt: Float,
        time: Float,
        /** 玩家已阵亡时仍更新跟随/动画，但不再下蛋或造成碰撞。 */
        combatActive: Boolean = true,
    ): TickResult {
        val chick = world.skyChick ?: return TickResult(null, world.skyEggs, false)
        if (world.phase != PlatformerPhase.PLAYING && combatActive) {
            return TickResult(chick, world.skyEggs, false)
        }

        val tile = world.tileF
        val w = chickWidth(world.tilePx)
        val h = chickHeight(world.tilePx)
        val playerCx = playerX + playerW * 0.5f

        val targetX = playerCx - w * 0.5f
        val targetY = playerY - tile * HEIGHT_ABOVE_PLAYER_TILES
        var x = chick.x + (targetX - chick.x) * min(1f, dt * FOLLOW_X_LERP)
        var y = chick.y + (targetY - chick.y) * min(1f, dt * FOLLOW_Y_LERP)
        y += sin(time * 2.4f + chick.animPhase * 0.2f) * tile * 0.08f
        y = adjustY(chick.copy(y = y), playerY, playerH, tile, h).y

        var eggCooldown = chick.eggCooldownSec - dt
        var dropFlash = (chick.dropFlashSec - dt).coerceAtLeast(0f)
        val chickCx = x + w * 0.5f

        var eggs = tickEggs(world, dt, tile)

        if (combatActive) {
            val aligned = abs(chickCx - playerCx) <= tile * ALIGN_TOLERANCE_TILES
            var nextEggId = (eggs.maxOfOrNull { it.id } ?: 0) + 1

            if (eggCooldown <= 0f && aligned) {
                eggCooldown = EGG_INTERVAL_SEC
                dropFlash = DROP_FLASH_SEC
                val eggX = chickCx - tile * 0.12f
                val eggY = y + h * 0.82f
                eggs = eggs + PlatformerSkyEgg(
                    id = nextEggId++,
                    x = eggX,
                    y = eggY,
                    vy = 40f,
                )
            }
        }

        var turnCooldown = chick.turnCooldownSec - dt
        var facingRight = chick.facingRight
        if (turnCooldown <= 0f) {
            facingRight = Random.nextBoolean()
            turnCooldown = PlatformerSkyChick.randomTurnCooldown()
        }

        val updatedChick = chick.copy(
            x = x,
            y = y,
            animPhase = chick.animPhase + dt * 9f,
            eggCooldownSec = eggCooldown,
            dropFlashSec = dropFlash,
            facingRight = facingRight,
            turnCooldownSec = turnCooldown,
        )

        val lethal = if (combatActive) {
            eggs.any { egg ->
                egg.alive && eggHitsPlayer(egg, playerX, playerY, playerW, playerH, tile)
            }
        } else {
            false
        }
        if (lethal) {
            eggs = eggs.filter { egg ->
                !eggHitsPlayer(egg, playerX, playerY, playerW, playerH, tile)
            }
        }

        return TickResult(updatedChick, eggs.filter { it.alive }, lethal)
    }

    private fun adjustY(
        chick: PlatformerSkyChick,
        playerY: Float,
        playerH: Float,
        tile: Float,
        chickH: Float,
    ): PlatformerSkyChick {
        val minY = tile * 0.15f
        val maxY = playerY - tile * MIN_CLEARANCE_ABOVE_PLAYER_TILES - chickH * 0.15f
        return chick.copy(y = chick.y.coerceIn(minY, maxY.coerceAtLeast(minY)))
    }

    private fun tickEggs(world: PlatformerWorld, dt: Float, tile: Float): List<PlatformerSkyEgg> {
        val scale = tile / PLATFORMER_TILE_PX
        val gravity = EGG_GRAVITY * scale
        return world.skyEggs.mapNotNull { egg ->
            if (!egg.alive) return@mapNotNull null
            var vy = egg.vy + gravity * dt
            var y = egg.y + vy * dt
            val r = eggRadius(tile)
            val tx = ((egg.x + r) / tile).toInt().coerceIn(0, world.width - 1)
            val ty = ((y + r * 2f) / tile).toInt().coerceIn(0, world.height - 1)
            val cell = world.cellAt(tx, ty)
            if (cell == PlatformerCell.SOLID || cell == PlatformerCell.CRATE || cell == PlatformerCell.PLATFORM) {
                return@mapNotNull null
            }
            if (y > (world.height + 2) * tile) return@mapNotNull null
            egg.copy(y = y, vy = vy)
        }
    }

    fun eggRadius(tile: Float): Float = tile * 0.14f

    fun eggHitsPlayer(
        egg: PlatformerSkyEgg,
        px: Float,
        py: Float,
        pw: Float,
        ph: Float,
        tile: Float,
    ): Boolean {
        if (!egg.alive) return false
        val r = eggRadius(tile)
        val cx = egg.x + r
        val cy = egg.y + r
        val insetX = pw * 0.18f
        val insetY = ph * 0.18f
        return cx + r > px + insetX &&
            cx - r < px + pw - insetX &&
            cy + r > py + insetY &&
            cy - r < py + ph - insetY
    }

    fun withSkyHazard(world: PlatformerWorld): PlatformerWorld = world.copy(
        skyChick = createInitial(world),
        skyEggs = emptyList(),
        lethalSkyEggHit = false,
    )

    fun resetOnRespawn(world: PlatformerWorld): PlatformerWorld = world.copy(
        skyChick = createInitial(world),
        skyEggs = emptyList(),
        lethalSkyEggHit = false,
    )
}
