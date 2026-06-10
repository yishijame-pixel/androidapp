package com.example.funlife.ui.screens.pacmaze.maptheme

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.funlife.social.game.engine.pacmaze.PacMazeMapDynamics
import com.example.funlife.social.game.engine.pacmaze.TileType

interface PacMazeMapThemeRenderer {
    fun render(scope: DrawScope, ctx: PacMazeMapRenderContext, particles: PacMazeParticleField?)
}

internal abstract class BaseMapThemeRenderer : PacMazeMapThemeRenderer {

    override fun render(scope: DrawScope, ctx: PacMazeMapRenderContext, particles: PacMazeParticleField?) {
        MapThemeTiles.drawBackground(scope, ctx)
        particles?.draw(scope, ctx.canvasSize, ctx.animPhase)
        drawTiles(scope, ctx)
        PacMazeEntityDraw.drawEntities(scope, ctx)
        PacMazeEntityDraw.drawMapFrame(scope, ctx)
    }

    protected open fun drawTiles(scope: DrawScope, ctx: PacMazeMapRenderContext) {
        val world = ctx.world
        val cell = ctx.cell
        for (y in 0 until world.height) {
            for (x in 0 until world.width) {
                val tile = world.tileAt(x, y)
                val rect = Rect(
                    ctx.offsetX + x * cell,
                    ctx.offsetY + y * cell,
                    ctx.offsetX + (x + 1) * cell,
                    ctx.offsetY + (y + 1) * cell,
                )
                drawTile(scope, ctx, tile, rect, x, y)
            }
        }
    }

    protected abstract fun drawTile(
        scope: DrawScope,
        ctx: PacMazeMapRenderContext,
        tile: TileType,
        rect: Rect,
        x: Int,
        y: Int,
    )
}

internal object ClassicMapThemeRenderer : BaseMapThemeRenderer() {
    override fun drawTile(scope: DrawScope, ctx: PacMazeMapRenderContext, tile: TileType, rect: Rect, x: Int, y: Int) {
        val p = ctx.config.palette
        val cell = ctx.cell
        when (tile) {
            TileType.WALL, TileType.BRICK_WALL, TileType.WOOD_WALL, TileType.TILE_WALL ->
                MapThemeTiles.drawClassicWall(scope, rect, cell, p)
            TileType.DOOR -> scope.drawRoundRect(
                color = p.wallFill,
                topLeft = androidx.compose.ui.geometry.Offset(rect.left + cell * 0.1f, rect.top + cell * 0.15f),
                size = androidx.compose.ui.geometry.Size(rect.width * 0.8f, rect.height * 0.7f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(cell * 0.1f),
            )
            TileType.TUNNEL, TileType.PORTAL -> MapThemeTiles.drawTunnel(scope, rect, cell, p)
            TileType.DYNAMIC_WALL -> MapThemeTiles.drawDynamicWall(scope, rect, cell, p, ctx, x, y)
            TileType.ENERGY_GATE -> MapThemeTiles.drawEnergyGate(scope, rect, cell, p, ctx.world.energyGateOpen, ctx.animPhase)
            TileType.PATH, TileType.PELLET, TileType.POWER, TileType.EMPTY -> {
                MapThemeTiles.drawPath(scope, rect, p)
                when (tile) {
                    TileType.PELLET -> MapThemeTiles.drawPelletDot(scope, rect, cell, p)
                    TileType.POWER -> MapThemeTiles.drawPower(scope, rect, cell, ctx.config, ctx.animPhase)
                    else -> Unit
                }
            }
        }
    }
}

internal object CyberpunkMapThemeRenderer : PacMazeMapThemeRenderer {

