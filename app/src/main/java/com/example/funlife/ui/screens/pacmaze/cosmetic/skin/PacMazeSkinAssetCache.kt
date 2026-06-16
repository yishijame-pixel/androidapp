package com.example.funlife.ui.screens.pacmaze.cosmetic.skin

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeSkinId
import kotlin.math.max
import kotlin.math.min

/**
 * 皮肤位图缓存：静态图 + 序列帧（sprite sheet）。
 * 仅从画面边缘 flood-fill 去背景，保留眼白等内部白色。
 */
object PacMazeSkinAssetCache {

    enum class AssetPrep {
        /** 仅去除与边缘相连的白边（保留眼白） */
        FACE_YELLOW,
        /** 仅去除与边缘相连的黑底（保留眼白） */
        FULL_BLACK_KEY,
    }

    private data class StaticAsset(val path: String, val prep: AssetPrep)

    private val staticAssets: Map<PacMazeSkinId, StaticAsset> = mapOf(
        PacMazeSkinId.FOOD_CHICK_DAZE to StaticAsset("pac_maze/skins/food_chick_daze.png", AssetPrep.FACE_YELLOW),
        PacMazeSkinId.FOOD_CHICK_BALLER to StaticAsset("pac_maze/skins/food_chick_baller.png", AssetPrep.FULL_BLACK_KEY),
    )

    private val spriteSheetAssets: Map<PacMazeSkinId, SpriteSheetAssetConfig> = mapOf(
        PacMazeSkinId.FOOD_CHICK_WALKER to SpriteSheetAssetConfig(
            assetPath = "pac_maze/skins/food_chick_walk_sheet.png",
            columns = 2,
            rows = 2,
            frameCount = 4,
            prep = AssetPrep.FACE_YELLOW,
            topCropRatio = 0.18f,
        ),
    )

    private val bitmaps = mutableMapOf<PacMazeSkinId, ImageBitmap>()
    private val spriteSheets = mutableMapOf<PacMazeSkinId, PacMazeSkinSpriteSheet>()

    fun ensureLoaded(context: Context) {
        val app = context.applicationContext
        staticAssets.forEach { (skinId, asset) ->
            if (bitmaps.containsKey(skinId)) return@forEach
            runCatching {
                app.assets.open(asset.path).use { stream ->
                    BitmapFactory.decodeStream(stream)?.let { prepareSkinBitmap(it, asset.prep) }?.asImageBitmap()
                }
            }.getOrNull()?.let { bitmap ->
                PacMazeBitmapFeetAnchor.registerGameplayAnchor(skinId, bitmap, asDefault = true)
                bitmaps[skinId] = bitmap
            }
        }
        spriteSheetAssets.forEach { (skinId, config) ->
            if (spriteSheets.containsKey(skinId)) return@forEach
            runCatching {
                app.assets.open(config.assetPath).use { stream ->
                    BitmapFactory.decodeStream(stream)?.let { splitSpriteSheet(skinId, it, config) }
                }
            }.getOrNull()?.let { spriteSheets[skinId] = it }
        }
    }

    fun bitmap(skinId: PacMazeSkinId): ImageBitmap? = bitmaps[skinId]

    internal fun spriteSheet(skinId: PacMazeSkinId): PacMazeSkinSpriteSheet? = spriteSheets[skinId]

    fun hasAsset(skinId: PacMazeSkinId): Boolean = skinId in staticAssets || skinId in spriteSheetAssets

