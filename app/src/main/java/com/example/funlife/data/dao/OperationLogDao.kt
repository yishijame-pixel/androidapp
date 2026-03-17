// OperationLogDao.kt - 操作日志数据访问对象
package com.example.funlife.data.dao

import androidx.room.*
import com.example.funlife.data.model.OperationLog
import kotlinx.coroutines.flow.Flow

@Dao
interface OperationLogDao {
    
    @Query("SELECT * FROM operation_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentLogs(limit: Int = 100): Flow<List<OperationLog>>
    
    @Query("SELECT * FROM operation_logs WHERE operation = :operation ORDER BY timestamp DESC LIMIT :limit")
    fun getLogsByOperation(operation: String, limit: Int = 100): Flow<List<OperationLog>>
    
    @Query("SELECT * FROM operation_logs WHERE userId = :userId ORDER BY timestamp DESC LIMIT :limit")
    fun getLogsByUser(userId: Long, limit: Int = 100): Flow<List<OperationLog>>
    
    @Query("SELECT * FROM operation_logs WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    fun getLogsByDateRange(startTime: Long, endTime: Long): Flow<List<OperationLog>>
    
    @Query("SELECT * FROM operation_logs WHERE result = :result ORDER BY timestamp DESC LIMIT :limit")
    fun getLogsByResult(result: String, limit: Int = 100): Flow<List<OperationLog>>
    
    @Insert
    suspend fun insert(log: OperationLog)
    
    @Query("DELETE FROM operation_logs WHERE timestamp < :beforeTime")
    suspend fun deleteOldLogs(beforeTime: Long)
    
    @Query("DELETE FROM operation_logs WHERE id IN (SELECT id FROM operation_logs ORDER BY timestamp ASC LIMIT :count)")
    suspend fun deleteOldest(count: Int)
    
    @Query("SELECT COUNT(*) FROM operation_logs")
    suspend fun getCount(): Int
    
    // 🔥 自动清理旧日志（保留最近30天或最多10000条）
    suspend fun cleanOldLogs(maxRecords: Int = 10000, maxDays: Int = 30) {
        // 删除超过30天的日志
        val thirtyDaysAgo = System.currentTimeMillis() - (maxDays * 24 * 60 * 60 * 1000L)
        deleteOldLogs(thirtyDaysAgo)
        
        // 如果还是超过最大记录数，删除最旧的
        val count = getCount()
        if (count > maxRecords) {
            deleteOldest(count - maxRecords)
        }
    }
}
