// HabitViewModel.kt - 习惯视图模型
package com.example.funlife.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.funlife.data.database.AppDatabase
import com.example.funlife.data.model.Habit
import com.example.funlife.data.model.HabitRecord
import com.example.funlife.repository.HabitRepository
import com.example.funlife.repository.CoinRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.LocalDate

data class HabitWithStats(
    val habit: Habit,
    val todayChecked: Boolean,
    val totalDays: Int,
    val currentStreak: Int,
    val longestStreak: Int,
    val completionRate: Float
)

sealed class CheckInResult {
    data class Success(val coins: Int, val hasBonus: Boolean) : CheckInResult()
    object Cancelled : CheckInResult()
    data class Failed(val message: String) : CheckInResult()
}

class HabitViewModel(application: Application) : AndroidViewModel(application) {
    
    private val database = AppDatabase.getDatabase(application)
    private val repository: HabitRepository
    private val coinRepository: CoinRepository
    private val context = application.applicationContext
    val habits: StateFlow<List<Habit>>
    
    private val _habitsWithStats = MutableStateFlow<List<HabitWithStats>>(emptyList())
    val habitsWithStats: StateFlow<List<HabitWithStats>> = _habitsWithStats.asStateFlow()
    
    // 刷新触发器
    private val _refreshTrigger = MutableStateFlow(0)
    
    // 🔥 获取当前用户ID
    private fun getCurrentUserId(): Long {
        val sessionManager = com.example.funlife.utils.UserSessionManager(context)
        return sessionManager.getCurrentUserId().takeIf { it > 0 } ?: 0L
    }
    
