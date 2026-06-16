package com.example.funlife.ui.screens.pacmaze.maptheme

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.funlife.social.game.engine.pacmaze.PacMazeMapDynamics
import com.example.funlife.social.game.engine.pacmaze.TileType
import kotlin.math.sin

internal object ExtendedThemeDraw {

    fun drawBackground(scope: DrawScope, ctx: PacMazeMapRenderContext) {
        val p = ctx.config.palette
        val id = ctx.config.id
        val w = ctx.canvasSize.width
        val h = ctx.canvasSize.height
        when (id) {
            PacMazeMapThemeId.STEAMPUNK -> {
                scope.drawRect(brush = Brush.verticalGradient(listOf(Color(0xFF3E2723), p.backgroundBottom)), size = ctx.canvasSize)
                scope.drawRect(
                    brush = Brush.radialGradient(
                        listOf(Color(0xFFFF8F00).copy(alpha = 0.12f), Color.Transparent),
                        center = Offset(w * 0.5f, h * 0.35f),
                        radius = w * 0.45f,
                    ),
                    size = ctx.canvasSize,
                )
            }
            PacMazeMapThemeId.VHS -> {
                scope.drawRect(color = Color(0xFF101010), size = ctx.canvasSize)
                scope.drawRect(brush = Brush.verticalGradient(listOf(Color(0xFF1A1A2E), p.backgroundBottom)), size = ctx.canvasSize)
            }
            PacMazeMapThemeId.ORBITAL -> {
                scope.drawRect(brush = Brush.verticalGradient(listOf(Color(0xFF0D1B2A), Color(0xFF000000))), size = ctx.canvasSize)
                repeat(40) { i ->
                    val sx = (i * 97 % 1000) / 1000f * w
                    val sy = (i * 53 % 1000) / 1000f * h * 0.7f
                    scope.drawCircle(Color.White.copy(alpha = 0.15f + (i % 5) * 0.05f), 1.2f, Offset(sx, sy))
                }
            }
            PacMazeMapThemeId.MAGMA -> {
                scope.drawRect(brush = Brush.verticalGradient(listOf(Color(0xFF1A0A0A), Color(0xFF050000))), size = ctx.canvasSize)
                scope.drawRect(
                    brush = Brush.radialGradient(
                        listOf(Color(0xFFFF5722).copy(alpha = 0.18f), Color.Transparent),
                        center = Offset(w * 0.5f, h * 0.85f),
                        radius = w * 0.55f,
                    ),
                    size = ctx.canvasSize,
                )
            }
            PacMazeMapThemeId.SUBMARINE -> {
                scope.drawRect(brush = Brush.verticalGradient(listOf(Color(0xFF01579B), Color(0xFF002033))), size = ctx.canvasSize)
                val wave = sin(ctx.animPhase * 0.8f) * 8f
                scope.drawRect(Color(0xFF0288D1).copy(alpha = 0.08f), topLeft = Offset(0f, h * 0.2f + wave), size = Size(w, h * 0.15f))
            }
            PacMazeMapThemeId.FROST -> {
                scope.drawRect(brush = Brush.verticalGradient(listOf(Color(0xFF37474F), Color(0xFF102027))), size = ctx.canvasSize)
                scope.drawRect(
                    brush = Brush.radialGradient(
                        listOf(Color(0xFFB3E5FC).copy(alpha = 0.12f), Color.Transparent),
                        center = Offset(w * 0.3f, h * 0.25f),
                        radius = w * 0.4f,
                    ),
                    size = ctx.canvasSize,
                )
            }
            PacMazeMapThemeId.ARCHIVE -> {
                scope.drawRect(brush = Brush.verticalGradient(listOf(Color(0xFF3E2723), Color(0xFF1A120B))), size = ctx.canvasSize)
            }
            PacMazeMapThemeId.METRO -> {
                scope.drawRect(brush = Brush.verticalGradient(listColor(0xFF263238, 0xFF101820)), size = ctx.canvasSize)
            }
            PacMazeMapThemeId.OPERA -> {
                scope.drawRect(brush = Brush.verticalGradient(listColor(0xFF4A148C, 0xFF120820)), size = ctx.canvasSize)
                scope.drawRect(Color(0xFFD32F2F).copy(alpha = 0.08f), topLeft = Offset(0f, 0f), size = Size(w, h * 0.12f))
            }
            PacMazeMapThemeId.GREENHOUSE -> {
                scope.drawRect(brush = Brush.verticalGradient(listColor(0xFF33691E, 0xFF1B2E0F)), size = ctx.canvasSize)
                scope.drawRect(Color(0xFF81C784).copy(alpha = 0.06f), size = ctx.canvasSize)
            }
            PacMazeMapThemeId.CHRONO -> {
                scope.drawRect(
                    brush = Brush.radialGradient(
                        listColor(0xFF283593, 0xFF0A0820),
                        center = Offset(w * 0.5f, h * 0.45f),
                        radius = w * 0.65f,
                    ),
                    size = ctx.canvasSize,
                )
            }
            PacMazeMapThemeId.MIRROR -> {
                scope.drawRect(brush = Brush.verticalGradient(listColor(0xFF1A237E, 0xFF0D0221)), size = ctx.canvasSize)
                val mid = w * 0.5f
                scope.drawLine(Color(0xFF7C4DFF).copy(alpha = 0.25f), Offset(mid, 0f), Offset(mid, h), 2f)
            }
            else -> MapThemeTiles.drawBackground(scope, ctx)
        }
    }

