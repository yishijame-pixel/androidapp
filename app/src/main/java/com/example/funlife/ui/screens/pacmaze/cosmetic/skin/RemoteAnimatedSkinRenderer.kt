package com.example.funlife.ui.screens.pacmaze.cosmetic.skin

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.funlife.social.game.engine.pacmaze.Direction
import com.example.funlife.ui.screens.pacmaze.character.PacMazeCharacterPose
import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeIkunCatalog
import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeSkinId
import com.example.funlife.ui.screens.pacmaze.maptheme.PacMazeMapThemeId
import com.example.funlife.ui.screens.pacmaze.maptheme.PacMazeThemePalette

internal class RemoteAnimatedSkinRenderer(
    override val skinId: PacMazeSkinId,
) : PacMazeSkinRenderer {

    private val gameplayDiameterMul: Float
        get() = 2.25f

    private val gameplayCellHeightFrac: Float
        get() = if (PacMazeIkunCatalog.contains(skinId)) PacMazeSkinBitmapDraw.IKUN_CELL_HEIGHT_FRAC
        else PacMazeSkinBitmapDraw.defaultCellHeightFrac

    private val gameplayCellWidthFrac: Float
        get() = if (PacMazeIkunCatalog.contains(skinId)) PacMazeSkinBitmapDraw.IKUN_CELL_WIDTH_FRAC
        else PacMazeSkinBitmapDraw.defaultCellWidthFrac

    private val tallGameplay: Boolean
        get() = PacMazeIkunCatalog.contains(skinId)

    override fun draw(
        scope: DrawScope,
        center: Offset,
        radius: Float,
        pose: PacMazeCharacterPose,
        themeId: PacMazeMapThemeId,
        palette: PacMazeThemePalette,
    ) {
        val corridorCellPx = PacMazeSkinRegistry.drawCorridorCellPx
            ?: PacMazeSkinBitmapDraw.estimateCorridorCellPx(radius)
        val verticalCellPx = PacMazeSkinRegistry.drawVerticalCellPx ?: corridorCellPx
        val tileCellPx = PacMazeSkinRegistry.drawTileCellPx ?: corridorCellPx

        if (pose.preferCoverOnly) {
            PacMazeRemoteSkinAnimCache.cover(skinId)?.let { cover ->
                drawBitmap(scope, center, radius, corridorCellPx, verticalCellPx, tileCellPx, cover, pose, walkBob = false)
            }
            return
        }

        val clip = PacMazeRemoteSkinAnimCatalog.pickClip(skinId, pose)
        PacMazeRemoteSkinAnimCache.requestClipAsync(skinId, clip)
        val frames = resolveFrames(skinId, clip, pose)
        if (frames != null) {
            val index = PacMazeRemoteSkinAnimCatalog.frameIndex(skinId, pose, clip, frames.size)
            val image = frames.getOrElse(index) { frames.first() }
            val walkBob = clip == PacMazeSkinAnimClip.WALK && pose.isMoving
            drawBitmap(
                scope, center, radius, corridorCellPx, verticalCellPx, tileCellPx,
                image, pose, walkBob = walkBob, clip = clip, frameCount = frames.size,
            )
            return
        }

        PacMazeRemoteSkinAnimCache.cover(skinId)?.let { cover ->
            drawBitmap(scope, center, radius, corridorCellPx, verticalCellPx, tileCellPx, cover, pose, walkBob = false)
        }
    }

    private fun resolveFrames(
        skinId: PacMazeSkinId,
        clip: PacMazeSkinAnimClip,
        pose: PacMazeCharacterPose,
    ): List<androidx.compose.ui.graphics.ImageBitmap>? {
        PacMazeRemoteSkinAnimCache.playbackFrames(skinId, clip)?.let { return it }
        if (clip != PacMazeSkinAnimClip.WALK) {
            PacMazeRemoteSkinAnimCache.playbackFrames(skinId, PacMazeSkinAnimClip.WALK)?.let { return it }
        }
        if (!pose.isMoving) {
            PacMazeRemoteSkinAnimCache.playbackFrames(skinId, PacMazeSkinAnimClip.IDLE)?.let { return it }
        }
        PacMazeRemoteSkinAnimCache.peekSingleWalkFrame(skinId)?.let { single ->
            return listOf(single)
        }
        return null
    }

    private fun drawBitmap(
        scope: DrawScope,
        center: Offset,
        radius: Float,
        corridorCellPx: Float,
        verticalCellPx: Float,
        tileCellPx: Float,
        image: androidx.compose.ui.graphics.ImageBitmap,
        pose: PacMazeCharacterPose,
        walkBob: Boolean = false,
        clip: PacMazeSkinAnimClip? = null,
        frameCount: Int = 1,
    ) {
        if (pose.powerActive) {
            FamilySkinHelpers.drawSoftPowerAura(
                scope = scope,
                center = center,
                radius = radius,
                accent = Color(0xFFFFEB3B),
                active = true,
            )
        }

        val bobOffset = when {
            PacMazeSkinRegistry.drawFeetAnchorPx != null &&
                (PacMazeSkinRegistry.drawTravelFacing == Direction.LEFT ||
                    PacMazeSkinRegistry.drawTravelFacing == Direction.RIGHT) -> 0f
            clip == PacMazeSkinAnimClip.WALK && pose.isMoving &&
                PacMazeRemoteSkinAnimCatalog.config(skinId)?.syncWalkCycleToSprite == true -> 0f
            walkBob && clip == PacMazeSkinAnimClip.WALK && pose.isMoving &&
                !PacMazeSkinRenderProfileCatalog.shouldDisableExtraWalkBob(skinId) -> {
                PacMazeSpriteWalkAnim.walkBob(radius, pose, frameCount.coerceAtLeast(1)) * 0.35f
            }
            else -> 0f
        }

        val drawFacing = PacMazeSkinRegistry.drawVisualFacing
            ?: PacMazeSkinRenderProfileCatalog.resolveDrawFacing(skinId, pose.facing)
        PacMazeSkinBitmapDraw.draw(
            scope = scope,
            center = center,
            radius = radius,
            corridorCellPx = corridorCellPx,
            image = image,
            facing = drawFacing,
            walkBob = bobOffset,
            diameterMul = gameplayDiameterMul,
            cellHeightFrac = gameplayCellHeightFrac,
            cellWidthFrac = gameplayCellWidthFrac,
            tallGameplay = tallGameplay,
            verticalCellPx = verticalCellPx,
            tileCellPx = tileCellPx,
            tileBottomY = PacMazeSkinRegistry.drawTileBottomYPx,
            skinId = skinId,
        )

        if (clip == PacMazeSkinAnimClip.ATTACK) {
            val feetY = center.y + radius * 0.18f + bobOffset
            scope.drawCircle(
                color = Color(0xFFFF7043).copy(alpha = 0.28f),
                radius = radius * 1.18f,
                center = Offset(center.x, feetY),
                style = Stroke(width = radius * 0.08f),
            )
        }
    }
}
