package com.example.funlife.ui.screens.pacmaze.maptheme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.funlife.social.game.engine.pacmaze.Direction
import com.example.funlife.social.game.engine.pacmaze.GhostKind
import com.example.funlife.social.game.engine.pacmaze.GhostSpecialty
import kotlin.math.sin

/** 经典 / 古风 / 花园主题幽灵绘制入口。 */
internal object PacMazeGhostShapeDraw {

    fun drawBody(
        scope: DrawScope,
        center: Offset,
        radius: Float,
        kind: GhostKind,
        bodyColor: Color,
        animPhase: Float,
        wobble: Float,
    ) = PacMazeGhostVisualDraw.drawBody(scope, center, radius, kind, bodyColor, animPhase, wobble)

    fun drawBodyOutline(
        scope: DrawScope,
        center: Offset,
        radius: Float,
        kind: GhostKind,
        outlineColor: Color,
        strokeWidth: Float,
        animPhase: Float,
        wobble: Float,
    ) = PacMazeGhostVisualDraw.drawBodyOutline(scope, center, radius, kind, outlineColor, strokeWidth, animPhase, wobble)

    fun drawEyes(
        scope: DrawScope,
        center: Offset,
        radius: Float,
        kind: GhostKind,
        direction: Direction?,
        frightened: Boolean,
        top: Float,
        animPhase: Float = 0f,
    ) = PacMazeGhostVisualDraw.drawEyes(scope, center, radius, kind, direction, frightened, top, animPhase)

    fun drawSpecialtyBadge(
        scope: DrawScope,
        center: Offset,
        radius: Float,
        specialty: GhostSpecialty,
        animPhase: Float,
    ) {
        if (!specialty.isActive) return
        val pulse = 0.65f + 0.35f * sin(animPhase * 2.4f)
        val badgeColor = when (specialty) {
            GhostSpecialty.PHASE_WALKER -> Color(0xFF64FFDA)
            GhostSpecialty.GATE_KEEPER -> Color(0xFFFFD54F)
            GhostSpecialty.NONE -> Color.Transparent
        }
        scope.drawCircle(
            color = badgeColor.copy(alpha = 0.85f * pulse),
            radius = radius * 0.16f,
            center = Offset(center.x + radius * 0.62f, center.y - radius * 0.62f),
        )
        when (specialty) {
            GhostSpecialty.PHASE_WALKER -> {
                scope.drawCircle(
                    color = Color(0xFF18FFFF).copy(alpha = 0.35f * pulse),
                    radius = radius * 0.28f,
                    center = center,
                )
            }
            GhostSpecialty.GATE_KEEPER -> {
                scope.drawRect(
                    color = Color(0xFFFFD54F).copy(alpha = 0.55f * pulse),
                    topLeft = Offset(center.x - radius * 0.55f, center.y + radius * 0.45f),
                    size = androidx.compose.ui.geometry.Size(radius * 1.1f, radius * 0.12f),
                )
            }
            GhostSpecialty.NONE -> Unit
        }
    }
}
