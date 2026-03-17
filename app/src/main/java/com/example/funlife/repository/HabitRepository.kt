// HabitRepository.kt - 习惯仓库
package com.example.funlife.repository

import com.example.funlife.data.dao.HabitDao
import com.example.funlife.data.model.Habit
import com.example.funlife.data.model.HabitRecord
import kotlinx.coroutines.flow.Flow

class HabitRepository(private val habitDao: HabitDao) {
    
    fun getAllHabits(userId: Long): Flow<List<Habit>> = habitDao.getAllActiveHabits(userId)
    
    suspend fun insertHabit(habit: Habit) = habitDao.insertHabit(habit)
    
    suspend fun updateHabit(habit: Habit) = habitDao.updateHabit(habit)
    
    suspend fun deleteHabit(habit: Habit) = habitDao.deleteHabit(habit)
    
    fun getHabitRecords(userId: Long, habitId: Int): Flow<List<HabitRecord>> = habitDao.getHabitRecords(userId, habitId)
    
    // 🔥 新增：检查指定日期是否已打卡
    suspend fun getRecordByDate(userId: Long, habitId: Int, date: String): HabitRecord? = habitDao.getRecordByDate(userId, habitId, date)
    
    suspend fun checkIn(userId: Long, habitId: Int, date: String, timestamp: String) {
        val record = HabitRecord(userId = userId, habitId = habitId, date = date, timestamp = timestamp)
        habitDao.insertRecord(record)
    }
    
    suspend fun cancelCheckIn(userId: Long, habitId: Int, date: String) {
        val record = habitDao.getRecordByDate(userId, habitId, date)
        record?.let { habitDao.deleteRecord(it) }
    }
    
    suspend fun getRecordCount(userId: Long, habitId: Int) = habitDao.getHabitRecordCount(userId, habitId)
    
    suspend fun updateMakeupCards(userId: Long, habitId: Int, cards: Int) = habitDao.updateMakeupCards(userId, habitId, cards)
    
    suspend fun getMakeupCards(userId: Long, habitId: Int) = habitDao.getMakeupCards(userId, habitId)
    
    suspend fun useMakeupCard(userId: Long, habitId: Int): Boolean {
        val cards = habitDao.getMakeupCards(userId, habitId)
        return if (cards > 0) {
            habitDao.updateMakeupCards(userId, habitId, cards - 1)
            true
        } else {
            false
        }
    }
    
    suspend fun earnMakeupCard(userId: Long, habitId: Int) {
        val cards = habitDao.getMakeupCards(userId, habitId)
        habitDao.updateMakeupCards(userId, habitId, cards + 1)
    }
    
    suspend fun removeMakeupCard(userId: Long, habitId: Int) {
        val cards = habitDao.getMakeupCards(userId, habitId)
        if (cards > 0) {
            habitDao.updateMakeupCards(userId, habitId, cards - 1)
        }
    }
}
