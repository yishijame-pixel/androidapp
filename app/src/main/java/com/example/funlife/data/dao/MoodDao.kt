// MoodDao.kt - 心情数据访问对象
package com.example.funlife.data.dao

import androidx.room.*
import com.example.funlife.data.model.MoodEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface MoodDao {
    
    @Query("SELECT * FROM mood_entries WHERE userId = :userId ORDER BY date DESC, timestamp DESC")
    fun getAllMoodEntries(userId: Long): Flow<List<MoodEntry>>
    
    @Query("SELECT * FROM mood_entries WHERE userId = :userId AND date = :date")
    suspend fun getMoodByDate(userId: Long, date: String): MoodEntry?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMood(mood: MoodEntry)
    
    @Update
    suspend fun updateMood(mood: MoodEntry)
    
    @Delete
    suspend fun deleteMood(mood: MoodEntry)
    
    @Query("SELECT * FROM mood_entries WHERE userId = :userId ORDER BY date DESC LIMIT :limit")
    fun getRecentMoods(userId: Long, limit: Int): Flow<List<MoodEntry>>

    /** 自 sinceDate（YYYY-MM-DD 字符串）以来 moodLevel 平均值，无记录返回 null（v53 心情低谷召回用） */
    @Query("SELECT AVG(moodLevel) FROM mood_entries WHERE userId = :userId AND date >= :sinceDate")
    suspend fun avgLevelSince(userId: Long, sinceDate: String): Double?

    /** 自 sinceDate 以来记录条数 */
    @Query("SELECT COUNT(*) FROM mood_entries WHERE userId = :userId AND date >= :sinceDate")
    suspend fun countSince(userId: Long, sinceDate: String): Int
}
