// HabitRepository.kt - 习惯仓库
package com.example.funlife.repository

import com.example.funlife.data.dao.HabitDao
import com.example.funlife.data.model.Habit
import com.example.funlife.data.model.HabitRecord
import kotlinx.coroutines.flow.Flow

class HabitRepository(private val habitDao: HabitDao) {
    
    val allHabits: Flow<List<Habit>> = habitDao.getAllActiveHabits()
    
    suspend fun insertHabit(habit: Habit) = habitDao.insertHabit(habit)
    
    suspend fun updateHabit(habit: Habit) = habitDao.updateHabit(habit)
    
    suspend fun deleteHabit(habit: Habit) = habitDao.deleteHabit(habit)
    
    fun getHabitRecords(habitId: Int): Flow<List<HabitRecord>> = habitDao.getHabitRecords(habitId)
    
    suspend fun checkIn(habitId: Int, date: String, timestamp: String) {
        val record = HabitRecord(habitId = habitId, date = date, timestamp = timestamp)
        habitDao.insertRecord(record)
    }
    
    suspend fun cancelCheckIn(habitId: Int, date: String) {
        val record = habitDao.getRecordByDate(habitId, date)
        record?.let { habitDao.deleteRecord(it) }
    }
    
    suspend fun getRecordCount(habitId: Int) = habitDao.getHabitRecordCount(habitId)
    
    suspend fun updateMakeupCards(habitId: Int, cards: Int) = habitDao.updateMakeupCards(habitId, cards)
    
    suspend fun getMakeupCards(habitId: Int) = habitDao.getMakeupCards(habitId)
    
    suspend fun useMakeupCard(habitId: Int): Boolean {
        val cards = habitDao.getMakeupCards(habitId)
        return if (cards > 0) {
            habitDao.updateMakeupCards(habitId, cards - 1)
            true
        } else {
            false
        }
    }
    
    suspend fun earnMakeupCard(habitId: Int) {
        val cards = habitDao.getMakeupCards(habitId)
        habitDao.updateMakeupCards(habitId, cards + 1)
    }
    
    suspend fun removeMakeupCard(habitId: Int) {
        val cards = habitDao.getMakeupCards(habitId)
        if (cards > 0) {
            habitDao.updateMakeupCards(habitId, cards - 1)
        }
    }
}
