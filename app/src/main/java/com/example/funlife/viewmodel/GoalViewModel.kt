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
    val activeGoals: StateFlow<List<Goal>>
    val countdowns: StateFlow<List<Countdown>>
    
    init {
        val database = AppDatabase.getDatabase(application)
        repository = GoalRepository(database.goalDao())
        activeGoals = repository.activeGoals.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
        countdowns = repository.allCountdowns.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }
    
    fun addGoal(title: String, category: String, targetDate: String?) {
        viewModelScope.launch {
            val goal = Goal(
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
            val countdown = Countdown(
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
