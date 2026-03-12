// MoodEntry.kt - 心情日记数据模型
package com.example.funlife.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mood_entries")
data class MoodEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val userId: Long = 0,          // 用户ID
    val date: String,              // 日期（yyyy-MM-dd）
    val mood: String,              // 心情emoji
    val moodLevel: Int,            // 心情等级 1-5
    val note: String = "",         // 日记内容
    val tags: String = "",         // 标签（逗号分隔）
    val timestamp: String          // 时间戳
)
