// Anniversary.kt - 纪念日数据模型
package com.example.funlife.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.temporal.ChronoUnit

// 纪念日类型枚举
enum class AnniversaryType(val displayName: String, val emoji: String) {
    BIRTHDAY("生日", "🎂"),
    LOVE("恋爱", "💕"),
    WEDDING("结婚", "💍"),
    FRIENDSHIP("友谊", "🤝"),
    WORK("工作", "💼"),
    CUSTOM("自定义", "⭐")
}

@Entity(tableName = "anniversaries")
data class Anniversary(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val userId: Long = 0,      // 用户ID
    val name: String,          // 纪念日名称
    val date: String,          // 日期 (格式: yyyy-MM-dd)
    val imageUri: String? = null,  // 背景图片 URI
    val isPinned: Boolean = false,  // 是否置顶
    val type: String = AnniversaryType.CUSTOM.name,  // 纪念日类型
    val isYearly: Boolean = true,  // 是否每年重复
    val note: String? = null,  // 备注
    val importance: Int = 3  // 重要程度 1-5星
) {
    // 获取类型枚举
    fun getTypeEnum(): AnniversaryType {
        return try {
            AnniversaryType.valueOf(type)
        } catch (e: Exception) {
            AnniversaryType.CUSTOM
        }
    }
    
    // 计算剩余天数（考虑每年重复）
    fun getDaysRemaining(): Long {
        val targetDate = LocalDate.parse(date)
        val today = LocalDate.now()
        
        if (isYearly) {
            // 每年重复，计算今年的纪念日
            val thisYearDate = targetDate.withYear(today.year)
            return if (thisYearDate.isBefore(today)) {
                // 今年已过，计算明年的
                ChronoUnit.DAYS.between(today, thisYearDate.plusYears(1))
            } else {
                ChronoUnit.DAYS.between(today, thisYearDate)
            }
        } else {
            // 不重复，直接计算
            return ChronoUnit.DAYS.between(today, targetDate)
        }
    }
    
    // 计算已经过去多少年
    fun getYearsPassed(): Int {
        val targetDate = LocalDate.parse(date)
        val today = LocalDate.now()
        return ChronoUnit.YEARS.between(targetDate, today).toInt()
    }
    
    // 获取格式化的日期显示
    fun getFormattedDate(): String {
        val localDate = LocalDate.parse(date)
        return "${localDate.year}年${localDate.monthValue}月${localDate.dayOfMonth}日"
    }
    
    // 获取简短日期显示（月日）
    fun getShortDate(): String {
        val localDate = LocalDate.parse(date)
        return "${localDate.monthValue}月${localDate.dayOfMonth}日"
    }
    
    // 获取完整描述
    fun getFullDescription(): String {
        val yearsPassed = getYearsPassed()
        return if (isYearly && yearsPassed > 0) {
            "已经 $yearsPassed 年了"
        } else {
            getFormattedDate()
        }
    }
}
