// PetRepository.kt - 宠物仓库
package com.example.funlife.repository

import com.example.funlife.data.dao.PetDao
import com.example.funlife.data.model.Pet
import kotlinx.coroutines.flow.Flow

class PetRepository(private val petDao: PetDao) {
    
    fun getPetByUserId(userId: Long): Flow<Pet?> = petDao.getPetByUserId(userId)
    
    suspend fun getPetByUserIdSync(userId: Long): Pet? = petDao.getPetByUserIdSync(userId)
    
    suspend fun getPetById(petId: Long): Pet? = petDao.getPetById(petId)
    
    suspend fun createPet(pet: Pet): Long = petDao.insertPet(pet)
    
    suspend fun updatePet(pet: Pet) = petDao.updatePet(pet)
    
    suspend fun deletePet(pet: Pet) = petDao.deletePet(pet)
    
    suspend fun feed(petId: Long, hungerBonus: Int) {
        val pet = petDao.getPetById(petId) ?: return
        val newValue = (pet.hungerValue + hungerBonus).coerceIn(0, 100)
        petDao.updateHungerValue(petId, newValue)
    }
    
    suspend fun clean(petId: Long, cleanBonus: Int) {
        val pet = petDao.getPetById(petId) ?: return
        val newValue = (pet.cleanValue + cleanBonus).coerceIn(0, 100)
        petDao.updateCleanValue(petId, newValue)
    }
    
    suspend fun play(petId: Long, moodBonus: Int) {
        val pet = petDao.getPetById(petId) ?: return
        val newValue = (pet.moodValue + moodBonus).coerceIn(0, 100)
        petDao.updateMoodValue(petId, newValue)
    }
    
    suspend fun heal(petId: Long, healthBonus: Int) {
        val pet = petDao.getPetById(petId) ?: return
        val newValue = (pet.healthValue + healthBonus).coerceIn(0, 100)
        petDao.updateHealthValue(petId, newValue)
    }
    
    suspend fun addExperience(petId: Long, exp: Int) {
        val pet = petDao.getPetById(petId) ?: return
        var newExp = pet.experience + exp
        var newLevel = pet.level
        
        // 检查是否升级
        while (newLevel < 30 && newExp >= newLevel * 100) {
            newExp -= newLevel * 100
            newLevel++
        }
        
        petDao.updateLevelAndExp(petId, newLevel, newExp)
    }
    
    suspend fun addIntimacy(petId: Long, amount: Int) {
        val pet = petDao.getPetById(petId) ?: return
        val newValue = (pet.intimacy + amount).coerceIn(0, 1000)
        petDao.updateIntimacy(petId, newValue)
    }
    
    suspend fun updatePetName(petId: Long, name: String) {
        petDao.updatePetName(petId, name)
    }
    
    suspend fun updateAppearance(petId: Long, appearance: String) {
        petDao.updateAppearance(petId, appearance)
    }
    
    /**
     * 时间衰减规则（按分钟计算，使变化在会话内可见）：
     * - 饥饿值：每 12 分钟 -1（≈ -5 / 小时）
     * - 清洁值：每 20 分钟 -1（≈ -3 / 小时）
     * - 心情值：每 15 分钟 -1，并被饥饿/清洁联动；属性低时下降更快
     * - 健康值：饥饿或清洁过低时受损，否则缓慢回血
     */
    suspend fun updatePetStatus(petId: Long) {
        val pet = petDao.getPetById(petId) ?: return
        val now = System.currentTimeMillis()
        val minutesPassed = ((now - pet.lastUpdateTime) / 60_000L).toInt()
        if (minutesPassed <= 0) return
        
        val hungerDrop = minutesPassed / 12
        val cleanDrop = minutesPassed / 20
        var moodDrop = minutesPassed / 15
        // 饥饿/清洁过低时心情额外下降
        if (pet.hungerValue < 30) moodDrop += minutesPassed / 10
        if (pet.cleanValue < 30) moodDrop += minutesPassed / 15
        
        val newHunger = (pet.hungerValue - hungerDrop).coerceIn(0, 100)
        val newClean = (pet.cleanValue - cleanDrop).coerceIn(0, 100)
        val newMood = (pet.moodValue - moodDrop).coerceIn(0, 100)
        val newHealth = calculateHealth(newHunger, newClean, pet.healthValue, minutesPassed)
        
        // 至少有一个值真的改变了才写入，避免每分钟空更新
        if (newHunger != pet.hungerValue || newClean != pet.cleanValue ||
            newMood != pet.moodValue || newHealth != pet.healthValue) {
            petDao.updatePet(
                pet.copy(
                    hungerValue = newHunger,
                    cleanValue = newClean,
                    moodValue = newMood,
                    healthValue = newHealth,
                    lastUpdateTime = now,
                    updatedAt = now
                )
            )
        }
    }
    
    private fun calculateHealth(hunger: Int, clean: Int, currentHealth: Int, minutes: Int): Int {
        return when {
            hunger < 10 || clean < 10 -> (currentHealth - minutes / 10).coerceIn(0, 100)
            hunger < 30 || clean < 30 -> (currentHealth - minutes / 30).coerceIn(0, 100)
            currentHealth < 100 -> (currentHealth + minutes / 20).coerceIn(0, 100)
            else -> currentHealth
        }
    }
}
