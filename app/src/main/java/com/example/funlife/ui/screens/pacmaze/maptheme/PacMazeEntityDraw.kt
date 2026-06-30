package com.example.funlife.ui.screens.pacmaze.maptheme

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import com.example.funlife.social.game.engine.pacmaze.Direction
import com.example.funlife.social.game.engine.pacmaze.GhostKind
import com.example.funlife.social.game.engine.pacmaze.GhostMode
import com.example.funlife.social.game.engine.pacmaze.GhostSpecialty
import com.example.funlife.social.game.engine.pacmaze.PacMazeEntity
import com.example.funlife.social.game.engine.pacmaze.ghostReleaseHintEntityId
import com.example.funlife.social.game.engine.pacmaze.ghostReleaseSecondsCeil
import com.example.funlife.social.game.engine.pacmaze.isPlayerPac
import com.example.funlife.social.game.engine.pacmaze.PacMazeWorldState
import com.example.funlife.ui.screens.pacmaze.PacMazePalette
import com.example.funlife.ui.screens.pacmaze.components.PacMazeEntityComfortScale
import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeCosmeticCatalog
import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazePlayerCosmeticDraw
import com.example.funlife.ui.screens.pacmaze.cosmetic.skin.PacMazeSkinBitmapDraw
import com.example.funlife.ui.screens.pacmaze.pacMazeGhostAccent
import kotlin.math.min
import kotlin.math.sin

internal object PacMazeEntityDraw {

    /** 玩家/幽灵/弹体统一裁剪到地图内缘，避免角色超出边框。 */
    fun withEntityMapClip(scope: DrawScope, ctx: PacMazeMapRenderContext, block: DrawScope.() -> Unit) {
        val clip = ctx.entityMapClipRect()
        scope.clipRect(
            left = clip.left,
            top = clip.top,
            right = clip.right,
            bottom = clip.bottom,
            block = block,
        )
    }

    fun drawEntities(scope: DrawScope, ctx: PacMazeMapRenderContext) {
        withEntityMapClip(scope, ctx) {
            drawEntitiesUnclipped(this, ctx)
        }
    }

