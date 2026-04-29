package com.example.funlife.data.dao

import androidx.room.*
import com.example.funlife.data.model.Riddle
import com.example.funlife.data.model.RiddleProgress
import com.example.funlife.data.model.RiddleStats
import kotlinx.coroutines.flow.Flow

@Dao
interface RiddleDao {
    @Query("SELECT * FROM riddles ORDER BY id")
    fun getAllRiddles(): Flow<List<Riddle>>
    
    @Query("SELECT * FROM riddles WHERE id = :riddleId")
    suspend fun getRiddleById(riddleId: Long): Riddle?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRiddle(riddle: Riddle): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRiddles(riddles: List<Riddle>)
    
    @Query("DELETE FROM riddles")
    suspend fun deleteAllRiddles()
    
    @Query("SELECT COUNT(*) FROM riddles")
    suspend fun getRiddleCount(): Int
}

@Dao
interface RiddleProgressDao {
    @Query("SELECT * FROM riddle_progress WHERE userId = :userId AND riddleId = :riddleId")
    suspend fun getProgress(userId: Long, riddleId: Long): RiddleProgress?
    
    @Query("SELECT * FROM riddle_progress WHERE userId = :userId")
    fun getUserProgress(userId: Long): Flow<List<RiddleProgress>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgress(progress: RiddleProgress)
    
    @Update
    suspend fun updateProgress(progress: RiddleProgress)
}

@Dao
interface RiddleStatsDao {
    @Query("SELECT * FROM riddle_stats WHERE userId = :userId")
    fun getStats(userId: Long): Flow<RiddleStats?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStats(stats: RiddleStats)
    
    @Update
    suspend fun updateStats(stats: RiddleStats)
    
    @Query("SELECT * FROM riddle_stats WHERE userId = :userId")
    suspend fun getStatsSync(userId: Long): RiddleStats?
}
