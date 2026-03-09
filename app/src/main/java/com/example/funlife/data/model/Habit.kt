// Habit.kt - 习惯数据模型
package com.example.funlife.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,              // 习惯名称
    val icon: String,              // 图标emoji
    val color: String,             // 颜色（hex格式）
    val targetDays: Int = 0,       // 目标天数（0表示无限）
    val createdAt: String,         // 创建时间
    val isActive: Boolean = true,  // 是否激活
    val makeupCards: Int = 0       // 补卡卡片数量
)
