// SpinWheelHistoryDao.kt - 转盘历史记录数据访问对象
package com.example.funlife.data.dao

import androidx.room.*
import com.example.funlife.data.model.SpinWheelHistory
import kotlinx.coroutines.flow.Flow

@Dao
interface SpinWheelHistoryDao {
    
    @Query("SELECT * FROM spin_wheel_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<SpinWheelHistory>>
    
    @Query("SELECT * FROM spin_wheel_history WHERE templateId = :templateId ORDER BY timestamp DESC")
    fun getHistoryByTemplate(templateId: Int): Flow<List<SpinWheelHistory>>
    
    @Query("SELECT * FROM spin_wheel_history ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentHistory(limit: Int = 20): Flow<List<SpinWheelHistory>>
    
    @Insert
    suspend fun insert(history: SpinWheelHistory): Long
    
    @Delete
    suspend fun delete(history: SpinWheelHistory)
    
    @Query("DELETE FROM spin_wheel_history")
    suspend fun deleteAll()
    
    @Query("SELECT COUNT(*) FROM spin_wheel_history")
    suspend fun getCount(): Int
    
    @Query("SELECT SUM(coinCost) FROM spin_wheel_history")
    suspend fun getTotalCoinCost(): Int?
    
    @Query("SELECT SUM(coinReward) FROM spin_wheel_history")
    suspend fun getTotalCoinReward(): Int?
    
    // 获取某个选项的统计
    @Query("SELECT COUNT(*) FROM spin_wheel_history WHERE result = :option")
    suspend fun getOptionHitCount(option: String): Int
    
    // 获取某个模板的使用次数
    @Query("SELECT COUNT(*) FROM spin_wheel_history WHERE templateId = :templateId")
    suspend fun getTemplateUsageCount(templateId: Int): Int
    
    // 按日期范围筛选历史记录
    @Query("SELECT * FROM spin_wheel_history WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    fun getHistoryByDateRange(startTime: Long, endTime: Long): Flow<List<SpinWheelHistory>>
    
    // 按模式筛选历史记录
    @Query("SELECT * FROM spin_wheel_history WHERE mode = :mode ORDER BY timestamp DESC")
    fun getHistoryByMode(mode: String): Flow<List<SpinWheelHistory>>
    
    // 组合筛选：日期范围 + 模式
    @Query("SELECT * FROM spin_wheel_history WHERE timestamp BETWEEN :startTime AND :endTime AND mode = :mode ORDER BY timestamp DESC")
    fun getHistoryByDateRangeAndMode(startTime: Long, endTime: Long, mode: String): Flow<List<SpinWheelHistory>>
    
    // 按结果搜索
    @Query("SELECT * FROM spin_wheel_history WHERE result LIKE '%' || :searchQuery || '%' ORDER BY timestamp DESC")
    fun searchHistoryByResult(searchQuery: String): Flow<List<SpinWheelHistory>>
}
