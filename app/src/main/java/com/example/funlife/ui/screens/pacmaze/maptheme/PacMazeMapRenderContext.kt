package com.example.funlife.ui.screens.pacmaze.maptheme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import com.example.funlife.social.game.engine.pacmaze.PacMazeEntity
import com.example.funlife.social.game.engine.pacmaze.PacMazeEnemyBullet
import com.example.funlife.social.game.engine.pacmaze.Direction
import com.example.funlife.social.game.engine.pacmaze.PacMazeMotion
import com.example.funlife.social.game.engine.pacmaze.PacMazeMapMarker
import com.example.funlife.social.game.engine.pacmaze.PacMazeLevelConfig
import com.example.funlife.social.game.engine.pacmaze.PacMazeProjectile
import com.example.funlife.social.game.engine.pacmaze.PacMazeWorldState
import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeAvatarLoadout
import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeIkunCatalog
import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeIkunGameplayScale
import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeSkinId
import com.example.funlife.social.game.engine.pacmaze.PacMazeEntityVisuals
import com.example.funlife.ui.screens.pacmaze.cosmetic.skin.PacMazeSkinRenderProfileCatalog
import com.example.funlife.ui.screens.pacmaze.cosmetic.trail.PacMazeTrailSample
import kotlin.math.min

data class PacMazeMapRenderContext(
    val world: PacMazeWorldState,
    val previous: PacMazeWorldState?,
    val blend: Float,
    /** 等比基准格（角色/幽灵/弹体半径，避免压扁） */
    val cell: Float,
    /** 实体绘制参考格（含地图宽/高拉伸后的等效尺寸）。 */
    val entityCell: Float = cell,
    /** 地图横向格宽（用户可调，仅地砖/墙体） */
    val cellX: Float = cell,
    /** 地图纵向格高（用户可调，仅地砖/墙体） */
    val cellY: Float = cell,
    val offsetX: Float,
    val offsetY: Float,
    val mapW: Float,
    val mapH: Float,
    val animPhase: Float,
    val canvasSize: Size,
    val config: PacMazeThemeConfig,
    val levelConfig: PacMazeLevelConfig? = null,
    val renderAnchor: (PacMazeEntity) -> Pair<Float, Float>,
    val trailSamples: List<PacMazeTrailSample> = emptyList(),
    val markers: List<PacMazeMapMarker> = emptyList(),
    val avatarLoadout: PacMazeAvatarLoadout = PacMazeAvatarLoadout(),
    val playerDrawScale: Float = 1f,
    /** 大地图自动实体放大（仅渲染，不含用户滑条）。 */
    val entityDrawBoost: Float = 1f,
    val minPlayerRadiusPx: Float = 0f,
    val minGhostRadiusPx: Float = 0f,
    /** 在线对战：本机控制的实体 id（pac_a / pac_b），用于拖尾与角色高亮。 */
    val onlineLocalEntityId: String = "",
) {
    val effectivePlayerDrawScale: Float get() = playerDrawScale * entityDrawBoost
  /** 地砖内绘制用（取较短边，避免装饰溢出）。 */
    fun tileMetric(rect: Rect): Float = min(rect.width, rect.height)

    fun gridToScreen(gridX: Float, gridY: Float): Offset =
        Offset(offsetX + (gridX + 0.5f) * cellX, offsetY + (gridY + 0.5f) * cellY)

    fun entityCenter(entity: PacMazeEntity): Offset {
        val (ax, ay) = renderAnchor(entity)
        return gridToScreen(ax, ay)
    }

    /** 绘制用锚点：略低于格心，角色脚踩地砖下半部。 */
    fun entityDrawCenter(entity: PacMazeEntity): Offset {
        val base = entityCenter(entity)
        val shiftY = min(cellX, cellY) * PacMazeIkunGameplayScale.ENTITY_DRAW_Y_SHIFT_FRAC
        return Offset(base.x, base.y + shiftY)
    }

    /**
     * 玩家绘制锚点：矢量手绘沿用格心+略下移；本地 PNG 图片皮肤单独锚路径中心。
     * 不改逻辑坐标与碰撞，仅影响渲染。
     */
    fun playerDrawCenter(entity: PacMazeEntity, skinId: PacMazeSkinId): Offset {
        if (PacMazeSkinRenderProfileCatalog.isAssetBitmap(skinId)) {
            return bitmapDrawCenter(entity)
        }
        val laneX = entityLaneCenterX(entity)
        val center = entityCenter(entity)
        if (PacMazeIkunCatalog.contains(skinId)) {
            return Offset(laneX, center.y)
        }
        val cell = min(cellX, cellY)
        val bodyRadius = cell * 0.44f
        return Offset(laneX, center.y + bodyRadius * 0.08f)
    }

    /** 本地 PNG 图片皮肤：视觉中心对齐路径（格心连线）中心。 */
    fun bitmapDrawCenter(entity: PacMazeEntity): Offset =
        Offset(entityLaneCenterX(entity), entityLaneCenterY(entity))

    fun playerVisualCenter(entity: PacMazeEntity, skinId: PacMazeSkinId): Offset =
        playerDrawCenter(entity, skinId)

    /** 轨道中心 X：与 [PacMazeMotion.centerX] 同一套逻辑坐标。 */
    fun entityLaneCenterX(entity: PacMazeEntity): Float {
        val (ax, _) = renderAnchor(entity)
        return offsetX + PacMazeMotion.centerX(ax) * cellX
    }

    /** 轨道中心 Y：与 [PacMazeMotion.centerY] 同一套逻辑坐标。 */
    fun entityLaneCenterY(entity: PacMazeEntity): Float {
        val (_, ay) = renderAnchor(entity)
        return offsetY + PacMazeMotion.centerY(ay) * cellY
    }

    /** 当前格左墙内侧 X（保留供调试/扩展）。 */
    fun entityCorridorLeftWallX(entity: PacMazeEntity): Float {
        val (ax, _) = renderAnchor(entity)
        val col = PacMazeMotion.tileX(ax).coerceIn(0, world.width - 1)
        return offsetX + col * cellX + cellX * 0.11f
    }

    /** 移动轴（逻辑方向优先）；与位图绘制朝向解耦，避免移动时锚点跳变。 */
    fun entityTravelDirection(entity: PacMazeEntity): Direction? =
        entity.direction ?: PacMazeEntityVisuals.travelFacing(entity)

    private fun isHorizontalTravel(direction: Direction?): Boolean =
        direction == Direction.LEFT || direction == Direction.RIGHT

    /** 位图皮肤绘制锚点：横走脚贴格底；竖走中心对准走廊。 */
    fun bitmapDrawAnchor(entity: PacMazeEntity, @Suppress("UNUSED_PARAMETER") facing: Direction? = null): Offset =
        bitmapTravelDrawAnchor(entity)

    fun ikunDrawAnchor(entity: PacMazeEntity, @Suppress("UNUSED_PARAMETER") facing: Direction? = null): Offset =
        bitmapTravelDrawAnchor(entity)

    private fun bitmapTravelDrawAnchor(entity: PacMazeEntity): Offset {
        val travel = entityTravelDirection(entity) ?: Direction.RIGHT
        val laneX = if (isHorizontalTravel(travel)) {
            entityLaneCenterX(entity)
        } else {
            entityLaneCenterXSnapped(entity)
        }
        val anchorY = if (isHorizontalTravel(travel)) {
            entityLaneCenterY(entity)
        } else {
            entityVerticalTravelAnchorY(entity, snapToTileColumn = true)
        }
        return Offset(laneX, anchorY)
    }

    private fun entityLaneCenterXSnapped(entity: PacMazeEntity): Float {
        val (ax, _) = renderAnchor(entity)
        val col = PacMazeMotion.tileX(ax).toFloat()
        return offsetX + PacMazeMotion.centerX(col) * cellX
    }

    /** 竖走：锚点在格心略下方，配合绕精灵中心旋转。 */
    fun entityVerticalTravelAnchorY(
        entity: PacMazeEntity,
        snapToTileColumn: Boolean = false,
    ): Float {
        val (_, ay) = renderAnchor(entity)
        val gridY = ay + PacMazeIkunGameplayScale.VERTICAL_TRAVEL_ANCHOR_Y_FRAC
        return offsetY + gridY * cellY
    }

    /** @deprecated 使用 [bitmapDrawAnchor] */
    fun bitmapFeetAnchor(entity: PacMazeEntity): Offset =
        bitmapDrawAnchor(entity, Direction.RIGHT)

    /** @deprecated 使用 [ikunDrawAnchor] */
    fun ikunFeetAnchor(entity: PacMazeEntity, facing: Direction): Offset =
        ikunDrawAnchor(entity, facing)

    /**
     * 走廊地板内缘 Y（横走脚点）：格顶 + (1 - 墙体内缩 + 下沉)。
     * 对齐霓虹墙内底线/豆粒通道，而非整格外缘。
     */
    fun entityCorridorFloorY(entity: PacMazeEntity, snapToTileRow: Boolean = false): Float {
        val (_, ay) = renderAnchor(entity)
        val rowAy = if (snapToTileRow) PacMazeMotion.tileY(ay).toFloat() else ay
        val inset = PacMazeIkunGameplayScale.CORRIDOR_WALL_INSET_FRAC
        val sink = PacMazeIkunGameplayScale.CORRIDOR_FLOOR_SINK_FRAC
        val margin = PacMazeMotion.BODY_RADIUS * 0.35f
        val footGridY = (rowAy + 1f - inset + sink).coerceIn(
            margin,
            world.height - margin,
        )
        val feetY = offsetY + footGridY * cellY
        val mapBottom = offsetY + world.height * cellY
        return feetY.coerceIn(offsetY + cellY * 0.04f, mapBottom - cellY * 0.04f)
    }

    /** @deprecated 使用 [entityCorridorFloorY] */
    fun entityTileBottomY(entity: PacMazeEntity, snapToTileRow: Boolean = false): Float =
        entityCorridorFloorY(entity, snapToTileRow)

    /** ikun 绘制裁剪区：地图内缘，防止脚/头穿出边框。 */
    fun ikunMapClipRect(): Rect = Rect(
        left = offsetX,
        top = offsetY,
        right = offsetX + mapW,
        bottom = offsetY + mapH,
    )

    fun projectileCenter(projectile: PacMazeProjectile): Offset {
        val prev = previous?.projectiles?.firstOrNull { it.id == projectile.id }
        val (ax, ay) = interpolatePoint(prev?.x, prev?.y, projectile.x, projectile.y)
        return gridToScreen(ax, ay)
    }

    fun enemyBulletCenter(bullet: PacMazeEnemyBullet): Offset {
        val prev = previous?.enemyBullets?.firstOrNull { it.id == bullet.id }
        val (ax, ay) = interpolatePoint(prev?.x, prev?.y, bullet.x, bullet.y)
        return gridToScreen(ax, ay)
    }

    private fun interpolatePoint(
        prevX: Float?,
        prevY: Float?,
        currX: Float,
        currY: Float,
    ): Pair<Float, Float> {
        if (prevX == null || prevY == null || blend >= 1f) return currX to currY
        return PacMazeMotion.renderPointAnchor(prevX, prevY, currX, currY, blend)
    }

    fun tileRect(x: Int, y: Int): Rect = Rect(
        offsetX + x * cellX,
        offsetY + y * cellY,
        offsetX + (x + 1) * cellX,
        offsetY + (y + 1) * cellY,
    )
}
