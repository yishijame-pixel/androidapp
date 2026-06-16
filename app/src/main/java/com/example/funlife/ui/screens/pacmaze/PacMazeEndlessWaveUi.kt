package com.example.funlife.ui.screens.pacmaze

import com.example.funlife.social.game.engine.pacmaze.PacMazeLevelProgression

enum class PacMazeEndlessSegment {
    /** W1–7 基础 chunk */
    CHUNK,
    /** W8+ 未解锁熔炉时的循环预热 */
    PREHEAT,
    /** W8+ 已解锁：循环 L14–L23 极限关 */
    MOLTEN,
}

data class PacMazeEndlessWaveInfo(
    val wave: Int,
    val segment: PacMazeEndlessSegment,
    val moltenLevelId: Int? = null,
) {
    val levelMeta: PacMazeLevelMeta? get() = moltenLevelId?.let { PacMazeLevelCatalog.find(it) }
}

object PacMazeEndlessWaveUi {
    const val MOLTEN_WAVE_START = 8
    private const val MOLTEN_LEVEL_START = 14
    private const val MOLTEN_LEVEL_COUNT = 10

    fun resolve(wave: Int, maxLevelReached: Int): PacMazeEndlessWaveInfo {
        val w = wave.coerceAtLeast(1)
        val moltenUnlocked = maxLevelReached >= PacMazeLevelProgression.TOTAL_LEVELS
        return when {
            w >= MOLTEN_WAVE_START && moltenUnlocked -> {
                val levelId = MOLTEN_LEVEL_START + ((w - MOLTEN_WAVE_START) % MOLTEN_LEVEL_COUNT)
                PacMazeEndlessWaveInfo(w, PacMazeEndlessSegment.MOLTEN, levelId)
            }
            w >= MOLTEN_WAVE_START -> PacMazeEndlessWaveInfo(w, PacMazeEndlessSegment.PREHEAT)
            else -> PacMazeEndlessWaveInfo(w, PacMazeEndlessSegment.CHUNK)
        }
    }

    fun badgeLabel(info: PacMazeEndlessWaveInfo): String = when (info.segment) {
        PacMazeEndlessSegment.MOLTEN -> "🔥W${info.wave}"
        PacMazeEndlessSegment.PREHEAT -> "W${info.wave}·预"
        PacMazeEndlessSegment.CHUNK -> "W${info.wave}"
    }

    fun hintText(info: PacMazeEndlessWaveInfo): String = when (info.segment) {
        PacMazeEndlessSegment.MOLTEN -> {
            val tag = levelTag(info)
            "🔥 熔炉 · W${info.wave} · $tag · 清空进下一波"
        }
        PacMazeEndlessSegment.PREHEAT ->
            "W${info.wave} 预热 · 通关 L${PacMazeLevelProgression.TOTAL_LEVELS} 解锁熔炉"
        PacMazeEndlessSegment.CHUNK ->
            "第 ${info.wave} 波 · 清空地图进入下一波"
    }

    fun bannerTitle(info: PacMazeEndlessWaveInfo): String = when (info.segment) {
        PacMazeEndlessSegment.MOLTEN -> "🔥 熔炉无尽"
        PacMazeEndlessSegment.PREHEAT -> "无尽预热"
        PacMazeEndlessSegment.CHUNK -> "第 ${info.wave} 波"
    }

    fun bannerSubtitle(info: PacMazeEndlessWaveInfo): String = when (info.segment) {
        PacMazeEndlessSegment.MOLTEN -> "W${info.wave} · ${levelTag(info)}"
        PacMazeEndlessSegment.PREHEAT ->
            "W${info.wave} · 通关 L${PacMazeLevelProgression.TOTAL_LEVELS} 解锁熔炉"
        PacMazeEndlessSegment.CHUNK -> "清空地图进入下一波"
    }

    fun resultMessage(info: PacMazeEndlessWaveInfo, score: Int): String = when (info.segment) {
        PacMazeEndlessSegment.MOLTEN -> "🔥 熔炉 W${info.wave} · ${levelTag(info)} · 得分 $score"
        PacMazeEndlessSegment.PREHEAT -> "W${info.wave} 预热 · 得分 $score"
        PacMazeEndlessSegment.CHUNK -> "第 ${info.wave} 波 · 得分 $score"
    }

    private fun levelTag(info: PacMazeEndlessWaveInfo): String {
        val meta = info.levelMeta
        return meta?.let { "L${it.id} ${it.name}" } ?: "L${info.moltenLevelId}"
    }
}
