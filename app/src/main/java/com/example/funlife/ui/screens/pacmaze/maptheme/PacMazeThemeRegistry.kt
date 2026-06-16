package com.example.funlife.ui.screens.pacmaze.maptheme

import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.funlife.social.game.engine.pacmaze.PacMazeLevelThemeAssignment
import com.example.funlife.social.game.engine.pacmaze.PacMazeLevelThemeKey
import com.example.funlife.social.game.engine.pacmaze.PacMazeRunMode

object PacMazeThemeRegistry {

    private const val TAG = "PacMazeThemeRegistry"

    fun themeForLevel(levelId: Int): PacMazeMapThemeId =
        fromThemeKey(PacMazeLevelThemeAssignment.forLevel(levelId))

    fun themeForRun(runMode: PacMazeRunMode, themeLevelId: Int): PacMazeMapThemeId =
        fromThemeKey(PacMazeLevelThemeAssignment.forRun(runMode, themeLevelId))

    private fun fromThemeKey(key: PacMazeLevelThemeKey): PacMazeMapThemeId =
        PacMazeMapThemeId.fromId(key.id) ?: PacMazeMapThemeId.CYBERPUNK

    fun configFor(themeId: PacMazeMapThemeId): PacMazeThemeConfig = when (themeId) {
        PacMazeMapThemeId.CLASSIC -> classic()
        PacMazeMapThemeId.CYBERPUNK -> cyberpunk()
        PacMazeMapThemeId.GARDEN -> garden()
        PacMazeMapThemeId.FOOD -> food()
        PacMazeMapThemeId.CHINESE -> chinese()
        PacMazeMapThemeId.ENDLESS -> endless()
        PacMazeMapThemeId.MAZE -> maze()
        PacMazeMapThemeId.STEAMPUNK -> steampunk()
        PacMazeMapThemeId.VHS -> vhs()
        PacMazeMapThemeId.ORBITAL -> orbital()
        PacMazeMapThemeId.MAGMA -> magma()
        PacMazeMapThemeId.SUBMARINE -> submarine()
        PacMazeMapThemeId.FROST -> frost()
        PacMazeMapThemeId.ARCHIVE -> archive()
        PacMazeMapThemeId.METRO -> metro()
        PacMazeMapThemeId.OPERA -> opera()
        PacMazeMapThemeId.GREENHOUSE -> greenhouse()
        PacMazeMapThemeId.CHRONO -> chrono()
        PacMazeMapThemeId.MIRROR -> mirror()
    }

