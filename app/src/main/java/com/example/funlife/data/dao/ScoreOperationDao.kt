package com.example.funlife.data.dao

import androidx.room.*
import com.example.funlife.data.model.ScoreOperation
import kotlinx.coroutines.flow.Flow

@Dao
interface ScoreOperationDao {

    @Query("SELECT * FROM score_operations WHERE userId = :userId AND gameSessionId = :sessionId ORDER BY timestamp ASC")
    fun getOperationsBySession(userId: Long, sessionId: Long): Flow<List<ScoreOperation>>

    @Query("SELECT * FROM score_operations WHERE userId = :userId ORDER BY timestamp ASC")
    suspend fun getAllOperations(userId: Long): List<ScoreOperation>

    @Query("DELETE FROM score_operations WHERE userId = :userId AND playerId = :playerId")
    suspend fun deleteByPlayerId(userId: Long, playerId: Int)

    @Insert
    suspend fun insert(operation: ScoreOperation)

    @Query("DELETE FROM score_operations WHERE userId = :userId AND gameSessionId = :sessionId")
    suspend fun deleteBySession(userId: Long, sessionId: Long)

    @Query("DELETE FROM score_operations WHERE userId = :userId")
    suspend fun deleteAll(userId: Long)

    @Query("SELECT MAX(gameSessionId) FROM score_operations WHERE userId = :userId")
    suspend fun getLatestSessionId(userId: Long): Long?
}
