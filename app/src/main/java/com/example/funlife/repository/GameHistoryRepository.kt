// GameHistoryRepository.kt - 游戏历史仓库
package com.example.funlife.repository

import com.example.funlife.data.dao.GameHistoryDao
import com.example.funlife.data.model.GameHistory
import kotlinx.coroutines.flow.Flow

class GameHistoryRepository(private val gameHistoryDao: GameHistoryDao) {
    
    fun getAllHistory(userId: Long): Flow<List<GameHistory>> = gameHistoryDao.getAllHistory(userId)
    
    fun getHistoryByType(userId: Long, gameType: String): Flow<List<GameHistory>> {
        return gameHistoryDao.getHistoryByType(userId, gameType)
    }
    
    fun getHistoryByPlayer(userId: Long, playerName: String): Flow<List<GameHistory>> {
        return gameHistoryDao.getHistoryByPlayer(userId, playerName)
    }
    
    fun getRecentHistory(userId: Long, limit: Int = 20): Flow<List<GameHistory>> {
        return gameHistoryDao.getRecentHistory(userId, limit)
    }
    
    suspend fun insert(history: GameHistory) {
        gameHistoryDao.insertHistory(history)
    }
    
    suspend fun delete(history: GameHistory) {
        gameHistoryDao.deleteHistory(history)
    }
    
    suspend fun clearAll(userId: Long) {
        gameHistoryDao.clearAllHistory(userId)
    }
    
    suspend fun deleteHistoryBefore(userId: Long, beforeDate: String) {
        gameHistoryDao.deleteHistoryBefore(userId, beforeDate)
    }
    
    suspend fun getCountByType(userId: Long, gameType: String): Int {
        return gameHistoryDao.getCountByType(userId, gameType)
    }
    
    suspend fun getTotalPlayers(userId: Long): Int {
        return gameHistoryDao.getTotalPlayers(userId)
    }

}
