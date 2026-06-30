package com.example.funlife.game.platformer

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

enum class PlatformerTrapType {
    /** 周期性发射水平弹丸 */
    TURRET,
    /** 开关式激光束（横/竖） */
    LASER,
    /** 来回巡逻的致命块 */
    MOVING_SPIKE,
    /** 上下往复的压板 */
    CRUSHER,
}

enum class PlatformerTrapAxis {
    HORIZONTAL,
    VERTICAL,
}

data class PlatformerTrapSpawn(
    val tileX: Int,
    val tileY: Int,
    val type: PlatformerTrapType,
    val spanTiles: Int = 4,
    val axis: PlatformerTrapAxis = PlatformerTrapAxis.HORIZONTAL,
    val cycleSec: Float = 2.4f,
    val phaseOffset: Float = 0f,
    val facingRight: Boolean = true,
)

data class PlatformerProjectile(
    val id: Int,
    val trapId: Int,
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float = 0f,
    val alive: Boolean = true,
    val source: PlatformerProjectileSource = PlatformerProjectileSource.TRAP,
    val shotKind: PlatformerPlayerShotKind = PlatformerPlayerShotKind.BULLET,
    val spinRad: Float = 0f,
    val bouncesLeft: Int = 0,
    val radius: Float = 0f,
) {
    companion object {
        const val PLAYER_TRAP_ID = -1
    }
}

data class PlatformerTrap(
    val id: Int,
    val type: PlatformerTrapType,
    val x: Float,
    val y: Float,
    val span: Float,
    val axis: PlatformerTrapAxis,
    val cycleSec: Float,
    val phaseOffset: Float,
    val facingRight: Boolean = true,
    val patrolLeft: Float = 0f,
    val patrolRight: Float = 0f,
    val animPhase: Float = 0f,
    val fireCooldown: Float = 0f,
)

object PlatformerTrapSystem {

    private const val TURRET_FIRE_INTERVAL = 2.2f
    private const val PROJECTILE_SPEED = 210f
    private const val LASER_ON_RATIO = 0.55f

    fun spawnFrom(
        level: PlatformerLevelDef,
        tilePx: Int = PLATFORMER_TILE_PX,
    ): List<PlatformerTrap> {
        val tile = tilePx.toFloat()
        return level.trapSpawns.mapIndexed { index, spawn ->
            val x = spawn.tileX * tile
            val y = spawn.tileY * tile
            val span = spawn.spanTiles.coerceAtLeast(1) * tile
            val patrol = span * 0.45f
            PlatformerTrap(
                id = index,
                type = spawn.type,
                x = x,
                y = y,
                span = span,
                axis = spawn.axis,
                cycleSec = spawn.cycleSec,
                phaseOffset = spawn.phaseOffset,
                facingRight = spawn.facingRight,
                patrolLeft = if (spawn.type == PlatformerTrapType.CRUSHER) y else x - patrol / 2f,
                patrolRight = x + patrol / 2f,
            )
        }
    }

    fun spawnSingle(spawn: PlatformerTrapSpawn, id: Int, tilePx: Int = PLATFORMER_TILE_PX): PlatformerTrap {
        val level = PlatformerLevelDef(
            id = 0,
            title = "",
            subtitle = "",
            theme = PlatformerTheme.GRASS,
            rows = emptyList(),
            skyTop = 0,
            skyBottom = 0,
            trapSpawns = listOf(spawn),
        )
        return spawnFrom(level, tilePx).first().copy(id = id)
    }

    fun tick(
        traps: List<PlatformerTrap>,
        projectiles: List<PlatformerProjectile>,
        dt: Float,
        time: Float,
        tilePx: Int,
    ): Pair<List<PlatformerTrap>, List<PlatformerProjectile>> {
        val tile = tilePx.toFloat()
        var nextProjId = (projectiles.maxOfOrNull { it.id } ?: -1) + 1
        val updatedTraps = traps.map { trap ->
            var t = trap.copy(animPhase = trap.animPhase + dt * 5f)
            when (trap.type) {
                PlatformerTrapType.MOVING_SPIKE -> {
                    val w = tile * 0.55f
                    var x = t.x + (if (t.facingRight) 1f else -1f) * 46f * dt
                    var facing = t.facingRight
                    if (x <= t.patrolLeft) {
                        x = t.patrolLeft
                        facing = true
                    } else if (x >= t.patrolRight - w) {
                        x = t.patrolRight - w
                        facing = false
                    }
                    t.copy(x = x, facingRight = facing)
                }
                PlatformerTrapType.CRUSHER -> {
                    val bob = kotlin.math.sin(time * 2.8f + t.id) * tile * 0.45f
                    t.copy(y = t.patrolLeft + bob)
                }
                PlatformerTrapType.TURRET -> {
                    var cd = (t.fireCooldown - dt).coerceAtLeast(0f)
                    t.copy(fireCooldown = cd)
                }
                else -> t
            }
        }.toMutableList()

        val newProjectiles = projectiles.mapNotNull { p ->
            if (!p.alive) return@mapNotNull null
            if (p.shotKind == PlatformerPlayerShotKind.BASKETBALL) return@mapNotNull p
            val nx = p.x + p.vx * dt
            val vy = if (p.source == PlatformerProjectileSource.PLAYER &&
                p.shotKind == PlatformerPlayerShotKind.KUNAI
            ) {
                p.vy + 280f * dt
            } else {
                p.vy
            }
            val ny = p.y + vy * dt
            if (nx < -tile * 2f || nx > 12000f || ny < -tile * 4f || ny > 8000f) {
                null
            } else {
                p.copy(x = nx, y = ny, vy = vy)
            }
        }.toMutableList()

        updatedTraps.forEach { trap ->
            if (trap.type != PlatformerTrapType.TURRET) return@forEach
            if (trap.fireCooldown > 0f) return@forEach
            val idx = updatedTraps.indexOfFirst { it.id == trap.id }
            if (idx < 0) return@forEach
            updatedTraps[idx] = trap.copy(fireCooldown = TURRET_FIRE_INTERVAL)
            val vx = if (trap.facingRight) PROJECTILE_SPEED else -PROJECTILE_SPEED
            newProjectiles += PlatformerProjectile(
                id = nextProjId++,
                trapId = trap.id,
                x = trap.x + tile * 0.35f,
                y = trap.y + tile * 0.25f,
                vx = vx,
            )
        }

        return updatedTraps to newProjectiles
    }

