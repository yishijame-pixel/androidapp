package com.example.funlife.social.game.engine.pacmaze

import kotlin.math.abs
import kotlin.math.floor

/**
 * 商业级亚像素移动：速度积分、中心吸附、输入缓冲转向、圆形碰撞体。
 */
object PacMazeMotion {

    const val BODY_RADIUS = 0.34f
    private const val EPS = 1e-4f
    private val DT = 1f / PacMazeConstants.TICKS_PER_SECOND

    fun pacSpeedPerTick(): Float = PacMazeConstants.PAC_SPEED_CELLS_PER_SEC * DT

    fun ghostStepPerTick(speedCellsPerSec: Float): Float = speedCellsPerSec * DT

    fun centerX(anchorX: Float): Float = anchorX + 0.5f

    fun centerY(anchorY: Float): Float = anchorY + 0.5f

    fun tileX(anchorX: Float): Int = floor((centerX(anchorX) - EPS).toDouble()).toInt()

    fun tileY(anchorY: Float): Int = floor((centerY(anchorY) - EPS).toDouble()).toInt()

    /**
     * 经典 Pac-Man 玩家 tick：Float 速度积分 + 输入缓冲 + 中心吸附 + 圆形碰撞体。
     */
    fun tickPlayer(
        state: PacMazeWorldState,
        entity: PacMazeEntity,
        input: PacMazeInputState,
    ): PacMazeEntity {
        if (!input.active) {
            return snapToGrid(state, entity, forGhost = false).copy(
                direction = null,
                velX = 0f,
                velY = 0f,
                inputActive = false,
            )
        }

        var e = entity.copy(inputActive = true)
        val desired = input.queued ?: input.current
        if (desired != null) {
            e = e.copy(nextDirection = desired, facing = desired)
        }

        if (e.direction == null) {
            val startDir = e.nextDirection ?: desired
            if (startDir != null && canMoveInDir(state, e.x, e.y, startDir, forGhost = false)) {
                e = setMovement(e, startDir)
            } else {
                return e.copy(velX = 0f, velY = 0f)
            }
        }

        e = tryTurnAtIntersection(state, e)

        val dir = e.direction ?: return e.copy(velX = 0f, velY = 0f)

        e = applyCenterSnap(e, dir)

        val step = PacMazeConstants.PAC_SPEED_CELLS_PER_SEC * DT
        val (dx, dy) = dir.delta()
        val targetX = e.x + dx * step
        val targetY = e.y + dy * step
        val (newX, newY) = integrateWithBodyCollision(state, e.x, e.y, targetX, targetY, forGhost = false)

        val speed = PacMazeConstants.PAC_SPEED_CELLS_PER_SEC
        val moved = abs(newX - e.x) > EPS || abs(newY - e.y) > EPS
        return e.copy(
            x = newX,
            y = newY,
            velX = if (moved) dx * speed else 0f,
            velY = if (moved) dy * speed else 0f,
            direction = dir,
        )
    }

    /**
     * 幽灵 tick：贴墙时对齐车道并零速，避免颤抖。
     */
    fun tickGhost(
        state: PacMazeWorldState,
        entity: PacMazeEntity,
        dir: Direction,
        speedCellsPerSec: Float,
    ): PacMazeEntity {
        val step = speedCellsPerSec * DT
        if (!canAdvanceInto(state, entity.x, entity.y, dir, step, forGhost = true)) {
            return snapLaneCenter(entity, dir).copy(
                direction = dir,
                facing = dir,
                velX = 0f,
                velY = 0f,
                speed = speedCellsPerSec,
            )
        }

        var e = applyCenterSnap(entity, dir)
        val (dx, dy) = dir.delta()
        val targetX = e.x + dx * step
        val targetY = e.y + dy * step
        val (newX, newY) = integrateWithBodyCollision(state, e.x, e.y, targetX, targetY, forGhost = true)

        val moved = abs(newX - e.x) > EPS || abs(newY - e.y) > EPS
        if (!moved) {
            return snapLaneCenter(entity, dir).copy(
                direction = dir,
                facing = dir,
                velX = 0f,
                velY = 0f,
                speed = speedCellsPerSec,
            )
        }
        return e.copy(
            x = newX,
            y = newY,
            velX = dx * speedCellsPerSec,
            velY = dy * speedCellsPerSec,
            speed = speedCellsPerSec,
        )
    }

    /** 贴墙时将垂直于移动轴的坐标吸附到车道中心。 */
    fun snapLaneCenter(entity: PacMazeEntity, dir: Direction): PacMazeEntity =
        when (dir) {
            Direction.LEFT, Direction.RIGHT -> entity.copy(y = tileY(entity.y).toFloat())
            Direction.UP, Direction.DOWN -> entity.copy(x = tileX(entity.x).toFloat())
        }

