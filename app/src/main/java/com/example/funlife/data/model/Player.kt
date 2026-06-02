// Player.kt - 玩家数据模型
package com.example.funlife.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "players",
    indices = [Index(value = ["userId"], name = "index_players_userId")]
)
data class Player(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val userId: Long = 0,      // 🔥 用户ID
    val name: String,          // 玩家名称
    val score: Int = 0,        // 当前分数
    val avatar: String = "tx_1" // 头像文件名
)
