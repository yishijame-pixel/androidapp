// StatisticsViewModel.kt - 统计视图模型
package com.example.funlife.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.funlife.data.database.AppDatabase
import com.example.funlife.data.model.AnniversaryStatistics
import com.example.funlife.data.model.ScoreStatistics
import com.example.funlife.repository.AnniversaryRepository
import com.example.funlife.repository.PlayerRepository
import com.example.funlife.repository.GameHistoryRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class StatisticsViewModel(application: Application) : AndroidViewModel(application) {
    
    private val anniversaryRepository: AnniversaryRepository
    private val playerRepository: PlayerRepository
    private val historyRepository: GameHistoryRepository
    
    val anniversaryStats: StateFlow<AnniversaryStatistics>
    val scoreStats: StateFlow<ScoreStatistics>
    
    init {
        val database = AppDatabase.getDatabase(application)
        anniversaryRepository = AnniversaryRepository(database.anniversaryDao())
        playerRepository = PlayerRepository(database.playerDao())
        historyRepository = GameHistoryRepository(database.gameHistoryDao())
        
        // 计算纪念日统计
        anniversaryStats = anniversaryRepository.allAnniversaries.map { anniversaries ->
            val today = LocalDate.now()
            val upcoming = anniversaries.filter { 
                val date = LocalDate.parse(it.date)
                ChronoUnit.DAYS.between(today, date) >= 0
            }
            val passed = anniversaries.filter {
                val date = LocalDate.parse(it.date)
                ChronoUnit.DAYS.between(today, date) < 0
            }
            val todayList = anniversaries.filter {
                val date = LocalDate.parse(it.date)
                ChronoUnit.DAYS.between(today, date) == 0L
            }
            val thisWeek = anniversaries.filter {
                val date = LocalDate.parse(it.date)
                val days = ChronoUnit.DAYS.between(today, date)
                days in 0..7
            }
            val thisMonth = anniversaries.filter {
                val date = LocalDate.parse(it.date)
                val days = ChronoUnit.DAYS.between(today, date)
                days in 0..30
            }
            val nearest = upcoming.minByOrNull { 
                val date = LocalDate.parse(it.date)
                ChronoUnit.DAYS.between(today, date)
            }
            
            AnniversaryStatistics(
                totalCount = anniversaries.size,
                upcomingCount = upcoming.size,
                passedCount = passed.size,
                todayCount = todayList.size,
                thisWeekCount = thisWeek.size,
                thisMonthCount = thisMonth.size,
                nearestAnniversary = nearest
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AnniversaryStatistics()
        )
        
        // 计算分数统计
        scoreStats = playerRepository.allPlayers.map { players ->
            val totalPlayers = players.size
            val highestScore = players.maxOfOrNull { it.score } ?: 0
            val averageScore = if (players.isNotEmpty()) {
                players.map { it.score }.average()
            } else 0.0
            val topPlayer = players.maxByOrNull { it.score }
            
            ScoreStatistics(
                totalPlayers = totalPlayers,
                highestScore = highestScore,
                averageScore = averageScore,
                topPlayer = topPlayer
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ScoreStatistics()
        )
    }
    
    // 获取游戏历史统计
    suspend fun getGameCount(gameType: String): Int {
        return historyRepository.getCountByType(gameType)
    }
    
    suspend fun getTotalPlayers(): Int {
        return historyRepository.getTotalPlayers()
    }
}
