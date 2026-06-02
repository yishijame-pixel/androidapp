// GoalDao.kt - 目标数据访问对象
package com.example.funlife.data.dao

import androidx.room.*
import com.example.funlife.data.model.Goal
import com.example.funlife.data.model.Countdown
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {
    
    @Query("SELECT * FROM goals WHERE userId = :userId AND isCompleted = 0 ORDER BY createdAt DESC")
    fun getActiveGoals(userId: Long): Flow<List<Goal>>
    
    @Query("SELECT * FROM goals WHERE userId = :userId AND isCompleted = 1 ORDER BY completedAt DESC")
    fun getCompletedGoals(userId: Long): Flow<List<Goal>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: Goal): Long
    
    @Update
    suspend fun updateGoal(goal: Goal)
    
    @Delete
    suspend fun deleteGoal(goal: Goal)
    
    @Query("SELECT * FROM countdowns WHERE userId = :userId ORDER BY targetDate ASC")
    fun getAllCountdowns(userId: Long): Flow<List<Countdown>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCountdown(countdown: Countdown)
    
    @Update
    suspend fun updateCountdown(countdown: Countdown)
    
    @Delete
    suspend fun deleteCountdown(countdown: Countdown)
}
