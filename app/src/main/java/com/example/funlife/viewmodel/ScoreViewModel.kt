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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class ScoreViewModel(application: Application) : AndroidViewModel(application) {
    
    private val context = application.applicationContext
    private val repository: PlayerRepository
    val players: StateFlow<List<Player>>
    
    // 🔥 胜利记录相关
    private val victoryRecordDao = AppDatabase.getDatabase(application).playerVictoryRecordDao()
    val victoryRecords: StateFlow<List<com.example.funlife.data.model.PlayerVictoryRecord>>
    
    // 🔥 操作记录相关
    private val scoreOperationDao = AppDatabase.getDatabase(application).scoreOperationDao()
    private var currentGameSessionId: Long = 0L  // 当前游戏会话ID
    private val _currentSessionOperations = MutableStateFlow<List<com.example.funlife.data.model.ScoreOperation>>(emptyList())
    val currentSessionOperations: StateFlow<List<com.example.funlife.data.model.ScoreOperation>> = _currentSessionOperations
    
    // 🔥 获取当前用户ID
    private fun getCurrentUserId(): Long {
        val sessionManager = com.example.funlife.utils.UserSessionManager(context)
        val userId = sessionManager.getCurrentUserId()
        Log.d("ScoreViewModel", "getCurrentUserId: raw=$userId, isLoggedIn=${sessionManager.isLoggedIn()}")
        // 如果未登录或userId无效，使用默认值0（表示游客模式）
        return if (userId > 0) userId else 0L
    }
    
    init {
        val database = AppDatabase.getDatabase(application)
        val playerDao = database.playerDao()
        repository = PlayerRepository(playerDao)
        
        players = repository.getAllPlayers(getCurrentUserId())
            .catch { e ->
                android.util.Log.e("ScoreViewModel", "Error loading players", e)
                emit(emptyList())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
        
        // 初始化胜利记录流（仅当前用户）
        victoryRecords = victoryRecordDao.getAllRecords(getCurrentUserId())
            .catch { e ->
                android.util.Log.e("ScoreViewModel", "Error loading victory records", e)
                emit(emptyList())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
    }
    
    // 添加玩家
    fun addPlayer(name: String, avatar: String = "tx_1") {
        viewModelScope.launch {
            val userId = getCurrentUserId()
            Log.d("ScoreViewModel", "=== ADD PLAYER START ===")
            Log.d("ScoreViewModel", "Current userId: $userId")
            Log.d("ScoreViewModel", "Player name: $name, avatar: $avatar")
            
            // 🔥 检查玩家数量限制（最多4个）
            val currentPlayers = players.value
            if (currentPlayers.size >= 4) {
                Log.w("ScoreViewModel", "Cannot add player: maximum 4 players reached")
                return@launch
            }
            
            val player = Player(userId = userId, name = name, score = 0, avatar = avatar)
            Log.d("ScoreViewModel", "Created player object: $player")
            
            try {
                repository.insert(player)
                Log.d("ScoreViewModel", "Player inserted successfully")
            } catch (e: Exception) {
                Log.e("ScoreViewModel", "Error inserting player", e)
            }
            
            Log.d("ScoreViewModel", "=== ADD PLAYER END ===")
        }
    }
    
    // 增加分数 - 使用 player.id 确保更新正确的玩家
    fun increaseScore(player: Player, points: Int = 1) {
        viewModelScope.launch {
            Log.d("ScoreViewModel", "=== INCREASE SCORE START ===")
            Log.d("ScoreViewModel", "Player object: $player")
            Log.d("ScoreViewModel", "Player ID: ${player.id}, Name: ${player.name}, Current Score: ${player.score}")
            Log.d("ScoreViewModel", "New score will be: ${player.score + points}")
            
            val scoreBefore = player.score
            val scoreAfter = player.score + points
            val updatedPlayer = player.copy(score = scoreAfter)
            Log.d("ScoreViewModel", "Updated player object: $updatedPlayer")
            
            repository.update(updatedPlayer)
            
            // 记录操作
            recordOperation(player, points, scoreBefore, scoreAfter)
            
            Log.d("ScoreViewModel", "=== INCREASE SCORE END ===")
        }
    }
    
    // 减少分数 - 使用 player.id 确保更新正确的玩家（允许负数）
    fun decreaseScore(player: Player, points: Int = 1) {
        viewModelScope.launch {
            val scoreBefore = player.score
            val scoreAfter = player.score - points  // 移除maxOf限制，允许负数
            Log.d("ScoreViewModel", "Decreasing score for player: ${player.name} (id=${player.id}) from $scoreBefore to $scoreAfter")
            val updatedPlayer = player.copy(score = scoreAfter)
            repository.update(updatedPlayer)
            
            // 记录操作
            recordOperation(player, -points, scoreBefore, scoreAfter)
        }
    }
    
    // 记录操作
    private suspend fun recordOperation(player: Player, operation: Int, scoreBefore: Int, scoreAfter: Int) {
        val userId = getCurrentUserId()
        if (currentGameSessionId == 0L) {
            // 如果还没有会话ID，创建一个新的
            val latestId = scoreOperationDao.getLatestSessionId(userId) ?: 0L
            currentGameSessionId = latestId + 1
            Log.d("ScoreViewModel", "Auto-started new game session: $currentGameSessionId")
        }
        
        val scoreOperation = com.example.funlife.data.model.ScoreOperation(
            userId = userId,
            gameSessionId = currentGameSessionId,
            playerId = player.id,
            playerName = player.name,
            playerAvatar = player.avatar,
            operation = operation,
            scoreBefore = scoreBefore,
            scoreAfter = scoreAfter
        )
        
        Log.d("ScoreViewModel", "=== RECORDING OPERATION ===")
        Log.d("ScoreViewModel", "Session ID: $currentGameSessionId")
        Log.d("ScoreViewModel", "Player: ${player.name} (id=${player.id})")
        Log.d("ScoreViewModel", "Operation: $operation")
        Log.d("ScoreViewModel", "Score: $scoreBefore -> $scoreAfter")
        
        try {
            scoreOperationDao.insert(scoreOperation)
            Log.d("ScoreViewModel", "Operation recorded successfully")
        } catch (e: Exception) {
            Log.e("ScoreViewModel", "Failed to record operation", e)
        }
        
        Log.d("ScoreViewModel", "=== OPERATION RECORDED ===")
    }
    
    // 开始新游戏会话
    fun startNewGameSession() {
        viewModelScope.launch {
            val latestId = scoreOperationDao.getLatestSessionId(getCurrentUserId()) ?: 0L
            currentGameSessionId = latestId + 1
            Log.d("ScoreViewModel", "=== START NEW GAME SESSION ===")
            Log.d("ScoreViewModel", "Latest session ID: $latestId")
            Log.d("ScoreViewModel", "New session ID: $currentGameSessionId")
            
            // 清空当前操作记录（不再需要监听Flow，因为对话框会直接查询数据库）
            _currentSessionOperations.value = emptyList()
            
            Log.d("ScoreViewModel", "=== GAME SESSION STARTED ===")
        }
    }
    
    // 获取当前会话ID
    fun getCurrentSessionId(): Long = currentGameSessionId
    
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
            repository.resetAllScores(getCurrentUserId())
        }
    }
    
    // 删除所有玩家
    fun deleteAllPlayers() {
        viewModelScope.launch {
            Log.d("ScoreViewModel", "Deleting all players")
            repository.deleteAllPlayers(getCurrentUserId())
        }
    }
    
    // 🔥 记录玩家胜利
    fun recordVictory(playerName: String, avatar: String) {
        viewModelScope.launch {
            Log.d("ScoreViewModel", "Recording victory for player: $playerName")
            victoryRecordDao.recordVictory(getCurrentUserId(), playerName, avatar)
        }
    }
    
    // 🔥 清空胜利记录（仅当前用户）
    fun clearVictoryRecords() {
        viewModelScope.launch {
            Log.d("ScoreViewModel", "Clearing victory records (current user only)")
            victoryRecordDao.deleteAll(getCurrentUserId())
        }
    }
    
    // 🔥 清空指定玩家的操作记录
    fun clearPlayerOperations(playerId: Int) {
        viewModelScope.launch {
            Log.d("ScoreViewModel", "Clearing operations for player ID: $playerId")
            scoreOperationDao.deleteByPlayerId(getCurrentUserId(), playerId)
        }
    }
    
    // 🔥 清空操作记录（仅当前用户）
    fun clearAllOperations() {
        viewModelScope.launch {
            Log.d("ScoreViewModel", "Clearing all operations (current user only)")
            scoreOperationDao.deleteAll(getCurrentUserId())
        }
    }
}
