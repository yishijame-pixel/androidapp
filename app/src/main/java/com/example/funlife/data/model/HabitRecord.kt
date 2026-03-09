// HabitRecord.kt - 习惯打卡记录
package com.example.funlife.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "habit_records")
data class HabitRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val habitId: Int,              // 关联的习惯ID
    val date: String,              // 打卡日期（yyyy-MM-dd）
    val note: String = "",         // 备注
    val timestamp: String          // 打卡时间戳
)
