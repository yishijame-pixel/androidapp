package com.example.funlife.ui.screens.pacmaze.maptheme

import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope

object PacMazeThemeRegistry {

    private const val TAG = "PacMazeThemeRegistry"

    fun themeForLevel(levelId: Int): PacMazeMapThemeId = when (levelId) {
        1, 2, 6 -> PacMazeMapThemeId.CYBERPUNK
        3, 7, 12 -> PacMazeMapThemeId.GARDEN
        4, 8 -> PacMazeMapThemeId.FOOD
        5, 9, 10, 11, 13 -> PacMazeMapThemeId.CHINESE
        else -> PacMazeMapThemeId.CYBERPUNK
    }

    fun configFor(themeId: PacMazeMapThemeId): PacMazeThemeConfig = when (themeId) {
        PacMazeMapThemeId.CLASSIC -> classic()
        PacMazeMapThemeId.CYBERPUNK -> cyberpunk()
        PacMazeMapThemeId.GARDEN -> garden()
        PacMazeMapThemeId.FOOD -> food()
        PacMazeMapThemeId.CHINESE -> chinese()
    }

    fun rendererFor(themeId: PacMazeMapThemeId): PacMazeMapThemeRenderer = when (themeId) {
        PacMazeMapThemeId.CLASSIC -> ClassicMapThemeRenderer
        PacMazeMapThemeId.CYBERPUNK -> CyberpunkMapThemeRenderer
        PacMazeMapThemeId.GARDEN -> GardenMapThemeRenderer
        PacMazeMapThemeId.FOOD -> FoodMapThemeRenderer
        PacMazeMapThemeId.CHINESE -> ChineseMapThemeRenderer
    }

    /** 主题绘制失败时降级经典渲染，避免整局闪退。 */
    fun renderSafe(
        scope: DrawScope,
        ctx: PacMazeMapRenderContext,
        particles: PacMazeParticleField?,
        themeId: PacMazeMapThemeId,
    ) {
        try {
            rendererFor(themeId).render(scope, ctx, particles)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Theme render failed theme=$themeId, fallback=CLASSIC", e)
            val classicConfig = configFor(PacMazeMapThemeId.CLASSIC)
            ClassicMapThemeRenderer.render(scope, ctx.copy(config = classicConfig), null)
        }
    }

    private fun classic() = PacMazeThemeConfig(
        id = PacMazeMapThemeId.CLASSIC,
        palette = PacMazeThemePalette(
            backgroundTop = Color(0xFF0A1028),
            backgroundBottom = Color(0xFF020408),
            pathFill = Color(0xFF060B18),
            pathGrid = Color(0xFF0D1528),
            wallFill = Color(0xFF1A237E),
            wallEdge = Color(0xFF5C6BC0),
            wallGlow = Color(0xFF7986CB),
            pelletPrimary = Color(0xFFFFF59D),
            pelletGlow = Color(0xFFFFF59D),
            powerCore = Color(0xFFFFAB40),
            powerGlow = Color(0xFFFF6E40),
            tunnelFill = Color(0xFF1E2A45),
            tunnelAccent = Color(0xFF4FC3F7),
            frameAccent = Color(0xFF9575FF),
            ghostColors = listOf(
                Color(0xFFFF5252),
                Color(0xFF40C4FF),
                Color(0xFFFFB74D),
                Color(0xFFE040FB),
            ),
        ),
        particles = PacMazeParticleConfig(enabled = false),
    )

    private fun cyberpunk() = PacMazeThemeConfig(
        id = PacMazeMapThemeId.CYBERPUNK,
        palette = PacMazeThemePalette(
            backgroundTop = Color(0xFF020208),
            backgroundBottom = Color(0xFF000000),
            pathFill = Color(0xFF04060C),
            pathGrid = Color(0xFF0A1020),
            wallFill = Color(0xFF0A1020),
            wallEdge = Color(0xFF00D4FF),
            wallGlow = Color(0xFF0066FF),
            pelletPrimary = Color(0xFFE0F7FA),
            pelletGlow = Color(0xFF00D4FF),
            powerCore = Color(0xFFFFEA00),
            powerGlow = Color(0xFFFF9100),
            tunnelFill = Color(0xFF12082A),
            tunnelAccent = Color(0xFFFF00AA),
            frameAccent = Color(0xFF00D4FF),
            ghostColors = listOf(
                Color(0xFFFF00AA),
                Color(0xFFFF1744),
                Color(0xFF00E5FF),
                Color(0xFFFFEA00),
            ),
        ),
        pelletGlyphs = listOf("<>", "[]", "{}", "01", "10"),
        powerLabel = "◆",
        particles = PacMazeParticleConfig(
            enabled = true,
            count = 72,
            glyphPool = listOf("0", "1"),
            color = Color(0xFF00E5FF),
            alpha = 0.38f,
        ),
    )

