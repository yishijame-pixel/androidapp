// GameHistoryRepository.kt - 游戏历史仓库
package com.example.funlife.repository

import com.example.funlife.data.dao.GameHistoryDao
import com.example.funlife.data.model.GameHistory
import kotlinx.coroutines.flow.Flow

class GameHistoryRepository(private val gameHistoryDao: GameHistoryDao) {
    
    val allHistory: Flow<List<GameHistory>> = gameHistoryDao.getAllHistory()
    
    fun getHistoryByType(gameType: String): Flow<List<GameHistory>> {
        return gameHistoryDao.getHistoryByType(gameType)
    }
    
    fun getHistoryByPlayer(playerName: String): Flow<List<GameHistory>> {
        return gameHistoryDao.getHistoryByPlayer(playerName)
    }
    
    fun getRecentHistory(limit: Int = 20): Flow<List<GameHistory>> {
        return gameHistoryDao.getRecentHistory(limit)
    }
    
    suspend fun insert(history: GameHistory) {
        gameHistoryDao.insertHistory(history)
    }
    
    suspend fun delete(history: GameHistory) {
        gameHistoryDao.deleteHistory(history)
    }
    
    suspend fun clearAll() {
        gameHistoryDao.clearAllHistory()
    }
    
    suspend fun deleteHistoryBefore(beforeDate: String) {
        gameHistoryDao.deleteHistoryBefore(beforeDate)
    }
    
    suspend fun getCountByType(gameType: String): Int {
        return gameHistoryDao.getCountByType(gameType)
    }
    
    suspend fun getTotalPlayers(): Int {
        return gameHistoryDao.getTotalPlayers()
    }
}
