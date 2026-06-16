package com.example.funlife.social.game.engine.pacmaze

import kotlin.math.abs
import kotlin.math.hypot

/**
 * 实体视觉状态：移动朝向、拖尾方向等与逻辑分离的渲染/动画辅助。
 */
object PacMazeEntityVisuals {

    const val VEL_EPS = 0.02f

    /** 当前实际位移方向（优先速度向量，其次逻辑方向）。 */
    fun travelFacing(entity: PacMazeEntity): Direction? {
        val fromVel = facingFromVelocity(entity.velX, entity.velY)
        if (fromVel != null) return fromVel
        return entity.direction ?: entity.facing
    }

    /** 精灵/位图绘制朝向：移动中始终跟位移一致，静止时保留最后朝向。 */
    fun spriteFacing(entity: PacMazeEntity): Direction =
        travelFacing(entity) ?: entity.facing

    fun isLocomoting(entity: PacMazeEntity): Boolean =
        hypot(entity.velX.toDouble(), entity.velY.toDouble()) > VEL_EPS ||
            (entity.inputActive && (entity.direction != null || entity.nextDirection != null))

    fun facingFromVelocity(velX: Float, velY: Float): Direction? {
        val ax = abs(velX)
        val ay = abs(velY)
        if (ax < VEL_EPS && ay < VEL_EPS) return null
        return when {
            ax >= ay -> if (velX >= 0f) Direction.RIGHT else Direction.LEFT
            else -> if (velY >= 0f) Direction.DOWN else Direction.UP
        }
    }

    /**
     * 拖尾锚点深度：从视觉中心到角色后缘，略向内重叠避免「脱节」。
     */
    fun trailRearAttachDepthPx(bodyRadiusPx: Float, cellPx: Float): Float =
        bodyRadiusPx * 0.48f + cellPx * 0.015f

    /**
     * 拖尾头部向角色方向延伸（像素），供丝带类渲染器接到身上。
     */
    fun trailHeadBleedPx(cellPx: Float, powerActive: Boolean = false): Float =
        cellPx * (if (powerActive) 0.38f else 0.32f)

    /** 拖尾采样点：角色视觉中心后方，沿速度反方向偏移。 */
    fun trailAnchorOffset(
        velX: Float,
        velY: Float,
        fallbackFacing: Direction?,
        cellX: Float,
        cellY: Float,
        trailDepthPx: Float,
    ): Pair<Float, Float> {
        val screenVelX = velX * cellX
        val screenVelY = velY * cellY
        val speed = hypot(screenVelX.toDouble(), screenVelY.toDouble()).toFloat()
        if (speed > VEL_EPS) {
            return (-screenVelX / speed * trailDepthPx) to (-screenVelY / speed * trailDepthPx)
        }
        val dir = fallbackFacing ?: Direction.RIGHT
        val (dx, dy) = dir.delta()
        return (-dx * trailDepthPx) to (-dy * trailDepthPx)
    }
}
