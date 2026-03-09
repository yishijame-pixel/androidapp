// MoodRepository.kt - 心情仓库
package com.example.funlife.repository

import com.example.funlife.data.dao.MoodDao
import com.example.funlife.data.model.MoodEntry
import kotlinx.coroutines.flow.Flow

class MoodRepository(private val moodDao: MoodDao) {
    
    val allMoods: Flow<List<MoodEntry>> = moodDao.getAllMoodEntries()
    
    suspend fun insertMood(mood: MoodEntry) = moodDao.insertMood(mood)
    
    suspend fun updateMood(mood: MoodEntry) = moodDao.updateMood(mood)
    
    suspend fun deleteMood(mood: MoodEntry) = moodDao.deleteMood(mood)
    
    suspend fun getMoodByDate(date: String) = moodDao.getMoodByDate(date)
    
    fun getRecentMoods(limit: Int) = moodDao.getRecentMoods(limit)
}
