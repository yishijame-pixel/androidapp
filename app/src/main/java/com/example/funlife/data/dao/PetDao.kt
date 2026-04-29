// PetDao.kt - 宠物数据访问对象
package com.example.funlife.data.dao

import androidx.room.*
import com.example.funlife.data.model.Pet
import kotlinx.coroutines.flow.Flow

@Dao
interface PetDao {
    @Query("SELECT * FROM pets WHERE userId = :userId LIMIT 1")
    fun getPetByUserId(userId: Long): Flow<Pet?>
    
    @Query("SELECT * FROM pets WHERE userId = :userId LIMIT 1")
    suspend fun getPetByUserIdSync(userId: Long): Pet?
    
    @Query("SELECT * FROM pets WHERE id = :petId")
    suspend fun getPetById(petId: Long): Pet?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPet(pet: Pet): Long
    
    @Update
    suspend fun updatePet(pet: Pet)
    
    @Delete
    suspend fun deletePet(pet: Pet)
    
    @Query("UPDATE pets SET hungerValue = :value, lastFeedTime = :time, updatedAt = :time WHERE id = :petId")
    suspend fun updateHungerValue(petId: Long, value: Int, time: Long = System.currentTimeMillis())
    
    @Query("UPDATE pets SET cleanValue = :value, lastCleanTime = :time, updatedAt = :time WHERE id = :petId")
    suspend fun updateCleanValue(petId: Long, value: Int, time: Long = System.currentTimeMillis())
    
    @Query("UPDATE pets SET moodValue = :value, updatedAt = :time WHERE id = :petId")
    suspend fun updateMoodValue(petId: Long, value: Int, time: Long = System.currentTimeMillis())
    
    @Query("UPDATE pets SET healthValue = :value, updatedAt = :time WHERE id = :petId")
    suspend fun updateHealthValue(petId: Long, value: Int, time: Long = System.currentTimeMillis())
    
    @Query("UPDATE pets SET experience = :exp, level = :level, updatedAt = :time WHERE id = :petId")
    suspend fun updateLevelAndExp(petId: Long, level: Int, exp: Int, time: Long = System.currentTimeMillis())
    
    @Query("UPDATE pets SET intimacy = :intimacy, updatedAt = :time WHERE id = :petId")
    suspend fun updateIntimacy(petId: Long, intimacy: Int, time: Long = System.currentTimeMillis())
    
    @Query("UPDATE pets SET name = :name, updatedAt = :time WHERE id = :petId")
    suspend fun updatePetName(petId: Long, name: String, time: Long = System.currentTimeMillis())
    
    @Query("UPDATE pets SET appearance = :appearance, updatedAt = :time WHERE id = :petId")
    suspend fun updateAppearance(petId: Long, appearance: String, time: Long = System.currentTimeMillis())
}
