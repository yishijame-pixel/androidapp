package com.example.funlife.game.platformer

/** 横屏视口：角色按 [PLATFORMER_LEVEL_ROWS] 格缩放；地图按实际行数贴底。 */
data class PlatformerViewport(
    val cell: Float,
    val offsetY: Float,
    val scale: Float,
    val viewWorldW: Float,
) {
    fun worldToScreenX(worldX: Float, camX: Float): Float = (worldX - camX) * scale

    fun worldToScreenY(worldY: Float): Float = offsetY + worldY * scale

    companion object {
        const val VISIBLE_TILES_W = 24f

        fun compute(world: PlatformerWorld, viewportW: Float, viewportH: Float): PlatformerViewport {
            val tilePx = world.tilePx
            val mapRows = world.height.toFloat()
            // 固定 14 格算 cell，避免 TMX 28 行地图把角色缩到一半
            val scaleRows = PLATFORMER_LEVEL_ROWS.toFloat()
            val cell = viewportH / scaleRows
            val scale = cell / tilePx
            val mapScreenH = mapRows * cell
            val offsetY = (viewportH - mapScreenH).coerceAtLeast(0f)

            val viewWorldW = viewportW / scale
            return PlatformerViewport(
                cell = cell,
                offsetY = offsetY,
                scale = scale,
                viewWorldW = viewWorldW,
            )
        }
    }
}
