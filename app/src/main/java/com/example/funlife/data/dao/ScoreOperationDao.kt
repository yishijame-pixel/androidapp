package com.example.funlife.data.dao

import androidx.room.*
import com.example.funlife.data.model.ScoreOperation
import kotlinx.coroutines.flow.Flow

@Dao
interface ScoreOperationDao {
    
    @Query("SELECT * FROM score_operations WHERE gameSessionId = :sessionId ORDER BY timestamp ASC")
    fun getOperationsBySession(sessionId: Long): Flow<List<ScoreOperation>>
    
    @Query("SELECT * FROM score_operations ORDER BY timestamp ASC")
    suspend fun getAllOperations(): List<ScoreOperation>
    
    @Query("DELETE FROM score_operations WHERE playerId = :playerId")
    suspend fun deleteByPlayerId(playerId: Int)
    
    @Insert
    suspend fun insert(operation: ScoreOperation)
    
    @Query("DELETE FROM score_operations WHERE gameSessionId = :sessionId")
    suspend fun deleteBySession(sessionId: Long)
    
    @Query("DELETE FROM score_operations")
    suspend fun deleteAll()
    
    @Query("SELECT MAX(gameSessionId) FROM score_operations")
    suspend fun getLatestSessionId(): Long?
}
