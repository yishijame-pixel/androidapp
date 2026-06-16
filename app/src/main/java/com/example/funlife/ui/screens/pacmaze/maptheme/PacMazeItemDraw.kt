package com.example.funlife.ui.screens.pacmaze.maptheme

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.example.funlife.social.game.engine.pacmaze.GhostMode
import com.example.funlife.social.game.engine.pacmaze.PacMazeItemConstants
import com.example.funlife.social.game.engine.pacmaze.PacMazeLevelProgression
import com.example.funlife.social.game.engine.pacmaze.PacMazeItemKind
import com.example.funlife.social.game.engine.pacmaze.PacMazeMagnetPull
import com.example.funlife.social.game.engine.pacmaze.primaryPac
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/** 道具工厂、地面道具与激活特效绘制。 */
internal object PacMazeItemDraw {

    fun draw(scope: DrawScope, ctx: PacMazeMapRenderContext) {
        drawSpawners(scope, ctx)
        drawFloorItems(scope, ctx)
        drawActiveEffects(scope, ctx)
    }

    /** 绘制飞行中的吸附豆子（应在角色层之后调用）。 */
    fun drawMagnetOverlay(scope: DrawScope, ctx: PacMazeMapRenderContext) {
        if (ctx.world.magnetPulls.isEmpty()) return
        val pac = ctx.world.primaryPac() ?: return
        drawMagnetPulls(
            scope,
            ctx,
            ctx.entityCenter(pac),
            ctx.cell,
            ctx.world.magnetPulls,
            ctx.animPhase,
        )
    }

    private fun drawSpawners(scope: DrawScope, ctx: PacMazeMapRenderContext) {
        val world = ctx.world
        if (world.itemSpawners.isEmpty()) return
        val cell = ctx.cell
        world.itemSpawners.forEachIndexed { index, def ->
            val runtime = world.itemSpawnerStates.firstOrNull { it.id == def.id }
            val rect = ctx.tileRect(def.x, def.y)
            val center = Offset(rect.center.x, rect.center.y)
            val accent = spawnerAccent(def.pool)
            val secondary = itemColor(def.pool.firstOrNull() ?: PacMazeItemKind.MAGNET)
            val cooldown = runtime?.cooldownTicks ?: def.intervalTicks
            val readyProgress = 1f - (cooldown.toFloat() / def.intervalTicks.coerceAtLeast(1)).coerceIn(0f, 1f)
            val readyGlow = readyProgress > 0.82f
            val spin = ctx.animPhase * 0.35f + index * 40f + def.x * 5f
            val breathe = 0.97f + 0.03f * sin(ctx.animPhase * 0.9f + index)

            drawSpawnerShadow(scope, center, cell)
            drawHexPlatform(scope, center, cell * 0.46f, accent, secondary, breathe)

            drawCooldownRing(scope, center, cell * 0.4f, readyProgress, accent, readyGlow, ctx.animPhase)

            def.pool.take(3).forEachIndexed { poolIndex, kind ->
                val angle = spin + poolIndex * (360f / def.pool.size.coerceAtLeast(1))
                val orbitR = cell * 0.3f
                val ox = center.x + cos(angle * PI / 180f).toFloat() * orbitR
                val oy = center.y + sin(angle * PI / 180f).toFloat() * orbitR
                scope.drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(itemColor(kind), itemColor(kind).copy(alpha = 0.2f)),
                        center = Offset(ox, oy),
                        radius = cell * 0.07f,
                    ),
                    radius = cell * 0.07f,
                    center = Offset(ox, oy),
                )
                drawMiniGlyph(scope, kind, Offset(ox, oy), cell * 0.045f)
            }

