package com.example.funlife.ui.screens.pacmaze.maptheme

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.funlife.social.game.engine.pacmaze.PacMazeMapDynamics
import com.example.funlife.social.game.engine.pacmaze.TileType
import kotlin.math.sin

internal object GardenThemeDraw {

    fun drawBackground(scope: DrawScope, ctx: PacMazeMapRenderContext) {
        val p = ctx.config.palette
        scope.drawRect(
            brush = Brush.verticalGradient(listOf(Color(0xFF8BC34A).copy(alpha = 0.25f), p.backgroundBottom)),
            size = ctx.canvasSize,
        )
        scope.drawRect(
            brush = Brush.radialGradient(
                colors = listOf(p.backgroundTop.copy(alpha = 0.65f), Color.Transparent),
                center = Offset(ctx.canvasSize.width / 2f, ctx.canvasSize.height * 0.35f),
                radius = ctx.canvasSize.minDimension * 0.6f,
            ),
            size = ctx.canvasSize,
        )
    }

    fun drawFloor(scope: DrawScope, rect: Rect, palette: PacMazeThemePalette, x: Int, y: Int, isPond: Boolean = false) {
        CourtyardMaterialDraw.drawGrassFloor(scope, rect, x, y, isPond = isPond)
    }

    fun drawHedgeEdges(scope: DrawScope, ctx: PacMazeMapRenderContext) {
        val world = ctx.world
        val p = ctx.config.palette
        val breath = 0.8f + 0.2f * sin(ctx.animPhase)
        for (y in 0 until world.height) {
            for (x in 0 until world.width) {
                if (!isWall(ctx, x, y)) continue
                val tile = world.tileAt(x, y)
                if (tile == TileType.WOOD_WALL || tile == TileType.TILE_WALL) continue
                val rect = ctx.tileRect(x, y)
                val tileCell = ctx.tileMetric(rect)
                val left = rect.left
                val top = rect.top
                val right = rect.right
                val bottom = rect.bottom
                val inset = tileCell * 0.08f
                val stroke = tileCell * 0.11f
                val color = p.wallEdge.themeAlpha(breath)
                val leaf = p.wallGlow.themeAlpha(breath * 0.85f)
                if (!isWall(ctx, x, y - 1)) {
                    scope.drawRoundRect(color = color, topLeft = Offset(left + inset, top + inset), size = Size(right - left - inset * 2, stroke), cornerRadius = CornerRadius(stroke / 2))
                    repeat(3) { i ->
                        val lx = left + inset + (right - left - inset * 2) * (i + 1) / 4f
                        scope.drawCircle(color = leaf, radius = tileCell * 0.045f, center = Offset(lx, top + inset + stroke * 0.5f))
                    }
                }
                if (!isWall(ctx, x, y + 1)) {
                    scope.drawRoundRect(color = color, topLeft = Offset(left + inset, bottom - inset - stroke), size = Size(right - left - inset * 2, stroke), cornerRadius = CornerRadius(stroke / 2))
                }
            }
        }
    }

    private fun isWall(ctx: PacMazeMapRenderContext, x: Int, y: Int): Boolean {
        val world = ctx.world
        if (x !in 0 until world.width || y !in 0 until world.height) return true
        return when (val tile = world.tileAt(x, y)) {
            TileType.WALL, TileType.BRICK_WALL, TileType.WOOD_WALL, TileType.TILE_WALL, TileType.DOOR -> true
            TileType.DYNAMIC_WALL ->
                PacMazeMapDynamics.isTileBlocking(world, tile, x, y, forGhost = false)
            else -> false
        }
    }
}

internal object FoodThemeDraw {

