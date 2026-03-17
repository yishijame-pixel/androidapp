// UserPreferences.kt - 用户偏好设置数据模型
package com.example.funlife.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_preferences")
data class UserPreferences(
    @PrimaryKey
    val userId: Long = 0,                              // 🔥 改为用户ID作为主键
    val isDarkMode: Boolean = false,                    // 深色模式
    val enableNotifications: Boolean = true,            // 启用通知
    val notificationDaysBefore: Int = 7,               // 提前几天通知
    val defaultScoreIncrement: Int = 1,                // 默认加分值
    val enableSound: Boolean = true,                    // 启用音效
    val enableVibration: Boolean = true,               // 启用震动
    val autoBackup: Boolean = false,                   // 自动备份
    val language: String = "zh",                       // 语言
    val sortOrder: String = "date_asc",                // 排序方式
    
    // 🔥 新增：转盘相关设置
    val wheelTheme: String = "default",                // 转盘主题
    val showWeightVisualization: Boolean = false,      // 显示权重可视化
    val particleEffectEnabled: Boolean = true,         // 粒子效果
    val fireworksEnabled: Boolean = true,              // 烟花效果
    val coinAnimationEnabled: Boolean = true,          // 金币动画
    
    // 🔥 新增：最后使用的模板和选项
    val lastTemplateId: Int? = null,                   // 最后使用的模板ID
    val lastCustomOptions: String = "",                // 最后自定义的选项（JSON格式）
    val lastSpinMode: String = "NORMAL"                // 最后使用的转盘模式
)
