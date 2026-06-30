package com.example.funlife.game.platformer.tmx

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.example.funlife.game.platformer.PlatformerViewport
import com.example.funlife.game.platformer.PlatformerWorld
import kotlin.math.max
import kotlin.math.min

object PlatformerTmxRenderer {

    fun draw(
        scope: DrawScope,
        world: PlatformerWorld,
        tileset: ImageBitmap,
        background: ImageBitmap?,
        camX: Float,
        vp: PlatformerViewport,
        viewportW: Float,
    ) {
        val tmx = world.tmx ?: return
        val tile = tmx.tilePx.toFloat()

        background?.let { bg ->
            val parallax = camX * vp.scale * 0.1f
            val drawW = vp.cell * tmx.width
            val drawH = vp.cell * tmx.height
            scope.drawImage(
                image = bg,
                dstOffset = IntOffset((-parallax).toInt(), vp.worldToScreenY(0f).toInt()),
                dstSize = IntSize(drawW.toInt().coerceAtLeast(1), drawH.toInt().coerceAtLeast(1)),
            )
        }

        val startTx = max(0, (camX / tile).toInt() - 1)
        val endTx = min(tmx.width - 1, ((camX + viewportW / vp.scale) / tile).toInt() + 2)

        listOf("bg0", "bg1").forEach { layerName ->
            val layer = tmx.layers[layerName] ?: return@forEach
            for (ty in 0 until tmx.height) {
                for (tx in startTx..endTx) {
                    val raw = layer[ty * tmx.width + tx]
                    val gid = tmx.strippedGid(raw)
                    if (gid <= 0) continue
                    blitGid(scope, tileset, tmx, gid, tx, ty, tile, camX, vp, raw)
                }
            }
        }
    }

    private fun blitGid(
        scope: DrawScope,
        tileset: ImageBitmap,
        tmx: PlatformerTmxMap,
        gid: Int,
        tx: Int,
        ty: Int,
        tile: Float,
        camX: Float,
        vp: PlatformerViewport,
        raw: Int,
    ) {
        val local = gid - tmx.tilesetFirstGid
        if (local < 0) return
        val col = local % tmx.tilesetColumns
        val row = local / tmx.tilesetColumns
        val srcX = col * tmx.tilePx
        val srcY = row * tmx.tilePx
        if (srcX + tmx.tilePx > tileset.width || srcY + tmx.tilePx > tileset.height) return
        val left = vp.worldToScreenX(tx * tile, camX)
        val top = vp.worldToScreenY(ty * tile)
        val dst = vp.cell
        val flipH = raw and PlatformerTmxMap.FLIP_H != 0
        scope.drawImage(
            image = tileset,
            srcOffset = IntOffset(srcX, srcY),
            srcSize = IntSize(tmx.tilePx, tmx.tilePx),
            dstOffset = IntOffset(left.toInt(), top.toInt()),
            dstSize = IntSize(dst.toInt().coerceAtLeast(1), dst.toInt().coerceAtLeast(1)),
        )
        if (flipH) {
            // 简化：暂不镜像，多数地砖对称
        }
    }
}