    fun canMoveInDir(
        state: PacMazeWorldState,
        x: Float,
        y: Float,
        dir: Direction,
        forGhost: Boolean,
        probeStep: Float = if (forGhost) {
            PacMazeConstants.GHOST_SPEED_CELLS_PER_SEC * DT
        } else {
            PacMazeConstants.PAC_SPEED_CELLS_PER_SEC * DT
        },
    ): Boolean {
        val (dx, dy) = dir.delta()
        val (nx, ny) = integrateWithBodyCollision(state, x, y, x + dx * probeStep, y + dy * probeStep, forGhost)
        return abs(nx - x) > EPS || abs(ny - y) > EPS
    }

    /**
     * 幽灵仅在到达格子中心且处于路口时重新决策（每格一次，非每帧）。
     */
    fun isGhostDecisionPoint(
        state: PacMazeWorldState,
        entity: PacMazeEntity,
    ): Boolean {
        val dir = entity.direction ?: return false
        if (!isAlignedForTurn(entity, dir)) return false
        if (!isAtTileCenter(entity, dir)) return false
        val exits = Direction.entries.count { canMoveInDir(state, entity.x, entity.y, it, forGhost = true) }
        return exits >= 2
    }

    private fun isAtTileCenter(entity: PacMazeEntity, dir: Direction): Boolean {
        val eps = 0.045f
        return when (dir) {
            Direction.LEFT, Direction.RIGHT -> abs(entity.x - tileX(entity.x)) <= eps
            Direction.UP, Direction.DOWN -> abs(entity.y - tileY(entity.y)) <= eps
        }
    }

    /** @deprecated 使用 [isGhostDecisionPoint] */
    fun isAtTurnPoint(
        state: PacMazeWorldState,
        entity: PacMazeEntity,
        forGhost: Boolean,
    ): Boolean = if (forGhost) isGhostDecisionPoint(state, entity) else false

    fun snapToGrid(
        state: PacMazeWorldState,
        entity: PacMazeEntity,
        forGhost: Boolean,
    ): PacMazeEntity {
        val tx = tileX(entity.x)
        val ty = tileY(entity.y)
        val snapped = entity.copy(x = tx.toFloat(), y = ty.toFloat())
        return if (isPositionLegal(state, snapped.x, snapped.y, forGhost)) snapped else entity
    }

    fun sanitize(
        state: PacMazeWorldState,
        entity: PacMazeEntity,
        forGhost: Boolean,
    ): PacMazeEntity {
        if (isPositionLegal(state, entity.x, entity.y, forGhost)) return entity
        val tx = tileX(entity.x)
        val ty = tileY(entity.y)
        val anchored = entity.copy(x = tx.toFloat(), y = ty.toFloat())
        if (isPositionLegal(state, anchored.x, anchored.y, forGhost)) return anchored
        return findNearestLegalAnchor(state, tx, ty, forGhost)?.let { (ax, ay) ->
            entity.copy(x = ax, y = ay)
        } ?: entity
    }

    fun isPositionLegal(
        state: PacMazeWorldState,
        anchorX: Float,
        anchorY: Float,
        forGhost: Boolean,
    ): Boolean {
        val cx = centerX(anchorX)
        val cy = centerY(anchorY)
        val minTx = floor((cx - BODY_RADIUS).toDouble()).toInt()
        val maxTx = floor((cx + BODY_RADIUS - EPS).toDouble()).toInt()
        val minTy = floor((cy - BODY_RADIUS).toDouble()).toInt()
        val maxTy = floor((cy + BODY_RADIUS - EPS).toDouble()).toInt()
        for (ty in minTy..maxTy) {
            for (tx in minTx..maxTx) {
                if (!PacMazeRules.isWalkable(state, tx, ty, forGhost)) return false
            }
        }
        return true
    }

    fun canAdvanceInto(
        state: PacMazeWorldState,
        anchorX: Float,
        anchorY: Float,
        dir: Direction,
        step: Float,
        forGhost: Boolean,
    ): Boolean {
        val (dx, dy) = dir.delta()
        val (nx, ny) = integrateWithBodyCollision(
            state,
            anchorX,
            anchorY,
            anchorX + dx * step,
            anchorY + dy * step,
            forGhost,
        )
        return abs(nx - anchorX) > EPS || abs(ny - anchorY) > EPS
    }

    fun advanceAlong(
        state: PacMazeWorldState,
        entity: PacMazeEntity,
        dir: Direction,
        step: Float,
        forGhost: Boolean,
    ): PacMazeEntity {
        if (step <= 0f) return entity

        val (dx, dy) = dir.delta()
        val targetX = entity.x + dx * step
        val targetY = entity.y + dy * step
        val (newX, newY) = integrateWithBodyCollision(state, entity.x, entity.y, targetX, targetY, forGhost)

        if (abs(newX - entity.x) <= EPS && abs(newY - entity.y) <= EPS) {
            return entity.copy(direction = dir, facing = if (forGhost) dir else entity.facing)
        }

        return entity.copy(
            x = newX,
            y = newY,
            direction = dir,
            facing = if (forGhost) dir else entity.facing,
        )
    }

