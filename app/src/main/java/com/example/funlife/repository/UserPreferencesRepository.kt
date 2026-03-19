// UserPreferencesRepository.kt - 用户偏好仓库
package com.example.funlife.repository

import com.example.funlife.data.dao.UserPreferencesDao
import com.example.funlife.data.model.UserPreferences
import kotlinx.coroutines.flow.Flow

class UserPreferencesRepository(private val userPreferencesDao: UserPreferencesDao) {
    
    // 🔥 修改：根据用户ID获取偏好
    fun getPreferences(userId: Long): Flow<UserPreferences?> = 
        userPreferencesDao.getPreferences(userId)
    
    // 🔥 新增：同步获取偏好
    suspend fun getPreferencesSync(userId: Long): UserPreferences? =
        userPreferencesDao.getPreferencesSync(userId)
    
    // 🔥 新增：获取或创建默认偏好
    suspend fun getOrCreatePreferences(userId: Long): UserPreferences {
        return userPreferencesDao.getPreferencesSync(userId) ?: run {
            val defaultPrefs = UserPreferences(userId = userId)
            userPreferencesDao.insertPreferences(defaultPrefs)
            defaultPrefs
        }
    }
    
    suspend fun insert(preferences: UserPreferences) {
        userPreferencesDao.insertPreferences(preferences)
    }
    
    suspend fun update(preferences: UserPreferences) {
        userPreferencesDao.updatePreferences(preferences)
    }
    
    // 🔥 修改：添加userId参数
    suspend fun updateDarkMode(userId: Long, isDarkMode: Boolean) {
        userPreferencesDao.updateDarkMode(userId, isDarkMode)
    }
    
    suspend fun updateNotifications(userId: Long, enable: Boolean) {
        userPreferencesDao.updateNotifications(userId, enable)
    }
    
    suspend fun updateScoreIncrement(userId: Long, increment: Int) {
        userPreferencesDao.updateScoreIncrement(userId, increment)
    }
    
    // 🔥 新增：转盘相关设置更新方法
    suspend fun updateSound(userId: Long, enable: Boolean) {
        userPreferencesDao.updateSound(userId, enable)
    }
    
    suspend fun updateVibration(userId: Long, enable: Boolean) {
        userPreferencesDao.updateVibration(userId, enable)
    }
    
    suspend fun updateWheelTheme(userId: Long, theme: String) {
        userPreferencesDao.updateWheelTheme(userId, theme)
    }
    
    suspend fun updateWeightVisualization(userId: Long, show: Boolean) {
        userPreferencesDao.updateWeightVisualization(userId, show)
    }
    
    suspend fun updateParticleEffect(userId: Long, enable: Boolean) {
        userPreferencesDao.updateParticleEffect(userId, enable)
    }
    
    suspend fun updateFireworks(userId: Long, enable: Boolean) {
        userPreferencesDao.updateFireworks(userId, enable)
    }
    
    suspend fun updateCoinAnimation(userId: Long, enable: Boolean) {
        userPreferencesDao.updateCoinAnimation(userId, enable)
    }
    
    suspend fun updateLastTemplate(userId: Long, templateId: Int?) {
        userPreferencesDao.updateLastTemplate(userId, templateId)
    }
    
    suspend fun updateLastCustomOptions(userId: Long, options: String) {
        userPreferencesDao.updateLastCustomOptions(userId, options)
    }
    
    suspend fun updateLastSpinMode(userId: Long, mode: String) {
        userPreferencesDao.updateLastSpinMode(userId, mode)
    }
    
    // 🔥 新增：保存最后使用的自定义模式ID
    suspend fun updateLastCustomModeId(userId: Long, modeId: Int?) {
        userPreferencesDao.updateLastCustomModeId(userId, modeId)
    }
}
