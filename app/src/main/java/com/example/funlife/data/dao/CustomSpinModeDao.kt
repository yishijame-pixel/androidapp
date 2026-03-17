// CustomSpinModeDao.kt - 自定义转盘模式数据访问对象
package com.example.funlife.data.dao

import androidx.room.*
import com.example.funlife.data.model.CustomSpinMode
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomSpinModeDao {
    
    @Query("SELECT * FROM custom_spin_modes WHERE userId = :userId AND isActive = 1 ORDER BY isDefault DESC, usageCount DESC")
    fun getAllActiveModes(userId: Long): Flow<List<CustomSpinMode>>
    
    @Query("SELECT * FROM custom_spin_modes WHERE userId = :userId AND id = :id")
    suspend fun getModeById(userId: Long, id: Int): CustomSpinMode?
    
    @Query("SELECT * FROM custom_spin_modes WHERE userId = :userId AND isDefault = 1 AND isActive = 1")
    fun getDefaultModes(userId: Long): Flow<List<CustomSpinMode>>
    
    @Query("SELECT * FROM custom_spin_modes WHERE userId = :userId AND isDefault = 0 AND isActive = 1")
    fun getCustomModes(userId: Long): Flow<List<CustomSpinMode>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(mode: CustomSpinMode): Long
    
    @Update
    suspend fun update(mode: CustomSpinMode)
    
    @Delete
    suspend fun delete(mode: CustomSpinMode)
    
    @Query("DELETE FROM custom_spin_modes WHERE userId = :userId AND id = :id")
    suspend fun deleteById(userId: Long, id: Int)
    
    @Query("UPDATE custom_spin_modes SET usageCount = usageCount + 1, updatedAt = :timestamp WHERE userId = :userId AND id = :id")
    suspend fun incrementUsageCount(userId: Long, id: Int, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE custom_spin_modes SET isActive = :isActive WHERE userId = :userId AND id = :id")
    suspend fun setActive(userId: Long, id: Int, isActive: Boolean)
    
    @Query("SELECT COUNT(*) FROM custom_spin_modes WHERE userId = :userId AND isDefault = 1")
    suspend fun getDefaultModesCount(userId: Long): Int
}