            scope.drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.95f),
                        accent.copy(alpha = 0.85f),
                        accent.copy(alpha = 0.15f),
                    ),
                    center = center,
                    radius = cell * 0.14f,
                ),
                radius = cell * 0.14f * breathe,
                center = center,
            )
            scope.drawCircle(
                color = Color.White.copy(alpha = 0.9f),
                radius = cell * 0.14f,
                center = center,
                style = Stroke(cell * 0.025f),
            )

            drawCornerPosts(scope, rect, cell, accent.copy(alpha = 0.65f))

            if (readyGlow) {
                val beamH = cell * (0.38f + 0.04f * sin(ctx.animPhase * 1.1f))
                scope.drawLine(
                    color = accent.copy(alpha = 0.55f),
                    start = center - Offset(0f, cell * 0.12f),
                    end = center - Offset(0f, cell * 0.12f + beamH),
                    strokeWidth = cell * 0.06f,
                    cap = StrokeCap.Round,
                )
                scope.drawCircle(
                    color = Color.White.copy(alpha = 0.75f),
                    radius = cell * 0.04f,
                    center = center - Offset(0f, cell * 0.12f + beamH),
                )
            }

            drawSpawnerScanLines(scope, rect, accent, ctx.animPhase + index)
        }
    }

    private fun drawFloorItems(scope: DrawScope, ctx: PacMazeMapRenderContext) {
        val cell = ctx.cell
        ctx.world.floorItems.forEach { item ->
            val rect = ctx.tileRect(item.x, item.y)
            val center = Offset(rect.center.x, rect.center.y)
            val phase = ctx.animPhase * 0.85f + item.x * 0.15f + item.y * 0.2f
            val bob = sin(phase) * cell * 0.022f
            val c = center + Offset(0f, bob - cell * 0.06f)
            val style = PacMazeItemVisuals.style(item.kind)
            val lifeRatio = item.ticksLeft.toFloat() / PacMazeItemConstants.FLOOR_LIFETIME_TICKS

            scope.drawOval(
                color = Color.Black.copy(alpha = 0.28f),
                topLeft = Offset(center.x - cell * 0.24f, center.y + cell * 0.2f),
                size = Size(cell * 0.48f, cell * 0.12f),
            )

            drawLifetimeRing(scope, c, cell * 0.38f, lifeRatio, style.primary, spin = 0f)
            PacMazeItemVisuals.drawBadgeBackground(scope, c, cell * 0.24f, style, pulse = 1f)
            PacMazeItemVisuals.drawKindGlyph(scope, item.kind, c, cell * 0.18f)
            PacMazeItemVisuals.drawLabelBanner(scope, c, cell, style)
        }
    }

    private fun drawActiveEffects(scope: DrawScope, ctx: PacMazeMapRenderContext) {
        val world = ctx.world
        val cell = ctx.cell
        val pac = world.primaryPac() ?: return
        val center = ctx.entityCenter(pac)
        val scale = ctx.effectivePlayerDrawScale.coerceIn(
            com.example.funlife.ui.screens.pacmaze.components.PAC_MAZE_PLAYER_SCALE_MIN,
            com.example.funlife.ui.screens.pacmaze.components.PAC_MAZE_PLAYER_SCALE_MAX * ctx.entityDrawBoost,
        )

        if (world.magnetTicksLeft > 0) {
            drawMagnetRange(scope, ctx, center, cell, ctx.animPhase)
        }

        if (world.shieldCharges > 0) {
            val shieldPulse = 0.7f + 0.3f * sin(ctx.animPhase * 3.5f)
            val r = cell * 0.52f * scale
            scope.rotate(ctx.animPhase * 6f, center) {
                drawHexPath(scope, center, r, Color(0xFF00E5FF).copy(alpha = 0.28f * shieldPulse), stroke = cell * 0.05f)
            }
            scope.drawCircle(
                color = Color(0xFF80DEEA).copy(alpha = 0.12f * shieldPulse),
                radius = r * 0.88f,
                center = center,
            )
        }

        if (world.frostTicksLeft > 0) {
            val frostRatio = (world.frostTicksLeft / PacMazeItemConstants.FROST_DURATION_TICKS.toFloat())
                .coerceIn(0.2f, 1f)
            val coldCenters = world.entities
                .filter { it.role != "pac" && it.ghostMode != GhostMode.EATEN }
                .map { ctx.entityCenter(it) }
            PacMazeGhostVisualEffects.drawScreenFrostOverlay(
                scope = scope,
                mapLeft = ctx.offsetX,
                mapTop = ctx.offsetY,
                mapW = ctx.mapW,
                mapH = ctx.mapH,
                cell = cell,
                frostRatio = frostRatio,
                animPhase = ctx.animPhase,
                coldCenters = coldCenters,
            )
        }

        if (world.speedBoostTicksLeft > 0) {
            repeat(2) { trail ->
                val offset = cell * 0.18f * (trail + 1)
                scope.drawRoundRect(
                    color = Color(0xFFFFD54F).copy(alpha = 0.15f - trail * 0.05f),
                    topLeft = Offset(center.x - cell * 0.2f, center.y + offset * scale),
                    size = Size(cell * 0.4f * scale, cell * 0.08f),
                    cornerRadius = CornerRadius(cell * 0.04f),
                )
            }
        }
    }

    private fun drawSpawnerShadow(scope: DrawScope, center: Offset, cell: Float) {
        scope.drawOval(
            color = Color.Black.copy(alpha = 0.35f),
            topLeft = Offset(center.x - cell * 0.34f, center.y + cell * 0.14f),
            size = Size(cell * 0.68f, cell * 0.16f),
        )
    }

    private fun drawHexPlatform(
        scope: DrawScope,
        center: Offset,
        radius: Float,
        primary: Color,
        secondary: Color,
        breathe: Float,
    ) {
        val r = radius * (0.94f + breathe * 0.06f)
        val path = hexPath(center, r)
        scope.drawPath(
            path,
            Brush.radialGradient(
                colors = listOf(primary.copy(alpha = 0.55f), secondary.copy(alpha = 0.25f)),
                center = center,
                radius = r,
            ),
            style = Fill,
        )
        scope.drawPath(path, Color.White.copy(alpha = 0.45f), style = Stroke(r * 0.06f))
    }

    private fun drawCooldownRing(
        scope: DrawScope,
        center: Offset,
        radius: Float,
        progress: Float,
        accent: Color,
        ready: Boolean,
        animPhase: Float,
    ) {
        scope.drawCircle(
            color = Color.Black.copy(alpha = 0.25f),
            radius = radius,
            center = center,
            style = Stroke(radius * 0.07f),
        )
        if (progress > 0.02f) {
            scope.drawArc(
                color = if (ready) Color.White.copy(alpha = 0.9f) else accent.copy(alpha = 0.85f),
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2f, radius * 2f),
                style = Stroke(radius * 0.07f, cap = StrokeCap.Round),
            )
        }
        if (ready) {
            scope.drawCircle(
                color = accent.copy(alpha = 0.2f + 0.05f * sin(animPhase * 1.2f)),
                radius = radius * 1.08f,
                center = center,
                style = Stroke(radius * 0.04f),
            )
        }
    }

    private fun drawLifetimeRing(
        scope: DrawScope,
        center: Offset,
        radius: Float,
        lifeRatio: Float,
        color: Color,
        spin: Float,
    ) {
        if (spin == 0f) {
            scope.drawArc(
                color = color.copy(alpha = 0.7f),
                startAngle = -90f,
                sweepAngle = 360f * lifeRatio,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2f, radius * 2f),
                style = Stroke(radius * 0.08f, cap = StrokeCap.Round),
            )
            return
        }
        scope.rotate(spin, center) {
            scope.drawArc(
                color = color.copy(alpha = 0.7f),
                startAngle = 0f,
                sweepAngle = 360f * lifeRatio,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2f, radius * 2f),
                style = Stroke(radius * 0.08f, cap = StrokeCap.Round),
            )
        }
    }

    private fun drawCornerPosts(scope: DrawScope, rect: Rect, cell: Float, color: Color) {
        val pad = cell * 0.1f
        val corners = listOf(
            Offset(rect.left + pad, rect.top + pad),
            Offset(rect.right - pad, rect.top + pad),
            Offset(rect.left + pad, rect.bottom - pad),
            Offset(rect.right - pad, rect.bottom - pad),
        )
        corners.forEach { corner ->
            scope.drawRoundRect(
                color = color,
                topLeft = Offset(corner.x - cell * 0.04f, corner.y - cell * 0.04f),
                size = Size(cell * 0.08f, cell * 0.08f),
                cornerRadius = CornerRadius(cell * 0.02f),
            )
        }
    }

    private fun drawSpawnerScanLines(scope: DrawScope, rect: Rect, accent: Color, phase: Float) {
        val y = rect.top + ((phase * 0.35f) % 1f) * rect.height
        scope.drawLine(
            accent.copy(alpha = 0.18f),
            Offset(rect.left + rect.width * 0.15f, y),
            Offset(rect.right - rect.width * 0.15f, y),
            strokeWidth = 1.5f,
        )
    }

    private fun drawHexPath(scope: DrawScope, center: Offset, radius: Float, color: Color, stroke: Float) {
        scope.drawPath(hexPath(center, radius), color, style = Stroke(stroke))
    }

    private fun hexPath(center: Offset, radius: Float): Path {
        val path = Path()
        for (i in 0 until 6) {
            val angle = (60f * i - 30f) * PI / 180f
            val x = center.x + cos(angle).toFloat() * radius
            val y = center.y + sin(angle).toFloat() * radius
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        return path
    }

    private fun drawSnowflake(scope: DrawScope, center: Offset, size: Float, color: Color, phase: Float) {
        scope.rotate(phase * 12f, center) {
            repeat(6) { i ->
                val angle = i * 60f
                val ex = center.x + cos(angle * PI / 180f).toFloat() * size
                val ey = center.y + sin(angle * PI / 180f).toFloat() * size
                scope.drawLine(color, center, Offset(ex, ey), strokeWidth = size * 0.14f, cap = StrokeCap.Round)
            }
        }
    }

    private fun drawMiniGlyph(scope: DrawScope, kind: PacMazeItemKind, center: Offset, size: Float) {
        drawItemGlyph(scope, kind, center, size, Color.White)
    }

    private fun drawItemGlyph(
        scope: DrawScope,
        kind: PacMazeItemKind,
        center: Offset,
        size: Float,
        tint: Color,
    ) {
        when (kind) {
            PacMazeItemKind.MAGNET -> {
                val gap = size * 0.08f
                scope.drawRoundRect(
                    color = Color(0xFF5C6BC0),
                    topLeft = Offset(center.x - size - gap / 2f, center.y - size * 0.9f),
                    size = Size(size, size * 1.8f),
                    cornerRadius = CornerRadius(size * 0.2f),
                )
                scope.drawRoundRect(
                    color = Color(0xFFEF5350),
                    topLeft = Offset(center.x + gap / 2f, center.y - size * 0.9f),
                    size = Size(size, size * 1.8f),
                    cornerRadius = CornerRadius(size * 0.2f),
                )
                scope.drawRect(
                    Color(0xFFEEEEEE),
                    Offset(center.x - size - gap / 2f, center.y - size * 0.15f),
                    Size(size * 2f + gap, size * 0.3f),
                )
            }
            PacMazeItemKind.SHIELD -> {
                val path = Path().apply {
                    moveTo(center.x, center.y - size * 1.1f)
                    cubicTo(
                        center.x + size, center.y - size * 0.9f,
                        center.x + size * 1.1f, center.y,
                        center.x, center.y + size * 1.1f,
                    )
                    cubicTo(
                        center.x - size * 1.1f, center.y,
                        center.x - size, center.y - size * 0.9f,
                        center.x, center.y - size * 1.1f,
                    )
                    close()
                }
                scope.drawPath(path, Brush.linearGradient(listOf(Color.White, tint)), style = Fill)
                scope.drawPath(path, tint.copy(alpha = 0.9f), style = Stroke(size * 0.1f))
                scope.drawLine(
                    Color.White.copy(alpha = 0.7f),
                    Offset(center.x, center.y - size * 0.55f),
                    Offset(center.x, center.y + size * 0.45f),
                    strokeWidth = size * 0.12f,
                    cap = StrokeCap.Round,
                )
            }
            PacMazeItemKind.FROST -> {
                repeat(6) { i ->
                    val angle = i * 60f
                    val outer = size * 1.1f
                    val ex = center.x + cos(angle * PI / 180f).toFloat() * outer
                    val ey = center.y + sin(angle * PI / 180f).toFloat() * outer
                    scope.drawLine(Color.White, center, Offset(ex, ey), strokeWidth = size * 0.14f, cap = StrokeCap.Round)
                    val mid = size * 0.55f
                    val mx = center.x + cos((angle + 30f) * PI / 180f).toFloat() * mid
                    val my = center.y + sin((angle + 30f) * PI / 180f).toFloat() * mid
                    scope.drawLine(tint, Offset(ex, ey), Offset(mx, my), strokeWidth = size * 0.1f, cap = StrokeCap.Round)
                }
                scope.drawCircle(Color.White.copy(alpha = 0.85f), radius = size * 0.18f, center = center)
            }
            PacMazeItemKind.SPEED -> {
                val bolt = Path().apply {
                    moveTo(center.x + size * 0.15f, center.y - size * 1.1f)
                    lineTo(center.x - size * 0.35f, center.y + size * 0.05f)
                    lineTo(center.x + size * 0.05f, center.y + size * 0.05f)
                    lineTo(center.x - size * 0.2f, center.y + size * 1.1f)
                    lineTo(center.x + size * 0.55f, center.y - size * 0.25f)
                    lineTo(center.x + size * 0.12f, center.y - size * 0.25f)
                    close()
                }
                scope.drawPath(bolt, Brush.linearGradient(listOf(Color(0xFFFFF59D), Color(0xFFFFB300))), style = Fill)
                scope.drawPath(bolt, Color.White.copy(alpha = 0.8f), style = Stroke(size * 0.06f))
            }
            PacMazeItemKind.DOUBLE -> {
                scope.drawCircle(
                    brush = Brush.radialGradient(listOf(Color(0xFFB9F6CA), Color(0xFF00C853)), center = center, radius = size),
                    radius = size,
                    center = center,
                )
                scope.drawLine(Color.White, Offset(center.x - size * 0.55f, center.y), Offset(center.x + size * 0.55f, center.y), strokeWidth = size * 0.16f, cap = StrokeCap.Round)
                scope.drawLine(Color.White, Offset(center.x, center.y - size * 0.55f), Offset(center.x, center.y + size * 0.55f), strokeWidth = size * 0.16f, cap = StrokeCap.Round)
                TextGlyph2x(scope, center + Offset(size * 0.42f, -size * 0.42f), size * 0.42f)
            }
            PacMazeItemKind.CHARGE -> {
                scope.drawCircle(
                    brush = Brush.radialGradient(listOf(Color(0xFFFF8A80), Color(0xFFD50000)), center = center, radius = size),
                    radius = size,
                    center = center,
                )
                val core = Path().apply {
                    moveTo(center.x, center.y - size * 0.75f)
                    lineTo(center.x + size * 0.55f, center.y)
                    lineTo(center.x, center.y + size * 0.75f)
                    lineTo(center.x - size * 0.55f, center.y)
                    close()
                }
                scope.drawPath(core, Color.White.copy(alpha = 0.9f), style = Fill)
                scope.drawCircle(Color.White.copy(alpha = 0.35f), radius = size * 0.22f, center = center - Offset(size * 0.22f, size * 0.22f))
            }
        }
    }

    private fun TextGlyph2x(scope: DrawScope, center: Offset, size: Float) {
        scope.drawRoundRect(
            color = Color.White,
            topLeft = Offset(center.x - size * 0.55f, center.y - size * 0.35f),
            size = Size(size * 1.1f, size * 0.7f),
            cornerRadius = CornerRadius(size * 0.12f),
        )
        scope.drawLine(
            Color(0xFF00C853),
            Offset(center.x - size * 0.25f, center.y),
            Offset(center.x + size * 0.3f, center.y),
            strokeWidth = size * 0.14f,
            cap = StrokeCap.Round,
        )
    }

    private fun spawnerAccent(pool: List<PacMazeItemKind>): Color {
        val kind = pool.firstOrNull() ?: PacMazeItemKind.MAGNET
        return itemColor(kind)
    }

    private fun itemColor(kind: PacMazeItemKind): Color = when (kind) {
        PacMazeItemKind.MAGNET -> Color(0xFF7E57C2)
        PacMazeItemKind.SHIELD -> Color(0xFF00BCD4)
        PacMazeItemKind.FROST -> Color(0xFF4FC3F7)
        PacMazeItemKind.SPEED -> Color(0xFFFFC107)
        PacMazeItemKind.DOUBLE -> Color(0xFF69F0AE)
        PacMazeItemKind.CHARGE -> Color(0xFFFF5252)
    }

    private fun drawMagnetRange(
        scope: DrawScope,
        ctx: PacMazeMapRenderContext,
        pacCenter: Offset,
        cell: Float,
        animPhase: Float,
    ) {
        val radiusPx = PacMazeLevelProgression.magnetRadiusCells(ctx.world.levelId) * cell
        val breathe = 0.92f + 0.04f * sin(animPhase * 0.9f)

        scope.drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF7C4DFF).copy(alpha = 0.16f),
                    Color(0xFF7C4DFF).copy(alpha = 0.08f),
                    Color.Transparent,
                ),
                center = pacCenter,
                radius = radiusPx * breathe,
            ),
            radius = radiusPx * breathe,
            center = pacCenter,
        )

        val dashSweep = 14f
        val gapSweep = 10f
        var angle = animPhase * 8f
        while (angle < 360f) {
            scope.drawArc(
                color = Color(0xFFB388FF).copy(alpha = 0.85f),
                startAngle = angle,
                sweepAngle = dashSweep,
                useCenter = false,
                topLeft = Offset(pacCenter.x - radiusPx * breathe, pacCenter.y - radiusPx * breathe),
                size = Size(radiusPx * 2f * breathe, radiusPx * 2f * breathe),
                style = Stroke(cell * 0.045f, cap = StrokeCap.Round),
            )
            angle += dashSweep + gapSweep
        }

        scope.drawCircle(
            color = Color(0xFFEDE7F6).copy(alpha = 0.35f),
            radius = radiusPx * breathe,
            center = pacCenter,
            style = Stroke(cell * 0.02f),
        )
    }

    private fun pullToScreen(ctx: PacMazeMapRenderContext, pull: PacMazeMagnetPull): Offset =
        ctx.gridToScreen(pull.x, pull.y)

    private fun drawMagnetPulls(
        scope: DrawScope,
        ctx: PacMazeMapRenderContext,
        pacCenter: Offset,
        cell: Float,
        pulls: List<PacMazeMagnetPull>,
        animPhase: Float,
    ) {
        pulls.forEachIndexed { index, pull ->
            val pos = pullToScreen(ctx, pull)
            val source = ctx.gridToScreen(pull.sourceX.toFloat(), pull.sourceY.toFloat())
            val trailColor = if (pull.isPower) Color(0xFFFFD54F) else Color(0xFFB388FF)
            val pacGx = (pacCenter.x - ctx.offsetX) / cell
            val pacGy = (pacCenter.y - ctx.offsetY) / cell
            val totalDist = hypot(pull.sourceX + 0.5f - pacGx, pull.sourceY + 0.5f - pacGy)
            val remainDist = hypot(pull.x - pacGx, pull.y - pacGy)
            val progress = if (totalDist > 0.01f) {
                (1f - remainDist / totalDist).coerceIn(0f, 1f)
            } else {
                1f
            }

            scope.drawLine(
                color = trailColor.copy(alpha = 0.18f + progress * 0.35f),
                start = source,
                end = pos,
                strokeWidth = if (pull.isPower) cell * 0.05f else cell * 0.03f,
                cap = StrokeCap.Round,
            )
            scope.drawLine(
                color = Color.White.copy(alpha = 0.35f + progress * 0.25f),
                start = pos,
                end = pacCenter,
                strokeWidth = if (pull.isPower) cell * 0.04f else cell * 0.025f,
                cap = StrokeCap.Round,
            )

            if (pull.isPower) {
                scope.drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFFF59D),
                            Color(0xFFFFB300),
                            Color(0xFFFF6F00).copy(alpha = 0.35f),
                        ),
                        center = pos,
                        radius = cell * 0.2f,
                    ),
                    radius = cell * 0.2f,
                    center = pos,
                )
                scope.drawCircle(
                    color = Color.White.copy(alpha = 0.9f),
                    radius = cell * 0.2f,
                    center = pos,
                    style = Stroke(cell * 0.03f),
                )
                repeat(4) { spark ->
                    val angle = animPhase * 30f + index * 40f + spark * 90f
                    val dist = cell * 0.28f
                    val sx = pos.x + cos(angle * PI / 180f).toFloat() * dist
                    val sy = pos.y + sin(angle * PI / 180f).toFloat() * dist
                    scope.drawCircle(Color(0xFFFFF59D).copy(alpha = 0.7f), cell * 0.03f, Offset(sx, sy))
                }
            } else {
                scope.drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White, trailColor),
                        center = pos,
                        radius = cell * 0.08f,
                    ),
                    radius = cell * 0.08f,
                    center = pos,
                )
            }
        }
    }
}
