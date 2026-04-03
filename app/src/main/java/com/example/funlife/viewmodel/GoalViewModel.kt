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
    
    // 🔥 改用 MutableStateFlow 直接管理数据
    private val _activeGoals = MutableStateFlow<List<Goal>>(emptyList())
    val activeGoals: StateFlow<List<Goal>> = _activeGoals
    
    private val _countdowns = MutableStateFlow<List<Countdown>>(emptyList())
    val countdowns: StateFlow<List<Countdown>> = _countdowns
    
    // 🔥 改用实时获取userId，而不是在init时缓存
    private fun getCurrentUserId(): Long {
        val sessionManager = com.example.funlife.utils.UserSessionManager(context)
        val userId = sessionManager.getCurrentUserId().takeIf { it > 0 } ?: 0L
        android.util.Log.d("GoalViewModel", "实时获取userId: $userId")
        return userId
    }
    
    init {
        val database = AppDatabase.getDatabase(application)
        repository = GoalRepository(database.goalDao())
        
        // 🔥 启动时立即加载数据
        loadGoals()
        loadCountdowns()
    }
    
    // 🔥 新增：主动加载目标数据
    private fun loadGoals() {
        viewModelScope.launch {
            try {
                val userId = getCurrentUserId()
                android.util.Log.d("GoalViewModel", "加载目标，userId: $userId")
                
                if (userId == 0L) {
                    _activeGoals.value = emptyList()
                    return@launch
                }
                
                repository.getActiveGoals(userId).collect { goals ->
                    android.util.Log.d("GoalViewModel", "获取到 ${goals.size} 个目标")
                    _activeGoals.value = goals
                }
            } catch (e: Exception) {
                android.util.Log.e("GoalViewModel", "加载目标失败", e)
                _activeGoals.value = emptyList()
            }
        }
    }
    
    // 🔥 新增：主动加载倒计时数据
    private fun loadCountdowns() {
        viewModelScope.launch {
            try {
                val userId = getCurrentUserId()
                android.util.Log.d("GoalViewModel", "加载倒计时，userId: $userId")
                
                if (userId == 0L) {
                    _countdowns.value = emptyList()
                    return@launch
                }
                
                repository.getAllCountdowns(userId).collect { countdowns ->
                    android.util.Log.d("GoalViewModel", "获取到 ${countdowns.size} 个倒计时")
                    _countdowns.value = countdowns
                }
            } catch (e: Exception) {
                android.util.Log.e("GoalViewModel", "加载倒计时失败", e)
                _countdowns.value = emptyList()
            }
        }
    }
    
    // 🔥 新增：公开方法，供UI层在用户切换时调用
    fun refreshForNewUser() {
        android.util.Log.d("GoalViewModel", "用户切换，刷新数据")
        loadGoals()
        loadCountdowns()
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