    init {
        val database = AppDatabase.getDatabase(application)
        repository = HabitRepository(database.habitDao())
        coinRepository = CoinRepository(database.coinDao())
        
        // 🔥 修复：添加异常处理和用户过滤
        habits = repository.getAllHabits(getCurrentUserId())
            .catch { e ->
                android.util.Log.e("HabitViewModel", "Error loading habits", e)
                emit(emptyList())  // 发送空列表作为降级方案
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
        
        // 监听习惯变化和刷新触发器，计算统计数据
        viewModelScope.launch {
            combine(habits, _refreshTrigger) { habitList, _ -> habitList }
                .collect { habitList ->
                    val statsMap = habitList.map { habit ->
                        calculateHabitStats(habit)
                    }
                    _habitsWithStats.value = statsMap
                }
        }
        
        // 初始化金币系统
        viewModelScope.launch {
            val userId = getCurrentUserId()
            coinRepository.initializeCoins(userId)
        }
    }
    
    private fun refreshStats() {
        _refreshTrigger.value += 1
    }
    
    private suspend fun calculateHabitStats(habit: Habit): HabitWithStats {
        val userId = getCurrentUserId()
        val today = LocalDate.now().toString()
        val records = repository.getHabitRecords(userId, habit.id).first()
        
        // 检查今天是否打卡
        val todayChecked = records.any { it.date == today }
        
        // 总打卡天数
        val totalDays = records.size
        
        // 计算当前连续天数
        var currentStreak = 0
        var checkDate = LocalDate.now()
        val recordDates = records.map { it.date }.toSet()
        
        while (recordDates.contains(checkDate.toString())) {
            currentStreak++
            checkDate = checkDate.minusDays(1)
        }
        
        // 计算最长连续天数
        var longestStreak = 0
        var tempStreak = 0
        val sortedDates = records.map { LocalDate.parse(it.date) }.sorted()
        
        for (i in sortedDates.indices) {
            if (i == 0) {
                tempStreak = 1
            } else {
                val daysDiff = java.time.temporal.ChronoUnit.DAYS.between(sortedDates[i - 1], sortedDates[i])
                if (daysDiff == 1L) {
                    tempStreak++
                } else {
                    longestStreak = maxOf(longestStreak, tempStreak)
                    tempStreak = 1
                }
            }
        }
        longestStreak = maxOf(longestStreak, tempStreak)
        
        // 计算完成率（最近30天）
        val thirtyDaysAgo = LocalDate.now().minusDays(30)
        val recentRecords = records.filter { 
            LocalDate.parse(it.date).isAfter(thirtyDaysAgo) || LocalDate.parse(it.date).isEqual(thirtyDaysAgo)
        }
        val completionRate = if (recentRecords.isNotEmpty()) {
            recentRecords.size / 30f
        } else {
            0f
        }
        
        return HabitWithStats(
            habit = habit,
            todayChecked = todayChecked,
            totalDays = totalDays,
            currentStreak = currentStreak,
            longestStreak = longestStreak,
            completionRate = completionRate
        )
    }
    
    fun addHabit(name: String, icon: String, color: String, targetDays: Int = 0) {
        viewModelScope.launch {
            // 🔥 新增：输入验证
            val nameValidation = com.example.funlife.utils.ValidationUtils.validateHabitName(name)
            if (nameValidation is com.example.funlife.utils.ValidationResult.Error) {
                android.util.Log.w("HabitViewModel", "Invalid habit name: ${nameValidation.message}")
                return@launch
            }
            
            val userId = getCurrentUserId()
            val habit = Habit(
                userId = userId,
                name = name,
                icon = icon,
                color = color,
                targetDays = targetDays,
                createdAt = LocalDateTime.now().toString()
            )
            repository.insertHabit(habit)
        }
    }
    
    fun toggleCheckIn(habitId: Int, isChecked: Boolean, date: String? = null): Flow<CheckInResult> = flow {
        val userId = getCurrentUserId()
        val targetDate = date ?: LocalDate.now().toString()
        val today = LocalDate.now().toString()
        
        // 🔥 修复：检查该日期是否已经打卡（防止重复补卡）
        val existingRecord = repository.getRecordByDate(userId, habitId, targetDate)
        
        // 如果已打卡，则取消打卡（用于测试）
        if (isChecked || existingRecord != null) {
            if (existingRecord == null) {
                emit(CheckInResult.Failed("该日期没有打卡记录"))
                return@flow
            }
            
            repository.cancelCheckIn(userId, habitId, targetDate)
            
            // 返还金币
            if (targetDate == today) {
                // 🔥 修复：取消今天的打卡时，重置连续天数（防止刷金币）
                // 不返还金币，作为取消打卡的惩罚
                // 这样可以防止用户通过反复打卡/取消打卡来刷连续天数
            } else {
                // 补卡取消，返还5金币和1张补卡
                coinRepository.removeCoins(userId, 5)
                repository.earnMakeupCard(userId, habitId)
            }
            
            // 刷新统计数据
            refreshStats()
            emit(CheckInResult.Cancelled)
            return@flow
        }
        
        // 打卡
        if (targetDate == today) {
            // 今天打卡，直接打卡并奖励金币
            repository.checkIn(userId, habitId, targetDate, LocalDateTime.now().toString())
            
            // 奖励10金币
            coinRepository.addCoins(userId, 10)
            
            // 连续打卡7天奖励1张补卡卡片和额外50金币
            val habit = habits.value.find { it.id == habitId }
            var bonusCoins = 0
            if (habit != null) {
                val stats = calculateHabitStats(habit)
                if (stats.currentStreak > 0 && (stats.currentStreak + 1) % 7 == 0) {
                    repository.earnMakeupCard(userId, habitId)
                    coinRepository.addCoins(userId, 50)
                    bonusCoins = 50
                }
            }
            
            // 刷新统计数据
            refreshStats()
            emit(CheckInResult.Success(10 + bonusCoins, bonusCoins > 0))
        } else {
            // 补卡，需要消耗补卡卡片
            val success = repository.useMakeupCard(userId, habitId)
            if (success) {
                repository.checkIn(userId, habitId, targetDate, LocalDateTime.now().toString())
                // 补卡也奖励5金币
                coinRepository.addCoins(userId, 5)
                // 刷新统计数据
                refreshStats()
                emit(CheckInResult.Success(5, false))
            } else {
                emit(CheckInResult.Failed("补卡卡片不足"))
            }
        }
    }
    
    fun getMakeupCards(habitId: Int): Flow<Int> = flow {
        val userId = getCurrentUserId()
        emit(repository.getMakeupCards(userId, habitId))
    }
    
    suspend fun canMakeup(habitId: Int): Boolean {
        val userId = getCurrentUserId()
        return repository.getMakeupCards(userId, habitId) > 0
    }
    
    fun deleteHabit(habit: Habit) {
        viewModelScope.launch {
            repository.deleteHabit(habit)
        }
    }
    
    fun getHabitRecords(habitId: Int): Flow<List<HabitRecord>> {
        val userId = getCurrentUserId()
        return repository.getHabitRecords(userId, habitId)
    }
}
