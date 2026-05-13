// PlayerVictoryRecord.kt - 玩家胜利记录数据模型
package com.example.funlife.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "player_victory_records")
data class PlayerVictoryRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val playerName: String,     // 玩家名字
    val avatar: String,         // 玩家头像
    val victoryCount: Int = 0,  // 胜利次数
    val lastVictoryTime: Long = System.currentTimeMillis() // 最后一次胜利时间
)