    fun drawBackground(scope: DrawScope, ctx: PacMazeMapRenderContext) {
        val p = ctx.config.palette
        scope.drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF4E342E), p.backgroundBottom),
                center = Offset(ctx.canvasSize.width / 2f, ctx.canvasSize.height * 0.6f),
                radius = ctx.canvasSize.maxDimension * 0.7f,
            ),
            size = ctx.canvasSize,
        )
    }

    fun drawFloor(scope: DrawScope, rect: Rect, palette: PacMazeThemePalette, x: Int, y: Int) {
        val checker = if ((x + y) % 2 == 0) palette.pathFill else palette.pathGrid.copy(alpha = 0.85f)
        scope.drawRect(color = checker, topLeft = rect.topLeft, size = rect.size)
        if ((x + y) % 3 == 0) {
            scope.drawCircle(
                color = palette.pelletGlow.copy(alpha = 0.12f),
                radius = rect.width * 0.08f,
                center = rect.center,
            )
        }
    }

    fun drawWall(scope: DrawScope, rect: Rect, cell: Float, palette: PacMazeThemePalette) {
        val pad = cell * 0.05f
        val inner = Rect(rect.left + pad, rect.top + pad, rect.right - pad, rect.bottom - pad)
        scope.drawRoundRect(
            brush = Brush.verticalGradient(listOf(palette.wallGlow, palette.wallFill)),
            topLeft = inner.topLeft,
            size = inner.size,
            cornerRadius = CornerRadius(cell * 0.1f),
        )
        scope.drawRoundRect(
            color = Color(0xFFFFF3E0).copy(alpha = 0.55f),
            topLeft = Offset(inner.left, inner.top),
            size = Size(inner.width, inner.height * 0.22f),
            cornerRadius = CornerRadius(cell * 0.08f),
        )
        scope.drawRoundRect(
            color = palette.wallEdge.copy(alpha = 0.6f),
            topLeft = inner.topLeft,
            size = inner.size,
            cornerRadius = CornerRadius(cell * 0.1f),
            style = Stroke(1.5f),
        )
    }
}

internal object ChineseThemeDraw {

