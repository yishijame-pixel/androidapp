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
    val totalEarned: Int = 0,  // 累计获得金币
    val shopPoints: Int = 0  // 商城积分（购买商品获得5积分，10积分抽一次商品转盘）
)
