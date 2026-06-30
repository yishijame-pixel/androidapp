package com.example.funlife.game.platformer.tmx

/** 解析后的 Tiled 正交地图（8×8 或任意格宽）。 */
data class PlatformerTmxMap(
    val assetDir: String,
    val width: Int,
    val height: Int,
    val tilePx: Int,
    val tilesetPath: String,
    val tilesetColumns: Int,
    val tilesetFirstGid: Int,
    val backgroundPath: String?,
    val layers: Map<String, IntArray>,
) {
    fun gid(layer: String, tx: Int, ty: Int): Int {
        if (tx !in 0 until width || ty !in 0 until height) return 0
        return layers[layer]?.get(ty * width + tx) ?: 0
    }

    fun strippedGid(raw: Int): Int = if (raw <= 0) 0 else raw and GID_MASK

    companion object {
        const val GID_MASK = 0x1FFFFFFF
        const val FLIP_H = 0x80000000.toInt()
        const val FLIP_V = 0x40000000.toInt()
        const val FLIP_D = 0x20000000.toInt()
    }
}
