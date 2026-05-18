package com.example.funlife.data.dao

import androidx.room.*
import com.example.funlife.data.model.Bill
import kotlinx.coroutines.flow.Flow

@Dao
interface BillDao {
    @Insert
    suspend fun insert(bill: Bill): Long

    @Update
    suspend fun update(bill: Bill)

    @Delete
    suspend fun delete(bill: Bill)

    @Query("SELECT * FROM bills WHERE userId = :userId ORDER BY timestamp DESC")
    fun getAllBills(userId: Long): Flow<List<Bill>>

    @Query("SELECT * FROM bills WHERE userId = :userId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentBills(userId: Long, limit: Int = 30): List<Bill>

    @Query("SELECT * FROM bills WHERE userId = :userId AND timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    suspend fun getBillsByDateRange(userId: Long, startTime: Long, endTime: Long): List<Bill>

    @Query("SELECT * FROM bills WHERE userId = :userId AND category = :category ORDER BY timestamp DESC")
    suspend fun getBillsByCategory(userId: Long, category: String): List<Bill>

    @Query("SELECT SUM(amount) FROM bills WHERE userId = :userId AND timestamp >= :startTime AND timestamp <= :endTime")
    suspend fun getTotalAmount(userId: Long, startTime: Long, endTime: Long): Double?

    @Query("SELECT COUNT(*) FROM bills WHERE userId = :userId AND category = :category AND timestamp >= :startTime")
    suspend fun getCategoryCount(userId: Long, category: String, startTime: Long): Int

    @Query("SELECT * FROM bills WHERE id = :billId")
    suspend fun getBillById(billId: Long): Bill?
}
