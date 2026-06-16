package com.example.funlife.ui.screens.pacmaze

import com.example.funlife.social.game.engine.pacmaze.GhostKind
import com.example.funlife.social.game.engine.pacmaze.PacMazeGhostRoster
import com.example.funlife.social.game.engine.pacmaze.PacMazeGhostSpawnDef
import com.example.funlife.social.game.engine.pacmaze.PacMazeLevelThemeAssignment

/** 关卡 Hub 用：展示本关幽灵阵容（无需加载 JSON）。 */
object PacMazeLevelGhostsUi {

    fun ghostCountForLevel(levelId: Int): Int = when {
        levelId <= 5 -> 3
        levelId <= 10 -> 4
        levelId <= 15 -> 5
        else -> 6
    }

    fun rosterForLevel(levelId: Int, ghostCount: Int = ghostCountForLevel(levelId)): List<PacMazeGhostSpawnDef> {
        val positions = List(ghostCount.coerceAtLeast(1)) { 0 to 0 }
        return PacMazeGhostRoster.resolveSpawns(
            levelId = levelId,
            positions = positions,
            hasDynamicTiles = levelId >= 14,
            hasEnergyGates = levelId >= 16,
            themeKey = PacMazeLevelThemeAssignment.forLevel(levelId),
        )
    }

    fun featuredKindsLabel(levelId: Int): String {
        if (levelId < 14) return ""
        val roster = rosterForLevel(levelId)
        return roster.map { it.kind.displayName }.distinct().take(4).joinToString(" · ")
    }

    fun label(kind: GhostKind, specialty: com.example.funlife.social.game.engine.pacmaze.GhostSpecialty): String =
        buildString {
            append(kind.displayName)
            if (specialty.isActive) append(" · ${specialty.emoji}${specialty.displayName}")
        }
}
