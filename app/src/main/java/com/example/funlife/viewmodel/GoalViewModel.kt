// GoalViewModel.kt
package com.example.funlife.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.funlife.data.database.AppDatabase
import com.example.funlife.data.model.Goal
import com.example.funlife.data.model.Countdown
import com.example.funlife.repository.GoalRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class GoalViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: GoalRepository
    private val context = application.applicationContext
    val activeGoals: StateFlow<List<Goal>>
    val countdowns: StateFlow<List<Countdown>>
    
    // 🔥 获取当前用户ID
    private fun getCurrentUserId(): Long {
        val sessionManager = com.example.funlife.utils.UserSessionManager(context)
        return sessionManager.getCurrentUserId().takeIf { it > 0 } ?: 0L
    }
    
    init {
        val database = AppDatabase.getDatabase(application)
        repository = GoalRepository(database.goalDao())
        
        // 🔥 修复：添加异常处理和用户过滤
        activeGoals = repository.getActiveGoals(getCurrentUserId())
            .catch { e ->
                android.util.Log.e("GoalViewModel", "Error loading goals", e)
                emit(emptyList())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
        
        countdowns = repository.getAllCountdowns(getCurrentUserId())
            .catch { e ->
                android.util.Log.e("GoalViewModel", "Error loading countdowns", e)
                emit(emptyList())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
    }
    
    fun addGoal(title: String, category: String, targetDate: String?) {
        viewModelScope.launch {
            // 🔥 新增：输入验证
            val titleValidation = com.example.funlife.utils.ValidationUtils.validateGoalTitle(title)
            if (titleValidation is com.example.funlife.utils.ValidationResult.Error) {
                android.util.Log.w("GoalViewModel", "Invalid goal title: ${titleValidation.message}")
                return@launch
            }
            
            val userId = getCurrentUserId()
            val goal = Goal(
                userId = userId,
                title = title,
                category = category,
                targetDate = targetDate,
                createdAt = LocalDateTime.now().toString()
            )
            repository.insertGoal(goal)
        }
    }
    
    fun addCountdown(title: String, targetDate: String, category: String, icon: String, color: String) {
        viewModelScope.launch {
            // 🔥 新增：输入验证
            val titleValidation = com.example.funlife.utils.ValidationUtils.validateGoalTitle(title)
            if (titleValidation is com.example.funlife.utils.ValidationResult.Error) {
                android.util.Log.w("GoalViewModel", "Invalid countdown title: ${titleValidation.message}")
                return@launch
            }
            
            val userId = getCurrentUserId()
            val countdown = Countdown(
                userId = userId,
                title = title,
                targetDate = targetDate,
                category = category,
                icon = icon,
                color = color,
                createdAt = LocalDateTime.now().toString()
            )
            repository.insertCountdown(countdown)
        }
    }
    
    fun deleteGoal(goal: Goal) {
        viewModelScope.launch {
            repository.deleteGoal(goal)
        }
    }
    
    fun deleteCountdown(countdown: Countdown) {
        viewModelScope.launch {
            repository.deleteCountdown(countdown)
        }
    }
}
