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

    private val _completedGoals = MutableStateFlow<List<Goal>>(emptyList())
    val completedGoals: StateFlow<List<Goal>> = _completedGoals

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
        loadCompletedGoals()
        loadCountdowns()
    }

    private fun loadCompletedGoals() {
        viewModelScope.launch {
            try {
                val userId = getCurrentUserId()
                if (userId == 0L) { _completedGoals.value = emptyList(); return@launch }
                repository.getCompletedGoals(userId).collect { _completedGoals.value = it }
            } catch (e: Exception) {
                android.util.Log.e("GoalViewModel", "加载已完成目标失败", e)
                _completedGoals.value = emptyList()
            }
        }
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
        loadCompletedGoals()
        loadCountdowns()
    }
    
    fun addGoal(
        title: String,
        category: String,
        targetDate: String?,
        description: String = "",
        progress: Int = 0,
        icon: String? = null
    ) {
        viewModelScope.launch {
            val titleValidation = com.example.funlife.utils.ValidationUtils.validateGoalTitle(title)
            if (titleValidation is com.example.funlife.utils.ValidationResult.Error) {
                android.util.Log.w("GoalViewModel", "Invalid goal title: ${titleValidation.message}")
                return@launch
            }
            val userId = getCurrentUserId()
            if (userId == 0L) return@launch
            val safeProgress = progress.coerceIn(0, 100)
            val goal = Goal(
                userId = userId,
                title = title.trim(),
                description = description.trim(),
                category = category,
                targetDate = targetDate,
                progress = safeProgress,
                isCompleted = safeProgress >= 100,
                createdAt = LocalDateTime.now().toString(),
                completedAt = if (safeProgress >= 100) LocalDateTime.now().toString() else null
            )
            val newId = repository.insertGoal(goal)
            if (!icon.isNullOrBlank() && newId > 0L) {
                com.example.funlife.data.GoalIconStore.set(context, userId, newId.toInt(), icon)
            }
        }
    }

    /** 全字段更新一个已有目标（保留 id/userId/createdAt）。 */
    fun updateGoal(
        original: Goal,
        title: String,
        description: String,
        category: String,
        targetDate: String?,
        progress: Int,
        icon: String? = null
    ) {
        viewModelScope.launch {
            val titleValidation = com.example.funlife.utils.ValidationUtils.validateGoalTitle(title)
            if (titleValidation is com.example.funlife.utils.ValidationResult.Error) return@launch
            val safeProgress = progress.coerceIn(0, 100)
            val now = LocalDateTime.now().toString()
            val nowCompleted = safeProgress >= 100
            val updated = original.copy(
                title = title.trim(),
                description = description.trim(),
                category = category,
                targetDate = targetDate,
                progress = safeProgress,
                isCompleted = nowCompleted,
                completedAt = when {
                    nowCompleted && original.completedAt == null -> now
                    !nowCompleted -> null
                    else -> original.completedAt
                }
            )
            repository.updateGoal(updated)
            if (icon != null) {
                com.example.funlife.data.GoalIconStore.set(context, original.userId, original.id, icon.ifBlank { null })
            }
        }
    }

    /** 快捷调节进度（拖动进度条场景），进度达 100 自动标为已完成。 */
    fun setGoalProgress(goal: Goal, progress: Int) {
        viewModelScope.launch {
            val safe = progress.coerceIn(0, 100)
            val now = LocalDateTime.now().toString()
            val nowCompleted = safe >= 100
            val updated = goal.copy(
                progress = safe,
                isCompleted = nowCompleted,
                completedAt = when {
                    nowCompleted && goal.completedAt == null -> now
                    !nowCompleted -> null
                    else -> goal.completedAt
                }
            )
            repository.updateGoal(updated)
        }
    }

    /** 手动切换 完成/未完成。 */
    fun toggleGoalCompletion(goal: Goal) {
        viewModelScope.launch {
            val newCompleted = !goal.isCompleted
            val now = LocalDateTime.now().toString()
            val updated = goal.copy(
                isCompleted = newCompleted,
                progress = if (newCompleted) 100 else goal.progress.coerceAtMost(99),
                completedAt = if (newCompleted) now else null
            )
            repository.updateGoal(updated)
        }
    }
    
    fun addCountdown(title: String, targetDate: String, category: String, icon: String, color: String, note: String = "") {
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
                note = note,
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

    /** 更新倒数日（保留 id/userId/createdAt）。 */
    fun updateCountdown(
        original: Countdown,
        title: String,
        targetDate: String,
        category: String,
        icon: String,
        color: String,
        note: String
    ) {
        viewModelScope.launch {
            val titleValidation = com.example.funlife.utils.ValidationUtils.validateGoalTitle(title)
            if (titleValidation is com.example.funlife.utils.ValidationResult.Error) return@launch
            val updated = original.copy(
                title = title.trim(),
                targetDate = targetDate,
                category = category,
                icon = icon,
                color = color,
                note = note
            )
            repository.updateCountdown(updated)
        }
    }
    
    // 🔥 修改：不自动删除已达成目标，保留作为历史记录
    // 用户可以在"查看更多"页面查看历史目标
    fun deleteExpiredCountdowns() {
        // 不再自动删除已达成的目标，保留它们作为历史记录
        // 用户可以手动删除或在历史页面查看
    }
}
