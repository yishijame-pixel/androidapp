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
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository: UserPreferencesRepository
    val preferences: StateFlow<UserPreferences>
    
    init {
        val database = AppDatabase.getDatabase(application)
        val preferencesDao = database.userPreferencesDao()
        repository = UserPreferencesRepository(preferencesDao)
        
        // 初始化默认偏好
        viewModelScope.launch {
            repository.insert(UserPreferences())
        }
        
        preferences = repository.preferences.map { 
            it ?: UserPreferences() 
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserPreferences()
        )
    }
    
    // 更新深色模式
    fun updateDarkMode(isDarkMode: Boolean) {
        viewModelScope.launch {
            repository.updateDarkMode(isDarkMode)
        }
    }
    
    // 更新通知设置
    fun updateNotifications(enable: Boolean) {
        viewModelScope.launch {
            repository.updateNotifications(enable)
        }
    }
    
    // 更新默认加分值
    fun updateScoreIncrement(increment: Int) {
        viewModelScope.launch {
            repository.updateScoreIncrement(increment)
        }
    }
    
    // 更新所有偏好
    fun updatePreferences(preferences: UserPreferences) {
        viewModelScope.launch {
            repository.update(preferences)
        }
    }
}
