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
import com.example.funlife.social.game.engine.pacmaze.PacMazeWorldState
import com.example.funlife.social.game.engine.pacmaze.TileType
import kotlin.math.sin

internal enum class CourtyardWallMaterial {
    BRICK,
    WOOD,
    TILE,
}

internal object CourtyardMaterialDraw {

    private val grassDark = Color(0xFF2A5230)
    private val grassMid = Color(0xFF3F7A48)
    private val grassLight = Color(0xFF6FAF72)
    private val brickBase = Color(0xFF4F717D)
    private val brickHighlight = Color(0xFF6A909C)
    private val brickDark = Color(0xFF354F58)
    private val mortar = Color(0xFF283840)
    private val woodBase = Color(0xFF6D4C2E)
    private val woodLight = Color(0xFFA67C52)
    private val woodDark = Color(0xFF3E2810)
    private val pillarRed = Color(0xFF8B2500)
    private val tileBase = Color(0xFF3D5248)
    private val tileGlaze = Color(0xFF5C7568)
    private val tileEdge = Color(0xFF243029)
    private val ridgeGold = Color(0xFFD4AF37)
    private val pondFill = Color(0xFF1E4A58)
    private val pondRipple = Color(0xFF3E8FA8)
    private val stonePath = Color(0xFF8B8B7A)

    fun drawGrassFloor(scope: DrawScope, rect: Rect, x: Int, y: Int, isPond: Boolean = false) {
        if (isPond) {
            drawPondTile(scope, rect, x, y)
            return
        }
        val seed = x * 17 + y * 31
        // 渐变首色用固定 alpha，避免旧版 0.92f+patch 在部分设备上越界崩溃
        scope.drawRect(
            brush = Brush.radialGradient(
                colors = listOf(grassLight, grassMid, grassDark),
                center = Offset(rect.center.x - rect.width * 0.08f, rect.center.y - rect.height * 0.1f),
                radius = rect.maxDimension * 0.72f,
            ),
            topLeft = rect.topLeft,
            size = rect.size,
        )
        repeat(6) { i ->
            val fx = rect.left + rect.width * (((seed + i * 7) % 11) / 11f)
            val fy = rect.top + rect.height * (((seed + i * 11) % 11) / 11f)
            val h = rect.height * (0.12f + (i % 3) * 0.04f)
            scope.drawLine(
                color = grassLight.themeAlpha(0.35f + (i % 3) * 0.12f),
                start = Offset(fx, fy),
                end = Offset(fx + 1.2f, fy - h),
                strokeWidth = 1.1f,
            )
        }
        if ((x + y) % 11 == 0) {
            scope.drawCircle(
                color = Color(0xFFFFD54F).copy(alpha = 0.4f),
                radius = rect.width * 0.045f,
                center = Offset(rect.left + rect.width * 0.62f, rect.top + rect.height * 0.38f),
            )
        }
        if ((x + y) % 13 == 0) {
            scope.drawCircle(
                color = stonePath.copy(alpha = 0.55f),
                radius = rect.width * 0.06f,
                center = Offset(rect.left + rect.width * 0.28f, rect.top + rect.height * 0.72f),
            )
        }
    }

    private fun drawPondTile(scope: DrawScope, rect: Rect, x: Int, y: Int) {
        scope.drawRect(
            brush = Brush.radialGradient(
                listOf(pondRipple.copy(alpha = 0.65f), pondFill, Color(0xFF153038)),
                center = rect.center,
                radius = rect.maxDimension * 0.7f,
            ),
            topLeft = rect.topLeft,
            size = rect.size,
        )
        val rippleY = rect.top + rect.height * (0.32f + 0.08f * sin((x + y) * 0.75f + x * 0.2f))
        scope.drawLine(
            color = Color.White.copy(alpha = 0.18f),
            start = Offset(rect.left + rect.width * 0.12f, rippleY),
            end = Offset(rect.right - rect.width * 0.12f, rippleY),
            strokeWidth = 1.4f,
        )
        if ((x + y) % 5 == 0) {
            scope.drawCircle(
                color = Color(0xFF81C784).copy(alpha = 0.55f),
                radius = rect.width * 0.11f,
                center = Offset(rect.center.x, rect.center.y + rect.height * 0.08f),
            )
            scope.drawCircle(
                color = Color(0xFFE8F5E9).copy(alpha = 0.35f),
                radius = rect.width * 0.04f,
                center = Offset(rect.center.x, rect.center.y + rect.height * 0.02f),
            )
        }
    }