    fun drawFloor(scope: DrawScope, rect: Rect, palette: PacMazeThemePalette, x: Int, y: Int, themeId: PacMazeMapThemeId) {
        when (themeId) {
            PacMazeMapThemeId.METRO -> {
                val tile = if ((x + y) % 2 == 0) palette.pathFill else palette.pathGrid
                scope.drawRect(tile, rect.topLeft, rect.size)
                if (y % 4 == 0) {
                    scope.drawRect(
                        palette.frameAccent.copy(alpha = 0.15f),
                        Offset(rect.left, rect.top),
                        Size(rect.width, rect.height * 0.08f),
                    )
                }
            }
            PacMazeMapThemeId.GREENHOUSE -> {
                scope.drawRect(palette.pathFill.copy(alpha = 0.85f), rect.topLeft, rect.size)
                scope.drawLine(palette.wallEdge.copy(alpha = 0.2f), rect.topLeft, Offset(rect.right, rect.bottom), 1f)
                scope.drawLine(palette.wallEdge.copy(alpha = 0.2f), Offset(rect.left, rect.bottom), Offset(rect.right, rect.top), 1f)
            }
            PacMazeMapThemeId.MIRROR -> {
                scope.drawRect(palette.pathFill, rect.topLeft, rect.size)
                if ((x + y) % 3 == 0) {
                    scope.drawCircle(palette.tunnelAccent.copy(alpha = 0.08f), rect.width * 0.12f, rect.center)
                }
            }
            PacMazeMapThemeId.VHS -> {
                val shift = if ((x + y) % 2 == 0) 1f else -1f
                scope.drawRect(palette.pathFill, rect.topLeft, rect.size)
                scope.drawRect(Color(0xFFFF1744).copy(alpha = 0.04f), Offset(rect.left + shift, rect.top), rect.size)
                scope.drawRect(Color(0xFF00E676).copy(alpha = 0.03f), Offset(rect.left - shift, rect.top), rect.size)
            }
            else -> MapThemeTiles.drawPath(scope, rect, palette)
        }
    }

