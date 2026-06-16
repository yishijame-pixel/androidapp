package com.example.funlife.repository

import com.example.funlife.data.dao.PacMazeProgressDao
import com.example.funlife.data.model.PacMazeProgress
import com.example.funlife.ui.screens.pacmaze.PacMazeLevelCatalog
import com.example.funlife.ui.screens.pacmaze.PacMazeStars
import com.example.funlife.ui.screens.pacmaze.PacMazeTestUnlock
import kotlinx.coroutines.flow.Flow

class PacMazeProgressRepository(
    private val dao: PacMazeProgressDao,
) {
    fun observeProgress(userId: Long): Flow<PacMazeProgress?> = dao.observeByUserId(userId)

    suspend fun getProgress(userId: Long): PacMazeProgress? = dao.getByUserId(userId)

    suspend fun ensureProgress(userId: Long): PacMazeProgress {
        val existing = dao.getByUserId(userId)
        if (existing != null) return existing
        val created = PacMazeProgress(userId = userId)
        dao.upsert(created)
        return created
    }

    suspend fun saveLevelResult(
        userId: Long,
        levelId: Int,
        score: Int,
        stars: Int,
    ) {
        val current = ensureProgress(userId)
        val newMaxLevel = maxOf(current.maxLevelReached, levelId + 1)
        val newStars = PacMazeStars.merge(current.starsBitmask, levelId, stars)
        dao.upsert(
            current.copy(
                maxLevelReached = newMaxLevel.coerceAtMost(PacMazeLevelCatalog.TOTAL_LEVELS),
                highScore = maxOf(current.highScore, score),
                starsBitmask = newStars,
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun saveEndlessResult(
        userId: Long,
        score: Int,
        wave: Int,
    ) {
        val current = ensureProgress(userId)
        dao.upsert(
            current.copy(
                endlessBestScore = maxOf(current.endlessBestScore, score),
                endlessBestWave = maxOf(current.endlessBestWave, wave),
                highScore = maxOf(current.highScore, score),
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun saveMazeResult(
        userId: Long,
        elapsedMs: Long,
        score: Int,
    ) {
        val current = ensureProgress(userId)
        val bestTime = when {
            current.mazeBestTimeMs <= 0L -> elapsedMs
            else -> minOf(current.mazeBestTimeMs, elapsedMs)
        }
        dao.upsert(
            current.copy(
                mazeBestTimeMs = bestTime,
                highScore = maxOf(current.highScore, score),
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun unlockAllLevelsForTesting(userId: Long) {
        if (!PacMazeTestUnlock.enabled) return
        val current = ensureProgress(userId)
        dao.upsert(
            current.copy(
                maxLevelReached = PacMazeLevelCatalog.TOTAL_LEVELS,
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun saveGameOver(userId: Long, score: Int) {
        val current = ensureProgress(userId)
        if (score <= current.highScore) return
        dao.upsert(
            current.copy(
                highScore = score,
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }
}
