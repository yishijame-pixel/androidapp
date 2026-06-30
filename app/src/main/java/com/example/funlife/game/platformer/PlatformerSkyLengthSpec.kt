package com.example.funlife.game.platformer

/** 高空章节 10 关长度预算（ID 53–62）。 */
object PlatformerSkyLengthSpec {

    const val SKY_LEVEL_START = 53
    const val SKY_LEVEL_COUNT = 10
    const val SKY_LEVEL_END = SKY_LEVEL_START + SKY_LEVEL_COUNT - 1
    const val SEGMENT_TAIL_PAD = PlatformerCampaignLengthSpec.SEGMENT_TAIL_PAD
    const val SCROLL_THRESHOLD_SEGMENTS = PlatformerCampaignLengthSpec.SCROLL_BUFFER_THRESHOLD_SEGMENTS

    data class SkyBudget(
        val levelId: Int,
        val segmentCount: Int,
        val targetTiles: Int,
        val targetMinutesMin: Float,
        val targetMinutesMax: Float,
        val useScrollBuffer: Boolean,
        val checkpointEverySegments: Int,
    ) {
        val estimatedMinutesAtPace: Float
            get() = targetTiles / (PlatformerCampaignLengthSpec.EFFECTIVE_TILES_PER_SECOND * 60f)
    }

    fun isSkyLevel(levelId: Int): Boolean = levelId in SKY_LEVEL_START..SKY_LEVEL_END

    fun budget(levelId: Int): SkyBudget {
        require(isSkyLevel(levelId)) { "Not a sky level: $levelId" }
        val segments = segmentCount(levelId)
        val useScroll = segments >= SCROLL_THRESHOLD_SEGMENTS
        val minutes = targetMinutes(levelId)
        return SkyBudget(
            levelId = levelId,
            segmentCount = segments,
            targetTiles = segments * PlatformerSegmentLibrary.SEGMENT_W + SEGMENT_TAIL_PAD,
            targetMinutesMin = minutes.first,
            targetMinutesMax = minutes.second,
            useScrollBuffer = useScroll,
            checkpointEverySegments = if (useScroll) {
                PlatformerCampaignLengthSpec.CHECKPOINT_EVERY_SEGMENTS
            } else {
                0
            },
        )
    }

    private fun segmentCount(levelId: Int): Int = when (levelId) {
        53 -> 12
        54 -> 13
        55 -> 14
        56 -> 14
        57 -> 15
        58 -> 16
        59 -> 16
        60 -> 18
        61 -> 18
        62 -> 20
        else -> 14
    }

    private fun targetMinutes(levelId: Int): Pair<Float, Float> = when (levelId) {
        53 -> 2.5f to 3.5f
        54 -> 2.8f to 3.8f
        55, 56 -> 3f to 4.5f
        57 -> 3.2f to 4.5f
        58, 59 -> 3.5f to 5.5f
        60, 61 -> 4f to 6.5f
        62 -> 4.5f to 7.5f
        else -> 3f to 5f
    }

    fun totalSkyTiles(): Int =
        (SKY_LEVEL_START..SKY_LEVEL_END).sumOf { budget(it).targetTiles }
}
