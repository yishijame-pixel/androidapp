// PlayerDao.kt - 玩家数据访问对象
package com.example.funlife.data.dao

import android.util.Log
import androidx.room.*
import com.example.funlife.data.model.Player
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerDao {
    
    // 获取所有玩家（按分数降序排序）
    @Query("SELECT * FROM players ORDER BY score DESC")
    fun getAllPlayers(): Flow<List<Player>>
    
    // 插入玩家
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayer(player: Player)
    
    // 更新玩家
    @Update
    suspend fun updatePlayer(player: Player)
    
    // 删除玩家
    @Delete
    suspend fun deletePlayer(player: Player)
    
    // 删除所有玩家
    @Query("DELETE FROM players")
    suspend fun deleteAllPlayers()
    
    // 重置所有分数
    @Query("UPDATE players SET score = 0")
    suspend fun resetAllScores()
}