    fun drawWall(scope: DrawScope, rect: Rect, cell: Float, palette: PacMazeThemePalette, themeId: PacMazeMapThemeId, animPhase: Float) {
        val pad = cell * 0.06f
        val inner = Rect(rect.left + pad, rect.top + pad, rect.right - pad, rect.bottom - pad)
        when (themeId) {
            PacMazeMapThemeId.STEAMPUNK -> {
                scope.drawRoundRect(
                    Brush.verticalGradient(listOf(palette.wallGlow, palette.wallFill)),
                    inner.topLeft, inner.size, CornerRadius(cell * 0.12f),
                )
                scope.drawCircle(Color(0xFFBCAAA4), cell * 0.05f, Offset(inner.left + cell * 0.15f, inner.top + cell * 0.2f))
                scope.drawCircle(Color(0xFFBCAAA4), cell * 0.05f, Offset(inner.right - cell * 0.15f, inner.bottom - cell * 0.2f))
                scope.drawLine(
                    palette.wallEdge.copy(alpha = 0.5f), Offset(inner.left, inner.center.y),
                    Offset(inner.right, inner.center.y), cell * 0.06f,
                )
            }
            PacMazeMapThemeId.VHS -> {
                scope.drawRect(palette.wallFill, inner.topLeft, inner.size)
                scope.drawRect(Color(0xFF00E5FF).copy(alpha = 0.12f), Offset(inner.left + 2f, inner.top), Size(inner.width, inner.height * 0.33f))
                scope.drawRect(Color(0xFFFF1744).copy(alpha = 0.1f), Offset(inner.left - 2f, inner.top), Size(inner.width, inner.height * 0.33f))
            }
            PacMazeMapThemeId.ORBITAL -> {
                scope.drawRect(palette.wallFill, inner.topLeft, inner.size)
                scope.drawRect(palette.wallEdge.copy(alpha = 0.55f), inner.topLeft, inner.size, style = Stroke(1.5f))
                val nodeR = cell * 0.06f
                listOf(
                    inner.topLeft,
                    Offset(inner.right, inner.top),
                    Offset(inner.right, inner.bottom),
                    Offset(inner.left, inner.bottom),
                    inner.center,
                ).forEach { pt ->
                    scope.drawCircle(palette.wallGlow.copy(alpha = 0.55f), nodeR, pt)
                }
                scope.drawLine(
                    palette.frameAccent.copy(alpha = 0.35f),
                    inner.topLeft, inner.bottomRight, 1f,
                )
                scope.drawLine(
                    palette.frameAccent.copy(alpha = 0.35f),
                    Offset(inner.right, inner.top), Offset(inner.left, inner.bottom), 1f,
                )
            }
            PacMazeMapThemeId.SUBMARINE -> {
                scope.drawRoundRect(
                    Brush.verticalGradient(listOf(palette.wallGlow.copy(alpha = 0.35f), palette.wallFill)),
                    inner.topLeft, inner.size, CornerRadius(cell * 0.22f),
                )
                scope.drawRoundRect(
                    palette.wallEdge.copy(alpha = 0.7f), inner.topLeft, inner.size,
                    CornerRadius(cell * 0.22f), style = Stroke(2f),
                )
                scope.drawCircle(Color(0xFF80D8FF).copy(alpha = 0.45f), cell * 0.12f, inner.center)
                repeat(3) { row ->
                    val y = inner.top + inner.height * (row + 1) / 4f
                    scope.drawLine(Color(0xFF01579B).copy(alpha = 0.5f), Offset(inner.left + cell * 0.08f, y), Offset(inner.right - cell * 0.08f, y), 1.2f)
                }
            }
            PacMazeMapThemeId.MAGMA -> {
                scope.drawRoundRect(
                    Brush.linearGradient(listOf(Color(0xFF212121), palette.wallFill)),
                    inner.topLeft, inner.size, CornerRadius(cell * 0.08f),
                )
                val pulse = 0.5f + 0.5f * sin(animPhase * 2f + rect.left * 0.01f)
                scope.drawRoundRect(
                    Color(0xFFFF5722).copy(alpha = 0.25f * pulse),
                    Offset(inner.left, inner.bottom - cell * 0.12f), Size(inner.width, cell * 0.12f), CornerRadius(2f),
                )
            }
            PacMazeMapThemeId.FROST -> {
                scope.drawRoundRect(
                    Brush.verticalGradient(listOf(Color(0xFFECEFF1), palette.wallFill)),
                    inner.topLeft, inner.size, CornerRadius(cell * 0.14f),
                )
                scope.drawRoundRect(
                    palette.wallEdge.copy(alpha = 0.45f), inner.topLeft, inner.size,
                    CornerRadius(cell * 0.14f), style = Stroke(1.2f),
                )
            }
            PacMazeMapThemeId.ARCHIVE -> {
                scope.drawRect(palette.wallFill, inner.topLeft, inner.size)
                repeat(3) { i ->
                    val ly = inner.top + inner.height * (i + 1) / 4f
                    scope.drawLine(Color(0xFF5D4037).copy(alpha = 0.6f), Offset(inner.left, ly), Offset(inner.right, ly), 1.2f)
                }
            }
            PacMazeMapThemeId.METRO -> {
                scope.drawRect(palette.wallFill, inner.topLeft, inner.size)
                scope.drawRect(
                    palette.wallEdge.copy(alpha = 0.35f),
                    Offset(inner.left, inner.top), Size(inner.width, inner.height * 0.15f),
                )
                val stripe = cell * 0.14f
                var sy = inner.top + inner.height * 0.2f
                while (sy < inner.bottom) {
                    scope.drawLine(Color(0xFFFFEB3B).copy(alpha = 0.25f), Offset(inner.left, sy), Offset(inner.right, sy), 1.5f)
                    sy += stripe
                }
            }
            PacMazeMapThemeId.OPERA -> {
                scope.drawRoundRect(
                    Brush.verticalGradient(listOf(Color(0xFF880E4F), palette.wallFill)),
                    inner.topLeft, inner.size, CornerRadius(cell * 0.06f),
                )
                scope.drawRoundRect(
                    Color(0xFFFFD54F).copy(alpha = 0.35f),
                    Offset(inner.left, inner.top), Size(inner.width, cell * 0.08f), CornerRadius(2f),
                )
            }
            PacMazeMapThemeId.GREENHOUSE -> {
                scope.drawRect(palette.wallFill.copy(alpha = 0.55f), inner.topLeft, inner.size)
                scope.drawRect(palette.wallEdge.copy(alpha = 0.65f), inner.topLeft, inner.size, style = Stroke(1.8f))
                val pane = cell * 0.28f
                var px = inner.left + pane * 0.5f
                while (px < inner.right) {
                    scope.drawLine(palette.wallGlow.copy(alpha = 0.3f), Offset(px, inner.top), Offset(px, inner.bottom), 1f)
                    px += pane
                }
                var py = inner.top + pane * 0.5f
                while (py < inner.bottom) {
                    scope.drawLine(palette.wallGlow.copy(alpha = 0.25f), Offset(inner.left, py), Offset(inner.right, py), 1f)
                    py += pane
                }
            }
            PacMazeMapThemeId.CHRONO -> {
                scope.drawRoundRect(palette.wallFill, inner.topLeft, inner.size, CornerRadius(cell * 0.2f))
                val tick = sin(animPhase * 3f)
                scope.drawLine(
                    palette.frameAccent.copy(alpha = 0.7f), inner.center,
                    Offset(inner.center.x + inner.width * 0.35f * tick, inner.center.y - inner.height * 0.2f), 2f,
                )
            }
            PacMazeMapThemeId.MIRROR -> {
                scope.drawRoundRect(palette.wallFill, inner.topLeft, inner.size, CornerRadius(cell * 0.1f))
                scope.drawRoundRect(
                    palette.tunnelAccent.copy(alpha = 0.35f), inner.topLeft, inner.size,
                    CornerRadius(cell * 0.1f), style = Stroke(1.5f),
                )
            }
            else -> MapThemeTiles.drawClassicWall(scope, rect, cell, palette)
        }
    }

