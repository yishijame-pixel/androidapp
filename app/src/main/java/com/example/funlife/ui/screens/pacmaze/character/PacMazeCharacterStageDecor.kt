package com.example.funlife.ui.screens.pacmaze.character

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeSkinId
import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeCosmeticCatalog
import kotlin.math.sin

/** 角色展示背景：Clean = 无彩色光晕/网格，仅角色 + 轻阴影 */
enum class PacMazeStageBackdrop {
    Decorated,
    Clean,
}

enum class CharacterStageKind {
    LINE_ART,
    OCEAN,
    DEFAULT,
}

fun PacMazeSkinId.stageKind(): CharacterStageKind = when {
    isLineArt() -> CharacterStageKind.LINE_ART
    isOcean() -> CharacterStageKind.OCEAN
    else -> CharacterStageKind.DEFAULT
}

object PacMazeCharacterStageDecor {

    fun neutralCardFill(selected: Boolean): Color =
        if (selected) Color(0xFF1A2236) else Color(0xFF151D30)

    fun drawGroundShadow(scope: DrawScope, centerYFraction: Float = 0.58f) {
        val center = Offset(scope.size.width / 2f, scope.size.height * centerYFraction)
        scope.drawOval(
            color = Color.Black.copy(alpha = 0.2f),
            topLeft = Offset(center.x - scope.size.width * 0.26f, center.y + scope.size.height * 0.05f),
            size = Size(scope.size.width * 0.52f, scope.size.height * 0.08f),
        )
    }

    fun drawBackdrop(
        scope: DrawScope,
        skinId: PacMazeSkinId,
        accent: Color,
        selected: Boolean,
        animPhase: Float,
        backdrop: PacMazeStageBackdrop,
    ) {
        when (backdrop) {
            PacMazeStageBackdrop.Decorated -> drawStageBackdrop(scope, skinId, accent, selected, animPhase)
            PacMazeStageBackdrop.Clean -> drawGroundShadow(scope)
        }
    }

    fun cardBackground(skinId: PacMazeSkinId, selected: Boolean, accent: Color): Brush {
        val kind = skinId.stageKind()
        return when (kind) {
            CharacterStageKind.OCEAN -> Brush.verticalGradient(
                listOf(
                    Color(0xFF1565C0).copy(alpha = if (selected) 0.55f else 0.28f),
                    Color(0xFF0A2744),
                    Color(0xFF061525),
                ),
            )
            CharacterStageKind.LINE_ART -> Brush.verticalGradient(
                listOf(
                    Color(0xFF3D3558).copy(alpha = if (selected) 0.7f else 0.4f),
                    Color(0xFF1E1A2E),
                    Color(0xFF12101C),
                ),
            )
            CharacterStageKind.DEFAULT -> Brush.verticalGradient(
                listOf(
                    if (selected) accent.copy(alpha = 0.22f) else Color(0xFF2A3550),
                    Color(0xFF151B28),
                    Color(0xFF0E121C),
                ),
            )
        }
    }

    fun stageBackground(
        skinId: PacMazeSkinId,
        selected: Boolean,
        accent: Color,
        backdrop: PacMazeStageBackdrop = PacMazeStageBackdrop.Decorated,
    ): Brush {
        if (backdrop == PacMazeStageBackdrop.Clean) {
            return Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
        }
        val kind = skinId.stageKind()
        return when (kind) {
            CharacterStageKind.OCEAN -> Brush.verticalGradient(
                listOf(
                    Color(0xFF1976D2).copy(alpha = if (selected) 1f else 0.82f),
                    Color(0xFF0D3B66),
                    Color(0xFF051A2E),
                ),
            )
            CharacterStageKind.LINE_ART -> Brush.verticalGradient(
                listOf(
                    Color(0xFF352F4A).copy(alpha = if (selected) 1f else 0.88f),
                    Color(0xFF1F1A30),
                    Color(0xFF14111F),
                ),
            )
            CharacterStageKind.DEFAULT -> Brush.verticalGradient(
                listOf(
                    accent.copy(alpha = if (selected) 0.28f else 0.1f),
                    Color(0xFF101828),
                    Color(0xFF0A0E16),
                ),
            )
        }
    }

