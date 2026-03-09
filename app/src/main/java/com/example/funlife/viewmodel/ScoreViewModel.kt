// ScoreViewModel.kt - 计分视图模型
package com.example.funlife.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.funlife.data.database.AppDatabase
import com.example.funlife.data.model.Player
import com.example.funlife.repository.PlayerRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ScoreViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository: PlayerRepository
    val players: StateFlow<List<Player>>
    
    init {
        val database = AppDatabase.getDatabase(application)
        val playerDao = database.playerDao()
        repository = PlayerRepository(playerDao)
        
        players = repository.allPlayers.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }
    
    // 添加玩家
    fun addPlayer(name: String) {
        viewModelScope.launch {
            val player = Player(name = name, score = 0)
            repository.insert(player)
            Log.d("ScoreViewModel", "Added player: $name")
        }
    }
    
    // 增加分数 - 使用 player.id 确保更新正确的玩家
    fun increaseScore(player: Player, points: Int = 1) {
        viewModelScope.launch {
            Log.d("ScoreViewModel", "=== INCREASE SCORE START ===")
            Log.d("ScoreViewModel", "Player object: $player")
            Log.d("ScoreViewModel", "Player ID: ${player.id}, Name: ${player.name}, Current Score: ${player.score}")
            Log.d("ScoreViewModel", "New score will be: ${player.score + points}")
            
            val updatedPlayer = player.copy(score = player.score + points)
            Log.d("ScoreViewModel", "Updated player object: $updatedPlayer")
            
            repository.update(updatedPlayer)
            Log.d("ScoreViewModel", "=== INCREASE SCORE END ===")
        }
    }
    
    // 减少分数 - 使用 player.id 确保更新正确的玩家
    fun decreaseScore(player: Player, points: Int = 1) {
        viewModelScope.launch {
            val newScore = maxOf(0, player.score - points)
            Log.d("ScoreViewModel", "Decreasing score for player: ${player.name} (id=${player.id}) from ${player.score} to $newScore")
            val updatedPlayer = player.copy(score = newScore)
            repository.update(updatedPlayer)
        }
    }
    
    // 删除玩家
    fun deletePlayer(player: Player) {
        viewModelScope.launch {
            Log.d("ScoreViewModel", "Deleting player: ${player.name} (id=${player.id})")
            repository.delete(player)
        }
    }
    
    // 重置所有分数
    fun resetAllScores() {
        viewModelScope.launch {
            Log.d("ScoreViewModel", "Resetting all scores")
            repository.resetAllScores()
        }
    }
}
