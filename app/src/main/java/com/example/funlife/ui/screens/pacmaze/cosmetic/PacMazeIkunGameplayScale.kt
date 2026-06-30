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

    /** 非走廊贴合路径（旧半径定标）；取消额外缩小，由位图通道常量定标。 */
    const val LEGACY_BITMAP_GAMEPLAY_SCALE = 1f

    /** 图片资源角色在走廊内的目标高度（格）；与 HUD 滑条无关。 */
    const val HEIGHT_CELL_FRAC = 2.05f
    const val PLATFORMER_HEIGHT_CELL_FRAC = 1.55f
    const val MIN_HEIGHT_CELL_FRAC = 1.78f
    const val WIDTH_CELL_FRAC = 0.99f
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
    /** PacMaze 走廊专用；横版冒险用 [PLATFORMER_FEET_FRAC_BIAS]。 */
    const val FEET_FRAC_GAMEPLAY_BIAS = 0.10f
    /** 略增大绘制脚点 frac（负 bias → 更大 fy → 精灵下移贴砖）。 */
    const val PLATFORMER_FEET_FRAC_BIAS = -0.012f
    /** 站立时额外下沉（格），仅作鞋底与碰撞盒底的微调。 */
    const val PLATFORMER_FEET_GROUND_NUDGE_CELL_FRAC = 0.035f

    /** 碰撞/通道逻辑用（勿与位图绘制混用）。 */
    const val CORRIDOR_INNER_WALKABLE_FRAC = 0.78f
    const val CORRIDOR_SPAN_OF_WALKABLE = 0.94f
    /** 仅图片资源角色绘制：在 100% 滑条下占满更多通道宽度。 */
    const val BITMAP_CORRIDOR_INNER_WALKABLE_FRAC = 0.99f
    const val BITMAP_CORRIDOR_SPAN_OF_WALKABLE = 1f
    /** 走廊贴合：在 wallBox 内缘留安全边，避免贴墙/穿出 */
    const val BITMAP_CORRIDOR_FILL_FRAC = 1.0f
    /** 位图不透明目标相对通道跨度的加成（行走皮肤 opaque 贴墙定标额外放大）。 */
    const val BITMAP_CORRIDOR_OPAQUE_BOOST = 1.32f
    /** 布局/contentFill 用的不透明高度上限（防止扫描误判整格不透明→越来越小） */
    const val BITMAP_LAYOUT_OPAQUE_HEIGHT_CAP = 0.72f
    /** 行走皮肤布局不透明高度上限（manifest 更大时截断，防止撑出通道） */
    const val BITMAP_WALK_LAYOUT_OPAQUE_HEIGHT = 0.44f
    const val BITMAP_WALK_LAYOUT_OPAQUE_WIDTH = 0.54f
    /** 行走皮肤通道占用比例（相对 wallBox 内可放最大矩形，100% 滑条下约满格） */
    const val BITMAP_WALK_CORRIDOR_SIZE_FRAC = 1.0f
    /** 行走 sprite 高度下限（格），防止定标/扫描异常缩成点。 */
    const val BITMAP_WALK_MIN_SPRITE_HEIGHT_CELL_FRAC = 1.55f
    /** 归一化 sheet 格内透明留白补偿：按不透明内容 bbox 反算放大倍率。 */
    const val BITMAP_CONTENT_FILL_MIN_FRAC = 0.18f
    /** 归一化 sheet 无扫描数据时的保守放大（约 40% 内容占比）。 */
    const val BITMAP_NORMALIZED_SHEET_FALLBACK_FILL_MUL = 2.45f
    const val BITMAP_CONTENT_FILL_MAX_MUL = 3.35f
    /** 位图角色最大绘制高度（格），防止中心锚点上下穿出地图。 */
    const val BITMAP_MAX_MAP_HEIGHT_CELL_FRAC = 1.46f
    /** 地图内缘裁剪留白（格），实体不超出霓虹边框。 */
    const val MAP_ENTITY_CLIP_INSET_CELL_FRAC = 0.018f
    /** 墙体贴合安全边距（格），防止步态 bob / 亚像素越界。 */
    const val BITMAP_WALL_FIT_MARGIN_CELL_FRAC = 0.006f
    /** 裁剪区底边相对侧边的缩减倍率（<1 少裁脚）。 */
    const val BITMAP_CLIP_BOTTOM_MARGIN_MUL = 0.12f
    /** 裁剪区底边额外下探（格），保证鞋底像素不被 clip 切掉。 */
    const val BITMAP_FEET_CLIP_BLEED_CELL_FRAC = 0.022f
    /** platformer 脚底 bbox 下缘留白（相对格高 frac）。 */
    const val BITMAP_FEET_BBOX_PAD_FRAC = 0.028f
    /** 墙体贴合缩放下限（仅渲染）；须允许小于 [MIN_AXIS_SCALE] 才能贴进单格通道。 */
    const val BITMAP_WALL_FIT_MIN_SCALE = 0.12f

    fun bitmapWallFitMarginPx(cellPerpPx: Float): Float =
        cellPerpPx * BITMAP_WALL_FIT_MARGIN_CELL_FRAC
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

    /** 位图/序列帧皮肤布局专用通道跨度（不影响幽灵与碰撞半径）。 */
    fun bitmapCorridorAcrossSpanPx(cellPerpPx: Float): Float =
        cellPerpPx * BITMAP_CORRIDOR_INNER_WALKABLE_FRAC * BITMAP_CORRIDOR_SPAN_OF_WALKABLE

    /**
     * 位图皮肤目标不透明高度（像素）：通道跨度 × 内容填充，与滑条无关。
     * 按**不透明 bbox**定标，而非整格 sheet（避免归一化大留白导致显示过小）。
     */
    fun bitmapOpaqueTargetHeightPx(
        verticalCellPx: Float,
        corridorCellPx: Float,
        cellHeightFrac: Float,
        contentHeightFrac: Float,
        contentWidthFrac: Float,
    ): Float {
        val corridorSpan = bitmapCorridorAcrossSpanPx(corridorCellPx)
        val fillMul = bitmapContentFillMul(contentHeightFrac, contentWidthFrac)
        return corridorSpan * fillMul * BITMAP_CORRIDOR_OPAQUE_BOOST
    }

    /** 格内透明留白 → 布局放大，使可见角色填满通道（仅位图皮肤）。 */
    fun bitmapContentFillMul(contentHeightFrac: Float, contentWidthFrac: Float): Float {
        val hMul = 1f / contentHeightFrac.coerceIn(BITMAP_CONTENT_FILL_MIN_FRAC, 1f)
        val wMul = 1f / contentWidthFrac.coerceIn(BITMAP_CONTENT_FILL_MIN_FRAC, 1f)
        return maxOf(hMul, wMul, BITMAP_NORMALIZED_SHEET_FALLBACK_FILL_MUL)
            .coerceIn(BITMAP_NORMALIZED_SHEET_FALLBACK_FILL_MUL, BITMAP_CONTENT_FILL_MAX_MUL)
    }

    /** 布局/contentFill 专用：取 manifest 与扫描中更保守（更小）的不透明占比。 */
    fun bitmapLayoutOpaqueSpan(contentHeightFrac: Float, contentWidthFrac: Float): Pair<Float, Float> {
        val h = contentHeightFrac.coerceIn(BITMAP_CONTENT_FILL_MIN_FRAC, BITMAP_LAYOUT_OPAQUE_HEIGHT_CAP)
        val w = contentWidthFrac.coerceIn(BITMAP_CONTENT_FILL_MIN_FRAC, 1f)
        return h to w
    }

    /** 100% HUD 滑条下位图基准倍率（与 opaque 贴墙定标叠乘）。 */
    const val BITMAP_INITIAL_VISUAL_MUL = 1.38f

    /** 视觉倍率上限：滑条 350% × 自动补偿 */
    val MAX_VISUAL_SCALE: Float
        get() = PAC_MAZE_PLAYER_SCALE_MAX * PacMazeEntityComfortScale.MAX_BOOST

    /**
     * HUD 滑条 → 位图视觉倍率（纯线性，不含大地图 auto-boost，避免 100%→140% 跳变）。
     */
    fun hudVisualScale(userScale: Float): Float =
        PacMazeEntityComfortScale.userDrawScaleEffective(userScale)

    /** 局内位图布局：100% 滑条含 [BITMAP_INITIAL_VISUAL_MUL] 基准放大，HUD 在其上线性缩放。 */
    fun bitmapLayoutVisualScale(userScale: Float): Float =
        hudVisualScale(userScale) * BITMAP_INITIAL_VISUAL_MUL

    /** @deprecated 使用 [hudVisualScale]；保留参数兼容。 */
    fun uniformVisualScale(userScale: Float, entityBoost: Float = 1f): Float =
        hudVisualScale(userScale).coerceIn(PAC_MAZE_PLAYER_SCALE_MIN, PAC_MAZE_PLAYER_SCALE_MAX)
}
