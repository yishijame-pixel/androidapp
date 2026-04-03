// MoodViewModel.kt
package com.example.funlife.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.funlife.data.database.AppDatabase
import com.example.funlife.data.model.MoodEntry
import com.example.funlife.repository.MoodRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime

class MoodViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: MoodRepository
    private val context = application.applicationContext
    
    // 🔥 改用 MutableStateFlow 直接管理数据
    private val _moods = MutableStateFlow<List<MoodEntry>>(emptyList())
    val moods: StateFlow<List<MoodEntry>> = _moods
    
    // 🔥 改用实时获取userId，而不是在init时缓存
    private fun getCurrentUserId(): Long {
        val sessionManager = com.example.funlife.utils.UserSessionManager(context)
        val userId = sessionManager.getCurrentUserId().takeIf { it > 0 } ?: 0L
        android.util.Log.d("MoodViewModel", "实时获取userId: $userId")
        return userId
    }
    
    init {
        val database = AppDatabase.getDatabase(application)
        repository = MoodRepository(database.moodDao())
        
        // 🔥 启动时立即加载数据
        loadMoods()
    }
    
    // 🔥 新增：主动加载心情数据
    private fun loadMoods() {
        viewModelScope.launch {
            try {
                val userId = getCurrentUserId()
                android.util.Log.d("MoodViewModel", "加载心情，userId: $userId")
                
                if (userId == 0L) {
                    _moods.value = emptyList()
                    return@launch
                }
                
                repository.getAllMoods(userId).collect { moods ->
                    android.util.Log.d("MoodViewModel", "获取到 ${moods.size} 条心情")
                    _moods.value = moods
                }
            } catch (e: Exception) {
                android.util.Log.e("MoodViewModel", "加载心情失败", e)
                _moods.value = emptyList()
            }
        }
    }
    
    // 🔥 新增：公开方法，供UI层在用户切换时调用
    fun refreshForNewUser() {
        android.util.Log.d("MoodViewModel", "用户切换，刷新数据")
        loadMoods()
    }
    
    fun addMood(mood: String, level: Int, note: String) {
        viewModelScope.launch {
            // 🔥 新增：输入验证
            val noteValidation = com.example.funlife.utils.ValidationUtils.validateMoodNote(note)
            if (noteValidation is com.example.funlife.utils.ValidationResult.Error) {
                android.util.Log.w("MoodViewModel", "Invalid mood note: ${noteValidation.message}")
                return@launch
            }
            
            val userId = getCurrentUserId()
            val entry = MoodEntry(
                userId = userId,
                date = LocalDate.now().toString(),
                mood = mood,
                moodLevel = level,
                note = note,
                timestamp = LocalDateTime.now().toString()
            )
            repository.insertMood(entry)
        }
    }
    
    fun deleteMood(mood: MoodEntry) {
        viewModelScope.launch {
            repository.deleteMood(mood)
        }
    }
}
