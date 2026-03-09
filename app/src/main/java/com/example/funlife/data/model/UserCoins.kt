// UserCoins.kt - 用户金币数据模型
package com.example.funlife.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_coins")
data class UserCoins(
    @PrimaryKey
    val id: Int = 1,  // 只有一条记录
    val coins: Int = 0,  // 金币数量
    val totalEarned: Int = 0  // 累计获得金币
)
