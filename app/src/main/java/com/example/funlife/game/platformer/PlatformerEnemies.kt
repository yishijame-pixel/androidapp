package com.example.funlife.game.platformer

import com.example.funlife.game.platformer.catalog.PlatformerEnemyCatalog
import kotlin.math.abs
import kotlin.math.sin

enum class PlatformerEnemyType {
    SLIME,
    MUSHROOM,
    BAT,
    GHOST,
    CHICKEN,
    SNAIL,
    BLUE_BIRD,
    SKULL,
}

enum class PlatformerEnemyBehavior {
    PATROL,
    FLY,
    FLOAT,
}

data class PlatformerEnemySpawn(
    val tileX: Int,
    val tileY: Int,
    val type: PlatformerEnemyType,
    val patrolTiles: Int = 4,
    val catalogId: String? = null,
)

data class PlatformerEnemy(
    val id: Int,
    val type: PlatformerEnemyType,
    val behavior: PlatformerEnemyBehavior,
    val x: Float,
    val y: Float,
    val vx: Float = 0f,
    val vy: Float = 0f,
    val patrolLeft: Float,
    val patrolRight: Float,
    val baseY: Float,
    val facingRight: Boolean = true,
    val animPhase: Float = 0f,
    val alive: Boolean = true,
    val catalogId: String? = null,
)

object PlatformerEnemySystem {

    private const val PATROL_SPEED = 52f
    private const val FLY_SPEED_X = 42f
    private const val FLY_AMP = 14f

    fun spawnFrom(
        level: PlatformerLevelDef,
        tilePx: Int = PLATFORMER_TILE_PX,
        world: PlatformerWorld? = null,
    ): List<PlatformerEnemy> {
        val tile = tilePx.toFloat()
        return level.enemySpawns.mapIndexed { index, spawn ->
            val behavior = spawn.catalogId?.let { PlatformerEnemyCatalog.behaviorFor(it) }
                ?: behaviorFor(spawn.type)
            val h = height(spawn.type, tilePx, spawn.catalogId)
            val x = spawn.tileX * tile
            val y = if (world != null) {
                when (behavior) {
                    PlatformerEnemyBehavior.FLY, PlatformerEnemyBehavior.FLOAT ->
                        PlatformerEnemyCollision.findHoverY(world, spawn.tileX, spawn.tileY, h)
                    else ->
                        PlatformerEnemyCollision.findStandY(world, spawn.tileX, spawn.tileY, h)
                }
            } else {
                when (behavior) {
                    PlatformerEnemyBehavior.FLY, PlatformerEnemyBehavior.FLOAT ->
                        spawn.tileY * tile - h * 0.55f
                    else ->
                        spawn.tileY * tile - h
                }
            }
            val patrol = spawn.patrolTiles.coerceAtLeast(1) * tile
            var patrolLeft = x - patrol / 2f
            var patrolRight = x + patrol / 2f
            if (world != null && behavior == PlatformerEnemyBehavior.PATROL) {
                val ew = width(spawn.type, tilePx, spawn.catalogId)
                val bounds = PlatformerEnemyCollision.computePatrolBounds(world, x, y, ew, h, patrol)
                patrolLeft = bounds.first
                patrolRight = bounds.second
            }
            PlatformerEnemy(
                id = index,
                type = spawn.type,
                behavior = behavior,
                x = x,
                y = y,
                patrolLeft = patrolLeft,
                patrolRight = patrolRight,
                baseY = y,
                facingRight = true,
                catalogId = spawn.catalogId,
            )
        }
    }

    fun spawnSingle(world: PlatformerWorld, spawn: PlatformerEnemySpawn, id: Int): PlatformerEnemy {
        val level = PlatformerLevelDef(
            id = 0,
            title = "",
            subtitle = "",
            theme = PlatformerTheme.GRASS,
            rows = emptyList(),
            skyTop = 0,
            skyBottom = 0,
            enemySpawns = listOf(spawn),
        )
        return spawnFrom(level, world.tilePx, world).first().copy(id = id)
    }

    fun tick(
        world: PlatformerWorld,
        enemies: List<PlatformerEnemy>,
        dt: Float,
        time: Float,
    ): List<PlatformerEnemy> {
        val tilePx = world.tilePx
        return enemies.map { e ->
            if (!e.alive) return@map e
            when (e.behavior) {
                PlatformerEnemyBehavior.PATROL -> tickPatrol(world, e, dt, tilePx)
                PlatformerEnemyBehavior.FLY -> tickFly(world, e, dt, time, tilePx)
                PlatformerEnemyBehavior.FLOAT -> tickFloat(e, dt, time)
            }
        }
    }

