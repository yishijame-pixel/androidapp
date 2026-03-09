// MoodDao.kt - 心情数据访问对象
package com.example.funlife.data.dao

import androidx.room.*
import com.example.funlife.data.model.MoodEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface MoodDao {
    
    @Query("SELECT * FROM mood_entries ORDER BY date DESC, timestamp DESC")
    fun getAllMoodEntries(): Flow<List<MoodEntry>>
    
    @Query("SELECT * FROM mood_entries WHERE date = :date")
    suspend fun getMoodByDate(date: String): MoodEntry?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMood(mood: MoodEntry)
    
    @Update
    suspend fun updateMood(mood: MoodEntry)
    
    @Delete
    suspend fun deleteMood(mood: MoodEntry)
    
    @Query("SELECT * FROM mood_entries ORDER BY date DESC LIMIT :limit")
    fun getRecentMoods(limit: Int): Flow<List<MoodEntry>>
}
