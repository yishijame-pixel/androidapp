// HabitDao.kt - 习惯数据访问对象
package com.example.funlife.data.dao

import androidx.room.*
import com.example.funlife.data.model.Habit
import com.example.funlife.data.model.HabitRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
    
    @Query("SELECT * FROM habits WHERE userId = :userId AND isActive = 1 ORDER BY createdAt DESC")
    fun getAllActiveHabits(userId: Long): Flow<List<Habit>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: Habit)
    
    @Update
    suspend fun updateHabit(habit: Habit)
    
    @Delete
    suspend fun deleteHabit(habit: Habit)
    
    @Query("SELECT * FROM habit_records WHERE userId = :userId AND habitId = :habitId ORDER BY date DESC")
    fun getHabitRecords(userId: Long, habitId: Int): Flow<List<HabitRecord>>
    
    @Query("SELECT * FROM habit_records WHERE userId = :userId AND habitId = :habitId AND date = :date")
    suspend fun getRecordByDate(userId: Long, habitId: Int, date: String): HabitRecord?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: HabitRecord)
    
    @Delete
    suspend fun deleteRecord(record: HabitRecord)
    
    @Query("SELECT COUNT(*) FROM habit_records WHERE userId = :userId AND habitId = :habitId")
    suspend fun getHabitRecordCount(userId: Long, habitId: Int): Int
    
    @Query("UPDATE habits SET makeupCards = :cards WHERE userId = :userId AND id = :habitId")
    suspend fun updateMakeupCards(userId: Long, habitId: Int, cards: Int)
    
    @Query("SELECT makeupCards FROM habits WHERE userId = :userId AND id = :habitId")
    suspend fun getMakeupCards(userId: Long, habitId: Int): Int
}
