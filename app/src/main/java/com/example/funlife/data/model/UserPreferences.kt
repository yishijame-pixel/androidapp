// UserPreferences.kt - 用户偏好设置数据模型
package com.example.funlife.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_preferences")
data class UserPreferences(
    @PrimaryKey
    val id: Int = 1,  // 只有一条记录
    val isDarkMode: Boolean = false,                    // 深色模式
    val enableNotifications: Boolean = true,            // 启用通知
    val notificationDaysBefore: Int = 7,               // 提前几天通知
    val defaultScoreIncrement: Int = 1,                // 默认加分值
    val enableSound: Boolean = true,                    // 启用音效
    val enableVibration: Boolean = true,               // 启用震动
    val autoBackup: Boolean = false,                   // 自动备份
    val language: String = "zh",                       // 语言
    val sortOrder: String = "date_asc"                 // 排序方式
)
