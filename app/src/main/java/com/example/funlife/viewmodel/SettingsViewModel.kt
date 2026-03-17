// SettingsViewModel.kt - 设置视图模型
package com.example.funlife.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.funlife.data.database.AppDatabase
import com.example.funlife.data.model.UserPreferences
import com.example.funlife.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    
    private val context = application.applicationContext
    private val repository: UserPreferencesRepository
    val preferences: StateFlow<UserPreferences>
    
    // 🔥 获取当前用户ID
    private fun getCurrentUserId(): Long {
        val sessionManager = com.example.funlife.utils.UserSessionManager(context)
        return sessionManager.getCurrentUserId().takeIf { it > 0 } ?: 0L
    }
    
    init {
        val database = AppDatabase.getDatabase(application)
        val preferencesDao = database.userPreferencesDao()
        repository = UserPreferencesRepository(preferencesDao)
        
        // 🔥 修改：根据用户ID初始化偏好
        viewModelScope.launch {
            val userId = getCurrentUserId()
            repository.getOrCreatePreferences(userId)
        }
        
        // 🔥 修改：根据用户ID获取偏好
        preferences = repository.getPreferences(getCurrentUserId())
            .map { it ?: UserPreferences(userId = getCurrentUserId()) }
            .catch { e ->
                android.util.Log.e("SettingsViewModel", "Error loading preferences", e)
                emit(UserPreferences(userId = getCurrentUserId()))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = UserPreferences(userId = getCurrentUserId())
            )
    }
    
    // 🔥 修改：更新深色模式
    fun updateDarkMode(isDarkMode: Boolean) {
        viewModelScope.launch {
            repository.updateDarkMode(getCurrentUserId(), isDarkMode)
        }
    }
    
    // 🔥 修改：更新通知设置
    fun updateNotifications(enable: Boolean) {
        viewModelScope.launch {
            repository.updateNotifications(getCurrentUserId(), enable)
        }
    }
    
    // 🔥 修改：更新默认加分值
    fun updateScoreIncrement(increment: Int) {
        viewModelScope.launch {
            repository.updateScoreIncrement(getCurrentUserId(), increment)
        }
    }
    
    // 更新所有偏好
    fun updatePreferences(preferences: UserPreferences) {
        viewModelScope.launch {
            repository.update(preferences)
        }
    }
}