    override fun render(scope: DrawScope, ctx: PacMazeMapRenderContext, particles: PacMazeParticleField?) {
        CyberVisualEffects.drawBackground(scope, ctx.canvasSize)
        particles?.draw(scope, ctx.canvasSize, ctx.animPhase)

        val world = ctx.world
        val cell = ctx.cell
        for (y in 0 until world.height) {
            for (x in 0 until world.width) {
                val tile = world.tileAt(x, y)
                val rect = Rect(
                    ctx.offsetX + x * cell,
                    ctx.offsetY + y * cell,
                    ctx.offsetX + (x + 1) * cell,
                    ctx.offsetY + (y + 1) * cell,
                )
                when (tile) {
                    TileType.WALL, TileType.BRICK_WALL, TileType.WOOD_WALL, TileType.TILE_WALL -> Unit
                    TileType.DOOR -> {
                        CyberCollectibles.drawFloor(scope, rect, x, y)
                        MapThemeTiles.drawPortal(scope, rect, cell, ctx.config.palette, ctx.animPhase)
                    }
                    TileType.DYNAMIC_WALL -> {
                        if (!PacMazeMapDynamics.isTileBlocking(world, tile, x, y, forGhost = false)) {
                            CyberCollectibles.drawFloor(scope, rect, x, y)
                        }
                    }
                    TileType.TUNNEL, TileType.PORTAL -> {
                        CyberCollectibles.drawFloor(scope, rect, x, y)
                        MapThemeTiles.drawPortal(scope, rect, cell, ctx.config.palette, ctx.animPhase)
                    }
                    TileType.ENERGY_GATE -> {
                        CyberCollectibles.drawFloor(scope, rect, x, y)
                        MapThemeTiles.drawEnergyGate(
                        scope, rect, cell, ctx.config.palette, ctx.world.energyGateOpen, ctx.animPhase,
                        )
                    }
                    else -> {
                        CyberCollectibles.drawFloor(scope, rect, x, y)
                        when (tile) {
                            TileType.PELLET -> CyberCollectibles.drawPellet(scope, rect, cell, x, y)
                            TileType.POWER -> CyberCollectibles.drawPower(scope, rect, cell, ctx.animPhase)
                            else -> Unit
                        }
                    }
                }
            }
        }

        CyberMazeWallPass.draw(scope, ctx)
        CyberMapDecorations.drawMarkers(scope, ctx)
        CyberHazardDraw.draw(scope, ctx)
        CyberEntityDraw.drawEntities(scope, ctx)
        CyberVisualEffects.drawNeonFrame(scope, ctx)
        CyberVisualEffects.drawCrtOverlay(scope, ctx.canvasSize, ctx.animPhase)
    }
}

internal object GardenMapThemeRenderer : PacMazeMapThemeRenderer {

    override fun render(scope: DrawScope, ctx: PacMazeMapRenderContext, particles: PacMazeParticleField?) {
        GardenThemeDraw.drawBackground(scope, ctx)
        particles?.draw(scope, ctx.canvasSize, ctx.animPhase)
        scope.drawCourtyardTiles(ctx, PacMazeMapThemeId.GARDEN)
        CourtyardMaterialDraw.drawWallInnerShadows(scope, ctx)
        GardenThemeDraw.drawHedgeEdges(scope, ctx)
        ThemeMarkerDraw.draw(scope, ctx)
        ThemeHazardDraw.draw(scope, ctx)
        PacMazeEntityDraw.drawEntities(scope, ctx)
        PacMazeEntityDraw.drawMapFrame(scope, ctx)
    }
}

internal object FoodMapThemeRenderer : PacMazeMapThemeRenderer {

    override fun render(scope: DrawScope, ctx: PacMazeMapRenderContext, particles: PacMazeParticleField?) {
        FoodThemeDraw.drawBackground(scope, ctx)
        particles?.draw(scope, ctx.canvasSize, ctx.animPhase)
        scope.drawThemedTiles(ctx, PacMazeMapThemeId.FOOD, FoodThemeDraw::drawWall)
        ThemeMarkerDraw.draw(scope, ctx)
        ThemeHazardDraw.draw(scope, ctx)
        PacMazeEntityDraw.drawEntities(scope, ctx)
        PacMazeEntityDraw.drawMapFrame(scope, ctx)
    }
}

internal object ChineseMapThemeRenderer : PacMazeMapThemeRenderer {

    override fun render(scope: DrawScope, ctx: PacMazeMapRenderContext, particles: PacMazeParticleField?) {
        ChineseThemeDraw.drawBackground(scope, ctx)
        particles?.draw(scope, ctx.canvasSize, ctx.animPhase)
        scope.drawCourtyardTiles(ctx, PacMazeMapThemeId.CHINESE)
        CourtyardMaterialDraw.drawWallInnerShadows(scope, ctx)
        ChineseThemeDraw.drawDoorLanterns(scope, ctx)
        ThemeMarkerDraw.draw(scope, ctx)
        ThemeHazardDraw.draw(scope, ctx)
        PacMazeEntityDraw.drawEntities(scope, ctx)
        PacMazeEntityDraw.drawMapFrame(scope, ctx)
    }
}
