package com.example.funlife.ui.screens.pacmaze.maptheme

import androidx.compose.ui.graphics.Color

data class PacMazeParticleConfig(
    val enabled: Boolean = true,
    val count: Int = 48,
    val speedMin: Float = 18f,
    val speedMax: Float = 42f,
    val glyphPool: List<String> = listOf("0", "1"),
    val color: Color = Color(0xFF00E5FF),
    val alpha: Float = 0.35f,
)

data class PacMazeThemePalette(
    val backgroundTop: Color,
    val backgroundBottom: Color,
    val pathFill: Color,
    val pathGrid: Color,
    val wallFill: Color,
    val wallEdge: Color,
    val wallGlow: Color,
    val pelletPrimary: Color,
    val pelletGlow: Color,
    val powerCore: Color,
    val powerGlow: Color,
    val tunnelFill: Color,
    val tunnelAccent: Color,
    val frameAccent: Color,
    val ghostColors: List<Color>,
)

data class PacMazeThemeConfig(
    val id: PacMazeMapThemeId,
    val palette: PacMazeThemePalette,
    val pelletGlyphs: List<String> = listOf("•"),
    val powerLabel: String = "★",
    val particles: PacMazeParticleConfig = PacMazeParticleConfig(),
)
