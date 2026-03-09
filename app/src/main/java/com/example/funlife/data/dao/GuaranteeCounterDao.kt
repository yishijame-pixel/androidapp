// GuaranteeCounterDao.kt - 保底计数器数据访问对象
package com.example.funlife.data.dao

import androidx.room.*
import com.example.funlife.data.model.GuaranteeCounter
import kotlinx.coroutines.flow.Flow

@Dao
interface GuaranteeCounterDao {
    
    @Query("SELECT * FROM guarantee_counter WHERE isEnabled = 1")
    fun getAllEnabledCounters(): Flow<List<GuaranteeCounter>>
    
    @Query("SELECT * FROM guarantee_counter WHERE optionText = :optionText LIMIT 1")
    suspend fun getCounterByOption(optionText: String): GuaranteeCounter?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(counter: GuaranteeCounter)
    
    @Update
    suspend fun update(counter: GuaranteeCounter)
    
    @Delete
    suspend fun delete(counter: GuaranteeCounter)
    
    @Query("DELETE FROM guarantee_counter")
    suspend fun deleteAll()
    
    // 增加计数
    @Query("UPDATE guarantee_counter SET currentCount = currentCount + 1, lastUpdated = :timestamp WHERE optionText = :optionText")
    suspend fun incrementCounter(optionText: String, timestamp: Long = System.currentTimeMillis())
    
    // 重置计数
    @Query("UPDATE guarantee_counter SET currentCount = 0, lastUpdated = :timestamp WHERE optionText = :optionText")
    suspend fun resetCounter(optionText: String, timestamp: Long = System.currentTimeMillis())
    
    // 批量重置所有计数器
    @Query("UPDATE guarantee_counter SET currentCount = 0, lastUpdated = :timestamp")
    suspend fun resetAllCounters(timestamp: Long = System.currentTimeMillis())
}
