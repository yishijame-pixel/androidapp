// MoodRepository.kt - 心情仓库
package com.example.funlife.repository

import com.example.funlife.data.dao.MoodDao
import com.example.funlife.data.model.MoodEntry
import kotlinx.coroutines.flow.Flow

class MoodRepository(private val moodDao: MoodDao) {
    
    fun getAllMoods(userId: Long): Flow<List<MoodEntry>> = moodDao.getAllMoodEntries(userId)
    
    suspend fun insertMood(mood: MoodEntry) = moodDao.insertMood(mood)
    
    suspend fun updateMood(mood: MoodEntry) = moodDao.updateMood(mood)
    
    suspend fun deleteMood(mood: MoodEntry) = moodDao.deleteMood(mood)
    
    suspend fun getMoodByDate(userId: Long, date: String) = moodDao.getMoodByDate(userId, date)
    
    fun getRecentMoods(userId: Long, limit: Int) = moodDao.getRecentMoods(userId, limit)
}
