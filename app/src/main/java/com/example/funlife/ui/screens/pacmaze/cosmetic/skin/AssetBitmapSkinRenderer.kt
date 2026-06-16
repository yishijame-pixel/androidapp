package com.example.funlife.ui.screens.pacmaze.cosmetic.skin

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.funlife.ui.screens.pacmaze.character.PacMazeCharacterPose
import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeIkunCatalog
import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeSkinId
import com.example.funlife.ui.screens.pacmaze.maptheme.PacMazeMapThemeId
import com.example.funlife.ui.screens.pacmaze.maptheme.PacMazeThemePalette
import kotlin.math.sin

internal class AssetBitmapSkinRenderer(
    override val skinId: PacMazeSkinId,
) : PacMazeSkinRenderer {

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

        val sprite = PacMazeSkinAssetCache.spriteSheet(skinId)
        val image = sprite?.pickFrame(pose) ?: PacMazeSkinAssetCache.bitmap(skinId) ?: return

        if (pose.powerActive) {
            FamilySkinHelpers.drawSoftPowerAura(
                scope = scope,
                center = center,
                radius = radius,
                accent = Color(0xFFFFEB3B),
                active = true,
            )
        }

        val walkBob = (sprite?.walkBob(radius, pose) ?: staticBob(radius, pose)).let { bob ->
            val scaled = if (PacMazeIkunCatalog.contains(skinId)) bob * 0.35f else bob
            val horizontalFeet = PacMazeSkinRegistry.drawFeetAnchorPx != null &&
                (PacMazeSkinRegistry.drawTravelFacing == com.example.funlife.social.game.engine.pacmaze.Direction.LEFT ||
                    PacMazeSkinRegistry.drawTravelFacing == com.example.funlife.social.game.engine.pacmaze.Direction.RIGHT)
            if (horizontalFeet) 0f else scaled
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
            walkBob = walkBob,
            diameterMul = spriteDiameterMul(),
            cellHeightFrac = PacMazeSkinBitmapDraw.defaultCellHeightFrac,
            cellWidthFrac = PacMazeSkinBitmapDraw.defaultCellWidthFrac,
            tallGameplay = false,
            verticalCellPx = verticalCellPx,
            tileCellPx = tileCellPx,
            tileBottomY = null,
            skinId = skinId,
            centerAnchored = true,
        )

        if (pose.powerActive) {
            val feetY = center.y + radius * 0.18f + walkBob
            scope.drawCircle(
                color = Color(0xFFFFEB3B).copy(alpha = 0.35f),
                radius = radius * 1.05f,
                center = Offset(center.x, feetY),
                style = Stroke(width = radius * 0.06f),
            )
        }
    }

    private fun PacMazeSkinSpriteSheet.pickFrame(pose: PacMazeCharacterPose): ImageBitmap {
        if (!pose.isMoving || frames.isEmpty()) return frames.first()
        val index = PacMazeSpriteWalkAnim.frameIndex(pose, frames.size)
        return frames[index]
    }

    private fun PacMazeSkinSpriteSheet.walkBob(radius: Float, pose: PacMazeCharacterPose): Float =
        PacMazeSpriteWalkAnim.walkBob(radius, pose, frames.size)

    private fun staticBob(radius: Float, pose: PacMazeCharacterPose): Float =
        sin(pose.animPhase * (if (pose.isMoving) 3.5f else 1.6f)) * radius * 0.03f

    private fun spriteDiameterMul(): Float = when (skinId) {
        PacMazeSkinId.FOOD_CHICK_WALKER -> 2.05f
        PacMazeSkinId.FOOD_CHICK_BALLER -> 2.02f
        PacMazeSkinId.FOOD_CHICK_DAZE -> 2.16f
        else -> if (PacMazeIkunCatalog.contains(skinId)) 2.25f else 1.84f
    }
}
