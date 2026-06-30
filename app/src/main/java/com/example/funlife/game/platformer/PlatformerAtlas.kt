package com.example.funlife.game.platformer

import androidx.compose.ui.geometry.Rect

/** Goodly 2x 图集瓦片坐标（col, row），图集 16×16 格，每格 32px。 */
data class AtlasTile(val col: Int, val row: Int) {
    fun uvRect(): Rect {
        val cell = PLATFORMER_TILE_PX.toFloat()
        return Rect(
            left = col * cell,
            top = row * cell,
            right = (col + 1) * cell,
            bottom = (row + 1) * cell,
        )
    }
}

/**
 * Goodly 2x 图集布局（见 assets/platformer/goodly_2x.png）：
 * - 草地 autotile：col 0–2, row 0–2
 * - 草地薄平台：col 3, row 0
 * - 金属 autotile：col 5–7, row 0–2
 * - 沙漠 autotile：col 10–12, row 5–7
 * - 冰面 autotile：col 10–12, row 10–12
 * - 紫色顶（幽暗）：col 3–5, row 0–2
 */
object PlatformerAtlas {
    const val COLS = 16
    const val ROWS = 16

    data class ThemeTiles(
        val fill: AtlasTile,
        val top: AtlasTile,
        val left: AtlasTile,
        val right: AtlasTile,
        val bottom: AtlasTile,
        val topLeft: AtlasTile,
        val topRight: AtlasTile,
        val platform: AtlasTile,
    )

    val grass = ThemeTiles(
        fill = AtlasTile(1, 1),
        top = AtlasTile(1, 0),
        left = AtlasTile(0, 1),
        right = AtlasTile(2, 1),
        bottom = AtlasTile(1, 2),
        topLeft = AtlasTile(0, 0),
        topRight = AtlasTile(2, 0),
        platform = AtlasTile(3, 0),
    )

    val metal = ThemeTiles(
        fill = AtlasTile(6, 1),
        top = AtlasTile(6, 0),
        left = AtlasTile(5, 1),
        right = AtlasTile(7, 1),
        bottom = AtlasTile(6, 2),
        topLeft = AtlasTile(5, 0),
        topRight = AtlasTile(7, 0),
        platform = AtlasTile(6, 3),
    )

    val desert = ThemeTiles(
        fill = AtlasTile(11, 6),
        top = AtlasTile(11, 5),
        left = AtlasTile(10, 6),
        right = AtlasTile(12, 6),
        bottom = AtlasTile(11, 7),
        topLeft = AtlasTile(10, 5),
        topRight = AtlasTile(12, 5),
        platform = AtlasTile(11, 4),
    )

    val spooky = ThemeTiles(
        fill = AtlasTile(4, 1),
        top = AtlasTile(4, 0),
        left = AtlasTile(3, 1),
        right = AtlasTile(5, 1),
        bottom = AtlasTile(4, 2),
        topLeft = AtlasTile(3, 0),
        topRight = AtlasTile(5, 0),
        platform = AtlasTile(4, 0),
    )

    val ice = ThemeTiles(
        fill = AtlasTile(11, 11),
        top = AtlasTile(11, 10),
        left = AtlasTile(10, 11),
        right = AtlasTile(12, 11),
        bottom = AtlasTile(11, 12),
        topLeft = AtlasTile(10, 10),
        topRight = AtlasTile(12, 10),
        platform = AtlasTile(11, 9),
    )

    fun tilesFor(theme: PlatformerTheme): ThemeTiles = when (theme) {
        PlatformerTheme.GRASS -> grass
        PlatformerTheme.METAL -> metal
        PlatformerTheme.DESERT, PlatformerTheme.PACK_DESERT -> desert
        PlatformerTheme.SPOOKY -> spooky
        PlatformerTheme.ICE, PlatformerTheme.PACK_WINTER -> ice
        PlatformerTheme.FORTRESS -> metal
        PlatformerTheme.PACK_FOREST -> grass
        PlatformerTheme.PACK_GRAVEYARD -> spooky
        PlatformerTheme.PACK_JUNGLE -> grass
        PlatformerTheme.PACK_SCIFI -> metal
        PlatformerTheme.PACK_GROTTO -> metal
        PlatformerTheme.PACK_MINIMAL -> metal
        PlatformerTheme.PACK_SUPERTUX -> ice
    }

    fun pickSolidTile(
        theme: PlatformerTheme,
        solidAbove: Boolean,
        solidBelow: Boolean,
        solidLeft: Boolean,
        solidRight: Boolean,
        isPlatform: Boolean,
    ): AtlasTile {
        val t = tilesFor(theme)
        if (isPlatform) return t.platform
        if (!solidAbove) {
            return when {
                solidLeft && solidRight -> t.top
                solidLeft -> t.topRight
                solidRight -> t.topLeft
                else -> t.top
            }
        }
        if (!solidBelow) return t.bottom
        if (!solidLeft && !solidRight) return t.fill
        if (!solidLeft) return t.left
        if (!solidRight) return t.right
        return t.fill
    }
}
