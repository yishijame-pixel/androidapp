// PlayerVictoryRecordDao.kt - 玩家胜利记录数据访问对象
package com.example.funlife.data.dao

import androidx.room.*
import com.example.funlife.data.model.PlayerVictoryRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerVictoryRecordDao {
    @Query("SELECT * FROM player_victory_records ORDER BY victoryCount DESC")
    fun getAllRecords(): Flow<List<PlayerVictoryRecord>>
    
    @Query("SELECT * FROM player_victory_records WHERE playerName = :playerName LIMIT 1")
    suspend fun getRecordByName(playerName: String): PlayerVictoryRecord?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: PlayerVictoryRecord)
    
    @Update
    suspend fun update(record: PlayerVictoryRecord)
    
    @Query("DELETE FROM player_victory_records")
    suspend fun deleteAll()
    
    @Transaction
    suspend fun recordVictory(playerName: String, avatar: String) {
        val existing = getRecordByName(playerName)
        if (existing != null) {
            // 更新现有记录
            update(existing.copy(
                victoryCount = existing.victoryCount + 1,
                lastVictoryTime = System.currentTimeMillis()
            ))
        } else {
            // 创建新记录
            insert(PlayerVictoryRecord(
                playerName = playerName,
                avatar = avatar,
                victoryCount = 1,
                lastVictoryTime = System.currentTimeMillis()
            ))
        }
    }
}
