// SpinWheelHistoryRepository.kt - 转盘历史记录仓库
package com.example.funlife.repository

import com.example.funlife.data.dao.SpinWheelHistoryDao
import com.example.funlife.data.model.SpinWheelHistory
import kotlinx.coroutines.flow.Flow

class SpinWheelHistoryRepository(private val historyDao: SpinWheelHistoryDao) {
    
    fun getAllHistory(userId: Long): Flow<List<SpinWheelHistory>> = historyDao.getAllHistory(userId)
    
    fun getHistoryByTemplate(userId: Long, templateId: Int): Flow<List<SpinWheelHistory>> = 
        historyDao.getHistoryByTemplate(userId, templateId)
    
    fun getRecentHistory(userId: Long, limit: Int = 20): Flow<List<SpinWheelHistory>> = 
        historyDao.getRecentHistory(userId, limit)
    
    suspend fun getCount(userId: Long): Int = historyDao.getCount(userId)
    
    suspend fun getTotalCoinCost(userId: Long): Int = historyDao.getTotalCoinCost(userId) ?: 0
    
    suspend fun getTotalCoinReward(userId: Long): Int = historyDao.getTotalCoinReward(userId) ?: 0
    
    suspend fun insert(history: SpinWheelHistory) {
        historyDao.insert(history)
    }
    
    suspend fun delete(history: SpinWheelHistory) {
        historyDao.delete(history)
    }
    
    suspend fun deleteAll(userId: Long) {
        historyDao.deleteAll(userId)
    }
    
    // 筛选功能
    fun getHistoryByDateRange(userId: Long, startTime: Long, endTime: Long): Flow<List<SpinWheelHistory>> = 
        historyDao.getHistoryByDateRange(userId, startTime, endTime)
    
    fun getHistoryByMode(userId: Long, mode: String): Flow<List<SpinWheelHistory>> = 
        historyDao.getHistoryByMode(userId, mode)
    
    fun getHistoryByDateRangeAndMode(userId: Long, startTime: Long, endTime: Long, mode: String): Flow<List<SpinWheelHistory>> = 
        historyDao.getHistoryByDateRangeAndMode(userId, startTime, endTime, mode)
    
    fun searchHistoryByResult(userId: Long, searchQuery: String): Flow<List<SpinWheelHistory>> = 
        historyDao.searchHistoryByResult(userId, searchQuery)
    
    // 🔥 新增：定期清理旧记录
    suspend fun cleanOldHistory(userId: Long, maxRecords: Int = 1000) {
        historyDao.cleanOldHistory(userId, maxRecords)
    }
}