    fun drawStonePathStrip(scope: DrawScope, rect: Rect, x: Int, y: Int) {
        val pad = rect.width * 0.1f
        val stone = Rect(rect.left + pad, rect.top + pad, rect.right - pad, rect.bottom - pad)
        scope.drawRoundRect(
            brush = Brush.linearGradient(listOf(Color(0xFF9E9E90), stonePath, Color(0xFF6F6F62))),
            topLeft = stone.topLeft,
            size = stone.size,
            cornerRadius = CornerRadius(stone.width * 0.2f),
        )
        scope.drawLine(
            color = Color.White.copy(alpha = 0.15f),
            start = Offset(stone.left + stone.width * 0.15f, stone.top + stone.height * 0.35f),
            end = Offset(stone.right - stone.width * 0.2f, stone.top + stone.height * 0.35f),
            strokeWidth = 0.8f,
        )
    }

    fun drawCourtyardWall(
        scope: DrawScope,
        ctx: PacMazeMapRenderContext,
        rect: Rect,
        cell: Float,
        x: Int,
        y: Int,
        themeId: PacMazeMapThemeId,
    ) {
        when (resolveMaterial(ctx.world, x, y, themeId)) {
            CourtyardWallMaterial.BRICK -> drawBlueBrickWall(scope, rect, cell)
            CourtyardWallMaterial.WOOD -> drawWoodCorridor(scope, rect, cell, ctx, x, y, themeId)
            CourtyardWallMaterial.TILE -> drawTileRoofWall(scope, rect, cell, ctx, x, y)
        }
    }