    fun drawStageBackdrop(
        scope: DrawScope,
        skinId: PacMazeSkinId,
        accent: Color,
        selected: Boolean,
        animPhase: Float,
    ) {
        val center = Offset(scope.size.width / 2f, scope.size.height * 0.58f)
        val min = scope.size.minDimension
        when (skinId.stageKind()) {
            CharacterStageKind.OCEAN -> {
                scope.drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF4FC3F7).copy(alpha = if (selected) 0.28f else 0.14f),
                            Color(0xFF0277BD).copy(alpha = 0.06f),
                            Color.Transparent,
                        ),
                        center = center,
                        radius = min * 0.5f,
                    ),
                    radius = min * 0.5f,
                    center = center,
                )
                repeat(5) { i ->
                    val phase = animPhase * 0.6f + i * 1.1f
                    val bx = center.x + sin(phase) * min * 0.18f + (i - 2) * min * 0.08f
                    val by = center.y - min * (0.22f + i * 0.05f) - (phase * 0.12f % 1f) * min * 0.15f
                    scope.drawCircle(
                        color = Color.White.copy(alpha = 0.06f + i * 0.025f),
                        radius = 1.5f + i * 0.8f,
                        center = Offset(bx, by),
                    )
                }
                val waveY = center.y + min * 0.22f
                scope.drawLine(
                    color = Color(0xFF81D4FA).copy(alpha = if (selected) 0.25f else 0.12f),
                    start = Offset(center.x - min * 0.4f, waveY + sin(animPhase) * 2f),
                    end = Offset(center.x + min * 0.4f, waveY - sin(animPhase * 0.8f) * 2f),
                    strokeWidth = 1.2f,
                )
            }
            CharacterStageKind.LINE_ART -> {
                scope.drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFFF8E1).copy(alpha = if (selected) 0.2f else 0.1f),
                            Color.Transparent,
                        ),
                        center = center,
                        radius = min * 0.46f,
                    ),
                    radius = min * 0.46f,
                    center = center,
                )
                scope.drawCircle(
                    color = accent.copy(alpha = if (selected) 0.4f else 0.18f),
                    radius = min * 0.38f,
                    center = center,
                    style = Stroke(width = 1.4f),
                )
                repeat(6) { i ->
                    val a = animPhase * 0.5f + i * 1.05f
                    scope.drawCircle(
                        color = accent.copy(alpha = 0.12f),
                        radius = 2f,
                        center = Offset(
                            center.x + sin(a) * min * 0.32f,
                            center.y + cosLike(a * 0.7f) * min * 0.2f,
                        ),
                    )
                }
            }
            CharacterStageKind.DEFAULT -> {
                scope.drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            accent.copy(alpha = if (selected) 0.38f else 0.14f),
                            Color.Transparent,
                        ),
                        center = center,
                        radius = min * 0.48f,
                    ),
                    radius = min * 0.48f,
                    center = center,
                )
            }
        }
        if (skinId.stageKind() != CharacterStageKind.OCEAN) {
            scope.drawOval(
                color = Color.Black.copy(alpha = if (selected) 0.32f else 0.22f),
                topLeft = Offset(center.x - scope.size.width * 0.28f, center.y + scope.size.height * 0.06f),
                size = Size(scope.size.width * 0.56f, scope.size.height * 0.09f),
            )
        }
    }

    fun drawHeroBackdrop(scope: DrawScope, skinId: PacMazeSkinId, accent: Color, glowAlpha: Float) {
        val center = Offset(scope.size.width / 2f, scope.size.height * 0.42f)
        val min = scope.size.minDimension
        when (skinId.stageKind()) {
            CharacterStageKind.OCEAN -> {
                scope.drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF29B6F6).copy(alpha = glowAlpha * 0.9f),
                            Color(0xFF01579B).copy(alpha = glowAlpha * 0.25f),
                            Color.Transparent,
                        ),
                        center = center,
                        radius = min * 0.58f,
                    ),
                    radius = min * 0.58f,
                    center = center,
                )
            }
            CharacterStageKind.LINE_ART -> {
                scope.drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFFF8E1).copy(alpha = glowAlpha * 0.55f),
                            accent.copy(alpha = glowAlpha * 0.2f),
                            Color.Transparent,
                        ),
                        center = center,
                        radius = min * 0.55f,
                    ),
                    radius = min * 0.55f,
                    center = center,
                )
            }
            CharacterStageKind.DEFAULT -> {
                scope.drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(accent.copy(alpha = glowAlpha), Color.Transparent),
                        center = center,
                        radius = min * 0.55f,
                    ),
                    radius = min * 0.55f,
                    center = center,
                )
            }
        }
        scope.drawCircle(
            color = accent.copy(alpha = 0.14f),
            radius = min * 0.28f,
            center = center,
            style = Stroke(width = 1.5f),
        )
    }

    fun familyLabel(skinId: PacMazeSkinId): String =
        PacMazeCosmeticCatalog.definition(skinId).styleFamily.label

    private fun cosLike(v: Float): Float = sin(v + 1.5708f)
}
