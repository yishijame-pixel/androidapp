package com.example.funlife.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.funlife.data.model.Riddle
import com.example.funlife.data.model.RiddleProgress
import com.example.funlife.data.model.RiddleStats
import com.example.funlife.repository.RiddleRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

private const val TAG = "RiddleViewModel"

class RiddleViewModel(
    private val repository: RiddleRepository,
    private val userId: Long
) : ViewModel() {
    
    private val _currentRiddleIndex = MutableStateFlow(0)
    val currentRiddleIndex: StateFlow<Int> = _currentRiddleIndex.asStateFlow()
    
    private val _userAnswer = MutableStateFlow("")
    val userAnswer: StateFlow<String> = _userAnswer.asStateFlow()
    
    private val _showResult = MutableStateFlow(false)
    val showResult: StateFlow<Boolean> = _showResult.asStateFlow()
    
    private val _isCorrect = MutableStateFlow(false)
    val isCorrect: StateFlow<Boolean> = _isCorrect.asStateFlow()
    
    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()
    
    // 直接使用MutableStateFlow来管理题目列表
    private val _allRiddles = MutableStateFlow<List<Riddle>>(emptyList())
    val allRiddles: StateFlow<List<Riddle>> = _allRiddles.asStateFlow()
    
    // 当前题目 - 简化为直接计算
    val currentRiddle: StateFlow<Riddle?> = combine(
        _allRiddles,
        _currentRiddleIndex
    ) { riddles, index ->
        val riddle = riddles.getOrNull(index)
        Log.d(TAG, "currentRiddle combine: riddles.size=${riddles.size}, index=$index, riddle=${riddle?.question?.take(20)}")
        riddle
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    
    // 统计信息
    private val _stats = MutableStateFlow<RiddleStats?>(null)
    val stats: StateFlow<RiddleStats?> = _stats.asStateFlow()
    
    init {
        Log.d(TAG, "========================================")
        Log.d(TAG, "RiddleViewModel init started for userId=$userId")
        Log.d(TAG, "========================================")
        
        viewModelScope.launch {
            try {
                // 1. 初始化题目
                Log.d(TAG, "Step 1: Initializing riddles...")
                repository.initializeRiddles()
                Log.d(TAG, "Step 1: Riddles initialization complete")
                
                // 2. 加载题目列表
                Log.d(TAG, "Step 2: Loading riddles from database...")
                repository.getAllRiddles().collect { riddles ->
                    Log.d(TAG, "Step 2: Received ${riddles.size} riddles from database")
                    if (riddles.isNotEmpty()) {
                        Log.d(TAG, "First riddle: ${riddles[0].question}")
                        Log.d(TAG, "Last riddle: ${riddles[riddles.size - 1].question}")
                    }
                    _allRiddles.value = riddles
                    if (riddles.isNotEmpty() && !_isInitialized.value) {
                        _isInitialized.value = true
                        Log.d(TAG, "Step 2: Initialization complete, isInitialized=true")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "ERROR: Failed to initialize riddles", e)
                _isInitialized.value = true
            }
        }
        
        // 加载统计信息
        viewModelScope.launch {
            repository.getStats(userId).collect { statsData ->
                Log.d(TAG, "Stats loaded: $statsData")
                _stats.value = statsData
            }
        }
    }
    
    fun updateAnswer(answer: String) {
        _userAnswer.value = answer
    }
    
    fun submitAnswer() {
        viewModelScope.launch {
            val riddle = currentRiddle.value ?: return@launch
            
            // 检查答案（忽略大小写和空格）
            val userAns = _userAnswer.value.trim().replace(" ", "").lowercase()
            val correctAns = riddle.answer.trim().replace(" ", "").lowercase()
            
            // 修复：空答案直接判错，且contains逻辑需要更严格
            val correct = if (userAns.isEmpty()) {
                false
            } else {
                userAns == correctAns || 
                (userAns.length >= 2 && correctAns.contains(userAns)) ||
                (correctAns.length >= 2 && userAns.contains(correctAns))
            }
            
            _isCorrect.value = correct
            _showResult.value = true
            
            // 更新进度
            val progress = repository.getProgress(userId, riddle.id) ?: RiddleProgress(
                userId = userId,
                riddleId = riddle.id
            )
            
            repository.updateProgress(
                progress.copy(
                    isAnswered = true,
                    isCorrect = correct,
                    attempts = progress.attempts + 1,
                    lastAttemptTime = System.currentTimeMillis()
                )
            )
            
            // 更新统计
            val currentStats = repository.getStatsSync(userId) ?: RiddleStats(userId = userId)
            val newStreak = if (correct) currentStats.currentStreak + 1 else 0
            val newScore = if (correct) currentStats.totalScore + (riddle.difficulty * 10) else currentStats.totalScore
            
            repository.updateStats(
                currentStats.copy(
                    totalAnswered = currentStats.totalAnswered + 1,
                    totalCorrect = if (correct) currentStats.totalCorrect + 1 else currentStats.totalCorrect,
                    currentStreak = newStreak,
                    maxStreak = maxOf(currentStats.maxStreak, newStreak),
                    totalScore = newScore
                )
            )
            
            // 重新加载统计信息
            _stats.value = repository.getStatsSync(userId)
        }
    }
    
    fun nextRiddle() {
        val riddles = _allRiddles.value
        if (_currentRiddleIndex.value < riddles.size - 1) {
            _currentRiddleIndex.value += 1
            resetAnswer()
        }
    }
    
    fun previousRiddle() {
        if (_currentRiddleIndex.value > 0) {
            _currentRiddleIndex.value -= 1
            resetAnswer()
        }
    }
    
    fun skipRiddle() {
        nextRiddle()
    }
    
    private fun resetAnswer() {
        _userAnswer.value = ""
        _showResult.value = false
        _isCorrect.value = false
    }
    
    fun dismissResult() {
        _showResult.value = false
    }
}
