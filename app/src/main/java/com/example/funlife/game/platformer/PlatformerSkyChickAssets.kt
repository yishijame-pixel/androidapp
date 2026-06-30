package com.example.funlife.game.platformer

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

/** 天空小鸡精灵图：8×2 格，1024×256。 */
object PlatformerSkyChickAssets {

    private const val ASSET_PATH = "platformer/sky_chick_enemy.png"
    private var cachedSheet: ImageBitmap? = null
    private var cachedCellW: Int = 0
    private var cachedCellH: Int = 0

    fun sheet(context: android.content.Context): ImageBitmap? {
        cachedSheet?.let { return it }
        return runCatching {
            val opts = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.ARGB_8888
                inScaled = false
            }
            context.assets.open(ASSET_PATH).use { stream ->
                BitmapFactory.decodeStream(stream, null, opts)?.asImageBitmap()?.also { bmp ->
                    cachedSheet = bmp
                    cachedCellW = bmp.width / PlatformerSkyChickSystem.SHEET_COLS
                    cachedCellH = bmp.height / PlatformerSkyChickSystem.SHEET_ROWS
                }
            }
        }.getOrNull()
    }

    fun loadInto(assets: PlatformerAssets, context: android.content.Context): PlatformerAssets {
        val sheet = sheet(context) ?: return assets
        return PlatformerAssets(
            atlas = assets.atlas,
            packTilesets = assets.packTilesets,
            enemyAssets = assets.enemyAssets,
            characterAssets = assets.characterAssets,
            tmxBitmaps = assets.tmxBitmaps,
            chickBasketball = assets.chickBasketball,
            skyChickSheet = sheet,
        )
    }

    fun cellSize(sheet: ImageBitmap): Pair<Int, Int> {
        if (cachedCellW > 0 && cachedCellH > 0) return cachedCellW to cachedCellH
        return sheet.width / PlatformerSkyChickSystem.SHEET_COLS to
            sheet.height / PlatformerSkyChickSystem.SHEET_ROWS
    }

    fun frameRect(frameIndex: Int, cellW: Int, cellH: Int): FrameRect {
        val col = frameIndex % PlatformerSkyChickSystem.SHEET_COLS
        val row = frameIndex / PlatformerSkyChickSystem.SHEET_COLS
        return FrameRect(col * cellW, row * cellH, cellW, cellH)
    }

    data class FrameRect(val srcX: Int, val srcY: Int, val w: Int, val h: Int)
}
