package com.example.funlife.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "riddles")
data class Riddle(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val question: String,
    val answer: String,
    val category: String = "脑筋急转弯",
    val difficulty: Int = 1 // 1-5星难度
)

@Entity(tableName = "riddle_progress")
data class RiddleProgress(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val riddleId: Long,
    val isAnswered: Boolean = false,
    val isCorrect: Boolean = false,
    val attempts: Int = 0,
    val lastAttemptTime: Long = System.currentTimeMillis()
)

@Entity(tableName = "riddle_stats")
data class RiddleStats(
    @PrimaryKey
    val userId: Long,
    val totalAnswered: Int = 0,
    val totalCorrect: Int = 0,
    val currentStreak: Int = 0,
    val maxStreak: Int = 0,
    val totalScore: Int = 0
)