    fun laserActive(trap: PlatformerTrap, time: Float): Boolean {
        val phase = ((time + trap.phaseOffset) % trap.cycleSec) / trap.cycleSec
        return phase < LASER_ON_RATIO
    }

    fun hitsPlayer(
        trap: PlatformerTrap,
        px: Float,
        py: Float,
        pw: Float,
        ph: Float,
        tilePx: Int,
        time: Float,
    ): Boolean {
        val tile = tilePx.toFloat()
        val insetX = pw * 0.15f
        val insetY = ph * 0.15f
        val pl = px + insetX
        val pr = px + pw - insetX
        val pt = py + insetY
        val pb = py + ph - insetY
        return when (trap.type) {
            PlatformerTrapType.LASER -> {
                if (!laserActive(trap, time)) return false
                if (trap.axis == PlatformerTrapAxis.HORIZONTAL) {
                    val beamY = trap.y + tile * 0.45f
                    val beamH = tile * 0.12f
                    pr > trap.x && pl < trap.x + trap.span &&
                        pb > beamY - beamH && pt < beamY + beamH
                } else {
                    val beamX = trap.x + tile * 0.45f
                    val beamW = tile * 0.12f
                    pb > trap.y && pt < trap.y + trap.span &&
                        pr > beamX - beamW && pl < beamX + beamW
                }
            }
            PlatformerTrapType.MOVING_SPIKE, PlatformerTrapType.CRUSHER -> {
                val ew = tile * 0.62f
                val eh = tile * 0.55f
                pr > trap.x && pl < trap.x + ew && pb > trap.y && pt < trap.y + eh
            }
            PlatformerTrapType.TURRET -> false
        }
    }

    fun projectileHitsPlayer(
        projectile: PlatformerProjectile,
        px: Float,
        py: Float,
        pw: Float,
        ph: Float,
        tilePx: Int,
    ): Boolean {
        if (!projectile.alive || projectile.source == PlatformerProjectileSource.PLAYER) return false
        val r = tilePx * 0.22f
        val cx = px + pw / 2f
        val cy = py + ph / 2f
        val dx = cx - (projectile.x + r)
        val dy = cy - (projectile.y + r)
        return dx * dx + dy * dy < (r + pw * 0.35f) * (r + pw * 0.35f)
    }

    /** 弹丸与实心格碰撞则销毁 */
    fun filterProjectiles(
        world: PlatformerWorld,
        projectiles: List<PlatformerProjectile>,
    ): List<PlatformerProjectile> {
        val tile = world.tileF
        return projectiles.map { p ->
            if (!p.alive) return@map p
            if (p.shotKind == PlatformerPlayerShotKind.BASKETBALL) return@map p
            val tx = ((p.x + tile * 0.2f) / tile).toInt()
            val ty = ((p.y + tile * 0.2f) / tile).toInt()
            val cell = world.cellAt(tx, ty)
            if (cell == PlatformerCell.SOLID || cell == PlatformerCell.CRATE) {
                p.copy(alive = false)
            } else {
                p
            }
        }.filter { it.alive }
    }

    /** 弹丸命中玩家后移除（不再继续飞行/重复判定）。 */
    fun removeProjectilesHittingPlayer(
        projectiles: List<PlatformerProjectile>,
        px: Float,
        py: Float,
        pw: Float,
        ph: Float,
        tilePx: Int,
    ): List<PlatformerProjectile> =
        projectiles.filter { p ->
            !projectileHitsPlayer(p, px, py, pw, ph, tilePx)
        }
}
