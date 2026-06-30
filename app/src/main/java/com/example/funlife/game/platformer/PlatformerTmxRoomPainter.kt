package com.example.funlife.game.platformer

import android.content.Context
import com.example.funlife.game.platformer.tmx.PlatformerTmxMap
import com.example.funlife.game.platformer.tmx.PlatformerTmxParser

/**
 * 将 8px TMX 房间绘制到 32px 逻辑格画布（1 TMX 格 = 1 逻辑格）。
 * 统一坐标系：主线一律 [PLATFORMER_TILE_PX]。
 */
object PlatformerTmxRoomPainter {

    /** 与 [PlatformerSegmentLibrary.SEGMENT_W] 对齐，便于滚动缓冲左移。 */
    const val ROOM_LOGICAL_W = PlatformerSegmentLibrary.SEGMENT_W

    private const val COLLISION_LAYER = "bg1"
    private const val DECO_LAYER = "bg0"

    private val cache = mutableMapOf<String, PlatformerTmxMap>()
    private val bakedRows = mutableMapOf<String, List<String>>()

    fun load(context: Context, assetPath: String): PlatformerTmxMap =
        cache.getOrPut(assetPath) { PlatformerTmxParser.load(context, assetPath) }

    /** 滚动缓冲追加时无 Context，使用预烘焙房间行。 */
    fun paintBakedOrPlaceholder(
        canvas: PlatformerMapCanvas,
        groundY: Int,
        startX: Int,
        assetPath: String?,
    ): Int {
        val rows = assetPath?.let { bakedRows[it] }
        if (rows != null) {
            blitRows(canvas, startX, groundY, rows)
            return ROOM_LOGICAL_W
        }
        return PlatformerSegmentLibrary.paint(
            canvas, groundY, startX,
            PlatformerSegmentLibrary.SegmentSpec(PlatformerSegmentLibrary.SegmentKind.STORY_ROOM),
            0,
        )
    }

    private fun blitRows(canvas: PlatformerMapCanvas, startX: Int, groundY: Int, rows: List<String>) {
        val h = PLATFORMER_LEVEL_ROWS
        rows.forEachIndexed { dy, row ->
            val cy = groundY - (h - 1 - dy)
            row.forEachIndexed { dx, ch ->
                if (ch != '.') canvas.set(startX + dx, cy, ch)
            }
        }
    }

    /**
     * 将 TMX 房间画入 canvas；取 TMX 底部 [PLATFORMER_LEVEL_ROWS] 行，宽 32 列裁切为 28 列居中。
     */
    fun paintFromTmx(
        context: Context,
        canvas: PlatformerMapCanvas,
        groundY: Int,
        startX: Int,
        assetPath: String,
    ): Int {
        val tmx = load(context, assetPath)
        val srcRowStart = (tmx.height - PLATFORMER_LEVEL_ROWS).coerceAtLeast(0)
        val cropStart = ((tmx.width - ROOM_LOGICAL_W) / 2).coerceAtLeast(0)
        val collision = tmx.layers[COLLISION_LAYER] ?: tmx.layers[DECO_LAYER]
            ?: IntArray(tmx.width * tmx.height)

        canvas.groundSpans(groundY, intArrayOf(startX, ROOM_LOGICAL_W - 2))

        for (dy in 0 until PLATFORMER_LEVEL_ROWS) {
            val ty = srcRowStart + dy
            if (ty >= tmx.height) continue
            val cy = groundY - (PLATFORMER_LEVEL_ROWS - 1 - dy)
            if (cy !in 0 until canvas.height) continue
            for (dx in 0 until ROOM_LOGICAL_W) {
                val tx = cropStart + dx
                if (tx >= tmx.width) continue
                val cx = startX + dx
                if (cx >= canvas.width) continue
                val idx = ty * tmx.width + tx
                val raw = collision.getOrElse(idx) { 0 }
                val gid = tmx.strippedGid(raw)
                if (gid <= 0) continue
                val local = gid - tmx.tilesetFirstGid + 1
                val groundBand = ty >= tmx.height - 10
                val platform = local in 1..64 && ty >= tmx.height / 3
                when {
                    groundBand || platform -> canvas.set(cx, cy, '#')
                    local in 583..590 -> canvas.set(cx, cy, 'G')
                    else -> Unit
                }
            }
        }
        canvas.spawn(startX + 2, groundY)
        canvas.backdrop(startX + 6, groundY - 1)
        canvas.backdrop(startX + ROOM_LOGICAL_W - 4, groundY - 1)
        bakedRows[assetPath] = extractRoomRows(canvas, startX, groundY)
        return ROOM_LOGICAL_W
    }

    private fun extractRoomRows(canvas: PlatformerMapCanvas, startX: Int, groundY: Int): List<String> {
        val h = PLATFORMER_LEVEL_ROWS
        val grid = canvas.toRows()
        return (0 until h).map { dy ->
            val cy = groundY - (h - 1 - dy)
            buildString {
                for (dx in 0 until ROOM_LOGICAL_W) {
                    val ch = grid.getOrNull(cy)?.getOrNull(startX + dx) ?: '.'
                    append(
                        when (ch) {
                            '#', '-', 'G', '@', 'O', '^', 'S', 'C', '+', 'T' -> ch
                            else -> '.'
                        },
                    )
                }
            }
        }
    }

    fun clearCache() {
        cache.clear()
        bakedRows.clear()
    }
}