    private fun splitSpriteSheet(
        skinId: PacMazeSkinId,
        source: Bitmap,
        config: SpriteSheetAssetConfig,
    ): PacMazeSkinSpriteSheet {
        val cellW = source.width / config.columns
        val cellH = source.height / config.rows
        val topCrop = (cellH * config.topCropRatio).toInt()
        val frames = buildList {
            var index = 0
            for (row in 0 until config.rows) {
                for (col in 0 until config.columns) {
                    if (index >= config.frameCount) break
                    val cell = Bitmap.createBitmap(source, col * cellW, row * cellH, cellW, cellH)
                    val trimmed = if (topCrop > 0 && topCrop < cellH - 8) {
                        Bitmap.createBitmap(cell, 0, topCrop, cellW, cellH - topCrop)
                    } else {
                        cell
                    }
                    if (trimmed !== cell) cell.recycle()
                    val prepared = prepareSkinBitmap(trimmed, config.prep)
                    if (prepared !== trimmed) trimmed.recycle()
                    val frameBitmap = prepared.asImageBitmap()
                    PacMazeBitmapFeetAnchor.registerGameplayAnchor(
                        skinId = skinId,
                        image = frameBitmap,
                        asDefault = index == 0,
                    )
                    add(frameBitmap)
                    index++
                }
            }
        }
        source.recycle()
        check(frames.size == config.frameCount) {
            "Sprite sheet ${config.assetPath} expected ${config.frameCount} frames, got ${frames.size}"
        }
        return PacMazeSkinSpriteSheet(frames = frames, columns = config.columns, rows = config.rows)
    }

    private fun prepareSkinBitmap(source: Bitmap, mode: AssetPrep): Bitmap {
        val mutable = source.copy(Bitmap.Config.ARGB_8888, true)
        val w = mutable.width
        val h = mutable.height
        removeEdgeBackground(mutable, mode)

        var minX = w
        var minY = h
        var maxX = 0
        var maxY = 0
        for (y in 0 until h) {
            for (x in 0 until w) {
                if (Color.alpha(mutable.getPixel(x, y)) > 12) {
                    minX = min(minX, x)
                    minY = min(minY, y)
                    maxX = max(maxX, x)
                    maxY = max(maxY, y)
                }
            }
        }

        if (maxX <= minX || maxY <= minY) return mutable

        val cw = maxX - minX + 1
        val ch = maxY - minY + 1
        val cropped = Bitmap.createBitmap(mutable, minX, minY, cw, ch)
        val side = max(cw, ch)
        val square = Bitmap.createBitmap(side, side, Bitmap.Config.ARGB_8888)
        Canvas(square).apply {
            drawColor(Color.TRANSPARENT)
            drawBitmap(cropped, (side - cw) / 2f, (side - ch) / 2f, null)
        }
        if (cropped !== mutable) cropped.recycle()
        if (mutable !== source) mutable.recycle()
        return square
    }

    private fun removeEdgeBackground(bitmap: Bitmap, mode: AssetPrep) {
        val w = bitmap.width
        val h = bitmap.height
        val visited = BooleanArray(w * h)
        val queue = ArrayDeque<Pair<Int, Int>>()

        fun key(x: Int, y: Int) = y * w + x
        fun matchesBg(x: Int, y: Int): Boolean = isRemovableBackground(bitmap.getPixel(x, y), mode)

        fun enqueue(x: Int, y: Int) {
            if (x !in 0 until w || y !in 0 until h) return
            val k = key(x, y)
            if (visited[k] || !matchesBg(x, y)) return
            visited[k] = true
            queue.add(x to y)
        }

        for (x in 0 until w) {
            enqueue(x, 0)
            enqueue(x, h - 1)
        }
        for (y in 0 until h) {
            enqueue(0, y)
            enqueue(w - 1, y)
        }

        while (queue.isNotEmpty()) {
            val (x, y) = queue.removeFirst()
            bitmap.setPixel(x, y, Color.TRANSPARENT)
            enqueue(x - 1, y)
            enqueue(x + 1, y)
            enqueue(x, y - 1)
            enqueue(x, y + 1)
        }
    }

    private fun isRemovableBackground(color: Int, mode: AssetPrep): Boolean {
        val a = Color.alpha(color)
        if (a < 12) return true
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)
        return when (mode) {
            AssetPrep.FACE_YELLOW -> r > 235 && g > 235 && b > 235
            AssetPrep.FULL_BLACK_KEY -> r < 28 && g < 28 && b < 28
        }
    }
}
