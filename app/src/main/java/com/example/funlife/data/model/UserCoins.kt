// UserCoins.kt - 用户金币数据模型
package com.example.funlife.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_coins")
data class UserCoins(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long = 0,  // 用户ID
    val coins: Int = 0,  // 金币数量
    val totalEarned: Int = 0  // 累计获得金币
)
