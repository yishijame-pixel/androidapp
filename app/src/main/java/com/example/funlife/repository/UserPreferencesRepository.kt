// UserPreferencesRepository.kt - 用户偏好仓库
package com.example.funlife.repository

import com.example.funlife.data.dao.UserPreferencesDao
import com.example.funlife.data.model.UserPreferences
import kotlinx.coroutines.flow.Flow

class UserPreferencesRepository(private val userPreferencesDao: UserPreferencesDao) {
    
    val preferences: Flow<UserPreferences?> = userPreferencesDao.getPreferences()
    
    suspend fun insert(preferences: UserPreferences) {
        userPreferencesDao.insertPreferences(preferences)
    }
    
    suspend fun update(preferences: UserPreferences) {
        userPreferencesDao.updatePreferences(preferences)
    }
    
    suspend fun updateDarkMode(isDarkMode: Boolean) {
        userPreferencesDao.updateDarkMode(isDarkMode)
    }
    
    suspend fun updateNotifications(enable: Boolean) {
        userPreferencesDao.updateNotifications(enable)
    }
    
    suspend fun updateScoreIncrement(increment: Int) {
        userPreferencesDao.updateScoreIncrement(increment)
    }
}
