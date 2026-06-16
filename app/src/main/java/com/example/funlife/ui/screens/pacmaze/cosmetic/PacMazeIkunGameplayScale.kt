package com.example.funlife.ui.screens.pacmaze.cosmetic

import com.example.funlife.social.game.engine.pacmaze.Direction
import com.example.funlife.ui.screens.pacmaze.components.PAC_MAZE_PLAYER_SCALE_MAX
import com.example.funlife.ui.screens.pacmaze.components.PAC_MAZE_PLAYER_SCALE_MIN
import com.example.funlife.ui.screens.pacmaze.components.PacMazeEntityComfortScale
import kotlin.math.min

/**
 * ikun / 本地 PNG 位图局内尺寸：贴合可走道 + HUD 等比视觉缩放（不改碰撞轨迹）。
 */
object PacMazeIkunGameplayScale {

    /** 非走廊贴合路径（旧半径定标）使用的全局缩小 */
    const val LEGACY_BITMAP_GAMEPLAY_SCALE = 0.88f

    const val HEIGHT_CELL_FRAC = 1.55f
    const val MIN_HEIGHT_CELL_FRAC = 1.38f
    const val WIDTH_CELL_FRAC = 0.92f
    const val SOFT_WIDTH_MUL = 2.65f
    const val HARD_WIDTH_CELL_FRAC = 2.85f

    const val ENTITY_DRAW_Y_SHIFT_FRAC = 0f
    const val DEFAULT_PLAYER_FLOOR_SHIFT_FRAC = 0.22f

    const val TILE_FEET_INSET_FRAC = 0f
    /** 霓虹墙体内缩（与 [PacMazeMapRenderContext.entityCorridorLeftWallX] / Cyber 0.10 一致） */
    const val CORRIDOR_WALL_INSET_FRAC = 0.11f
    /** 脚踩地板内缘后再略下沉，贴霓虹底线 */
    const val CORRIDOR_FLOOR_SINK_FRAC = 0.042f
    @Deprecated("Use CORRIDOR_FLOOR_SINK_FRAC", ReplaceWith("CORRIDOR_FLOOR_SINK_FRAC"))
    const val FEET_SINK_CELL_FRAC = CORRIDOR_FLOOR_SINK_FRAC
    const val FEET_GROUND_NUDGE_CELL_FRAC = 0.045f
    const val FEET_FRAC_GAMEPLAY_BIAS = 0.10f

    const val CORRIDOR_INNER_WALKABLE_FRAC = 0.78f
    const val CORRIDOR_SPAN_OF_WALKABLE = 0.94f
    const val VERTICAL_CORRIDOR_SPAN_FRAC = CORRIDOR_SPAN_OF_WALKABLE
    const val HORIZONTAL_CORRIDOR_SPAN_FRAC = CORRIDOR_SPAN_OF_WALKABLE

    const val VERTICAL_LANE_HUG_FRAC = 0f
    const val VERTICAL_BODY_HEIGHT_MUL = 1.05f
    const val FEET_Y_FRAC_MIN = 0.72f
    const val VERTICAL_TRAVEL_ANCHOR_Y_FRAC = 0.58f

    const val MIN_AXIS_SCALE = 0.42f

    /** 单格通道半宽（格），与视觉 [corridorAcrossSpanPx] 对齐。 */
    fun corridorHalfWidthCells(): Float =
        0.5f * CORRIDOR_INNER_WALKABLE_FRAC * CORRIDOR_SPAN_OF_WALKABLE

    /** 定标用通道格宽：取 min(cellX,cellY)，避免竖/横走因地图拉伸切换尺寸。 */
    fun corridorFitCellPx(cellX: Float, cellY: Float): Float = min(cellX, cellY)

    fun perpendicularCorridorCellPx(cellX: Float, cellY: Float, @Suppress("UNUSED_PARAMETER") facing: Direction? = null): Float =
        corridorFitCellPx(cellX, cellY)

    fun corridorAcrossSpanPx(cellPerpPx: Float): Float =
        cellPerpPx * CORRIDOR_INNER_WALKABLE_FRAC * CORRIDOR_SPAN_OF_WALKABLE

    /** 视觉倍率上限：滑条 350% × 自动补偿 */
    val MAX_VISUAL_SCALE: Float
        get() = PAC_MAZE_PLAYER_SCALE_MAX * PacMazeEntityComfortScale.MAX_BOOST

    /**
     * HUD 滑条 → 位图视觉倍率（纯线性，不含大地图 auto-boost，避免 100%→140% 跳变）。
     */
    fun hudVisualScale(userScale: Float): Float =
        PacMazeEntityComfortScale.userDrawScaleEffective(userScale)

    /** @deprecated 使用 [hudVisualScale]；保留参数兼容。 */
    fun uniformVisualScale(userScale: Float, entityBoost: Float = 1f): Float =
        hudVisualScale(userScale).coerceIn(PAC_MAZE_PLAYER_SCALE_MIN, PAC_MAZE_PLAYER_SCALE_MAX)
}
