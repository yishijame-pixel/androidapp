// DailyReward.kt - 每日奖励领取记录
package com.example.funlife.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_rewards")
data class DailyReward(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val rewardType: String, // "free_coins"
    val lastClaimDate: String, // 格式: "yyyy-MM-dd"
    val claimCount: Int = 0 // 累计领取次数
)
