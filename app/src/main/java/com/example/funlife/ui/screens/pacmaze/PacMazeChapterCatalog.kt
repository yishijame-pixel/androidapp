package com.example.funlife.ui.screens.pacmaze

import com.example.funlife.ui.screens.pacmaze.maptheme.PacMazeMapThemeId
import com.example.funlife.ui.screens.pacmaze.maptheme.PacMazeThemeRegistry

/** 单人闯关主题章节（基础 1–13 关 + 极限 14–23 关）。 */
enum class PacMazeCampaignChapter(
    val themeId: PacMazeMapThemeId,
    val tagline: String,
    val tier: Int = 1,
) {
    CYBER(PacMazeMapThemeId.CYBERPUNK, "数据管道 · 霓虹深渊", tier = 1),
    GARDEN(PacMazeMapThemeId.GARDEN, "树篱花圃 · 曲径通幽", tier = 1),
    FOOD(PacMazeMapThemeId.FOOD, "方糖群岛 · 糖浆螺旋", tier = 1),
    CHINESE(PacMazeMapThemeId.CHINESE, "青砖回廊 · 龙门关隘", tier = 1),
    STEAMPUNK(PacMazeMapThemeId.STEAMPUNK, "铜管齿轮 · 活塞机关", tier = 2),
    VHS(PacMazeMapThemeId.VHS, "扫描线 · 404 乱码", tier = 2),
    ORBITAL(PacMazeMapThemeId.ORBITAL, "舱段编号 · 气闸矩阵", tier = 2),
    MAGMA(PacMazeMapThemeId.MAGMA, "黑曜石道 · 熔岩脉冲", tier = 2),
    SUBMARINE(PacMazeMapThemeId.SUBMARINE, "声呐网格 · 水密门", tier = 2),
    FROST(PacMazeMapThemeId.FROST, "霜花墙 · 冷凝走廊", tier = 2),
    ARCHIVE(PacMazeMapThemeId.ARCHIVE, "书架索引 · 符卷封印", tier = 2),
    METRO(PacMazeMapThemeId.METRO, "站牌区间 · 闸机蹲守", tier = 2),
    OPERA(PacMazeMapThemeId.OPERA, "幕布场次 · 双影包抄", tier = 2),
    GREENHOUSE(PacMazeMapThemeId.GREENHOUSE, "玻璃格架 · 补给爆发", tier = 2),
    ;

    val displayName: String get() = themeId.displayName

    companion object {
        val foundationChapters: List<PacMazeCampaignChapter> = entries.filter { it.tier == 1 }
        val extremeChapters: List<PacMazeCampaignChapter> = entries.filter { it.tier == 2 }
    }
}

object PacMazeChapterCatalog {
    val chapters: List<PacMazeCampaignChapter> = PacMazeCampaignChapter.entries

    fun levelsInChapter(chapter: PacMazeCampaignChapter): List<Int> =
        (1..PacMazeLevelCatalog.TOTAL_LEVELS).filter {
            PacMazeThemeRegistry.themeForLevel(it) == chapter.themeId
        }

    fun chapterForLevel(levelId: Int): PacMazeCampaignChapter =
        chapters.first { it.themeId == PacMazeThemeRegistry.themeForLevel(levelId) }

    fun chapterProgress(
        chapter: PacMazeCampaignChapter,
        maxLevelReached: Int,
        starsBitmask: Int,
    ): ChapterProgress {
        val levels = levelsInChapter(chapter)
        val cleared = levels.count { it <= maxLevelReached }
        val stars = levels.filter { it <= maxLevelReached }.sumOf { decodePacMazeStars(starsBitmask, it) }
        val current = levels.firstOrNull { it >= maxLevelReached.coerceIn(1, PacMazeLevelCatalog.TOTAL_LEVELS) }
            ?: levels.lastOrNull()
        return ChapterProgress(
            chapter = chapter,
            levelIds = levels,
            clearedCount = cleared,
            totalCount = levels.size,
            totalStars = stars,
            maxPossibleStars = levels.size * 3,
            currentLevelId = current,
        )
    }
}

data class ChapterProgress(
    val chapter: PacMazeCampaignChapter,
    val levelIds: List<Int>,
    val clearedCount: Int,
    val totalCount: Int,
    val totalStars: Int,
    val maxPossibleStars: Int,
    val currentLevelId: Int?,
)
