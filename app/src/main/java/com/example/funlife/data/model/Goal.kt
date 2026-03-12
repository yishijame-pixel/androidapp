// Goal.kt - 目标数据模型
package com.example.funlife.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "goals")
data class Goal(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val userId: Long = 0,          // 用户ID
    val title: String,             // 目标标题
    val description: String = "",  // 目标描述
    val category: String,          // 分类（工作、生活、学习等）
    val targetDate: String?,       // 目标日期（yyyy-MM-dd），null表示无截止日期
    val progress: Int = 0,         // 进度 0-100
    val isCompleted: Boolean = false, // 是否完成
    val createdAt: String,         // 创建时间
    val completedAt: String? = null   // 完成时间
)
