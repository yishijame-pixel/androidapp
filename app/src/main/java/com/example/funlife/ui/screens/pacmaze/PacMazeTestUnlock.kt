package com.example.funlife.ui.screens.pacmaze

import com.example.funlife.BuildConfig
import com.example.funlife.social.game.engine.pacmaze.GhostKind

/** Debug / QA：全解锁关卡与图鉴，便于测试。 */
object PacMazeTestUnlock {
    /** 设为 true 可在 Release 包测试；发版前改回 false。 */
    private const val FORCE_QA_UNLOCK = true

    val enabled: Boolean = FORCE_QA_UNLOCK || BuildConfig.DEBUG

    fun effectiveMaxLevelReached(actual: Int, totalLevels: Int = PacMazeLevelCatalog.TOTAL_LEVELS): Int =
        if (enabled) totalLevels else actual.coerceIn(1, totalLevels)

    fun isLevelUnlocked(levelId: Int, maxLevelReached: Int): Boolean =
        enabled || levelId <= maxLevelReached

    fun ghostCodexMask(actualMask: Int): Int =
        if (enabled) GhostKind.entries.fold(0) { acc, kind -> acc or kind.codexBit } else actualMask

    fun isGhostCodexUnlocked(kind: GhostKind, mask: Int): Boolean =
        enabled || (mask and kind.codexBit) != 0
}
