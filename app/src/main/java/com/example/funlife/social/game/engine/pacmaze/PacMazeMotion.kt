package com.example.funlife.social.game.engine.pacmaze

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.hypot

/**
 * 商业级亚像素移动：速度积分、中心吸附、输入缓冲转向、圆形碰撞体。
 */
object PacMazeMotion {

    /** 圆形碰撞体半径（格）；与默认绘制半径 [PacMazeEntityComfortScale] 对齐。 */
    const val BODY_RADIUS = 0.38f

    /** 单格通道允许的最大碰撞半径；超出则无法通过走廊。 */
    const val MAX_CORRIDOR_BODY_RADIUS = 0.494f

    private fun resolveBodyRadius(forGhost: Boolean, bodyRadius: Float): Float =
        if (forGhost) BODY_RADIUS else bodyRadius

    /** 贴墙时回退皮肤厚度，避免浮点探入墙格。 */
    private const val COLLISION_SKIN = 0.006f
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
        input: PacMazeTickInput,
        movementMode: PacMazeMovementMode = PacMazeMovementMode.AUTO,
        cosmeticSpeedMultiplier: Float = 1f,
        passRadius: Float = BODY_RADIUS,
    ): PacMazeEntity {
        if (!input.active) {
            return if (movementMode == PacMazeMovementMode.AUTO) {
                tickPlayerCoast(state, entity, cosmeticSpeedMultiplier, passRadius)
            } else {
                entity.copy(
                    inputActive = false,
                    velX = 0f,
                    velY = 0f,
                    direction = null,
                )
            }
        }

        var e = entity.copy(inputActive = true)

        when (input.mode) {
            PacMazeInputMode.DeadZone -> {
                if (movementMode == PacMazeMovementMode.MANUAL) {
                    return e.copy(velX = 0f, velY = 0f, direction = null, nextDirection = null)
                }
                val coastDir = e.direction
                    ?: return e.copy(velX = 0f, velY = 0f, inputActive = true)
                return integratePlayerMovement(
                    state,
                    e,
                    coastDir,
                    desiredForRetry = e.nextDirection,
                    cosmeticSpeedMultiplier = cosmeticSpeedMultiplier,
                    passRadius = passRadius,
                )
            }
            PacMazeInputMode.Spin -> {
                val facing = input.facing ?: e.facing
                e = e.copy(facing = facing, nextDirection = facing)
                val coastDir = e.direction
                if (coastDir != null) {
                    return integratePlayerMovement(
                        state,
                        e,
                        coastDir,
                        desiredForRetry = facing,
                        cosmeticSpeedMultiplier = cosmeticSpeedMultiplier,
                        passRadius = passRadius,
                    )
                }
                return e.copy(velX = 0f, velY = 0f)
            }
            PacMazeInputMode.Committed, PacMazeInputMode.Pending, PacMazeInputMode.Idle -> Unit
        }

        val desired = input.desired
        if (desired != null) {
            e = applyDesiredDirection(state, e, desired, passRadius)
        }

        if (e.direction == null) {
            val startDir = desired ?: e.nextDirection
            if (startDir != null) {
                e = tryStartMovement(state, e, startDir, passRadius)
                if (e.direction == null) {
                    return e.copy(velX = 0f, velY = 0f)
                }
            } else {
                return e.copy(velX = 0f, velY = 0f)
            }
        }

        e = tryTurnAtIntersection(state, e, preemptive = true, passRadius = passRadius)

        val dir = e.direction ?: return e.copy(velX = 0f, velY = 0f)
        return integratePlayerMovement(
            state,
            e,
            dir,
            desiredForRetry = desired,
            cosmeticSpeedMultiplier = cosmeticSpeedMultiplier,
            passRadius = passRadius,
        )
    }

    /** 手指离开摇杆：经典 Pac 惯性滑行，直到撞墙或方向被清空。 */
    private fun tickPlayerCoast(
        state: PacMazeWorldState,
        entity: PacMazeEntity,
        cosmeticSpeedMultiplier: Float,
        passRadius: Float,
    ): PacMazeEntity {
        val dir = entity.direction
            ?: return entity.copy(
                inputActive = false,
                velX = 0f,
                velY = 0f,
            )
        val coasting = integratePlayerMovement(
            state,
            entity.copy(inputActive = false),
            dir,
            desiredForRetry = entity.nextDirection,
            cosmeticSpeedMultiplier = cosmeticSpeedMultiplier,
            passRadius = passRadius,
        )
        if (hypot(coasting.velX.toDouble(), coasting.velY.toDouble()) < PacMazeEntityVisuals.VEL_EPS) {
            return coasting.copy(direction = null, velX = 0f, velY = 0f)
        }
        return coasting
    }

    private fun applyDesiredDirection(
        state: PacMazeWorldState,
        entity: PacMazeEntity,
        desired: Direction,
        passRadius: Float,
    ): PacMazeEntity {
        var e = entity.copy(nextDirection = desired)
        val canGo = canMoveInDir(state, e.x, e.y, desired, forGhost = false, bodyRadius = passRadius)
        when {
            e.direction == desired -> e = e.copy(facing = desired)
            e.direction == null && canGo -> e = e.copy(facing = desired)
            else -> e = e.copy(facing = desired)
        }
        if (e.direction == null && canGo) {
            e = tryStartMovement(
                state,
                e.copy(nextDirection = desired, facing = desired),
                desired,
                passRadius,
            )
        }
        if (e.direction != null && desired != e.direction) {
            e = tryTurnAtIntersection(state, e, preemptive = true, passRadius = passRadius)
            val currentDir = e.direction
            if (currentDir != null && e.direction != desired &&
                isOpenTurnPoint(state, e, passRadius) &&
                canMoveInDir(state, e.x, e.y, desired, forGhost = false, bodyRadius = passRadius)
            ) {
                val aligned = isAlignedForTurn(e, currentDir, PacMazeConstants.TURN_PREEMPT_EPS)
                if (aligned) {
                    var turned = snapPerpendicularAxis(e, currentDir)
                    turned = setMovement(turned, desired)
                    e = turned.copy(nextDirection = desired)
                }
            }
        }
        return e
    }

    private fun integratePlayerMovement(
        state: PacMazeWorldState,
        entity: PacMazeEntity,
        dir: Direction,
        desiredForRetry: Direction?,
        cosmeticSpeedMultiplier: Float,
        passRadius: Float,
    ): PacMazeEntity {
        var e = applyCenterSnap(entity, dir)

        val speedMul = PacMazeItems.pacSpeedMultiplier(state) *
            cosmeticSpeedMultiplier.coerceIn(0.72f, 1.12f)
        val step = PacMazeConstants.PAC_SPEED_CELLS_PER_SEC * DT * speedMul
        val (dx, dy) = dir.delta()
        val targetX = e.x + dx * step
        val targetY = e.y + dy * step
        var (newX, newY) = integrateWithBodyCollision(
            state, e.x, e.y, targetX, targetY, forGhost = false, bodyRadius = passRadius,
        )
        var moved = abs(newX - e.x) > EPS || abs(newY - e.y) > EPS

        if (!moved) {
            e = snapLaneCenterSafe(state, e, dir, forGhost = false)
            val (retryX, retryY) = integrateWithBodyCollision(
                state,
                e.x,
                e.y,
                e.x + dx * step,
                e.y + dy * step,
                forGhost = false,
                bodyRadius = passRadius,
            )
            if (abs(retryX - e.x) > EPS || abs(retryY - e.y) > EPS) {
                newX = retryX
                newY = retryY
                moved = true
            }
        }

        if (!moved && desiredForRetry != null && desiredForRetry != dir &&
            canMoveInDir(state, e.x, e.y, desiredForRetry, forGhost = false, bodyRadius = passRadius)
        ) {
            val turned = tryTurnAtIntersection(
                state,
                snapLaneCenterSafe(state, e, dir, forGhost = false),
                preemptive = false,
                passRadius = passRadius,
            )
            if (turned.direction == desiredForRetry) {
                return integratePlayerMovement(
                    state,
                    turned,
                    desiredForRetry,
                    desiredForRetry = null,
                    cosmeticSpeedMultiplier = cosmeticSpeedMultiplier,
                    passRadius = passRadius,
                )
            }
        }

        val speed = PacMazeConstants.PAC_SPEED_CELLS_PER_SEC * speedMul
        val displayFacing = if (moved) {
            dir
        } else {
            PacMazeEntityVisuals.travelFacing(e) ?: dir
        }
        return clampEntityToLegal(
            state,
            e.copy(
                x = newX,
                y = newY,
                velX = if (moved) dx * speed else 0f,
                velY = if (moved) dy * speed else 0f,
                direction = if (moved) dir else e.direction,
                facing = displayFacing,
            ),
            forGhost = false,
            bodyRadius = passRadius,
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
        if (!canAdvanceInto(state, entity.x, entity.y, dir, step, forGhost = true, ghost = entity)) {
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
        val (newX, newY) = integrateWithBodyCollision(state, e.x, e.y, targetX, targetY, forGhost = true, ghost = entity)

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

        var phaseCooldown = entity.phaseWalkCooldownTicksLeft
        if (entity.ghostSpecialty == GhostSpecialty.PHASE_WALKER && moved) {
            val tx = tileX(newX)
            val ty = tileY(newY)
            if (state.tileAt(tx, ty) == TileType.DYNAMIC_WALL &&
                !PacMazeMapDynamics.isDynamicStripeOpen(state, tx, ty)
            ) {
                phaseCooldown = PacMazeGhostRoster.PHASE_WALK_COOLDOWN_TICKS
            }
        }

        return e.copy(
            x = newX,
            y = newY,
            direction = dir,
            facing = dir,
            velX = dx * speedCellsPerSec,
            velY = dy * speedCellsPerSec,
            speed = speedCellsPerSec,
            phaseWalkCooldownTicksLeft = phaseCooldown,
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
        ghost: PacMazeEntity? = null,
        probeStep: Float = if (forGhost) {
            PacMazeConstants.GHOST_SPEED_CELLS_PER_SEC * DT
        } else {
            PacMazeConstants.PAC_SPEED_CELLS_PER_SEC * DT
        },
        bodyRadius: Float = BODY_RADIUS,
    ): Boolean {
        val (dx, dy) = dir.delta()
        val radius = resolveBodyRadius(forGhost, bodyRadius)
        val (nx, ny) = integrateWithBodyCollision(
            state, x, y, x + dx * probeStep, y + dy * probeStep, forGhost, ghost, radius,
        )
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
        val exits = Direction.entries.count { canMoveInDir(state, entity.x, entity.y, it, forGhost = true, ghost = entity) }
        // 经典 Pac：仅在三岔及以上路口重新决策，直道不每格摇摆
        return exits >= 3
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
        val ghost = if (forGhost) entity else null
        return if (isPositionLegal(state, snapped.x, snapped.y, forGhost, ghost)) snapped else entity
    }

    fun sanitize(
        state: PacMazeWorldState,
        entity: PacMazeEntity,
        forGhost: Boolean,
        bodyRadius: Float = BODY_RADIUS,
    ): PacMazeEntity {
        val ghost = if (forGhost) entity else null
        val radius = resolveBodyRadius(forGhost, bodyRadius)
        if (isPositionLegal(state, entity.x, entity.y, forGhost, ghost, radius)) return entity
        val tx = tileX(entity.x)
        val ty = tileY(entity.y)
        val anchored = entity.copy(x = tx.toFloat(), y = ty.toFloat())
        if (isPositionLegal(state, anchored.x, anchored.y, forGhost, ghost, radius)) return anchored
        findNearestLegalAnchor(state, tx, ty, forGhost, ghost, radius)?.let { (ax, ay) ->
            return entity.copy(x = ax, y = ay, velX = 0f, velY = 0f)
        }
        return clampEntityToLegal(
            state,
            entity.copy(velX = 0f, velY = 0f, direction = null),
            forGhost,
            ghost,
            radius,
        )
    }

    /**
     * 将实体锚点约束到最近合法位置；用于每帧移动后的硬边界兜底。
     */
    fun clampEntityToLegal(
        state: PacMazeWorldState,
        entity: PacMazeEntity,
        forGhost: Boolean,
        ghost: PacMazeEntity? = if (forGhost) entity else null,
        bodyRadius: Float = BODY_RADIUS,
    ): PacMazeEntity {
        val radius = resolveBodyRadius(forGhost, bodyRadius)
        if (isPositionLegal(state, entity.x, entity.y, forGhost, ghost, radius)) return entity

        val originTx = tileX(entity.x).coerceIn(0, state.width - 1)
        val originTy = tileY(entity.y).coerceIn(0, state.height - 1)
        findNearestLegalAnchor(state, originTx, originTy, forGhost, ghost, radius)?.let { (ax, ay) ->
            return copyWithClampCorrection(entity, ax, ay)
        }

        var low = 0f
        var high = 1f
        val targetX = originTx.toFloat()
        val targetY = originTy.toFloat()
        repeat(12) {
            val mid = (low + high) * 0.5f
            val testX = entity.x + (targetX - entity.x) * mid
            val testY = entity.y + (targetY - entity.y) * mid
            if (isPositionLegal(state, testX, testY, forGhost, ghost, radius)) {
                low = mid
            } else {
                high = mid
            }
        }
        if (low <= EPS) {
            return entity.copy(
                x = targetX,
                y = targetY,
                velX = 0f,
                velY = 0f,
                direction = null,
            )
        }
        val correctedX = entity.x + (targetX - entity.x) * low
        val correctedY = entity.y + (targetY - entity.y) * low
        return copyWithClampCorrection(entity, correctedX, correctedY)
    }

    /** 微调位置时保留速度，避免窄道每帧被 clamp 清零导致竖向卡顿。 */
    private fun copyWithClampCorrection(
        entity: PacMazeEntity,
        newX: Float,
        newY: Float,
    ): PacMazeEntity {
        val correction = kotlin.math.hypot(newX - entity.x, newY - entity.y)
        val keepVelocity = correction <= pacSpeedPerTick() * 1.5f
        return entity.copy(
            x = newX,
            y = newY,
            velX = if (keepVelocity) entity.velX else 0f,
            velY = if (keepVelocity) entity.velY else 0f,
        )
    }

    fun isPositionLegal(
        state: PacMazeWorldState,
        anchorX: Float,
        anchorY: Float,
        forGhost: Boolean,
        ghost: PacMazeEntity? = null,
        bodyRadius: Float = BODY_RADIUS,
    ): Boolean {
        val radius = resolveBodyRadius(forGhost, bodyRadius)
        if (!isAnchorInsideWorld(state, anchorX, anchorY, radius)) return false
        val cx = centerX(anchorX)
        val cy = centerY(anchorY)
        val tx = tileX(anchorX)
        val ty = tileY(anchorY)
        if (!isTileWalkable(state, tx, ty, forGhost, ghost)) return false

        // 仅检测正交邻格：避免 T 字路口对角墙格误挡（「左边有路但过不去」）。
        val tileLeft = tx.toFloat()
        val tileRight = tx + 1f
        val tileTop = ty.toFloat()
        val tileBottom = ty + 1f
        if (cx - radius < tileLeft - EPS &&
            !isTileWalkable(state, tx - 1, ty, forGhost, ghost)
        ) {
            return false
        }
        if (cx + radius > tileRight + EPS &&
            !isTileWalkable(state, tx + 1, ty, forGhost, ghost)
        ) {
            return false
        }
        if (cy - radius < tileTop - EPS &&
            !isTileWalkable(state, tx, ty - 1, forGhost, ghost)
        ) {
            return false
        }
        if (cy + radius > tileBottom + EPS &&
            !isTileWalkable(state, tx, ty + 1, forGhost, ghost)
        ) {
            return false
        }
        return true
    }

    private fun isTileWalkable(
        state: PacMazeWorldState,
        tx: Int,
        ty: Int,
        forGhost: Boolean,
        ghost: PacMazeEntity?,
    ): Boolean {
        if (tx !in 0 until state.width || ty !in 0 until state.height) return false
        return PacMazeRules.isWalkable(state, tx, ty, forGhost, ghost)
    }

    /** 锚点中心不得超出地图可走区域外缘（硬边界，防止圆形碰撞体探出地图）。 */
    private fun isAnchorInsideWorld(
        state: PacMazeWorldState,
        anchorX: Float,
        anchorY: Float,
        bodyRadius: Float,
    ): Boolean {
        val cx = centerX(anchorX)
        val cy = centerY(anchorY)
        val minCx = bodyRadius
        val maxCx = state.width - bodyRadius
        val minCy = bodyRadius
        val maxCy = state.height - bodyRadius
        return cx in minCx..maxCx && cy in minCy..maxCy
    }

    fun canAdvanceInto(
        state: PacMazeWorldState,
        anchorX: Float,
        anchorY: Float,
        dir: Direction,
        step: Float,
        forGhost: Boolean,
        ghost: PacMazeEntity? = null,
    ): Boolean {
        val (dx, dy) = dir.delta()
        val (nx, ny) = integrateWithBodyCollision(
            state,
            anchorX,
            anchorY,
            anchorX + dx * step,
            anchorY + dy * step,
            forGhost,
            ghost,
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
        val ghost = if (forGhost) entity else null
        val (newX, newY) = integrateWithBodyCollision(state, entity.x, entity.y, targetX, targetY, forGhost, ghost)

        if (abs(newX - entity.x) <= EPS && abs(newY - entity.y) <= EPS) {
            return entity.copy(direction = dir, facing = dir)
        }

        return entity.copy(
            x = newX,
            y = newY,
            direction = dir,
            facing = dir,
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
        ghost: PacMazeEntity? = null,
        bodyRadius: Float = BODY_RADIUS,
    ): Pair<Float, Float> {
        val radius = resolveBodyRadius(forGhost, bodyRadius)
        if (isPositionLegal(state, nx, ny, forGhost, ghost, radius)) return nx to ny

        val dx = nx - x
        val dy = ny - y
        if (abs(dx) <= EPS && abs(dy) <= EPS) return x to y

        var low = 0f
        var high = 1f
        repeat(10) {
            val mid = (low + high) * 0.5f
            if (isPositionLegal(state, x + dx * mid, y + dy * mid, forGhost, ghost, radius)) {
                low = mid
            } else {
                high = mid
            }
        }
        if (low <= EPS) return x to y
        val safe = (low - COLLISION_SKIN).coerceAtLeast(0f)
        return x + dx * safe to y + dy * safe
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

    /** 实体渲染锚点：smoothstep 插值 + 速度外推（blend 应为已 smooth 的 0~1）。 */
    fun renderEntityAnchor(prev: PacMazeEntity?, curr: PacMazeEntity, blend: Float): Pair<Float, Float> {
        if (prev == null || blend >= 1f) return curr.x to curr.y
        val extrap = DT * PacMazeConstants.RENDER_VEL_EXTRAP * blend
        val x = lerpAnchor(prev.x, curr.x, blend) + curr.velX * extrap
        val y = lerpAnchor(prev.y, curr.y, blend) + curr.velY * extrap
        return x to y
    }

    /**
     * 渲染锚点不得超出逻辑合法区，防止插值/外推造成「穿墙」画面。
     */
    fun clampRenderAnchor(
        state: PacMazeWorldState,
        entity: PacMazeEntity,
        renderX: Float,
        renderY: Float,
        forGhost: Boolean = entity.role == "ghost",
    ): Pair<Float, Float> {
        val ghost = if (forGhost) entity else null
        if (isPositionLegal(state, renderX, renderY, forGhost, ghost)) return renderX to renderY
        if (isPositionLegal(state, entity.x, entity.y, forGhost, ghost)) return entity.x to entity.y
        val snapped = snapToGrid(state, entity, forGhost)
        return snapped.x to snapped.y
    }

    /** 弹体/子弹渲染锚点（blend 应为已 smooth 的 0~1）。 */
    fun renderPointAnchor(
        prevX: Float,
        prevY: Float,
        currX: Float,
        currY: Float,
        blend: Float,
    ): Pair<Float, Float> {
        if (blend >= 1f) return currX to currY
        return lerpAnchor(prevX, currX, blend) to lerpAnchor(prevY, currY, blend)
    }

    fun smoothBlend(raw: Float): Float {
        val t = raw.coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }

    private fun tryStartMovement(
        state: PacMazeWorldState,
        entity: PacMazeEntity,
        startDir: Direction,
        passRadius: Float,
    ): PacMazeEntity {
        if (canMoveInDir(state, entity.x, entity.y, startDir, forGhost = false, bodyRadius = passRadius)) {
            return clampEntityToLegal(state, setMovement(entity, startDir), forGhost = false, bodyRadius = passRadius)
        }
        val aligned = snapLaneCenterSafe(state, entity, startDir, forGhost = false)
        if (canMoveInDir(state, aligned.x, aligned.y, startDir, forGhost = false, bodyRadius = passRadius)) {
            return clampEntityToLegal(state, setMovement(aligned, startDir), forGhost = false, bodyRadius = passRadius)
        }
        return clampEntityToLegal(state, aligned, forGhost = false, bodyRadius = passRadius)
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

    private fun isAlignedForTurn(
        entity: PacMazeEntity,
        currentDir: Direction,
        eps: Float = PacMazeConstants.TURN_ALIGN_EPS,
    ): Boolean {
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

    private fun applyCenterSnap(entity: PacMazeEntity, dir: Direction): PacMazeEntity =
        railPerpendicularAxis(entity, dir)

    /** 直道：垂直于移动轴立即贴轨道中心，避免窄道漂移触发 clamp 顿挫。 */
    private fun railPerpendicularAxis(entity: PacMazeEntity, dir: Direction): PacMazeEntity =
        when (dir) {
            Direction.LEFT, Direction.RIGHT -> entity.copy(y = tileY(entity.y).toFloat())
            Direction.UP, Direction.DOWN -> entity.copy(x = tileX(entity.x).toFloat())
        }

    /** 贴墙时将垂直于移动轴的坐标吸附到车道中心（仅保留合法结果）。 */
    private fun snapLaneCenterSafe(
        state: PacMazeWorldState,
        entity: PacMazeEntity,
        dir: Direction,
        forGhost: Boolean,
        ghost: PacMazeEntity? = null,
        bodyRadius: Float = BODY_RADIUS,
    ): PacMazeEntity {
        val radius = resolveBodyRadius(forGhost, bodyRadius)
        val snapped = snapLaneCenter(entity, dir)
        if (isPositionLegal(state, snapped.x, snapped.y, forGhost, ghost, radius)) return snapped
        return when (dir) {
            Direction.LEFT, Direction.RIGHT -> {
                val baseTy = tileY(entity.y)
                (-1..1).map { baseTy + it }
                    .filter { it in 0 until state.height }
                    .map { ty -> entity.copy(y = ty.toFloat()) }
                    .firstOrNull { isPositionLegal(state, it.x, it.y, forGhost, ghost, radius) }
                    ?: entity
            }
            Direction.UP, Direction.DOWN -> {
                val baseTx = tileX(entity.x)
                (-1..1).map { baseTx + it }
                    .filter { it in 0 until state.width }
                    .map { tx -> entity.copy(x = tx.toFloat()) }
                    .firstOrNull { isPositionLegal(state, it.x, it.y, forGhost, ghost, radius) }
                    ?: entity
            }
        }
    }

    private fun tryTurnAtIntersection(
        state: PacMazeWorldState,
        entity: PacMazeEntity,
        preemptive: Boolean = false,
        passRadius: Float = BODY_RADIUS,
    ): PacMazeEntity {
        val next = entity.nextDirection ?: return entity
        val currentDir = entity.direction ?: return entity
        if (next == currentDir) return entity
        val alignEps = if (preemptive) PacMazeConstants.TURN_PREEMPT_EPS else PacMazeConstants.TURN_ALIGN_EPS
        if (!isAlignedForTurn(entity, currentDir, alignEps)) return entity
        if (!canMoveInDir(state, entity.x, entity.y, next, forGhost = false, bodyRadius = passRadius)) return entity

        var turned = snapPerpendicularAxis(entity, currentDir)
        turned = setMovement(turned, next)
        return clampEntityToLegal(
            state,
            turned.copy(nextDirection = next),
            forGhost = false,
            bodyRadius = passRadius,
        )
    }

    /** 路口/开阔格：转圈改向时不强制等下一格中心。 */
    private fun isOpenTurnPoint(
        state: PacMazeWorldState,
        entity: PacMazeEntity,
        passRadius: Float,
    ): Boolean {
        val exits = Direction.entries.count {
            canMoveInDir(state, entity.x, entity.y, it, forGhost = false, bodyRadius = passRadius)
        }
        return exits >= 3
    }

    private fun findNearestLegalAnchor(
        state: PacMazeWorldState,
        originTx: Int,
        originTy: Int,
        forGhost: Boolean,
        ghost: PacMazeEntity? = null,
        bodyRadius: Float = BODY_RADIUS,
    ): Pair<Float, Float>? {
        val radius = resolveBodyRadius(forGhost, bodyRadius)
        for (searchRadius in 0..6) {
            for (dy in -searchRadius..searchRadius) {
                for (dx in -searchRadius..searchRadius) {
                    if (abs(dx) != searchRadius && abs(dy) != searchRadius) continue
                    val tx = originTx + dx
                    val ty = originTy + dy
                    val ax = tx.toFloat()
                    val ay = ty.toFloat()
                    if (isPositionLegal(state, ax, ay, forGhost, ghost, radius)) return ax to ay
                }
            }
        }
        return null
    }
}
