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
    val lastSpinMode: String = "NORMAL",               // 最后使用的转盘模式（基础模式）
    val lastCustomModeId: Int? = null,                 // 🔥 新增：最后使用的自定义模式ID
    
    // 🔥 新增：每个模式独立的选项配置
    val normalModeOptions: String = "",                // 普通模式的选项（JSON格式，6个）
    val advancedModeOptions: String = "",              // 进阶模式的选项（JSON格式，8个）
    val luckyModeOptions: String = "",                 // 幸运模式的选项（JSON格式，8个）
    
    // 🔥 首页今日寄语自定义文字（空 = 显示每日语录，按用户ID + 日期挑一句）
    val homePanelText: String = "",                     // 用户自定义寄语（留空显示每日精选）
    val homePanelTextStyle: String = "pink",            // 艺术字颜色主题：pink, purple, blue, gold, rainbow
    
    // 🔥 新增：转盘结算面板皮肤
    val spinResultPanelSkin: String = "js_1",            // 转盘结算面板皮肤：js_1, js_2, js_3, js_4, js_5
    
    // 🔥 新增：转盘按钮皮肤
    val spinButtonSkin: String = "pf_1",                 // 转盘按钮皮肤：pf_1 到 pf_27
    
    // 🔥 新增：转盘旋转音量
    val spinRotationVolume: Float = 0.7f,                // 转盘旋转音量：0.0 到 1.0
    
    // 🔥 新增：装备的头像框
    val equippedAvatarFrame: String? = null              // 装备的头像框资源路径（如 "xiangkuang/1.png"）
)
