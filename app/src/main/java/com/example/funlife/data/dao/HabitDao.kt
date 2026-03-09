// HabitDao.kt - 习惯数据访问对象
package com.example.funlife.data.dao

import androidx.room.*
import com.example.funlife.data.model.Habit
import com.example.funlife.data.model.HabitRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
    
    @Query("SELECT * FROM habits WHERE isActive = 1 ORDER BY createdAt DESC")
    fun getAllActiveHabits(): Flow<List<Habit>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: Habit)
    
    @Update
    suspend fun updateHabit(habit: Habit)
    
    @Delete
    suspend fun deleteHabit(habit: Habit)
    
    @Query("SELECT * FROM habit_records WHERE habitId = :habitId ORDER BY date DESC")
    fun getHabitRecords(habitId: Int): Flow<List<HabitRecord>>
    
    @Query("SELECT * FROM habit_records WHERE habitId = :habitId AND date = :date")
    suspend fun getRecordByDate(habitId: Int, date: String): HabitRecord?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: HabitRecord)
    
    @Delete
    suspend fun deleteRecord(record: HabitRecord)
    
    @Query("SELECT COUNT(*) FROM habit_records WHERE habitId = :habitId")
    suspend fun getHabitRecordCount(habitId: Int): Int
    
    @Query("UPDATE habits SET makeupCards = :cards WHERE id = :habitId")
    suspend fun updateMakeupCards(habitId: Int, cards: Int)
    
    @Query("SELECT makeupCards FROM habits WHERE id = :habitId")
    suspend fun getMakeupCards(habitId: Int): Int
}
