// GoalRepository.kt - 目标仓库
package com.example.funlife.repository

import com.example.funlife.data.dao.GoalDao
import com.example.funlife.data.model.Goal
import com.example.funlife.data.model.Countdown
import kotlinx.coroutines.flow.Flow

class GoalRepository(private val goalDao: GoalDao) {
    
    fun getActiveGoals(userId: Long): Flow<List<Goal>> = goalDao.getActiveGoals(userId)
    fun getCompletedGoals(userId: Long): Flow<List<Goal>> = goalDao.getCompletedGoals(userId)
    fun getAllCountdowns(userId: Long): Flow<List<Countdown>> = goalDao.getAllCountdowns(userId)
    
    suspend fun insertGoal(goal: Goal): Long = goalDao.insertGoal(goal)
    
    suspend fun updateGoal(goal: Goal) = goalDao.updateGoal(goal)
    
    suspend fun deleteGoal(goal: Goal) = goalDao.deleteGoal(goal)
    
    suspend fun insertCountdown(countdown: Countdown) = goalDao.insertCountdown(countdown)
    
    suspend fun updateCountdown(countdown: Countdown) = goalDao.updateCountdown(countdown)
    
    suspend fun deleteCountdown(countdown: Countdown) = goalDao.deleteCountdown(countdown)
}
