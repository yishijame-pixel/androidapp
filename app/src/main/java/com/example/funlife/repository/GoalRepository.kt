// GoalRepository.kt - 目标仓库
package com.example.funlife.repository

import com.example.funlife.data.dao.GoalDao
import com.example.funlife.data.model.Goal
import com.example.funlife.data.model.Countdown
import kotlinx.coroutines.flow.Flow

class GoalRepository(private val goalDao: GoalDao) {
    
    val activeGoals: Flow<List<Goal>> = goalDao.getActiveGoals()
    val completedGoals: Flow<List<Goal>> = goalDao.getCompletedGoals()
    val allCountdowns: Flow<List<Countdown>> = goalDao.getAllCountdowns()
    
    suspend fun insertGoal(goal: Goal) = goalDao.insertGoal(goal)
    
    suspend fun updateGoal(goal: Goal) = goalDao.updateGoal(goal)
    
    suspend fun deleteGoal(goal: Goal) = goalDao.deleteGoal(goal)
    
    suspend fun insertCountdown(countdown: Countdown) = goalDao.insertCountdown(countdown)
    
    suspend fun updateCountdown(countdown: Countdown) = goalDao.updateCountdown(countdown)
    
    suspend fun deleteCountdown(countdown: Countdown) = goalDao.deleteCountdown(countdown)
}
