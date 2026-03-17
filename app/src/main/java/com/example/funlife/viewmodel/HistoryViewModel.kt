// HistoryViewModel.kt - 历史记录视图模型
package com.example.funlife.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.funlife.data.database.AppDatabase
import com.example.funlife.data.model.GameHistory
import com.example.funlife.repository.GameHistoryRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class HistoryViewModel(application: Application) : AndroidViewModel(application) {
    
    private val context = application.applicationContext
    private val repository: GameHistoryRepository
    val recentHistory: StateFlow<List<GameHistory>>
    
    // 🔥 获取当前用户ID
    private fun getCurrentUserId(): Long {
        val sessionManager = com.example.funlife.utils.UserSessionManager(context)
        return sessionManager.getCurrentUserId().takeIf { it > 0 } ?: 0L
    }
    
    init {
        val database = AppDatabase.getDatabase(application)
        val historyDao = database.gameHistoryDao()
        repository = GameHistoryRepository(historyDao)
        
        recentHistory = repository.getRecentHistory(getCurrentUserId(), 50)
            .catch { e ->
                android.util.Log.e("HistoryViewModel", "Error loading history", e)
                emit(emptyList())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
    }
    
    // 添加历史记录
    fun addHistory(history: GameHistory) {
        viewModelScope.launch {
            repository.insert(history)
        }
    }
    
    // 删除历史记录
    fun deleteHistory(history: GameHistory) {
        viewModelScope.launch {
            repository.delete(history)
        }
    }
    
    // 清空所有历史
    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAll(getCurrentUserId())
        }
    }
}
