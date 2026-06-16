package com.example.funlife.data.model

/** 迷宫模式本地战绩（按难度 + 每日挑战）。 */
data class PacMazeMazeStats(
    val dailyDate: String = "",
    val dailyBestTimeMs: Long = 0,
    val dailyBestStars: Int = 0,
    val bestTimeByDifficulty: Map<String, Long> = emptyMap(),
    val bestStarsByDifficulty: Map<String, Int> = emptyMap(),
    val lastDifficultyId: String = "standard",
    val lastContractId: String = "none",
    val useDailyChallenge: Boolean = true,
)