    fun rendererFor(themeId: PacMazeMapThemeId): PacMazeMapThemeRenderer = when (themeId) {
        PacMazeMapThemeId.CLASSIC -> ClassicMapThemeRenderer
        PacMazeMapThemeId.CYBERPUNK -> CyberpunkMapThemeRenderer
        PacMazeMapThemeId.GARDEN -> GardenMapThemeRenderer
        PacMazeMapThemeId.FOOD -> FoodMapThemeRenderer
        PacMazeMapThemeId.CHINESE -> ChineseMapThemeRenderer
        PacMazeMapThemeId.ENDLESS -> EndlessMapThemeRenderer
        PacMazeMapThemeId.MAZE -> MazeMapThemeRenderer
        PacMazeMapThemeId.STEAMPUNK, PacMazeMapThemeId.VHS, PacMazeMapThemeId.ORBITAL,
        PacMazeMapThemeId.MAGMA, PacMazeMapThemeId.SUBMARINE, PacMazeMapThemeId.FROST,
        PacMazeMapThemeId.ARCHIVE, PacMazeMapThemeId.METRO, PacMazeMapThemeId.OPERA,
        PacMazeMapThemeId.GREENHOUSE,
        -> ExtendedMapThemeRenderer
        PacMazeMapThemeId.CHRONO -> ChronoMapThemeRenderer
        PacMazeMapThemeId.MIRROR -> MirrorMapThemeRenderer
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

    private fun endless() = PacMazeThemeConfig(
        id = PacMazeMapThemeId.ENDLESS,
        palette = PacMazeThemePalette(
            backgroundTop = Color(0xFF120028),
            backgroundBottom = Color(0xFF000008),
            pathFill = Color(0xFF0A0618),
            pathGrid = Color(0xFF160A28),
            wallFill = Color(0xFF120820),
            wallEdge = Color(0xFF7C4DFF),
            wallGlow = Color(0xFFE040FB),
            pelletPrimary = Color(0xFFE1BEE7),
            pelletGlow = Color(0xFFB388FF),
            powerCore = Color(0xFFFFEA00),
            powerGlow = Color(0xFFFF9100),
            tunnelFill = Color(0xFF1A0830),
            tunnelAccent = Color(0xFFEA80FC),
            frameAccent = Color(0xFF7C4DFF),
            ghostColors = listOf(
                Color(0xFFEA80FC),
                Color(0xFFFF4081),
                Color(0xFF40C4FF),
                Color(0xFFFFD740),
            ),
        ),
        pelletGlyphs = listOf("∞", "·", "◆"),
        powerLabel = "∞",
        particles = PacMazeParticleConfig(
            enabled = true,
            count = 48,
            glyphPool = listOf("∞", "·"),
            color = Color(0xFFB388FF),
            alpha = 0.42f,
            speedMin = 10f,
            speedMax = 28f,
        ),
    )

    private fun maze() = PacMazeThemeConfig(
        id = PacMazeMapThemeId.MAZE,
        palette = PacMazeThemePalette(
            backgroundTop = Color(0xFF1C1A24),
            backgroundBottom = Color(0xFF0A0810),
            pathFill = Color(0xFF14121A),
            pathGrid = Color(0xFF1E1C26),
            wallFill = Color(0xFF2A2832),
            wallEdge = Color(0xFF5C6578),
            wallGlow = Color(0xFF78909C),
            pelletPrimary = Color(0xFFCFD8DC),
            pelletGlow = Color(0xFF90A4AE),
            powerCore = Color(0xFFFFB74D),
            powerGlow = Color(0xFFFF9800),
            tunnelFill = Color(0xFF252330),
            tunnelAccent = Color(0xFFFFB74D),
            frameAccent = Color(0xFFFFB74D),
            ghostColors = listOf(
                Color(0xFF78909C),
                Color(0xFF5C6BC0),
                Color(0xFFFFB74D),
                Color(0xFF8D6E63),
            ),
        ),
        pelletGlyphs = listOf("·"),
        particles = PacMazeParticleConfig(
            enabled = true,
            count = 16,
            glyphPool = listOf("·", "░"),
            color = Color(0xFF90A4AE),
            alpha = 0.28f,
            speedMin = 4f,
            speedMax = 10f,
        ),
    )

    private fun extremeConfig(
        id: PacMazeMapThemeId,
        top: Long, bottom: Long, path: Long, grid: Long,
        wall: Long, edge: Long, glow: Long,
        pellet: Long, pelletGlow: Long, power: Long, powerGlow: Long,
        tunnel: Long, tunnelAcc: Long, frame: Long,
        glyphs: List<String>,
        particles: PacMazeParticleConfig,
        powerLabel: String = "◆",
        ghostColors: List<Color> = listOf(
            Color(0xFFFF5252), Color(0xFF40C4FF), Color(0xFFFFB74D), Color(0xFFB388FF),
        ),
    ) = PacMazeThemeConfig(
        id = id,
        palette = PacMazeThemePalette(
            backgroundTop = Color(top),
            backgroundBottom = Color(bottom),
            pathFill = Color(path),
            pathGrid = Color(grid),
            wallFill = Color(wall),
            wallEdge = Color(edge),
            wallGlow = Color(glow),
            pelletPrimary = Color(pellet),
            pelletGlow = Color(pelletGlow),
            powerCore = Color(power),
            powerGlow = Color(powerGlow),
            tunnelFill = Color(tunnel),
            tunnelAccent = Color(tunnelAcc),
            frameAccent = Color(frame),
            ghostColors = ghostColors,
        ),
        pelletGlyphs = glyphs,
        powerLabel = powerLabel,
        particles = particles,
    )

    private fun steampunk() = extremeConfig(
        id = PacMazeMapThemeId.STEAMPUNK,
        top = 0xFF4E342E, bottom = 0xFF1A0F0A, path = 0xFF2A1A12, grid = 0xFF3D2818,
        wall = 0xFF5D4037, edge = 0xFFFF8F00, glow = 0xFFFFB74D,
        pellet = 0xFFFFF176, pelletGlow = 0xFFFFCC80, power = 0xFFFF6F00, powerGlow = 0xFFFF3D00,
        tunnel = 0xFF3E2723, tunnelAcc = 0xFFFFAB40, frame = 0xFFFF8F00,
        glyphs = listOf("⚙", "·", "◆"),
        particles = PacMazeParticleConfig(enabled = true, count = 22, glyphPool = listOf("~", "·"),
            color = Color(0xFFFFCC80), alpha = 0.35f, speedMin = 6f, speedMax = 16f),
        powerLabel = "⚙",
    )

    private fun vhs() = extremeConfig(
        id = PacMazeMapThemeId.VHS,
        top = 0xFF1A1A2E, bottom = 0xFF050508, path = 0xFF12121A, grid = 0xFF1E1E28,
        wall = 0xFF2A2A38, edge = 0xFF00E5FF, glow = 0xFFFF1744,
        pellet = 0xFFE0F7FA, pelletGlow = 0xFF18FFFF, power = 0xFFFFEA00, powerGlow = 0xFFFF1744,
        tunnel = 0xFF1A1030, tunnelAcc = 0xFF00E676, frame = 0xFF18FFFF,
        glyphs = listOf("▒", "░", "404"),
        particles = PacMazeParticleConfig(enabled = true, count = 36, glyphPool = listOf("▒", "░"),
            color = Color(0xFF18FFFF), alpha = 0.3f, speedMin = 12f, speedMax = 30f),
    )

    private fun orbital() = extremeConfig(
        id = PacMazeMapThemeId.ORBITAL,
        top = 0xFF0D1B2A, bottom = 0xFF000000, path = 0xFF101820, grid = 0xFF1A2430,
        wall = 0xFF263238, edge = 0xFF90CAF9, glow = 0xFF42A5F5,
        pellet = 0xFFE3F2FD, pelletGlow = 0xFF64B5F6, power = 0xFFFFD54F, powerGlow = 0xFFFFB300,
        tunnel = 0xFF152030, tunnelAcc = 0xFF82B1FF, frame = 0xFF90CAF9,
        glyphs = listOf("·", "✦", "◉"),
        particles = PacMazeParticleConfig(enabled = true, count = 48, glyphPool = listOf("·", "+"),
            color = Color(0xFFBBDEFB), alpha = 0.4f, speedMin = 4f, speedMax = 12f),
    )

    private fun magma() = extremeConfig(
        id = PacMazeMapThemeId.MAGMA,
        top = 0xFF3E2723, bottom = 0xFF0A0000, path = 0xFF1A0A08, grid = 0xFF2A120E,
        wall = 0xFF212121, edge = 0xFFFF5722, glow = 0xFFFF7043,
        pellet = 0xFFFFCCBC, pelletGlow = 0xFFFF5722, power = 0xFFFFD740, powerGlow = 0xFFFF6F00,
        tunnel = 0xFF2A1008, tunnelAcc = 0xFFFFAB40, frame = 0xFFFF5722,
        glyphs = listOf("●", "◆", "·"),
        particles = PacMazeParticleConfig(enabled = true, count = 28, glyphPool = listOf("·", "*"),
            color = Color(0xFFFF7043), alpha = 0.45f, speedMin = 8f, speedMax = 22f),
    )

    private fun submarine() = extremeConfig(
        id = PacMazeMapThemeId.SUBMARINE,
        top = 0xFF01579B, bottom = 0xFF002033, path = 0xFF01365A, grid = 0xFF014F7A,
        wall = 0xFF0277BD, edge = 0xFF4FC3F7, glow = 0xFF0288D1,
        pellet = 0xFFB3E5FC, pelletGlow = 0xFF4DD0E1, power = 0xFFFFEB3B, powerGlow = 0xFFFFC107,
        tunnel = 0xFF004D73, tunnelAcc = 0xFF80DEEA, frame = 0xFF4FC3F7,
        glyphs = listOf("○", "·", "~"),
        particles = PacMazeParticleConfig(enabled = true, count = 32, glyphPool = listOf("○", "·"),
            color = Color(0xFF80DEEA), alpha = 0.38f, speedMin = 6f, speedMax = 18f),
    )

    private fun frost() = extremeConfig(
        id = PacMazeMapThemeId.FROST,
        top = 0xFF37474F, bottom = 0xFF102027, path = 0xFF1C313A, grid = 0xFF263238,
        wall = 0xFF455A64, edge = 0xFFB3E5FC, glow = 0xFF81D4FA,
        pellet = 0xFFE1F5FE, pelletGlow = 0xFF4FC3F7, power = 0xFFFFF176, powerGlow = 0xFFFFD54F,
        tunnel = 0xFF1A2830, tunnelAcc = 0xFF80D8FF, frame = 0xFFB3E5FC,
        glyphs = listOf("❄", "·", "✦"),
        particles = PacMazeParticleConfig(enabled = true, count = 26, glyphPool = listOf("*", "·"),
            color = Color(0xFFB3E5FC), alpha = 0.42f, speedMin = 4f, speedMax = 14f),
    )

    private fun archive() = extremeConfig(
        id = PacMazeMapThemeId.ARCHIVE,
        top = 0xFF4E342E, bottom = 0xFF1A120B, path = 0xFF2A1F18, grid = 0xFF3D2E24,
        wall = 0xFF5D4037, edge = 0xFFD7CCC8, glow = 0xFF8D6E63,
        pellet = 0xFFFFF59D, pelletGlow = 0xFFFFD54F, power = 0xFFFFCA28, powerGlow = 0xFFFF8F00,
        tunnel = 0xFF332018, tunnelAcc = 0xFFFFCC80, frame = 0xFFD7CCC8,
        glyphs = listOf("符", "·", "卷"),
        particles = PacMazeParticleConfig(enabled = true, count = 18, glyphPool = listOf("·", "✦"),
            color = Color(0xFFFFCC80), alpha = 0.32f, speedMin = 4f, speedMax = 10f),
    )

    private fun metro() = extremeConfig(
        id = PacMazeMapThemeId.METRO,
        top = 0xFF263238, bottom = 0xFF101820, path = 0xFF1C2428, grid = 0xFF283038,
        wall = 0xFF37474F, edge = 0xFFFFEB3B, glow = 0xFFFFC107,
        pellet = 0xFFECEFF1, pelletGlow = 0xFF90A4AE, power = 0xFFFF5252, powerGlow = 0xFFFF1744,
        tunnel = 0xFF202830, tunnelAcc = 0xFFFFEB3B, frame = 0xFFFFEB3B,
        glyphs = listOf("M", "·", "●"),
        particles = PacMazeParticleConfig(enabled = true, count = 14, glyphPool = listOf("·"),
            color = Color(0xFFCFD8DC), alpha = 0.25f, speedMin = 8f, speedMax = 20f),
    )

    private fun opera() = extremeConfig(
        id = PacMazeMapThemeId.OPERA,
        top = 0xFF4A148C, bottom = 0xFF120820, path = 0xFF2A1040, grid = 0xFF381858,
        wall = 0xFF6A1B9A, edge = 0xFFFFD54F, glow = 0xFFE040FB,
        pellet = 0xFFF8BBD0, pelletGlow = 0xFFF06292, power = 0xFFFFD740, powerGlow = 0xFFFF6F00,
        tunnel = 0xFF301050, tunnelAcc = 0xFFFF4081, frame = 0xFFFFD54F,
        glyphs = listOf("戏", "·", "✦"),
        particles = PacMazeParticleConfig(enabled = true, count = 20, glyphPool = listOf("✦", "·"),
            color = Color(0xFFEA80FC), alpha = 0.38f, speedMin = 5f, speedMax = 14f),
    )

    private fun greenhouse() = extremeConfig(
        id = PacMazeMapThemeId.GREENHOUSE,
        top = 0xFF33691E, bottom = 0xFF1B2E0F, path = 0xFF254015, grid = 0xFF2E5018,
        wall = 0xFF558B2F, edge = 0xFFA5D6A7, glow = 0xFF81C784,
        pellet = 0xFFDCEDC8, pelletGlow = 0xFF9CCC65, power = 0xFFFFF176, powerGlow = 0xFFFFD54F,
        tunnel = 0xFF203810, tunnelAcc = 0xFFCCFF90, frame = 0xFF8BC34A,
        glyphs = listOf("✿", "·", "◆"),
        particles = PacMazeParticleConfig(enabled = true, count = 24, glyphPool = listOf("·", "+"),
            color = Color(0xFFC5E1A5), alpha = 0.35f, speedMin = 4f, speedMax = 12f),
    )

    private fun chrono() = extremeConfig(
        id = PacMazeMapThemeId.CHRONO,
        top = 0xFF283593, bottom = 0xFF0A0820, path = 0xFF151040, grid = 0xFF1E1850,
        wall = 0xFF3949AB, edge = 0xFFFFD54F, glow = 0xFF7986CB,
        pellet = 0xFFE8EAF6, pelletGlow = 0xFF9FA8DA, power = 0xFFFFEB3B, powerGlow = 0xFFFFC107,
        tunnel = 0xFF181030, tunnelAcc = 0xFFFFD54F, frame = 0xFFFFD54F,
        glyphs = listOf("⏱", "·", "◉"),
        particles = PacMazeParticleConfig(enabled = true, count = 30, glyphPool = listOf("·", "◉"),
            color = Color(0xFFFFD54F), alpha = 0.4f, speedMin = 6f, speedMax = 18f),
    )

    private fun mirror() = extremeConfig(
        id = PacMazeMapThemeId.MIRROR,
        top = 0xFF1A237E, bottom = 0xFF0D0221, path = 0xFF151040, grid = 0xFF1E1858,
        wall = 0xFF311B92, edge = 0xFF7C4DFF, glow = 0xFFB388FF,
        pellet = 0xFFD1C4E9, pelletGlow = 0xFF9575CD, power = 0xFFFFEB3B, powerGlow = 0xFFFF9100,
        tunnel = 0xFF120830, tunnelAcc = 0xFFEA80FC, frame = 0xFF7C4DFF,
        glyphs = listOf("⟷", "·", "◆"),
        particles = PacMazeParticleConfig(enabled = true, count = 20, glyphPool = listOf("·", "⟷"),
            color = Color(0xFFB388FF), alpha = 0.35f, speedMin = 4f, speedMax = 12f),
    )
}