    fun drawWallInnerShadows(scope: DrawScope, ctx: PacMazeMapRenderContext) {
        val world = ctx.world
        val cell = ctx.cell
        for (y in 0 until world.height) {
            for (x in 0 until world.width) {
                if (!world.tileAt(x, y).isWalkableFloor()) continue
                val rect = Rect(
                    ctx.offsetX + x * cell,
                    ctx.offsetY + y * cell,
                    ctx.offsetX + (x + 1) * cell,
                    ctx.offsetY + (y + 1) * cell,
                )
                if (isSolidWall(world, x, y - 1)) {
                    scope.drawRect(
                        brush = Brush.verticalGradient(
                            listOf(Color.Black.copy(alpha = 0.22f), Color.Transparent),
                            startY = rect.top,
                            endY = rect.top + cell * 0.35f,
                        ),
                        topLeft = rect.topLeft,
                        size = Size(rect.width, cell * 0.35f),
                    )
                }
                if (isSolidWall(world, x - 1, y)) {
                    scope.drawRect(
                        brush = Brush.horizontalGradient(
                            listOf(Color.Black.copy(alpha = 0.16f), Color.Transparent),
                            startX = rect.left,
                            endX = rect.left + cell * 0.28f,
                        ),
                        topLeft = rect.topLeft,
                        size = Size(cell * 0.28f, rect.height),
                    )
                }
                if (isSolidWall(world, x + 1, y)) {
                    scope.drawRect(
                        brush = Brush.horizontalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.16f)),
                            startX = rect.right - cell * 0.28f,
                            endX = rect.right,
                        ),
                        topLeft = Offset(rect.right - cell * 0.28f, rect.top),
                        size = Size(cell * 0.28f, rect.height),
                    )
                }
            }
        }
    }

    fun drawCourtyardPellet(
        scope: DrawScope,
        rect: Rect,
        cell: Float,
        themeId: PacMazeMapThemeId,
        x: Int,
        y: Int,
    ) {
        val center = rect.center
        val r = cell * 0.075f
        val core = when (themeId) {
            PacMazeMapThemeId.CHINESE -> Color(0xFFFFF176)
            PacMazeMapThemeId.GARDEN -> Color(0xFFFFFDE7)
            else -> Color(0xFFFFF59D)
        }
        scope.drawCircle(color = core.copy(alpha = 0.25f), radius = r * 2.2f, center = center)
        scope.drawCircle(color = core, radius = r, center = center)
        if (themeId == PacMazeMapThemeId.CHINESE && (x + y) % 2 == 0) {
            scope.drawCircle(
                color = Color(0xFFFFD54F).copy(alpha = 0.5f),
                radius = r * 0.35f,
                center = Offset(center.x + r * 0.4f, center.y - r * 0.3f),
            )
        }
    }

    fun drawCourtyardPower(scope: DrawScope, rect: Rect, cell: Float, animPhase: Float, themeId: PacMazeMapThemeId) {
        val center = rect.center
        val pulse = 0.88f + 0.12f * sin(animPhase * 2.2f)
        val r = cell * 0.14f * pulse
        val glow = when (themeId) {
            PacMazeMapThemeId.CHINESE -> Color(0xFFFFCA28)
            PacMazeMapThemeId.GARDEN -> Color(0xFF81C784)
            else -> Color(0xFFFFD54F)
        }
        scope.drawCircle(color = glow.copy(alpha = 0.35f), radius = r * 1.8f, center = center)
        scope.drawCircle(color = glow, radius = r, center = center)
        scope.drawCircle(color = Color.White.copy(alpha = 0.75f), radius = r * 0.35f, center = center)
    }

    fun resolveMaterial(world: PacMazeWorldState, x: Int, y: Int, themeId: PacMazeMapThemeId): CourtyardWallMaterial {
        when (world.tileAt(x, y)) {
            TileType.BRICK_WALL -> return CourtyardWallMaterial.BRICK
            TileType.WOOD_WALL -> return CourtyardWallMaterial.WOOD
            TileType.TILE_WALL -> return CourtyardWallMaterial.TILE
            else -> Unit
        }
        val up = isWalkable(world, x, y - 1)
        val down = isWalkable(world, x, y + 1)
        val left = isWalkable(world, x - 1, y)
        val right = isWalkable(world, x + 1, y)
        val openSides = listOf(up, down, left, right).count { it }

        if (down && !up) return CourtyardWallMaterial.TILE
        if (up && !down && themeId == PacMazeMapThemeId.CHINESE) return CourtyardWallMaterial.TILE
        if ((left && right && !up && !down) || (up && down && !left && !right)) return CourtyardWallMaterial.WOOD
        if (openSides >= 2 && openSides <= 3) return CourtyardWallMaterial.WOOD
        return CourtyardWallMaterial.BRICK
    }

    private fun drawBlueBrickWall(scope: DrawScope, rect: Rect, cell: Float) {
        scope.drawRect(color = mortar, topLeft = rect.topLeft, size = rect.size)
        val cols = 3
        val rows = 2
        val gap = cell * 0.035f
        val brickW = (rect.width - gap * (cols + 1)) / cols
        val brickH = (rect.height - gap * (rows + 1)) / rows
        for (row in 0 until rows) {
            val offsetX = if (row % 2 == 1) brickW * 0.5f else 0f
            for (col in 0 until cols) {
                val left = rect.left + gap + col * (brickW + gap) + offsetX
                if (left + brickW > rect.right - gap) continue
                val top = rect.top + gap + row * (brickH + gap)
                scope.drawRoundRect(
                    brush = Brush.verticalGradient(listOf(brickHighlight, brickBase, brickDark)),
                    topLeft = Offset(left, top),
                    size = Size(brickW, brickH),
                    cornerRadius = CornerRadius(1.8f),
                )
                scope.drawLine(
                    color = Color.White.copy(alpha = 0.12f),
                    start = Offset(left + 1f, top + 1.5f),
                    end = Offset(left + brickW - 1f, top + 1.5f),
                    strokeWidth = 0.8f,
                )
            }
        }
    }

    private fun drawWoodCorridor(
        scope: DrawScope,
        rect: Rect,
        cell: Float,
        ctx: PacMazeMapRenderContext,
        x: Int,
        y: Int,
        themeId: PacMazeMapThemeId,
    ) {
        val up = isWalkable(ctx.world, x, y - 1)
        val down = isWalkable(ctx.world, x, y + 1)
        val left = isWalkable(ctx.world, x - 1, y)
        val right = isWalkable(ctx.world, x + 1, y)
        val horizontal = left || right
        val vertical = up || down

        scope.drawRect(
            brush = Brush.verticalGradient(listOf(woodLight.copy(alpha = 0.18f), woodDark.copy(alpha = 0.12f))),
            topLeft = rect.topLeft,
            size = rect.size,
        )

        val post = cell * 0.13f
        if (horizontal) {
            val beamH = cell * 0.2f
            val beamY = rect.center.y - beamH / 2f
            scope.drawRoundRect(
                brush = Brush.horizontalGradient(listOf(woodDark, woodBase, woodLight, woodBase, woodDark)),
                topLeft = Offset(rect.left + post, beamY),
                size = Size(rect.width - post * 2, beamH),
                cornerRadius = CornerRadius(beamH * 0.35f),
            )
            if (themeId == PacMazeMapThemeId.CHINESE) {
                scope.drawLine(
                    color = pillarRed.copy(alpha = 0.55f),
                    start = Offset(rect.left + post * 0.6f, rect.top + post),
                    end = Offset(rect.left + post * 0.6f, rect.bottom - post),
                    strokeWidth = post * 0.35f,
                )
                scope.drawLine(
                    color = pillarRed.copy(alpha = 0.55f),
                    start = Offset(rect.right - post * 1.2f, rect.top + post),
                    end = Offset(rect.right - post * 1.2f, rect.bottom - post),
                    strokeWidth = post * 0.35f,
                )
            }
            listOf(rect.left + post * 0.5f, rect.right - post * 1.5f).forEach { px ->
                scope.drawRoundRect(
                    color = woodDark,
                    topLeft = Offset(px, rect.top + post * 0.8f),
                    size = Size(post, rect.height - post * 1.6f),
                    cornerRadius = CornerRadius(post * 0.25f),
                )
            }
            val latticeY = beamY + beamH * 0.15f
            scope.drawLine(
                color = woodDark.copy(alpha = 0.45f),
                start = Offset(rect.left + post * 1.5f, latticeY),
                end = Offset(rect.right - post * 1.5f, latticeY + beamH * 0.5f),
                strokeWidth = 1f,
            )
        }
        if (vertical) {
            val plankW = cell * 0.16f
            var px = rect.left + cell * 0.18f
            var idx = 0
            while (px < rect.right - plankW) {
                scope.drawRoundRect(
                    brush = Brush.verticalGradient(
                        listOf(
                            if (idx % 2 == 0) woodLight else woodBase,
                            woodDark,
                        ),
                    ),
                    topLeft = Offset(px, rect.top + post),
                    size = Size(plankW * 0.82f, rect.height - post * 2),
                    cornerRadius = CornerRadius(1.5f),
                )
                px += plankW
                idx++
            }
        }
        if (!horizontal && !vertical) drawBlueBrickWall(scope, rect, cell)
    }

    private fun drawTileRoofWall(
        scope: DrawScope,
        rect: Rect,
        cell: Float,
        ctx: PacMazeMapRenderContext,
        x: Int,
        y: Int,
    ) {
        val bodyH = rect.height * 0.52f
        val body = Rect(rect.left, rect.top + rect.height - bodyH, rect.right, rect.bottom)
        drawBlueBrickWall(scope, body, cell)

        val roof = Rect(rect.left, rect.top, rect.right, rect.top + rect.height * 0.52f)
        scope.drawRect(color = tileEdge.copy(alpha = 0.45f), topLeft = roof.topLeft, size = roof.size)
        val tileW = cell * 0.44f
        val tileH = cell * 0.2f
        var row = 0
        var ty = roof.top
        while (ty < roof.bottom - tileH * 0.5f && row < 4) {
            var tx = roof.left - tileW * 0.15f + if (row % 2 == 1) tileW * 0.42f else 0f
            while (tx < roof.right + tileW * 0.2f) {
                val path = Path().apply {
                    moveTo(tx, ty + tileH)
                    quadraticBezierTo(tx + tileW / 2f, ty - tileH * 0.45f, tx + tileW, ty + tileH)
                    lineTo(tx + tileW * 0.88f, ty + tileH * 0.85f)
                    quadraticBezierTo(tx + tileW / 2f, ty - tileH * 0.15f, tx + tileW * 0.12f, ty + tileH * 0.85f)
                    close()
                }
                scope.drawPath(
                    path = path,
                    brush = Brush.verticalGradient(listOf(tileGlaze, tileBase)),
                )
                scope.drawPath(path = path, color = tileEdge.copy(alpha = 0.55f), style = Stroke(cell * 0.025f))
                tx += tileW * 0.82f
            }
            ty += tileH * 0.58f
            row++
        }
        scope.drawLine(
            color = ridgeGold.copy(alpha = 0.75f),
            start = Offset(rect.left + cell * 0.08f, roof.top + roof.height * 0.22f),
            end = Offset(rect.right - cell * 0.08f, roof.top + roof.height * 0.22f),
            strokeWidth = cell * 0.045f,
        )
        if (isWalkable(ctx.world, x, y - 1)) {
            scope.drawLine(
                color = Color.Black.copy(alpha = 0.28f),
                start = Offset(rect.left, rect.top + rect.height * 0.5f),
                end = Offset(rect.right, rect.top + rect.height * 0.5f),
                strokeWidth = cell * 0.05f,
            )
        }
    }

    fun drawWoodGate(scope: DrawScope, rect: Rect, cell: Float, themeId: PacMazeMapThemeId) {
        val post = cell * 0.15f
        val gap = rect.width * 0.36f
        listOf(rect.left + post, rect.right - post * 2f).forEach { px ->
            scope.drawRoundRect(
                brush = Brush.verticalGradient(listOf(woodLight, woodDark)),
                topLeft = Offset(px, rect.top + post * 0.4f),
                size = Size(post, rect.height - post * 0.9f),
                cornerRadius = CornerRadius(post * 0.22f),
            )
            if (themeId == PacMazeMapThemeId.CHINESE) {
                scope.drawRect(
                    color = pillarRed.copy(alpha = 0.7f),
                    topLeft = Offset(px, rect.top + post * 0.5f),
                    size = Size(post, post * 0.55f),
                )
            }
        }
        scope.drawRoundRect(
            brush = Brush.horizontalGradient(listOf(woodDark, woodLight, woodDark)),
            topLeft = Offset(rect.left + post, rect.top + post * 0.55f),
            size = Size(rect.width - post * 2, cell * 0.18f),
            cornerRadius = CornerRadius(cell * 0.05f),
        )
        val arch = Path().apply {
            moveTo(rect.center.x - gap / 2f, rect.bottom - post)
            quadraticBezierTo(rect.center.x, rect.top + post * 0.35f, rect.center.x + gap / 2f, rect.bottom - post)
        }
        scope.drawPath(arch, color = Color(0xFF2C1810).copy(alpha = 0.35f), style = Stroke(cell * 0.06f))
        scope.drawPath(arch, color = woodLight.copy(alpha = 0.55f), style = Stroke(cell * 0.03f))
    }

    private fun isSolidWall(world: PacMazeWorldState, x: Int, y: Int): Boolean {
        if (x !in 0 until world.width || y !in 0 until world.height) return true
        val tile = world.tileAt(x, y)
        return tile.isSolidWall() || tile == TileType.DOOR
    }

    private fun isWalkable(world: PacMazeWorldState, x: Int, y: Int): Boolean {
        if (x !in 0 until world.width || y !in 0 until world.height) return false
        return world.tileAt(x, y).isWalkableFloor()
    }
}
