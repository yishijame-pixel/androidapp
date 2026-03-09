// Player.kt - 玩家数据模型
package com.example.funlife.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "players")
data class Player(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,          // 玩家名称
    val score: Int = 0         // 当前分数
)
