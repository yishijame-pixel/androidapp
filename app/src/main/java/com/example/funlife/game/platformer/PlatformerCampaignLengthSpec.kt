package com.example.funlife.game.platformer

/**
 * 企业级主线长度 KPI：从目标游玩时长反推格数预算。
 *
 * 有效前进速度取 ~2 格/秒（含跳跃、战斗、探索，非满速 260px/s）。
 */
object PlatformerCampaignLengthSpec {

    const val EFFECTIVE_TILES_PER_SECOND = 2.0f
    const val SEGMENT_W = PlatformerSegmentLibrary.SEGMENT_W
    const val SEGMENT_TAIL_PAD = 8
    const val SCROLL_BUFFER_THRESHOLD_SEGMENTS = 15
    const val INITIAL_BUFFER_SEGMENTS = 8
    const val CHECKPOINT_EVERY_SEGMENTS = 8

    /** 全主线目标：~8h 首通 → ~5.5 万逻辑格。 */
    const val CAMPAIGN_TARGET_TOTAL_TILES = 55_000

    enum class Chapter(
        val ids: IntRange,
        val segmentCount: Int,
        val targetMinutesMin: Float,
        val targetMinutesMax: Float,
    ) {
        CLASSIC(1..6, 12, 3f, 5f),
        PACK(7..16, 22, 5f, 7f),
        STORY(17..22, 29, 6f, 8f),
        EPIC(23..34, 43, 8f, 12f),
        TIER(35..52, 54, 10f, 15f),
        ;

        val targetTiles: Int
            get() = segmentCount * SEGMENT_W + SEGMENT_TAIL_PAD

        val targetTilesMin: Int
            get() = (targetMinutesMin * 60 * EFFECTIVE_TILES_PER_SECOND).toInt()

        val targetTilesMax: Int
            get() = (targetMinutesMax * 60 * EFFECTIVE_TILES_PER_SECOND).toInt()
    }

    data class LevelBudget(
        val levelId: Int,
        val chapter: Chapter,
        val segmentCount: Int,
        val targetTiles: Int,
        val targetMinutesMin: Float,
        val targetMinutesMax: Float,
        val useScrollBuffer: Boolean,
        val checkpointEverySegments: Int,
    ) {
        val estimatedMinutesAtPace: Float
            get() = targetTiles / (EFFECTIVE_TILES_PER_SECOND * 60f)
    }

    fun chapterOf(levelId: Int): Chapter = Chapter.entries.first { levelId in it.ids }

    fun budget(levelId: Int): LevelBudget {
        val chapter = chapterOf(levelId)
        val segments = chapter.segmentCount
        return LevelBudget(
            levelId = levelId,
            chapter = chapter,
            segmentCount = segments,
            targetTiles = segments * SEGMENT_W + SEGMENT_TAIL_PAD,
            targetMinutesMin = chapter.targetMinutesMin,
            targetMinutesMax = chapter.targetMinutesMax,
            useScrollBuffer = segments >= SCROLL_BUFFER_THRESHOLD_SEGMENTS,
            checkpointEverySegments = if (segments >= SCROLL_BUFFER_THRESHOLD_SEGMENTS) {
                CHECKPOINT_EVERY_SEGMENTS
            } else {
                0
            },
        )
    }

    fun allBudgets(): List<LevelBudget> =
        (1..PLATFORMER_CAMPAIGN_LEVEL_COUNT).map(::budget)

    fun totalCampaignTiles(): Int = allBudgets().sumOf { it.targetTiles }
}