    fun drawOverlay(scope: DrawScope, ctx: PacMazeMapRenderContext) {
        when (ctx.config.id) {
            PacMazeMapThemeId.VHS -> {
                val h = ctx.canvasSize.height
                val scanY = (ctx.animPhase * 40f) % h
                scope.drawRect(Color.White.copy(alpha = 0.03f), topLeft = Offset(0f, scanY), size = Size(ctx.canvasSize.width, 4f))
            }
            PacMazeMapThemeId.MIRROR -> PacMazeModeOverlayDraw.drawMazeFog(scope, ctx)
            PacMazeMapThemeId.CHRONO -> {
                val pulse = 0.85f + 0.15f * sin(ctx.animPhase * 2f)
                scope.drawRect(Color(0xFFFFD54F).copy(alpha = 0.04f * pulse), size = ctx.canvasSize)
            }
            else -> Unit
        }
    }

    fun drawTiles(scope: DrawScope, ctx: PacMazeMapRenderContext) {
        val world = ctx.world
        val p = ctx.config.palette
        val themeId = ctx.config.id
        for (y in 0 until world.height) {
            for (x in 0 until world.width) {
                val tile = world.tileAt(x, y)
                val rect = ctx.tileRect(x, y)
                val cell = ctx.tileMetric(rect)
                when (tile) {
                    TileType.WALL, TileType.BRICK_WALL, TileType.WOOD_WALL, TileType.TILE_WALL ->
                        drawWall(scope, rect, cell, p, themeId, ctx.animPhase)
                    TileType.DOOR -> {
                        drawFloor(scope, rect, p, x, y, themeId)
                        PacMazePortalVisual.drawPortalTile(scope, ctx, rect, cell, x, y)
                    }
                    TileType.DYNAMIC_WALL -> {
                        if (PacMazeMapDynamics.isTileBlocking(world, tile, x, y, forGhost = false)) {
                            drawWall(scope, rect, cell, p, themeId, ctx.animPhase)
                        } else {
                            drawFloor(scope, rect, p, x, y, themeId)
                        }
                    }
                    TileType.TUNNEL, TileType.PORTAL -> {
                        drawFloor(scope, rect, p, x, y, themeId)
                        PacMazePortalVisual.drawPortalTile(scope, ctx, rect, cell, x, y)
                    }
                    TileType.ENERGY_GATE -> {
                        drawFloor(scope, rect, p, x, y, themeId)
                        MapThemeTiles.drawEnergyGate(scope, rect, cell, p, world.energyGateOpen, ctx.animPhase)
                    }
                    TileType.PATH, TileType.PELLET, TileType.POWER, TileType.EMPTY -> {
                        drawFloor(scope, rect, p, x, y, themeId)
                        when (tile) {
                            TileType.PELLET -> MapThemeTiles.drawPelletGlyph(scope, rect, cell, ctx.config, x, y)
                            TileType.POWER -> MapThemeTiles.drawPower(scope, rect, cell, ctx.config, ctx.animPhase)
                            else -> Unit
                        }
                    }
                }
            }
        }
    }

