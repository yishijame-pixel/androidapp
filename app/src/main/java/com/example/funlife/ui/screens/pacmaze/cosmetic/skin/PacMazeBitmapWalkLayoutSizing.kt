package com.example.funlife.ui.screens.pacmaze.cosmetic.skin

import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeIkunGameplayScale
import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeSkinId

/** 行走皮肤局内稳态尺寸 + 运行时贴墙钳制。 */
internal object PacMazeBitmapWalkLayoutSizing {

    private var cacheKey: String? = null
    private var cacheWidth: Float = 0f
    private var cacheHeight: Float = 0f

    fun invalidate() {
        cacheKey = null
        cacheWidth = 0f
        cacheHeight = 0f
    }

    private val sizingClip get() = PacMazeSkinAnimClip.WALK

    fun canonicalLayoutAspect(skinId: PacMazeSkinId): Float {
        val manifest = PacMazeSkinAnimManifest.loadForSkin(skinId) ?: return 0.72f
        val sheet = manifest.clipSheet(sizingClip)
            ?: manifest.clips.values.firstNotNullOfOrNull { it.sheet }
            ?: return 0.72f
        if (sheet.cellH <= 0 || sheet.cellW <= 0) return 0.72f
        val cellAspect = sheet.cellW.toFloat() / sheet.cellH
        val content = contentBoundsFor(skinId)
        return (cellAspect * content.opaqueWidthFrac / content.opaqueHeightFrac).coerceIn(0.35f, 3.5f)
    }

    /** 定标用内容占比：manifest 原始值，不截断（避免角色过小）。 */
    fun layoutOpaqueSpan(skinId: PacMazeSkinId, @Suppress("UNUSED_PARAMETER") clip: PacMazeSkinAnimClip): PacMazeSheetCellFeetCache.ContentSpan =
        PacMazeSheetCellFeetCache.manifestContentSpanForLayout(skinId, sizingClip)

    fun contentBoundsFor(skinId: PacMazeSkinId): PacMazeBitmapCorridorCenterFit.ContentBoundsFrac {
        val manifest = PacMazeSkinAnimManifest.loadForSkin(skinId)
        manifest?.let { loaded ->
            PacMazeBitmapCorridorCenterFit.fromManifestPlatformer(loaded, sizingClip)?.let { return it }
        }
        val span = layoutOpaqueSpan(skinId, sizingClip)
        return PacMazeBitmapCorridorCenterFit.fromLayoutSpan(span.minHeightFrac, span.minWidthFrac)
    }

    fun walkContentCenterPivot(skinId: PacMazeSkinId): Pair<Float, Float> =
        contentBoundsFor(skinId).pivot()

    fun walkGameplayPivot(skinId: PacMazeSkinId): Pair<Float, Float> = walkContentCenterPivot(skinId)

    fun manifestWalkCellSize(skinId: PacMazeSkinId): Pair<Int, Int>? {
        val manifest = PacMazeSkinAnimManifest.loadForSkin(skinId) ?: return null
        val sheet = manifest.clipSheet(sizingClip)
            ?: manifest.clips.values.firstNotNullOfOrNull { it.sheet }
            ?: return null
        if (sheet.cellW <= 0 || sheet.cellH <= 0) return null
        return sheet.cellW to sheet.cellH
    }

    /**
     * 布局定标用格尺寸：normalized 皮肤走 manifest.canvas；walk 已归一化而 attack/jump 仍为大格时对齐 walk。
     * 绘制仍用 [sheet] 的 srcRect，仅避免局内换 clip 时角色突然缩小。
     */
    fun resolveSheetLayoutCellSize(
        skinId: PacMazeSkinId,
        clip: PacMazeSkinAnimClip,
        sheet: PacMazeSkinSheetPlayback,
    ): Pair<Int, Int> {
        val manifest = PacMazeSkinAnimManifest.loadForSkin(skinId)
        if (manifest?.normalized == true) {
            manifest.canvas?.let { c ->
                if (c.w > 0 && c.h > 0) return c.w to c.h
            }
        }
        manifestWalkCellSize(skinId)?.let { (walkW, walkH) ->
            if (clip != PacMazeSkinAnimClip.WALK &&
                (sheet.cellW > walkW * 3 / 2 || sheet.cellH > walkH * 3 / 2)
            ) {
                return walkW to walkH
            }
        }
        return sheet.cellW to sheet.cellH
    }

    fun resolveSheetLayoutFeetFracY(
        skinId: PacMazeSkinId,
        clip: PacMazeSkinAnimClip,
        sheet: PacMazeSkinSheetPlayback,
    ): Float? {
        val (layoutW, layoutH) = resolveSheetLayoutCellSize(skinId, clip, sheet)
        if (layoutW == sheet.cellW && layoutH == sheet.cellH) return null
        val manifest = PacMazeSkinAnimManifest.loadForSkin(skinId)
        manifest?.anchorFrac?.y?.let { y ->
            return y.coerceIn(PacMazeIkunGameplayScale.FEET_Y_FRAC_MIN, 0.999f)
        }
        return null
    }

    fun resolveGameplaySize(skinId: PacMazeSkinId): Pair<Float, Float> {
        val cellX = PacMazeSkinRegistry.drawCellXPx ?: PacMazeSkinRegistry.drawCorridorCellPx ?: 48f
        val cellY = PacMazeSkinRegistry.drawCellYPx ?: PacMazeSkinRegistry.drawVerticalCellPx ?: cellX
        val userScale = PacMazeSkinRegistry.drawUserScale
        val aspect = canonicalLayoutAspect(skinId)
        val content = contentBoundsFor(skinId)
        val key = "${skinId.storageKey}:${sizingClip.name}:${"%.2f".format(cellX)}:${"%.2f".format(cellY)}:" +
            "${"%.3f".format(userScale)}:${"%.3f".format(aspect)}:" +
            "${"%.3f".format(content.opaqueHeightFrac)}"
        if (key == cacheKey && cacheWidth > 1f && cacheHeight > 1f) {
            return cacheWidth to cacheHeight
        }

        val fit = PacMazeBitmapCorridorCenterFit.resolveStableSize(
            aspect = aspect,
            content = content,
            cellX = cellX,
            cellY = cellY,
            visualScale = PacMazeIkunGameplayScale.bitmapLayoutVisualScale(userScale),
        )
        cacheKey = key
        cacheWidth = fit.width
        cacheHeight = fit.height
        return fit.width to fit.height
    }

    /** 与 [resolveGameplaySize] 相同：全图统一稳态尺寸，位置变化不缩不放。 */
    fun resolveGameplaySizeAt(skinId: PacMazeSkinId): Pair<Float, Float> = resolveGameplaySize(skinId)
}
