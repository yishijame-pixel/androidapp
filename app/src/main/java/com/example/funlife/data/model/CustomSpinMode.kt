// CustomSpinMode.kt - 自定义转盘模式
package com.example.funlife.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 自定义转盘模式
 * 允许用户创建自己的转盘模式
 */
@Entity(tableName = "custom_spin_modes")
data class CustomSpinMode(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,                    // 模式名称
    val emoji: String = "🎲",            // 模式图标
    val description: String = "",        // 模式描述
    val costPerSpin: Int = 0,           // 每次消耗金币
    val hasReward: Boolean = false,      // 是否有金币奖励
    val rewardMultiplier: Float = 1.0f,  // 奖励倍率
    val primaryColor: String = "#6366F1", // 主题色
    val secondaryColor: String = "#8B5CF6", // 次要色
    val isDefault: Boolean = false,      // 是否为默认模式
    val isActive: Boolean = true,        // 是否启用
    val usageCount: Int = 0,            // 使用次数
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    // 检查是否有足够金币
    fun canAfford(coins: Int): Boolean {
        return coins >= costPerSpin
    }
    
    // 获取显示名称
    fun getDisplayName(): String {
        return "$emoji $name"
    }
    
    // 获取特性列表
    fun getFeatures(): List<String> {
        val features = mutableListOf<String>()
        
        if (costPerSpin == 0) {
            features.add("免费使用")
        } else {
            features.add("消耗 $costPerSpin 金币/次")
        }
        
        if (hasReward) {
            features.add("有机会获得金币奖励")
            if (rewardMultiplier > 1.0f) {
                features.add("奖励倍率 ${rewardMultiplier}x")
            }
        }
        
        return features
    }
}

/**
 * 预设模式类型
 */
enum class PresetModeType {
    NORMAL,      // 普通模式
    ADVANCED,    // 进阶模式
    LUCKY,       // 幸运模式
    CUSTOM       // 自定义模式
}

/**
 * 创建预设模式
 */
fun createPresetMode(type: PresetModeType): CustomSpinMode {
    return when (type) {
        PresetModeType.NORMAL -> CustomSpinMode(
            name = "普通模式",
            emoji = "🎯",
            description = "免费使用，适合日常决策",
            costPerSpin = 0,
            hasReward = false,
            primaryColor = "#6366F1",
            secondaryColor = "#8B5CF6",
            isDefault = true
        )
        
        PresetModeType.ADVANCED -> CustomSpinMode(
            name = "进阶模式",
            emoji = "⚡",
            description = "消耗金币，体验更刺激",
            costPerSpin = 10,
            hasReward = false,
            primaryColor = "#6366F1",
            secondaryColor = "#4F46E5",
            isDefault = true
        )
        
        PresetModeType.LUCKY -> CustomSpinMode(
            name = "幸运模式",
            emoji = "💰",
            description = "消耗金币，有机会获得奖励",
            costPerSpin = 20,
            hasReward = true,
            rewardMultiplier = 1.5f,
            primaryColor = "#FFD700",
            secondaryColor = "#FFA500",
            isDefault = true
        )
        
        PresetModeType.CUSTOM -> CustomSpinMode(
            name = "自定义模式",
            emoji = "✨",
            description = "完全自定义的转盘模式",
            costPerSpin = 0,
            hasReward = false,
            isDefault = false
        )
    }
}