    private fun tickPatrol(
        world: PlatformerWorld,
        e: PlatformerEnemy,
        dt: Float,
        tilePx: Int,
    ): PlatformerEnemy {
        val w = width(e.type, tilePx, e.catalogId)
        val h = height(e.type, tilePx, e.catalogId)
        var vx = if (e.vx == 0f) PATROL_SPEED else e.vx
        var facing = e.facingRight
        if (!facing) vx = -abs(vx) else vx = abs(vx)

        var x = e.x + vx * dt
        var y = e.y

        if (x < e.patrolLeft) {
            x = e.patrolLeft
            vx = abs(vx)
            facing = true
        } else if (x > e.patrolRight - w) {
            x = e.patrolRight - w
            vx = -abs(vx)
            facing = false
        }

        if (PlatformerEnemyCollision.isLedgeAhead(world, x, y, w, h, facing)) {
            vx = -vx
            facing = !facing
            x = e.x
        } else if (!PlatformerEnemyCollision.canStandAt(world, x, y, w, h)) {
            vx = -vx
            facing = !facing
            x = e.x
        }

        val (resolvedX, hitWall) = PlatformerEnemyCollision.resolveHorizontal(world, x, y, w, h)
        if (hitWall) {
            vx = -vx
            facing = !facing
            x = resolvedX
        }

        y = PlatformerEnemyCollision.snapToGround(world, x, y, w, h)
        val depen = PlatformerEnemyCollision.depenetrate(world, x, y, w, h)
        x = depen.first
        y = depen.second
        y = PlatformerEnemyCollision.snapToGround(world, x, y, w, h)

        return e.copy(
            x = x,
            y = y,
            vx = vx,
            facingRight = facing,
            baseY = y,
            animPhase = e.animPhase + dt * 7f,
        )
    }

    private fun tickFly(
        world: PlatformerWorld,
        e: PlatformerEnemy,
        dt: Float,
        time: Float,
        tilePx: Int,
    ): PlatformerEnemy {
        val w = width(e.type, tilePx, e.catalogId)
        val h = height(e.type, tilePx, e.catalogId)
        var facing = e.facingRight
        var x = e.x + (if (facing) 1f else -1f) * FLY_SPEED_X * dt

        if (x <= e.patrolLeft) {
            x = e.patrolLeft
            facing = true
        } else if (x >= e.patrolRight - w) {
            x = e.patrolRight - w
            facing = false
        }

        val (resolvedX, hitWall) = PlatformerEnemyCollision.resolveHorizontal(world, x, e.y, w, h)
        if (hitWall) {
            facing = !facing
            x = resolvedX
        }

        val bob = sin(time * 2.6f + e.id * 0.9f) * FLY_AMP
        return e.copy(
            x = x,
            y = e.baseY + bob,
            facingRight = facing,
            animPhase = e.animPhase + dt * 5.5f,
        )
    }

    private fun tickFloat(e: PlatformerEnemy, dt: Float, time: Float): PlatformerEnemy {
        val bob = sin(time * 2.2f + e.id * 0.7f) * 12f
        return e.copy(y = e.baseY + bob, animPhase = e.animPhase + dt * 4f)
    }

    fun hitsPlayer(
        enemy: PlatformerEnemy,
        px: Float,
        py: Float,
        pw: Float,
        ph: Float,
        tilePx: Int = PLATFORMER_TILE_PX,
    ): Boolean {
        if (!enemy.alive) return false
        val ew = width(enemy.type, tilePx, enemy.catalogId)
        val eh = height(enemy.type, tilePx, enemy.catalogId)
        val insetX = pw * 0.2f
        val insetY = ph * 0.2f
        return px + insetX < enemy.x + ew &&
            px + pw - insetX > enemy.x &&
            py + insetY < enemy.y + eh &&
            py + ph - insetY > enemy.y
    }

    fun stompDefeats(
        enemy: PlatformerEnemy,
        px: Float,
        py: Float,
        pw: Float,
        ph: Float,
        playerVy: Float,
        tilePx: Int = PLATFORMER_TILE_PX,
    ): Boolean {
        if (!enemy.alive || playerVy <= 0f) return false
        val feet = py + ph
        val ew = width(enemy.type, tilePx, enemy.catalogId)
        val eh = height(enemy.type, tilePx, enemy.catalogId)
        val footCx = px + pw / 2f
        val tile = tilePx.toFloat()
        return footCx in enemy.x..(enemy.x + ew) &&
            feet in enemy.y..(enemy.y + eh * 0.35f + tile * 0.05f)
    }

    fun width(type: PlatformerEnemyType, tilePx: Int = PLATFORMER_TILE_PX, catalogId: String? = null): Float {
        if (catalogId != null) return PlatformerEnemyCatalog.width(catalogId, type, tilePx)
        return when (type) {
        PlatformerEnemyType.SKULL -> tilePx * 0.9f
        PlatformerEnemyType.SNAIL -> tilePx * 0.65f
        PlatformerEnemyType.BAT -> tilePx * 0.7f
        else -> tilePx * 0.72f
        }
    }

    fun height(type: PlatformerEnemyType, tilePx: Int = PLATFORMER_TILE_PX, catalogId: String? = null): Float {
        if (catalogId != null) return PlatformerEnemyCatalog.height(catalogId, type, tilePx)
        return when (type) {
        PlatformerEnemyType.SKULL -> tilePx * 0.95f
        PlatformerEnemyType.SNAIL -> tilePx * 0.55f
        PlatformerEnemyType.CHICKEN -> tilePx * 0.82f
        PlatformerEnemyType.BAT -> tilePx * 0.62f
        else -> tilePx * 0.72f
        }
    }

    private fun behaviorFor(type: PlatformerEnemyType): PlatformerEnemyBehavior = when (type) {
        PlatformerEnemyType.BAT,
        PlatformerEnemyType.GHOST,
        PlatformerEnemyType.BLUE_BIRD,
        -> PlatformerEnemyBehavior.FLY
        PlatformerEnemyType.SKULL -> PlatformerEnemyBehavior.FLOAT
        else -> PlatformerEnemyBehavior.PATROL
    }
}
