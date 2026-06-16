package com.example.funlife.ui.screens.pacmaze.cosmetic

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.funlife.social.game.engine.pacmaze.Direction
import com.example.funlife.social.game.engine.pacmaze.PacMazeEntity
import com.example.funlife.social.game.engine.pacmaze.PacMazeEntityVisuals
import com.example.funlife.ui.screens.pacmaze.character.PacMazeCharacterDraw
import com.example.funlife.ui.screens.pacmaze.cosmetic.skin.PacMazeBitmapFacingState
import com.example.funlife.ui.screens.pacmaze.cosmetic.skin.PacMazeSkinRenderProfileCatalog
import com.example.funlife.ui.screens.pacmaze.cosmetic.skin.PacMazeSkinRegistry
import com.example.funlife.ui.screens.pacmaze.cosmetic.trail.PacMazeTrailRegistry
import com.example.funlife.ui.screens.pacmaze.maptheme.PacMazeMapRenderContext
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

object PacMazePlayerCosmeticDraw {

    private const val TRAIL_MOTION_EPS = 0.01f

    /** 仅在实际位移时产生拖尾（非摇杆按住、非朝向锁定）。 */
    fun isTrailMotionActive(pac: PacMazeEntity): Boolean =
        hypot(pac.velX, pac.velY) > TRAIL_MOTION_EPS

    fun drawTrail(scope: DrawScope, ctx: PacMazeMapRenderContext) {
        if (ctx.trailSamples.isEmpty()) return
        PacMazeTrailRegistry.draw(
            scope = scope,
            trailId = ctx.avatarLoadout.trailId,
            samples = ctx.trailSamples,
            palette = ctx.config.palette,
            cell = ctx.cell,
            powerActive = ctx.world.powerTicksLeft > 0,
        )
    }

    fun drawSkin(scope: DrawScope, ctx: PacMazeMapRenderContext, pac: PacMazeEntity, center: Offset) {
        drawSkinSafe(scope, ctx, pac, center)
    }

    fun drawSkinSafe(scope: DrawScope, ctx: PacMazeMapRenderContext, pac: PacMazeEntity, center: Offset) {
        val skinId = ctx.avatarLoadout.skinId
        val isAssetBitmap = PacMazeSkinRenderProfileCatalog.isAssetBitmap(skinId)
        val ikun = PacMazeIkunCatalog.contains(skinId) && !isAssetBitmap
        val isBitmap = PacMazeSkinRenderProfileCatalog.isBitmapResource(skinId)
        val visualCell = min(ctx.cellX, ctx.cellY)
        // ikun 身高跟纵向格高走，避免地图拉宽时相对地板错位/缩小
        val verticalCell = if (ikun) ctx.cellY else max(ctx.cellX, ctx.cellY)
        val radius = PacMazeCosmeticCatalog.visualRadius(
            cell = visualCell,
            loadout = ctx.avatarLoadout,
            userDrawScale = ctx.playerDrawScale,
            entityDrawBoost = ctx.entityDrawBoost,
            minRadiusPx = ctx.minPlayerRadiusPx,
        )
        val pose = PacMazeCharacterDraw.poseFrom(
            entity = pac,
            animPhase = ctx.animPhase,
            powerTicksLeft = ctx.world.powerTicksLeft,
            attackCooldownTicksLeft = ctx.world.attackCooldownTicksLeft,
            attackCooldownTotal = PacMazeCosmeticCatalog.attackCooldownTicks(ctx.avatarLoadout.skinId),
            speedBoostTicksLeft = ctx.world.speedBoostTicksLeft,
        )
        val travelFacing = PacMazeEntityVisuals.travelFacing(pac) ?: pac.facing
        val travelAxis = ctx.entityTravelDirection(pac) ?: travelFacing
        if (ctx.world.tick == 0L && PacMazeSkinRenderProfileCatalog.isBitmapResource(skinId)) {
            PacMazeBitmapFacingState.clear(pac.id)
        }
        val drawFacing = if (PacMazeSkinRenderProfileCatalog.isBitmapResource(skinId)) {
            PacMazeSkinRenderProfileCatalog.resolveBitmapDrawFacing(skinId, pac)
        } else {
            PacMazeSkinRenderProfileCatalog.resolveDrawFacing(skinId, travelFacing)
        }
        // 竖走通道宽 = cellX，横走 = cellY；避免 min(cellX,cellY) 低估宽度
        val corridorCell = if (isBitmap || ikun) {
            PacMazeIkunGameplayScale.corridorFitCellPx(ctx.cellX, ctx.cellY)
        } else {
            visualCell
        }
        val feetAnchor = when {
            !isBitmap -> null
            ikun -> ctx.ikunDrawAnchor(pac)
            else -> ctx.bitmapDrawAnchor(pac)
        }
        val tileBottomY = if (isBitmap && !ikun) {
            val horizontal = travelAxis == Direction.LEFT || travelAxis == Direction.RIGHT
            ctx.entityCorridorFloorY(pac, snapToTileRow = horizontal)
        } else null
        try {
            PacMazeSkinRegistry.draw(
                scope = scope,
                skinId = skinId,
                center = center,
                radius = radius,
                pose = pose,
                themeId = ctx.config.id,
                palette = ctx.config.palette,
                corridorCellPx = corridorCell,
                verticalCellPx = verticalCell,
                tileCellPx = ctx.cellY,
                tileBottomYPx = tileBottomY,
                feetAnchorPx = feetAnchor,
                visualFacing = drawFacing,
                travelFacing = travelAxis,
                userDrawScale = ctx.playerDrawScale,
                entityDrawBoost = ctx.entityDrawBoost,
            )
        } catch (_: Throwable) {
            scope.drawCircle(
                color = Color.White.copy(alpha = 0.92f),
                radius = radius.coerceAtLeast(ctx.minPlayerRadiusPx),
                center = center,
            )
        }
    }

    fun pushTrailSample(
        buffer: com.example.funlife.ui.screens.pacmaze.cosmetic.trail.PacMazeTrailBuffer,
        screenPos: Offset,
        pac: PacMazeEntity,
        powerTicksLeft: Int,
        cell: Float,
        cellX: Float = cell,
        cellY: Float = cell,
        trailDepthPx: Float = cell * 0.28f,
    ) {
        if (!isTrailMotionActive(pac)) {
            buffer.reset()
            return
        }
        val travelFacing = PacMazeEntityVisuals.travelFacing(pac)
        val (ox, oy) = PacMazeEntityVisuals.trailAnchorOffset(
            velX = pac.velX,
            velY = pac.velY,
            fallbackFacing = travelFacing,
            cellX = cellX,
            cellY = cellY,
            trailDepthPx = trailDepthPx,
        )
        val screenVel = Offset(pac.velX * cellX, pac.velY * cellY)
        buffer.push(
            position = screenPos + Offset(ox, oy),
            velocity = screenVel,
            powerBoost = powerTicksLeft > 0,
            minStepPx = kotlin.math.min(cellX, cellY) * 0.06f,
        )
    }
}
