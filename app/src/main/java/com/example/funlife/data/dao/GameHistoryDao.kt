// GameHistoryDao.kt - 游戏历史数据访问对象
package com.example.funlife.data.dao

import androidx.room.*
import com.example.funlife.data.model.GameHistory
import kotlinx.coroutines.flow.Flow

@Dao
interface GameHistoryDao {
    
    // 获取所有历史记录（按时间降序）
    @Query("SELECT * FROM game_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<GameHistory>>
    
    // 根据游戏类型获取历史
    @Query("SELECT * FROM game_history WHERE gameType = :gameType ORDER BY timestamp DESC")
    fun getHistoryByType(gameType: String): Flow<List<GameHistory>>
    
    // 根据玩家名称获取历史
    @Query("SELECT * FROM game_history WHERE playerName = :playerName ORDER BY timestamp DESC")
    fun getHistoryByPlayer(playerName: String): Flow<List<GameHistory>>
    
    // 获取最近N条记录
    @Query("SELECT * FROM game_history ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentHistory(limit: Int): Flow<List<GameHistory>>
    
    // 插入历史记录
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: GameHistory)
    
    // 删除历史记录
    @Delete
    suspend fun deleteHistory(history: GameHistory)
    
    // 清空所有历史
    @Query("DELETE FROM game_history")
    suspend fun clearAllHistory()
    
    // 删除指定日期之前的记录
    @Query("DELETE FROM game_history WHERE timestamp < :beforeDate")
    suspend fun deleteHistoryBefore(beforeDate: String)
    
    // 获取统计信息
    @Query("SELECT COUNT(*) FROM game_history WHERE gameType = :gameType")
    suspend fun getCountByType(gameType: String): Int
    
    @Query("SELECT COUNT(DISTINCT playerName) FROM game_history")
    suspend fun getTotalPlayers(): Int
}