    private fun listColor(vararg hex: Long): List<Color> = hex.map { Color(it) }
}

internal object ExtendedMapThemeRenderer : PacMazeMapThemeRenderer {
    override fun render(scope: DrawScope, ctx: PacMazeMapRenderContext, particles: PacMazeParticleField?) {
        ExtendedThemeDraw.drawBackground(scope, ctx)
        particles?.draw(scope, ctx.canvasSize, ctx.animPhase)
        ExtendedThemeDraw.drawTiles(scope, ctx)
        ThemeMarkerDraw.draw(scope, ctx)
        ThemeHazardDraw.draw(scope, ctx)
        PacMazeItemDraw.draw(scope, ctx)
        PacMazeEntityDraw.drawEntities(scope, ctx)
        PacMazeItemDraw.drawMagnetOverlay(scope, ctx)
        PacMazeEntityDraw.drawMapFrame(scope, ctx)
        ExtendedThemeDraw.drawOverlay(scope, ctx)
    }
}

internal object ChronoMapThemeRenderer : PacMazeMapThemeRenderer {
    override fun render(scope: DrawScope, ctx: PacMazeMapRenderContext, particles: PacMazeParticleField?) {
        ExtendedMapThemeRenderer.render(scope, ctx, particles)
    }
}

internal object MirrorMapThemeRenderer : PacMazeMapThemeRenderer {
    override fun render(scope: DrawScope, ctx: PacMazeMapRenderContext, particles: PacMazeParticleField?) {
        ExtendedMapThemeRenderer.render(scope, ctx, particles)
    }
}
