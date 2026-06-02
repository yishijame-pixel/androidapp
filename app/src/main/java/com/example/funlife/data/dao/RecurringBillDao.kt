// RecurringBillDao.kt — 🔒 强制 userId 参数
package com.example.funlife.data.dao

import androidx.room.*
import com.example.funlife.data.model.RecurringBill
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringBillDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(rb: RecurringBill): Long

    @Update
    suspend fun update(rb: RecurringBill)

    @Delete
    suspend fun delete(rb: RecurringBill)

    @Query("SELECT * FROM recurring_bills WHERE userId = :userId ORDER BY isActive DESC, sortOrder ASC, id ASC")
    fun getAll(userId: Long): Flow<List<RecurringBill>>

    @Query("SELECT * FROM recurring_bills WHERE userId = :userId AND isActive = 1 ORDER BY sortOrder ASC, id ASC")
    suspend fun getActive(userId: Long): List<RecurringBill>

    @Query("SELECT * FROM recurring_bills WHERE userId = :userId AND id = :id LIMIT 1")
    suspend fun getById(userId: Long, id: Long): RecurringBill?

    @Query("UPDATE recurring_bills SET isActive = :active, updatedAt = :now WHERE userId = :userId AND id = :id")
    suspend fun setActive(userId: Long, id: Long, active: Boolean, now: Long = System.currentTimeMillis())

    @Query("UPDATE recurring_bills SET lastGeneratedAt = :ts, updatedAt = :now WHERE userId = :userId AND id = :id")
    suspend fun markGenerated(userId: Long, id: Long, ts: Long, now: Long = System.currentTimeMillis())
}
