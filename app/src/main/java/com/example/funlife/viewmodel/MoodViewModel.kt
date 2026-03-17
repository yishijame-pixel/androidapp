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
    val moods: StateFlow<List<MoodEntry>>
    
    // 🔥 获取当前用户ID
    private fun getCurrentUserId(): Long {
        val sessionManager = com.example.funlife.utils.UserSessionManager(context)
        return sessionManager.getCurrentUserId().takeIf { it > 0 } ?: 0L
    }
    
    init {
        val database = AppDatabase.getDatabase(application)
        repository = MoodRepository(database.moodDao())
        
        // 🔥 修复：添加异常处理和用户过滤
        moods = repository.getAllMoods(getCurrentUserId())
            .catch { e ->
                android.util.Log.e("MoodViewModel", "Error loading moods", e)
                emit(emptyList())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
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
