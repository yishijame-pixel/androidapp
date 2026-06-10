package com.example.funlife.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "pac_maze_progress",
    indices = [Index(value = ["userId"], unique = true)],
)
data class PacMazeProgress(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val maxLevelReached: Int = 1,
    val highScore: Int = 0,
    val starsBitmask: Int = 0,
    val endlessBestScore: Int = 0,
    val endlessBestWave: Int = 0,
    val mazeBestTimeMs: Long = 0,
    val updatedAt: Long = System.currentTimeMillis(),
)
