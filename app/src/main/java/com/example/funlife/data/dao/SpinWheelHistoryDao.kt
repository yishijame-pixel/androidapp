// SpinWheelHistoryDao.kt - 转盘历史记录数据访问对象
package com.example.funlife.data.dao

import androidx.room.*
import com.example.funlife.data.model.SpinWheelHistory
import kotlinx.coroutines.flow.Flow

@Dao
interface SpinWheelHistoryDao {
    
    @Query("SELECT * FROM spin_wheel_history WHERE userId = :userId ORDER BY timestamp DESC")
    fun getAllHistory(userId: Long): Flow<List<SpinWheelHistory>>
    
    @Query("SELECT * FROM spin_wheel_history WHERE userId = :userId AND templateId = :templateId ORDER BY timestamp DESC")
    fun getHistoryByTemplate(userId: Long, templateId: Int): Flow<List<SpinWheelHistory>>
    
    @Query("SELECT * FROM spin_wheel_history WHERE userId = :userId ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentHistory(userId: Long, limit: Int = 20): Flow<List<SpinWheelHistory>>
    
    @Insert
    suspend fun insert(history: SpinWheelHistory): Long
    
    @Delete
    suspend fun delete(history: SpinWheelHistory)
    
    @Query("DELETE FROM spin_wheel_history WHERE userId = :userId")
    suspend fun deleteAll(userId: Long)
    
    @Query("SELECT COUNT(*) FROM spin_wheel_history WHERE userId = :userId")
    suspend fun getCount(userId: Long): Int
    
    // 🔥 新增：删除最旧的记录
    @Query("DELETE FROM spin_wheel_history WHERE userId = :userId AND id IN (SELECT id FROM spin_wheel_history WHERE userId = :userId ORDER BY timestamp ASC LIMIT :count)")
    suspend fun deleteOldest(userId: Long, count: Int)
    
    // 🔥 新增：清理旧记录（保留最近的 maxRecords 条）
    suspend fun cleanOldHistory(userId: Long, maxRecords: Int = 1000) {
        val count = getCount(userId)
        if (count > maxRecords) {
            deleteOldest(userId, count - maxRecords)
        }
    }
    
    @Query("SELECT SUM(coinCost) FROM spin_wheel_history WHERE userId = :userId")
    suspend fun getTotalCoinCost(userId: Long): Int?
    
    @Query("SELECT SUM(coinReward) FROM spin_wheel_history WHERE userId = :userId")
    suspend fun getTotalCoinReward(userId: Long): Int?
    
    // 获取某个选项的统计
    @Query("SELECT COUNT(*) FROM spin_wheel_history WHERE userId = :userId AND result = :option")
    suspend fun getOptionHitCount(userId: Long, option: String): Int
    
    // 获取某个模板的使用次数
    @Query("SELECT COUNT(*) FROM spin_wheel_history WHERE userId = :userId AND templateId = :templateId")
    suspend fun getTemplateUsageCount(userId: Long, templateId: Int): Int
    
    // 按日期范围筛选历史记录
    @Query("SELECT * FROM spin_wheel_history WHERE userId = :userId AND timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    fun getHistoryByDateRange(userId: Long, startTime: Long, endTime: Long): Flow<List<SpinWheelHistory>>
    
    // 按模式筛选历史记录
    @Query("SELECT * FROM spin_wheel_history WHERE userId = :userId AND mode = :mode ORDER BY timestamp DESC")
    fun getHistoryByMode(userId: Long, mode: String): Flow<List<SpinWheelHistory>>
    
    // 组合筛选：日期范围 + 模式
    @Query("SELECT * FROM spin_wheel_history WHERE userId = :userId AND timestamp BETWEEN :startTime AND :endTime AND mode = :mode ORDER BY timestamp DESC")
    fun getHistoryByDateRangeAndMode(userId: Long, startTime: Long, endTime: Long, mode: String): Flow<List<SpinWheelHistory>>
    
    // 按结果搜索
    @Query("SELECT * FROM spin_wheel_history WHERE userId = :userId AND result LIKE '%' || :searchQuery || '%' ORDER BY timestamp DESC")
    fun searchHistoryByResult(userId: Long, searchQuery: String): Flow<List<SpinWheelHistory>>
}