    fun drawBackground(scope: DrawScope, ctx: PacMazeMapRenderContext) {
        val p = ctx.config.palette
        scope.drawRect(
            brush = Brush.verticalGradient(
                listOf(Color(0xFF2A3550), Color(0xFF121820), p.backgroundBottom),
            ),
            size = ctx.canvasSize,
        )
        val mountain = Path().apply {
            val w = ctx.canvasSize.width
            val h = ctx.canvasSize.height
            moveTo(0f, h * 0.72f)
            lineTo(w * 0.15f, h * 0.58f)
            lineTo(w * 0.28f, h * 0.66f)
            lineTo(w * 0.42f, h * 0.52f)
            lineTo(w * 0.55f, h * 0.62f)
            lineTo(w * 0.68f, h * 0.48f)
            lineTo(w * 0.82f, h * 0.58f)
            lineTo(w, h * 0.5f)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        scope.drawPath(mountain, color = Color(0xFF1A2230).copy(alpha = 0.85f))
        scope.drawRect(
            brush = Brush.radialGradient(
                colors = listOf(p.backgroundTop.copy(alpha = 0.45f), Color.Transparent),
                center = Offset(ctx.canvasSize.width / 2f, ctx.canvasSize.height * 0.28f),
                radius = ctx.canvasSize.minDimension * 0.55f,
            ),
            size = ctx.canvasSize,
        )
    }

    fun drawFloor(scope: DrawScope, rect: Rect, palette: PacMazeThemePalette, x: Int, y: Int) {
        CourtyardMaterialDraw.drawGrassFloor(scope, rect, x, y, isPond = false)
    }

    fun drawDoorLanterns(scope: DrawScope, ctx: PacMazeMapRenderContext) {
        val world = ctx.world
        val pulse = 0.75f + 0.25f * sin(ctx.animPhase * 1.8f)
        for (y in 0 until world.height) {
            for (x in 0 until world.width) {
                if (world.tileAt(x, y) != TileType.DOOR) continue
                val rect = ctx.tileRect(x, y)
                val tileCell = ctx.tileMetric(rect)
                listOf(rect.left + tileCell * 0.12f, rect.right - tileCell * 0.22f).forEach { lx ->
                    val lantern = Rect(lx, rect.top + tileCell * 0.08f, lx + tileCell * 0.1f, rect.top + tileCell * 0.32f)
                    scope.drawRoundRect(
                        brush = Brush.verticalGradient(
                            listOf(Color(0xFFD32F2F), Color(0xFF8B0000)),
                        ),
                        topLeft = lantern.topLeft,
                        size = lantern.size,
                        cornerRadius = CornerRadius(tileCell * 0.04f),
                    )
                    scope.drawLine(
                        color = Color(0xFFD4AF37).copy(alpha = 0.7f),
                        start = Offset(lantern.center.x, lantern.top - tileCell * 0.04f),
                        end = Offset(lantern.center.x, lantern.top),
                        strokeWidth = tileCell * 0.025f,
                    )
                    scope.drawCircle(
                        color = Color(0xFFFFD54F).copy(alpha = 0.35f * pulse),
                        radius = tileCell * 0.14f,
                        center = Offset(lantern.center.x, lantern.center.y),
                    )
                }
            }
        }
    }

}

internal fun DrawScope.drawCourtyardTiles(ctx: PacMazeMapRenderContext, themeId: PacMazeMapThemeId) {
    val world = ctx.world
    val p = ctx.config.palette
    for (y in 0 until world.height) {
        for (x in 0 until world.width) {
            val tile = world.tileAt(x, y)
            val rect = ctx.tileRect(x, y)
            val cell = ctx.tileMetric(rect)
            when (tile) {
                TileType.WALL, TileType.BRICK_WALL, TileType.WOOD_WALL, TileType.TILE_WALL ->
                    CourtyardMaterialDraw.drawCourtyardWall(this, ctx, rect, cell, x, y, themeId)
                TileType.DOOR -> {
                    drawCourtyardFloor(this, ctx, themeId, rect, p, x, y, tile)
                    PacMazePortalVisual.drawPortalTile(this, ctx, rect, cell, x, y)
                }
                TileType.DYNAMIC_WALL -> {
                    if (PacMazeMapDynamics.isTileBlocking(world, tile, x, y, forGhost = false)) {
                        CourtyardMaterialDraw.drawCourtyardWall(this, ctx, rect, cell, x, y, themeId)
                    } else {
                        drawCourtyardFloor(this, ctx, themeId, rect, p, x, y, tile)
                    }
                }
                TileType.TUNNEL, TileType.PORTAL -> {
                    drawCourtyardFloor(this, ctx, themeId, rect, p, x, y, tile)
                    PacMazePortalVisual.drawPortalTile(this, ctx, rect, cell, x, y)
                }
                TileType.ENERGY_GATE -> {
                    drawCourtyardFloor(this, ctx, themeId, rect, p, x, y, tile)
                    MapThemeTiles.drawEnergyGate(this, rect, cell, p, world.energyGateOpen, ctx.animPhase)
                }
                TileType.PATH, TileType.PELLET, TileType.POWER, TileType.EMPTY -> {
                    drawCourtyardFloor(this, ctx, themeId, rect, p, x, y, tile)
                    if (isNearDoor(world, x, y)) {
                        CourtyardMaterialDraw.drawStonePathStrip(this, rect, x, y)
                    }
                    when (tile) {
                        TileType.PELLET -> CourtyardMaterialDraw.drawCourtyardPellet(this, rect, cell, themeId, x, y)
                        TileType.POWER -> CourtyardMaterialDraw.drawCourtyardPower(this, rect, cell, ctx.animPhase, themeId)
                        else -> Unit
                    }
                }
                else -> Unit
            }
        }
    }
}

private fun drawCourtyardFloor(
    scope: DrawScope,
    ctx: PacMazeMapRenderContext,
    themeId: PacMazeMapThemeId,
    rect: Rect,
    palette: PacMazeThemePalette,
    x: Int,
    y: Int,
    tile: TileType,
) {
    val isPond = themeId == PacMazeMapThemeId.GARDEN && tile == TileType.EMPTY
    when (themeId) {
        PacMazeMapThemeId.GARDEN -> GardenThemeDraw.drawFloor(scope, rect, palette, x, y, isPond = isPond)
        PacMazeMapThemeId.CHINESE -> ChineseThemeDraw.drawFloor(scope, rect, palette, x, y)
        else -> CourtyardMaterialDraw.drawGrassFloor(scope, rect, x, y, isPond = isPond)
    }
}

private fun isNearDoor(world: com.example.funlife.social.game.engine.pacmaze.PacMazeWorldState, x: Int, y: Int): Boolean {
    for (dy in -1..1) {
        for (dx in -1..1) {
            val nx = x + dx
            val ny = y + dy
            if (nx in 0 until world.width && ny in 0 until world.height) {
                if (world.tileAt(nx, ny) == TileType.DOOR) return true
            }
        }
    }
    return false
}

internal fun DrawScope.drawThemedTiles(
    ctx: PacMazeMapRenderContext,
    themeId: PacMazeMapThemeId,
    drawWall: (DrawScope, Rect, Float, PacMazeThemePalette) -> Unit,
) {
    val world = ctx.world
    val p = ctx.config.palette
    for (y in 0 until world.height) {
        for (x in 0 until world.width) {
            val tile = world.tileAt(x, y)
            val rect = ctx.tileRect(x, y)
            val cell = ctx.tileMetric(rect)
            when (tile) {
                TileType.WALL, TileType.BRICK_WALL, TileType.WOOD_WALL, TileType.TILE_WALL ->
                    drawWall(this, rect, cell, p)
                TileType.DOOR -> {
                    drawThemedFloor(this, ctx, themeId, rect, p, x, y)
                    PacMazePortalVisual.drawPortalTile(this, ctx, rect, cell, x, y)
                }
                TileType.DYNAMIC_WALL -> {
                    if (PacMazeMapDynamics.isTileBlocking(world, tile, x, y, forGhost = false)) {
                        drawWall(this, rect, cell, p)
                    } else {
                        drawThemedFloor(this, ctx, themeId, rect, p, x, y)
                    }
                }
                TileType.TUNNEL, TileType.PORTAL -> {
                    drawThemedFloor(this, ctx, themeId, rect, p, x, y)
                    PacMazePortalVisual.drawPortalTile(this, ctx, rect, cell, x, y)
                }
                TileType.ENERGY_GATE -> {
                    drawThemedFloor(this, ctx, themeId, rect, p, x, y)
                    MapThemeTiles.drawEnergyGate(this, rect, cell, p, world.energyGateOpen, ctx.animPhase)
                }
                TileType.PATH, TileType.PELLET, TileType.POWER, TileType.EMPTY -> {
                    drawThemedFloor(this, ctx, themeId, rect, p, x, y)
                    when (tile) {
                        TileType.PELLET -> MapThemeTiles.drawPelletGlyph(this, rect, cell, ctx.config, x, y)
                        TileType.POWER -> MapThemeTiles.drawPower(this, rect, cell, ctx.config, ctx.animPhase)
                        else -> Unit
                    }
                }
            }
        }
    }
}

private fun drawThemedFloor(
    scope: DrawScope,
    ctx: PacMazeMapRenderContext,
    themeId: PacMazeMapThemeId,
    rect: Rect,
    palette: PacMazeThemePalette,
    x: Int,
    y: Int,
) {
    when (themeId) {
        PacMazeMapThemeId.GARDEN -> GardenThemeDraw.drawFloor(scope, rect, palette, x, y)
        PacMazeMapThemeId.FOOD -> FoodThemeDraw.drawFloor(scope, rect, palette, x, y)
        PacMazeMapThemeId.CHINESE -> ChineseThemeDraw.drawFloor(scope, rect, palette, x, y)
        else -> MapThemeTiles.drawPath(scope, rect, palette)
    }
}
