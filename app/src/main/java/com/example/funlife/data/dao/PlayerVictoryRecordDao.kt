// PlayerVictoryRecordDao.kt - 玩家胜利记录数据访问对象
package com.example.funlife.data.dao

import androidx.room.*
import com.example.funlife.data.model.PlayerVictoryRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerVictoryRecordDao {
    @Query("SELECT * FROM player_victory_records WHERE userId = :userId ORDER BY victoryCount DESC")
    fun getAllRecords(userId: Long): Flow<List<PlayerVictoryRecord>>

    @Query("SELECT * FROM player_victory_records WHERE userId = :userId AND playerName = :playerName LIMIT 1")
    suspend fun getRecordByName(userId: Long, playerName: String): PlayerVictoryRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: PlayerVictoryRecord)

    @Update
    suspend fun update(record: PlayerVictoryRecord)

    @Query("DELETE FROM player_victory_records WHERE userId = :userId")
    suspend fun deleteAll(userId: Long)

    @Transaction
    suspend fun recordVictory(userId: Long, playerName: String, avatar: String) {
        val existing = getRecordByName(userId, playerName)
        if (existing != null) {
            update(existing.copy(
                victoryCount = existing.victoryCount + 1,
                lastVictoryTime = System.currentTimeMillis()
            ))
        } else {
            insert(PlayerVictoryRecord(
                userId = userId,
                playerName = playerName,
                avatar = avatar,
                victoryCount = 1,
                lastVictoryTime = System.currentTimeMillis()
            ))
        }
    }
}
