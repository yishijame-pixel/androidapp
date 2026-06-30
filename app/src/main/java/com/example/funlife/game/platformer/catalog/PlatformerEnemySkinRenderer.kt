package com.example.funlife.game.platformer.catalog

import com.example.funlife.game.platformer.PLATFORMER_TILE_PX
import com.example.funlife.game.platformer.PlatformerEnemyBehavior
import com.example.funlife.game.platformer.PlatformerPlayerSprites
import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeIkunGameplayScale
import com.example.funlife.ui.screens.pacmaze.cosmetic.skin.PacMazeSkinAnimManifest
import com.example.funlife.ui.screens.pacmaze.cosmetic.skin.PacMazeSkinSheetPlayback

/**
 * Catalog 敌人绘制：sprite sheet 主路径（整张 decode 一次 + srcRect 切帧）。
 */
object PlatformerEnemySkinRenderer {

    fun resolveSheetDraw(
        catalogId: String,
        behavior: PlatformerEnemyBehavior,
        animPhase: Float,
    ): PlatformerRemoteAnimCache.CatalogSheetDraw? =
        PlatformerRemoteAnimCache.resolveEnemySheetDraw(catalogId, behavior, animPhase)

    fun layoutHeightFrac(catalogId: String): Float =
        PlatformerRemoteAnimCache.configById(catalogId)?.heightCellFrac
            ?: PlatformerEnemyCatalog.heightCellFrac(catalogId)

    fun layoutForDrawSheet(
        catalogId: String,
        sheet: PacMazeSkinSheetPlayback,
        cellPx: Float,
        tilePx: Int,
    ): PlatformerPlayerSprites.ChickSpriteLayout {
        val cfg = PlatformerRemoteAnimCache.configById(catalogId)
        val heightFrac = layoutHeightFrac(catalogId)
        val scale = (tilePx / PLATFORMER_TILE_PX.toFloat()).coerceIn(0.5f, 1f)
        val targetH = cellPx * heightFrac * scale
        val assetRoot = cfg?.assetRoot
        val manifest = assetRoot?.let { PacMazeSkinAnimManifest.load(it) }
        val refW = manifest?.canvas?.w ?: sheet.cellW
        val refH = manifest?.canvas?.h ?: sheet.cellH
        val feet = manifest?.anchorFrac?.let { it.y to it.x } ?: (0.88f to 0.5f)
        val feetY = (feet.first - PacMazeIkunGameplayScale.PLATFORMER_FEET_FRAC_BIAS)
            .coerceIn(PacMazeIkunGameplayScale.FEET_Y_FRAC_MIN, 0.999f)
        val feetX = feet.second.coerceIn(0.12f, 0.88f)
        val refHf = refH.coerceAtLeast(1).toFloat()
        val refWf = refW.coerceAtLeast(1).toFloat()
        val boxH = targetH
        val boxW = refWf * (targetH / refHf)
        val fitScale = minOf(
            boxW / sheet.cellW.coerceAtLeast(1),
            boxH / sheet.cellH.coerceAtLeast(1),
        )
        val frameW = sheet.cellW * fitScale
        val frameH = sheet.cellH * fitScale
        val anchorX = boxW * feetX
        val anchorY = targetH * feetY
        return PlatformerPlayerSprites.ChickSpriteLayout(
            dstW = boxW,
            dstH = boxH,
            feetYFrac = feetY,
            feetXFrac = feetX,
            frameW = frameW,
            frameH = frameH,
            frameOx = anchorX - frameW * feetX,
            frameOy = anchorY - frameH * feetY,
        )
    }

    /** Craftpix 敌人素材默认朝右；与玩家 catalog 角色一致。 */
    fun mirrorHorizontally(catalogId: String, facingRight: Boolean): Boolean {
        val assetRoot = PlatformerRemoteAnimCache.configById(catalogId)?.assetRoot
        val render = assetRoot?.let { PacMazeSkinAnimManifest.load(it)?.render }
        val facesRight = when (render?.invertBitmapFacing) {
            true -> false
            false -> true
            null -> true
        }
        return facingRight != facesRight
    }
}
