package com.example.funlife.game.platformer.catalog

import androidx.compose.ui.graphics.ImageBitmap
import com.example.funlife.game.platformer.PlatformerCharacterId
import com.example.funlife.game.platformer.PlatformerPlayer
import com.example.funlife.game.platformer.PlatformerPlayerSprites
import com.example.funlife.game.platformer.PLATFORMER_TILE_PX
import com.example.funlife.resource.ResourceStore
import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeIkunGameplayScale
import com.example.funlife.ui.screens.pacmaze.cosmetic.skin.PacMazeSkinAnimClip
import com.example.funlife.ui.screens.pacmaze.cosmetic.skin.PacMazeSkinAnimManifest
import com.example.funlife.ui.screens.pacmaze.cosmetic.skin.PacMazeSkinSheetPlayback

/**
 * Catalog 驱动横版角色绘制：manifest 归一化布局 + 远程序列帧。
 */
object PlatformerSkinRenderer {

    /** 目标「可见身体高度」→ 归一化整格 canvas 高度（walk-only 高瘦 sheet 勿按整格 2 格高缩放）。 */
    private fun canvasTargetHeight(
        assetRoot: String?,
        desiredOpaqueHeightPx: Float,
    ): Float {
        if (assetRoot.isNullOrBlank()) return desiredOpaqueHeightPx
        val manifest = PacMazeSkinAnimManifest.load(assetRoot) ?: return desiredOpaqueHeightPx
        val oh = manifest.minOpaqueContentSpanFrac(PacMazeSkinAnimClip.WALK)?.first
            ?.coerceIn(0.35f, 1f) ?: 0.75f
        return desiredOpaqueHeightPx / oh
    }

    private fun defaultHeightCellFrac(): Float =
        PacMazeIkunGameplayScale.PLATFORMER_HEIGHT_CELL_FRAC

    fun layoutForDraw(
        characterId: PlatformerCharacterId,
        frame: ImageBitmap,
        player: PlatformerPlayer,
        cellPx: Float,
        tilePx: Int,
    ): PlatformerPlayerSprites.ChickSpriteLayout {
        val entry = PlatformerContentCatalog.characterForEnum(characterId)
        val cfg = PlatformerRemoteAnimCache.config(characterId)
        val assetRoot = cfg?.assetRoot ?: entry?.assetRoot
        val heightFrac = entry?.render?.heightCellFrac ?: cfg?.heightCellFrac
            ?: defaultHeightCellFrac()
        val scale = (tilePx / PLATFORMER_TILE_PX.toFloat()).coerceIn(0.5f, 1f)
        val desiredOpaqueH = cellPx * heightFrac * scale
        val targetH = canvasTargetHeight(assetRoot, desiredOpaqueH)
        if (assetRoot != null) {
            layoutFromManifest(assetRoot, targetH, frame, player, cellPx)?.let { return it }
        }
        if (characterId == PlatformerCharacterId.CHICK_PRO_MAX) {
            return PlatformerPlayerSprites.layoutForDraw(frame, player, cellPx, tilePx)
        }
        val aspect = frame.width.toFloat() / frame.height.coerceAtLeast(1)
        val dstH = targetH
        val dstW = dstH * aspect
        return PlatformerPlayerSprites.ChickSpriteLayout(
            dstW = dstW,
            dstH = dstH,
            feetYFrac = 0.88f,
            feetXFrac = 0.5f,
            frameW = dstW,
            frameH = dstH,
        )
    }

    private fun layoutFromManifest(
        assetRoot: String,
        targetH: Float,
        frame: ImageBitmap,
        player: PlatformerPlayer,
        cellPx: Float,
    ): PlatformerPlayerSprites.ChickSpriteLayout? {
        val manifest = PacMazeSkinAnimManifest.load(assetRoot) ?: return null
        if (!manifest.normalized) return null
        val canvas = manifest.canvas ?: return null
        val feet = manifest.anchorFrac ?: return null
        return PlatformerPlayerSprites.layoutNormalizedFrame(
            targetH = targetH,
            canvasW = canvas.w,
            canvasH = canvas.h,
            feetY = feet.y,
            feetX = feet.x,
        )
    }

    fun mirrorHorizontally(characterId: PlatformerCharacterId, facingRight: Boolean): Boolean {
        val entry = PlatformerContentCatalog.characterForEnum(characterId)
        return when (characterId) {
            PlatformerCharacterId.TREASURE_HUNTER, PlatformerCharacterId.PIXEL_WALKER -> !facingRight
            PlatformerCharacterId.CHICK_PRO_MAX -> PlatformerPlayerSprites.mirrorHorizontally(facingRight)
            else -> {
                val assetFacesRight = catalogAssetDefaultFacesRight(characterId, entry)
                facingRight != assetFacesRight
            }
        }
    }

    /**
     * Craftpix 横版序列帧默认朝右；manifest.render.invertBitmapFacing 可显式覆盖。
     * legacy mirrorDefault=true 表示「素材默认朝左」，与 Craftpix 相反，此处以 invert 优先。
     */
    private fun catalogAssetDefaultFacesRight(
        characterId: PlatformerCharacterId,
        entry: PlatformerContentCatalog.CharacterEntry?,
    ): Boolean {
        val assetRoot = PlatformerRemoteAnimCache.config(characterId)?.assetRoot ?: entry?.assetRoot
        val render = assetRoot?.let { PacMazeSkinAnimManifest.load(it)?.render }
        when (render?.invertBitmapFacing) {
            true -> return false
            false -> return true
            null -> Unit
        }
        // Craftpix 横版序列帧原图默认朝右（content_catalog 里 mirrorDefault 批量写反了，此处不再沿用）
        return true
    }

    fun layoutForDrawSheet(
        characterId: PlatformerCharacterId,
        sheet: PacMazeSkinSheetPlayback,
        player: PlatformerPlayer,
        cellPx: Float,
        tilePx: Int,
    ): PlatformerPlayerSprites.ChickSpriteLayout {
        val entry = PlatformerContentCatalog.characterForEnum(characterId)
        val cfg = PlatformerRemoteAnimCache.config(characterId)
        val assetRoot = cfg?.assetRoot ?: entry?.assetRoot
        val heightFrac = entry?.render?.heightCellFrac ?: cfg?.heightCellFrac
            ?: defaultHeightCellFrac()
        val scale = (tilePx / PLATFORMER_TILE_PX.toFloat()).coerceIn(0.5f, 1f)
        val desiredOpaqueH = cellPx * heightFrac * scale
        val targetH = canvasTargetHeight(assetRoot, desiredOpaqueH)
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

    fun isManifestNormalized(characterId: PlatformerCharacterId): Boolean {
        val cfg = PlatformerRemoteAnimCache.config(characterId) ?: return false
        return PacMazeSkinAnimManifest.load(cfg.assetRoot)?.normalized == true
    }
}