    private fun drawEntitiesUnclipped(scope: DrawScope, ctx: PacMazeMapRenderContext) {
        val world = ctx.world
        val cell = ctx.entityCell
        val animPhase = ctx.animPhase
        val palette = ctx.config.palette

        world.entities.filter { it.role == "ghost" }.forEach { entity ->
            val center = ctx.entityCenter(entity)
            val radius = PacMazeEntityComfortScale.resolveGhostRadius(
                entityCell = cell,
                boost = ctx.entityDrawBoost,
                minRadiusPx = ctx.minGhostRadiusPx,
            )
            val idx = entity.id.removePrefix("ghost_").toIntOrNull() ?: 0
            val baseColor = palette.ghostColors[idx % palette.ghostColors.size]
            val frozen = world.frostTicksLeft > 0 && entity.ghostMode != GhostMode.EATEN
            val frostRatio = if (frozen) {
                (world.frostTicksLeft.toFloat() / com.example.funlife.social.game.engine.pacmaze.PacMazeItemConstants.FROST_DURATION_TICKS)
                    .coerceIn(0.35f, 1f)
            } else {
                0f
            }
            scope.drawGhost(
                center = center,
                radius = radius,
                corridorCellPx = cell,
                baseColor = baseColor,
                kind = entity.ghostKind,
                specialty = entity.ghostSpecialty,
                mode = entity.ghostMode,
                direction = entity.direction ?: entity.facing,
                animPhase = animPhase + idx,
                ghostsFrozen = frozen,
                frostRatio = frostRatio,
                hitStunTicksLeft = entity.hitStunTicksLeft,
                powerTicksLeft = world.powerTicksLeft,
                burstActive = entity.opportunistBurstTicksLeft > 0,
                moving = entity.velX != 0f || entity.velY != 0f,
            )
        }

        world.projectiles.forEach { projectile ->
            val center = ctx.projectileCenter(projectile)
            val radius = cell * 0.14f * ctx.entityDrawBoost
            scope.drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(palette.powerCore, palette.powerGlow, Color.Transparent),
                    center = center,
                    radius = radius * 2.2f,
                ),
                radius = radius * 2.2f,
                center = center,
            )
            scope.drawCircle(color = Color.White, radius = radius, center = center)
        }

        drawPlayerPacs(scope, ctx)
    }

    /** 绘制所有玩家豆（pac / pac_a / pac_b）；在线对战共用。 */
    fun drawPlayerPacs(scope: DrawScope, ctx: PacMazeMapRenderContext) {
        val world = ctx.world
        val cell = ctx.entityCell
        val animPhase = ctx.animPhase
        val palette = ctx.config.palette
        val pacEntities = world.entities.filter { it.isPlayerPac() }
        if (pacEntities.isEmpty()) return
        val hintEntityId = world.ghostReleaseHintEntityId(ctx.onlineLocalEntityId)
        var trailDrawn = false
        pacEntities.forEach { entity ->
            val skinId = ctx.avatarLoadout.skinId
            val center = ctx.playerDrawCenter(entity, skinId)
            val (renderX, renderY) = ctx.renderAnchor(entity)
            com.example.funlife.ui.screens.pacmaze.debug.PacMazeMotionDiag.onRenderSample(
                entityId = entity.id,
                direction = entity.direction ?: entity.facing,
                logicX = entity.x,
                logicY = entity.y,
                renderX = renderX,
                renderY = renderY,
                screenCenterY = center.y,
                blend = ctx.blend,
                cellYPx = ctx.cellY,
            )
            val visualCell = min(ctx.cellX, ctx.cellY)
            val radius = PacMazeCosmeticCatalog.visualRadius(
                cell = visualCell,
                loadout = ctx.avatarLoadout,
                userDrawScale = ctx.playerDrawScale,
                entityDrawBoost = ctx.entityDrawBoost,
                minRadiusPx = ctx.minPlayerRadiusPx,
            ).coerceAtLeast(ctx.minPlayerRadiusPx)
            val markerCenter = center
            val isLocal = ctx.onlineLocalEntityId.isNotBlank() && entity.id == ctx.onlineLocalEntityId
            val showReleaseHint = hintEntityId != null && entity.id == hintEntityId
            if (showReleaseHint) {
                scope.drawGhostReleaseSafeRing(
                    center = markerCenter,
                    radius = radius,
                    animPhase = animPhase,
                )
            }
            if (pacEntities.size > 1) {
                val roleTint = when {
                    isLocal -> PacMazePalette.accentMint
                    entity.role == "pac_b" -> PacMazePalette.accentOrange
                    entity.role == "pac_a" -> PacMazePalette.accentMint
                    else -> palette.frameAccent
                }
                scope.drawCircle(
                    color = roleTint.copy(alpha = 0.6f),
                    radius = radius * 1.12f,
                    center = center,
                    style = Stroke(width = (radius * 0.07f).coerceIn(2f, 4.5f)),
                )
            }
            if (!trailDrawn && (isLocal || ctx.onlineLocalEntityId.isBlank())) {
                PacMazePlayerCosmeticDraw.drawTrail(scope, ctx)
                trailDrawn = true
            }
            PacMazePlayerCosmeticDraw.drawSkinSafe(scope, ctx, entity, center)
            if (showReleaseHint) {
                scope.drawPlayerReleaseMarker(
                    playerCenter = markerCenter,
                    radius = radius,
                    cell = cell,
                    secondsLeft = world.ghostReleaseSecondsCeil(),
                    animPhase = animPhase,
                )
            }
        }
    }

    fun drawMapFrame(scope: DrawScope, ctx: PacMazeMapRenderContext) {
        scope.drawRoundRect(
            color = ctx.config.palette.frameAccent.copy(alpha = 0.35f),
            topLeft = Offset(ctx.offsetX - 3f, ctx.offsetY - 3f),
            size = Size(ctx.mapW + 6f, ctx.mapH + 6f),
            cornerRadius = CornerRadius(6f),
            style = Stroke(width = 2.5f),
        )
    }

    private fun DrawScope.drawGhost(
        center: Offset,
        radius: Float,
        corridorCellPx: Float,
        baseColor: Color,
        kind: GhostKind,
        specialty: GhostSpecialty,
        mode: GhostMode,
        direction: Direction?,
        animPhase: Float,
        ghostsFrozen: Boolean = false,
        frostRatio: Float = 0f,
        hitStunTicksLeft: Int = 0,
        powerTicksLeft: Int = 0,
        burstActive: Boolean = false,
        moving: Boolean = true,
    ) {
        val kindAccent = pacMazeGhostAccent(kind)
        val bodyColor = PacMazeGhostVisualEffects.resolveBodyColor(
            baseTint = baseColor,
            mode = mode,
            powerTicksLeft = powerTicksLeft,
            hitStunTicksLeft = hitStunTicksLeft,
            ghostsFrozen = ghostsFrozen,
            kindAccent = kindAccent,
        ).let { if (burstActive) it.copy(alpha = 1f) else it }
        val wobbleScale = if (moving && !ghostsFrozen && hitStunTicksLeft <= 0) 0.012f else 0f
        val wobble = sin(animPhase * 1.4f) * radius * wobbleScale
        val top = center.y - radius + wobble
        val stunActive = hitStunTicksLeft > 0 && mode != GhostMode.EATEN

        PacMazeSkinBitmapDraw.clipCorridor(this, center, corridorCellPx) {
            if (mode != GhostMode.EATEN) {
                if (stunActive) {
                    PacMazeGhostVisualEffects.drawHitStunAccent(
                        this, center, radius, kindAccent, hitStunTicksLeft, animPhase,
                    )
                }
                PacMazeGhostShapeDraw.drawBody(
                    scope = this,
                    center = center,
                    radius = radius,
                    kind = kind,
                    bodyColor = bodyColor,
                    animPhase = animPhase,
                    wobble = wobble,
                )
                if (stunActive) {
                    PacMazeGhostShapeDraw.drawBodyOutline(
                        scope = this,
                        center = center,
                        radius = radius,
                        kind = kind,
                        outlineColor = Color.White.copy(alpha = 0.88f),
                        strokeWidth = radius * 0.07f,
                        animPhase = animPhase,
                        wobble = wobble,
                    )
                    PacMazeGhostShapeDraw.drawBodyOutline(
                        scope = this,
                        center = center,
                        radius = radius,
                        kind = kind,
                        outlineColor = kindAccent.copy(alpha = 0.95f),
                        strokeWidth = radius * 0.11f,
                        animPhase = animPhase,
                        wobble = wobble,
                    )
                }
                PacMazeGhostShapeDraw.drawEyes(
                    scope = this,
                    center = center,
                    radius = radius,
                    kind = kind,
                    direction = direction,
                    frightened = mode == GhostMode.FRIGHTENED,
                    top = top,
                    animPhase = animPhase,
                )
                PacMazeGhostShapeDraw.drawSpecialtyBadge(
                    scope = this,
                    center = center,
                    radius = radius,
                    specialty = specialty,
                    animPhase = animPhase,
                )
                if (burstActive) {
                    drawCircle(
                        color = Color(0xFFFF9100).copy(alpha = 0.25f + 0.15f * sin(animPhase * 3f)),
                        radius = radius * 1.15f,
                        center = center,
                    )
                }
            } else {
                PacMazeGhostShapeDraw.drawEyes(
                    scope = this,
                    center = center,
                    radius = radius * 0.55f,
                    kind = kind,
                    direction = direction,
                    frightened = false,
                    top = center.y - radius * 0.3f,
                )
            }

            if (ghostsFrozen) {
                PacMazeGhostVisualEffects.drawIceEncasement(
                    this, center, radius, animPhase, frostRatio, FrozenGhostShape.ROUND_GHOST,
                )
                PacMazeGhostVisualEffects.drawFrozenFace(
                    this, center, radius, frostRatio, FrozenGhostShape.ROUND_GHOST,
                )
            }
        }

        PacMazeGhostVisualEffects.drawStunSparks(this, center, radius, hitStunTicksLeft, animPhase)
    }

    private fun DrawScope.drawGhostReleaseSafeRing(
        center: Offset,
        radius: Float,
        animPhase: Float,
    ) {
        val pulse = 0.92f + 0.08f * sin(animPhase * 4f)
        drawCircle(
            color = PacMazePalette.accentMint.copy(alpha = 0.42f + 0.18f * sin(animPhase * 3f)),
            radius = radius * 1.22f * pulse,
            center = center,
            style = Stroke(width = (radius * 0.07f).coerceIn(2f, 5f)),
        )
        drawCircle(
            color = PacMazePalette.accentGold.copy(alpha = 0.22f),
            radius = radius * 1.08f,
            center = center,
            style = Stroke(width = (radius * 0.04f).coerceIn(1.5f, 3f)),
        )
    }

    private fun DrawScope.drawPlayerReleaseMarker(
        playerCenter: Offset,
        radius: Float,
        cell: Float,
        secondsLeft: Int,
        animPhase: Float,
    ) {
        val bob = sin(animPhase * 3.2f) * cell * 0.04f
        val badgeCenter = Offset(playerCenter.x, playerCenter.y - radius * 1.55f - bob)
        val badgeW = cell * 1.75f
        val badgeH = cell * 0.58f
        val left = badgeCenter.x - badgeW / 2f
        val top = badgeCenter.y - badgeH / 2f

        drawLine(
            color = PacMazePalette.accentGold.copy(alpha = 0.55f),
            start = badgeCenter,
            end = Offset(playerCenter.x, playerCenter.y - radius * 0.92f),
            strokeWidth = (cell * 0.05f).coerceIn(1.5f, 3f),
        )

        drawRoundRect(
            brush = Brush.horizontalGradient(
                listOf(PacMazePalette.accentOrange, PacMazePalette.accentGold),
            ),
            topLeft = Offset(left, top),
            size = Size(badgeW, badgeH),
            cornerRadius = CornerRadius(badgeH / 2f),
        )
        drawRoundRect(
            color = Color.White.copy(alpha = 0.28f),
            topLeft = Offset(left + badgeW * 0.04f, top + badgeH * 0.08f),
            size = Size(badgeW * 0.92f, badgeH * 0.38f),
            cornerRadius = CornerRadius(badgeH / 2f),
        )
        drawRoundRect(
            color = Color.White.copy(alpha = 0.85f),
            topLeft = Offset(left - cell * 0.04f, top - cell * 0.04f),
            size = Size(badgeW + cell * 0.08f, badgeH + cell * 0.08f),
            cornerRadius = CornerRadius(badgeH / 2f),
            style = Stroke(width = (cell * 0.05f).coerceIn(1.5f, 2.5f)),
        )

        val pinR = cell * 0.16f
        drawCircle(
            color = PacMazePalette.accentGold,
            radius = pinR,
            center = Offset(badgeCenter.x, top - pinR * 0.35f),
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.9f),
            radius = pinR * 0.55f,
            center = Offset(badgeCenter.x, top - pinR * 0.35f),
        )

        drawContext.canvas.nativeCanvas.apply {
            val label = if (secondsLeft > 0) "你 · 安全 ${secondsLeft}s" else "你"
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.White.toArgb()
                textSize = cell * 0.31f
                typeface = Typeface.DEFAULT_BOLD
                textAlign = Paint.Align.CENTER
            }
            drawText(label, badgeCenter.x, badgeCenter.y + cell * 0.11f, paint)
        }
    }
}
