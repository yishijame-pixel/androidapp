// AnniversaryReminderDao.kt - 纪念日提醒数据访问对象
package com.example.funlife.data.dao

import androidx.room.*
import com.example.funlife.data.model.AnniversaryReminder
import kotlinx.coroutines.flow.Flow

@Dao
interface AnniversaryReminderDao {
    
    @Query("SELECT * FROM anniversary_reminders WHERE userId = :userId")
    fun getAllReminders(userId: Long): Flow<List<AnniversaryReminder>>
    
    @Query("SELECT * FROM anniversary_reminders WHERE anniversaryId = :anniversaryId AND userId = :userId LIMIT 1")
    fun getReminderByAnniversary(anniversaryId: Int, userId: Long): Flow<AnniversaryReminder?>
    
    @Query("SELECT * FROM anniversary_reminders WHERE userId = :userId AND isEnabled = 1")
    fun getEnabledReminders(userId: Long): Flow<List<AnniversaryReminder>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: AnniversaryReminder)
    
    @Update
    suspend fun updateReminder(reminder: AnniversaryReminder)
    
    @Delete
    suspend fun deleteReminder(reminder: AnniversaryReminder)
    
    @Query("DELETE FROM anniversary_reminders WHERE anniversaryId = :anniversaryId")
    suspend fun deleteByAnniversaryId(anniversaryId: Int)
}
