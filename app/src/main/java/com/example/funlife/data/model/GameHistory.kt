// GameHistory.kt - 游戏历史记录数据模型
package com.example.funlife.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Entity(tableName = "game_history")
data class GameHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val gameType: String,           // 游戏类型：score_counter, spin_wheel
    val playerName: String,         // 玩家名称
    val score: Int = 0,             // 分数
    val result: String = "",        // 结果描述
    val timestamp: String = LocalDateTime.now().toString()  // 时间戳
) {
    // 获取格式化的时间
    fun getFormattedTime(): String {
        return try {
            val dateTime = LocalDateTime.parse(timestamp)
            dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        } catch (e: Exception) {
            timestamp
        }
    }
    
    // 获取简短时间
    fun getShortTime(): String {
        return try {
            val dateTime = LocalDateTime.parse(timestamp)
            dateTime.format(DateTimeFormatter.ofPattern("MM-dd HH:mm"))
        } catch (e: Exception) {
            timestamp
        }
    }
}