    private fun garden() = PacMazeThemeConfig(
        id = PacMazeMapThemeId.GARDEN,
        palette = PacMazeThemePalette(
            backgroundTop = Color(0xFF2A5A45),
            backgroundBottom = Color(0xFF122820),
            pathFill = Color(0xFF1A3828),
            pathGrid = Color(0xFF254A38),
            wallFill = Color(0xFF2E5E3A),
            wallEdge = Color(0xFF7CB342),
            wallGlow = Color(0xFF9CCC65),
            pelletPrimary = Color(0xFFFFF176),
            pelletGlow = Color(0xFFFFD54F),
            powerCore = Color(0xFFFFEB3B),
            powerGlow = Color(0xFFFFC107),
            tunnelFill = Color(0xFF1E4620),
            tunnelAccent = Color(0xFFAED581),
            frameAccent = Color(0xFF8BC34A),
            ghostColors = listOf(
                Color(0xFFE57373),
                Color(0xFF4DB6AC),
                Color(0xFFFFB74D),
                Color(0xFFBA68C8),
            ),
        ),
        pelletGlyphs = listOf("·", "✦"),
        particles = PacMazeParticleConfig(
            enabled = true,
            count = 24,
            glyphPool = listOf("*", "+", "."),
            color = Color(0xFFFFAB91),
            alpha = 0.4f,
            speedMin = 8f,
            speedMax = 22f,
        ),
    )

    private fun food() = PacMazeThemeConfig(
        id = PacMazeMapThemeId.FOOD,
        palette = PacMazeThemePalette(
            backgroundTop = Color(0xFF3E2723),
            backgroundBottom = Color(0xFF1A0F0D),
            pathFill = Color(0xFF2A1815),
            pathGrid = Color(0xFF3D2620),
            wallFill = Color(0xFF5D4037),
            wallEdge = Color(0xFF8D6E63),
            wallGlow = Color(0xFFFFAB91),
            pelletPrimary = Color(0xFFFF80AB),
            pelletGlow = Color(0xFFFF4081),
            powerCore = Color(0xFFFFD740),
            powerGlow = Color(0xFFFF6E40),
            tunnelFill = Color(0xFF4E342E),
            tunnelAccent = Color(0xFFFFCC80),
            frameAccent = Color(0xFFFF7043),
            ghostColors = listOf(
                Color(0xFFEF5350),
                Color(0xFF42A5F5),
                Color(0xFFFFCA28),
                Color(0xFFAB47BC),
            ),
        ),
        pelletGlyphs = listOf("🍬", "🍭", "●"),
        powerLabel = "🍭",
        particles = PacMazeParticleConfig(
            enabled = true,
            count = 18,
            glyphPool = listOf("✦", "·"),
            color = Color(0xFFFFAB91),
            alpha = 0.35f,
            speedMin = 6f,
            speedMax = 14f,
        ),
    )

    private fun chinese() = PacMazeThemeConfig(
        id = PacMazeMapThemeId.CHINESE,
        palette = PacMazeThemePalette(
            backgroundTop = Color(0xFF243048),
            backgroundBottom = Color(0xFF101820),
            pathFill = Color(0xFF182030),
            pathGrid = Color(0xFF283850),
            wallFill = Color(0xFF455A64),
            wallEdge = Color(0xFF78909C),
            wallGlow = Color(0xFFFFD54F),
            pelletPrimary = Color(0xFFFFF59D),
            pelletGlow = Color(0xFFFFD54F),
            powerCore = Color(0xFFFFCA28),
            powerGlow = Color(0xFFFF8F00),
            tunnelFill = Color(0xFF263238),
            tunnelAccent = Color(0xFFFFB300),
            frameAccent = Color(0xFFFFCA28),
            ghostColors = listOf(
                Color(0xFFE53935),
                Color(0xFF5C6BC0),
                Color(0xFFFFB300),
                Color(0xFF8E24AA),
            ),
        ),
        pelletGlyphs = listOf("气", "·"),
        particles = PacMazeParticleConfig(
            enabled = true,
            count = 20,
            glyphPool = listOf("气", "·", "✦"),
            color = Color(0xFFFFD54F),
            alpha = 0.45f,
            speedMin = 6f,
            speedMax = 16f,
        ),
    )
}