    /**
     * 沿移动方向积分；碰撞仅检测圆形碰撞体覆盖的格子（非全图扫描）。
     */
    fun integrateWithBodyCollision(
        state: PacMazeWorldState,
        x: Float,
        y: Float,
        nx: Float,
        ny: Float,
        forGhost: Boolean,
    ): Pair<Float, Float> {
        if (isPositionLegal(state, nx, ny, forGhost)) return nx to ny

        val dx = nx - x
        val dy = ny - y
        if (abs(dx) <= EPS && abs(dy) <= EPS) return x to y

        var low = 0f
        var high = 1f
        repeat(10) {
            val mid = (low + high) * 0.5f
            if (isPositionLegal(state, x + dx * mid, y + dy * mid, forGhost)) {
                low = mid
            } else {
                high = mid
            }
        }
        if (low <= EPS) return x to y
        return x + dx * low to y + dy * low
    }

    /** @deprecated 使用 [integrateWithBodyCollision] */
    fun integrateTileBoundary(
        state: PacMazeWorldState,
        x: Float,
        y: Float,
        nx: Float,
        ny: Float,
        forGhost: Boolean,
    ): Pair<Float, Float> = integrateWithBodyCollision(state, x, y, nx, ny, forGhost)

    fun lerpAnchor(start: Float, end: Float, blend: Float): Float = start + (end - start) * blend

    fun smoothBlend(raw: Float): Float {
        val t = raw.coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }

    private fun setMovement(entity: PacMazeEntity, dir: Direction): PacMazeEntity {
        val (dx, dy) = dir.delta()
        val speed = PacMazeConstants.PAC_SPEED_CELLS_PER_SEC
        return entity.copy(
            direction = dir,
            facing = dir,
            velX = dx * speed,
            velY = dy * speed,
        )
    }

    private fun isAlignedForTurn(entity: PacMazeEntity, currentDir: Direction): Boolean {
        val eps = PacMazeConstants.TURN_ALIGN_EPS
        return when (currentDir) {
            Direction.LEFT, Direction.RIGHT -> abs(entity.y - tileY(entity.y)) <= eps
            Direction.UP, Direction.DOWN -> abs(entity.x - tileX(entity.x)) <= eps
        }
    }

    private fun snapPerpendicularAxis(entity: PacMazeEntity, dir: Direction): PacMazeEntity =
        when (dir) {
            Direction.LEFT, Direction.RIGHT -> {
                val target = tileY(entity.y).toFloat()
                entity.copy(y = lerpAnchor(entity.y, target, PacMazeConstants.CENTER_SNAP_PULL))
            }
            Direction.UP, Direction.DOWN -> {
                val target = tileX(entity.x).toFloat()
                entity.copy(x = lerpAnchor(entity.x, target, PacMazeConstants.CENTER_SNAP_PULL))
            }
        }

    private fun applyCenterSnap(entity: PacMazeEntity, dir: Direction): PacMazeEntity {
        val eps = PacMazeConstants.CENTER_SNAP_EPS
        val pull = PacMazeConstants.CENTER_SNAP_PULL
        return when (dir) {
            Direction.LEFT, Direction.RIGHT -> {
                val ty = tileY(entity.y)
                val offset = entity.y - ty
                when {
                    abs(offset) <= eps -> entity.copy(y = ty.toFloat())
                    abs(offset) < 0.5f -> entity.copy(y = lerpAnchor(entity.y, ty.toFloat(), pull))
                    else -> entity
                }
            }
            Direction.UP, Direction.DOWN -> {
                val tx = tileX(entity.x)
                val offset = entity.x - tx
                when {
                    abs(offset) <= eps -> entity.copy(x = tx.toFloat())
                    abs(offset) < 0.5f -> entity.copy(x = lerpAnchor(entity.x, tx.toFloat(), pull))
                    else -> entity
                }
            }
        }
    }

    private fun tryTurnAtIntersection(
        state: PacMazeWorldState,
        entity: PacMazeEntity,
    ): PacMazeEntity {
        val next = entity.nextDirection ?: return entity
        val currentDir = entity.direction ?: return entity
        if (next == currentDir) return entity
        if (!isAlignedForTurn(entity, currentDir)) return entity
        if (!canMoveInDir(state, entity.x, entity.y, next, forGhost = false)) return entity

        var turned = snapPerpendicularAxis(entity, currentDir)
        turned = setMovement(turned, next)
        return turned.copy(nextDirection = next)
    }

    private fun findNearestLegalAnchor(
        state: PacMazeWorldState,
        originTx: Int,
        originTy: Int,
        forGhost: Boolean,
    ): Pair<Float, Float>? {
        for (radius in 0..3) {
            for (dy in -radius..radius) {
                for (dx in -radius..radius) {
                    if (abs(dx) != radius && abs(dy) != radius) continue
                    val tx = originTx + dx
                    val ty = originTy + dy
                    val ax = tx.toFloat()
                    val ay = ty.toFloat()
                    if (isPositionLegal(state, ax, ay, forGhost)) return ax to ay
                }
            }
        }
        return null
    }
}
