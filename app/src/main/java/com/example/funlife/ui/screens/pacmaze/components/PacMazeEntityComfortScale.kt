package com.example.funlife.ui.screens.pacmaze.components

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.max

/**
 * 大地图全屏容纳时格子变小；适度抬高实体 **渲染** 尺寸（不改碰撞/移速）。
 * 用户已在 HUD 调大「角色」时不再叠加自动补偿，避免过大。
 */
object PacMazeEntityComfortScale {

    val MinPlayerRadius: Dp = 11.dp
    val MinGhostRadius: Dp = 9.dp

    /** 低于此屏高占比才触发补偿。 */
    private const val PLAYER_SOFT_FLOOR_FRAC = 0.028f
    private const val GHOST_SOFT_FLOOR_FRAC = 0.022f

    /** 补偿目标：约占屏高 3.8% / 3.1%（介于「太小」与「占满格」之间）。 */
    private const val PLAYER_TARGET_FRAC = 0.038f
    private const val GHOST_TARGET_FRAC = 0.031f

    private const val PLAYER_CELL_RATIO = 0.38f
    private const val GHOST_CELL_RATIO = 0.42f

    /** 用户滑条超过此值视为已手动放大，跳过玩家侧自动补偿。 */
    private const val USER_SCALE_AUTO_SKIP = 1.28f

    const val MAX_BOOST = 2.75f

    data class Metrics(
        val entityCell: Float,
        val boost: Float,
        val minPlayerRadiusPx: Float,
        val minGhostRadiusPx: Float,
    )

    /** 用户滑条 → 有效绘制倍率（线性，HUD 百分比即实际倍率）。 */
    fun userDrawScaleEffective(userScale: Float): Float =
        userScale.coerceIn(PAC_MAZE_PLAYER_SCALE_MIN, PAC_MAZE_PLAYER_SCALE_MAX)

    fun compute(
        baseCellPx: Float,
        canvasContentHeightPx: Float,
        playerUserScale: Float,
        playerTierScale: Float,
        densityPxPerDp: Float,
    ): Metrics {
        if (baseCellPx <= 0f) {
            return Metrics(baseCellPx, 1f, 0f, 0f)
        }
        // 实体尺寸跟基准格走；地图宽/高拉伸只影响地砖，避免拉宽地图时角色跟着暴涨。
        val entityCell = baseCellPx
        val userScale = userDrawScaleEffective(playerUserScale)
        val tierScale = playerTierScale.coerceAtLeast(0.5f)
        val userTunedScale = kotlin.math.abs(playerUserScale - 1f) > 0.04f

        val naturalPlayerDraw = entityCell * PLAYER_CELL_RATIO * userScale * tierScale
        val naturalGhost = entityCell * GHOST_CELL_RATIO

        val targetPlayer = max(
            canvasContentHeightPx * PLAYER_TARGET_FRAC,
            MinPlayerRadius.value * densityPxPerDp,
        )
        val targetGhost = max(
            canvasContentHeightPx * GHOST_TARGET_FRAC,
            MinGhostRadius.value * densityPxPerDp,
        )
        val softPlayer = canvasContentHeightPx * PLAYER_SOFT_FLOOR_FRAC
        val softGhost = canvasContentHeightPx * GHOST_SOFT_FLOOR_FRAC

        val playerBoost = when {
            userTunedScale -> 1f
            userScale >= USER_SCALE_AUTO_SKIP -> 1f
            naturalPlayerDraw >= softPlayer -> 1f
            else -> {
                val base = entityCell * PLAYER_CELL_RATIO * tierScale
                (targetPlayer / base.coerceAtLeast(1f)).coerceAtMost(MAX_BOOST)
            }
        }
        val ghostBoost = if (naturalGhost >= softGhost) {
            1f
        } else {
            (targetGhost / naturalGhost.coerceAtLeast(1f)).coerceAtMost(MAX_BOOST)
        }
        val boost = maxOf(playerBoost, ghostBoost).coerceIn(1f, MAX_BOOST)

        return Metrics(
            entityCell = entityCell,
            boost = boost,
            minPlayerRadiusPx = 0f,
            minGhostRadiusPx = 0f,
        )
    }

    /** @deprecated 旧 API；测试兼容。 */
    fun boost(cellPx: Float, gridWidth: Int, gridHeight: Int): Float =
        compute(
            baseCellPx = cellPx,
            canvasContentHeightPx = 420f,
            playerUserScale = 1f,
            playerTierScale = 1f,
            densityPxPerDp = 2.5f,
        ).boost

    fun resolvePlayerRadius(
        entityCell: Float,
        tierScale: Float,
        userScale: Float,
        boost: Float,
        minRadiusPx: Float,
    ): Float {
        val scaled = entityCell * PLAYER_CELL_RATIO * tierScale * userDrawScaleEffective(userScale) * boost
        return if (minRadiusPx > 0f) max(scaled, minRadiusPx) else scaled
    }

    fun resolveGhostRadius(
        entityCell: Float,
        boost: Float,
        minRadiusPx: Float,
        kindMul: Float = 1f,
    ): Float {
        val scaled = entityCell * GHOST_CELL_RATIO * boost * kindMul
        return if (minRadiusPx > 0f) max(scaled, minRadiusPx) else scaled
    }

    fun isComfortActive(boost: Float): Boolean = boost >= 1.12f
}
