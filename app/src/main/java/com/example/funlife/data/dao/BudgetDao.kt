// BudgetDao.kt — 预算 DAO（Phase 2B）
// 🔒 所有查询带 userId 参数（DEVELOPMENT_PRINCIPLES §1.1）
package com.example.funlife.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.funlife.data.model.Budget
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(budget: Budget): Long

    @Update
    suspend fun update(budget: Budget)

    @Delete
    suspend fun delete(budget: Budget)

    @Query("SELECT * FROM budgets WHERE userId = :userId AND isActive = 1 ORDER BY sortOrder ASC, id ASC")
    fun getActiveBudgets(userId: Long): Flow<List<Budget>>

    @Query("SELECT * FROM budgets WHERE userId = :userId ORDER BY isActive DESC, sortOrder ASC, id ASC")
    fun getAllBudgets(userId: Long): Flow<List<Budget>>

    @Query("SELECT * FROM budgets WHERE userId = :userId AND id = :id LIMIT 1")
    suspend fun getById(userId: Long, id: Long): Budget?

    @Query("""
        SELECT * FROM budgets WHERE userId = :userId
            AND scope = :scope AND period = :period
            AND (targetKey IS :targetKey OR targetKey = :targetKey)
        LIMIT 1
    """)
    suspend fun findOne(
        userId: Long,
        scope: String,
        period: String,
        targetKey: String?
    ): Budget?

    @Query("UPDATE budgets SET isActive = :active, updatedAt = :now WHERE userId = :userId AND id = :id")
    suspend fun setActive(userId: Long, id: Long, active: Boolean, now: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM budgets WHERE userId = :userId")
    suspend fun count(userId: Long): Int
}
