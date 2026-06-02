package com.example.funlife.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 分数操作记录
 * 记录每一次加分或减分操作
 */
@Entity(
    tableName = "score_operations",
    indices = [androidx.room.Index("userId")]
)
data class ScoreOperation(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long = 0L,    // 🔒 多账号隔离
    val gameSessionId: Long,  // 游戏会话ID，用于区分不同局游戏
    val playerId: Int,        // 玩家ID（改为Int以匹配Player.id）
    val playerName: String,   // 玩家名字
    val playerAvatar: String, // 玩家头像
    val operation: Int,       // 操作值：+1, +2, +4, +6, -1等
    val scoreBefore: Int,     // 操作前的分数
    val scoreAfter: Int,      // 操作后的分数
    val timestamp: Long = System.currentTimeMillis()  // 操作时间戳
)
