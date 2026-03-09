// SpinWheelHistoryRepository.kt - 转盘历史记录仓库
package com.example.funlife.repository

import com.example.funlife.data.dao.SpinWheelHistoryDao
import com.example.funlife.data.model.SpinWheelHistory
import kotlinx.coroutines.flow.Flow

class SpinWheelHistoryRepository(private val historyDao: SpinWheelHistoryDao) {
    
    fun getAllHistory(): Flow<List<SpinWheelHistory>> = historyDao.getAllHistory()
    
    fun getHistoryByTemplate(templateId: Int): Flow<List<SpinWheelHistory>> = 
        historyDao.getHistoryByTemplate(templateId)
    
    fun getRecentHistory(limit: Int = 20): Flow<List<SpinWheelHistory>> = 
        historyDao.getRecentHistory(limit)
    
    suspend fun getCount(): Int = historyDao.getCount()
    
    suspend fun getTotalCoinCost(): Int = historyDao.getTotalCoinCost() ?: 0
    
    suspend fun getTotalCoinReward(): Int = historyDao.getTotalCoinReward() ?: 0
    
    suspend fun insert(history: SpinWheelHistory) {
        historyDao.insert(history)
    }
    
    suspend fun delete(history: SpinWheelHistory) {
        historyDao.delete(history)
    }
    
    suspend fun deleteAll() {
        historyDao.deleteAll()
    }
    
    // 筛选功能
    fun getHistoryByDateRange(startTime: Long, endTime: Long): Flow<List<SpinWheelHistory>> = 
        historyDao.getHistoryByDateRange(startTime, endTime)
    
    fun getHistoryByMode(mode: String): Flow<List<SpinWheelHistory>> = 
        historyDao.getHistoryByMode(mode)
    
    fun getHistoryByDateRangeAndMode(startTime: Long, endTime: Long, mode: String): Flow<List<SpinWheelHistory>> = 
        historyDao.getHistoryByDateRangeAndMode(startTime, endTime, mode)
    
    fun searchHistoryByResult(searchQuery: String): Flow<List<SpinWheelHistory>> = 
        historyDao.searchHistoryByResult(searchQuery)
}
