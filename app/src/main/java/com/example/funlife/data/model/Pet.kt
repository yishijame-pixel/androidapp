// Pet.kt - 宠物数据模型
package com.example.funlife.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pets")
data class Pet(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,                    // 用户ID
    val name: String,                    // 宠物名字
    val type: PetType,                   // 宠物类型
    val level: Int = 1,                  // 等级 1-30
    val experience: Int = 0,             // 当前经验值
    val hungerValue: Int = 100,          // 饥饿值 0-100
    val cleanValue: Int = 100,           // 清洁值 0-100
    val moodValue: Int = 100,            // 心情值 0-100
    val healthValue: Int = 100,          // 健康值 0-100
    val intimacy: Int = 0,               // 亲密度 0-1000
    val birthday: Long = System.currentTimeMillis(), // 领养日期
    val lastFeedTime: Long = System.currentTimeMillis(),
    val lastCleanTime: Long = System.currentTimeMillis(),
    val lastPlayTime: Long = System.currentTimeMillis(),
    val lastUpdateTime: Long = System.currentTimeMillis(), // 最后更新时间
    val appearance: String = "",         // 外观配置 JSON
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    // 获取成长阶段
    fun getGrowthStage(): GrowthStage {
        return when (level) {
            in 1..5 -> GrowthStage.BABY
            in 6..15 -> GrowthStage.CHILD
            in 16..25 -> GrowthStage.ADULT
            else -> GrowthStage.PERFECT
        }
    }
    
    // 获取下一级所需经验
    fun getExpForNextLevel(): Int {
        return level * 100 // 简单公式：等级 * 100
    }
    
    // 是否需要喂食
    fun needsFeeding(): Boolean = hungerValue < 30
    
    // 是否需要清洁
    fun needsCleaning(): Boolean = cleanValue < 30
    
    // 是否生病
    fun isSick(): Boolean = healthValue < 50
    
    // 获取心情状态
    fun getMoodState(): MoodState {
        return when {
            moodValue >= 80 -> MoodState.HAPPY
            moodValue >= 50 -> MoodState.NORMAL
            moodValue >= 20 -> MoodState.SAD
            else -> MoodState.DEPRESSED
        }
    }
}

// 宠物类型
enum class PetType {
    CAT,      // 猫
    DOG,      // 狗
    RABBIT,   // 兔子
    HAMSTER   // 仓鼠
}

// 成长阶段
enum class GrowthStage {
    BABY,     // 幼年期 1-5级
    CHILD,    // 少年期 6-15级
    ADULT,    // 成年期 16-25级
    PERFECT   // 完全体 26-30级
}

// 心情状态
enum class MoodState {
    HAPPY,      // 开心
    NORMAL,     // 正常
    SAD,        // 难过
    DEPRESSED   // 沮丧
}
