// Countdown.kt - 倒数日数据模型
package com.example.funlife.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Entity(tableName = "countdowns")
data class Countdown(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,             // 倒数日标题
    val targetDate: String,        // 目标日期（yyyy-MM-dd）
    val category: String,          // 分类
    val icon: String,              // 图标emoji
    val color: String,             // 颜色
    val note: String = "",         // 备注
    val createdAt: String          // 创建时间
) {
    // 计算剩余天数
    fun getDaysRemaining(): Long {
        val target = LocalDate.parse(targetDate)
        val today = LocalDate.now()
        return ChronoUnit.DAYS.between(today, target)
    }
}
