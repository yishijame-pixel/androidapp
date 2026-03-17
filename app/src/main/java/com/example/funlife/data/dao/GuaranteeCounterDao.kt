// GuaranteeCounterDao.kt - 保底计数器数据访问对象
package com.example.funlife.data.dao

import androidx.room.*
import com.example.funlife.data.model.GuaranteeCounter
import kotlinx.coroutines.flow.Flow

@Dao
interface GuaranteeCounterDao {
    
    @Query("SELECT * FROM guarantee_counter WHERE userId = :userId AND isEnabled = 1")
    fun getAllEnabledCounters(userId: Long): Flow<List<GuaranteeCounter>>
    
    @Query("SELECT * FROM guarantee_counter WHERE userId = :userId AND optionText = :optionText LIMIT 1")
    suspend fun getCounterByOption(userId: Long, optionText: String): GuaranteeCounter?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(counter: GuaranteeCounter)
    
    @Update
    suspend fun update(counter: GuaranteeCounter)
    
    @Delete
    suspend fun delete(counter: GuaranteeCounter)
    
    @Query("DELETE FROM guarantee_counter WHERE userId = :userId")
    suspend fun deleteAll(userId: Long)
    
    // 增加计数
    @Query("UPDATE guarantee_counter SET currentCount = currentCount + 1, lastUpdated = :timestamp WHERE userId = :userId AND optionText = :optionText")
    suspend fun incrementCounter(userId: Long, optionText: String, timestamp: Long = System.currentTimeMillis())
    
    // 重置计数
    @Query("UPDATE guarantee_counter SET currentCount = 0, lastUpdated = :timestamp WHERE userId = :userId AND optionText = :optionText")
    suspend fun resetCounter(userId: Long, optionText: String, timestamp: Long = System.currentTimeMillis())
    
    // 批量重置所有计数器
    @Query("UPDATE guarantee_counter SET currentCount = 0, lastUpdated = :timestamp WHERE userId = :userId")
    suspend fun resetAllCounters(userId: Long, timestamp: Long = System.currentTimeMillis())
}
