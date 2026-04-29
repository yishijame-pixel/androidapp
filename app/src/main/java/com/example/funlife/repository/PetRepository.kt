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
    
    // 更新宠物状态（时间衰减）
    suspend fun updatePetStatus(petId: Long) {
        val pet = petDao.getPetById(petId) ?: return
        val currentTime = System.currentTimeMillis()
        val hoursPassed = (currentTime - pet.lastUpdateTime) / (1000 * 60 * 60)
        
        if (hoursPassed > 0) {
            // 饥饿值每小时 -5
            val newHunger = (pet.hungerValue - (hoursPassed * 5).toInt()).coerceIn(0, 100)
            // 清洁值每2小时 -3
            val newClean = (pet.cleanValue - ((hoursPassed / 2) * 3).toInt()).coerceIn(0, 100)
            // 心情值根据其他属性计算
            val newMood = calculateMood(newHunger, newClean, pet.healthValue)
            // 健康值根据其他属性计算
            val newHealth = calculateHealth(newHunger, newClean, pet.healthValue)
            
            val updatedPet = pet.copy(
                hungerValue = newHunger,
                cleanValue = newClean,
                moodValue = newMood,
                healthValue = newHealth,
                lastUpdateTime = currentTime,
                updatedAt = currentTime
            )
            petDao.updatePet(updatedPet)
        }
    }
    
    private fun calculateMood(hunger: Int, clean: Int, health: Int): Int {
        val avgStatus = (hunger + clean + health) / 3
        return when {
            avgStatus >= 70 -> 100
            avgStatus >= 50 -> 80
            avgStatus >= 30 -> 50
            else -> 20
        }.coerceIn(0, 100)
    }
    
    private fun calculateHealth(hunger: Int, clean: Int, currentHealth: Int): Int {
        return when {
            hunger < 10 || clean < 10 -> (currentHealth - 10).coerceIn(0, 100)
            hunger < 30 || clean < 30 -> (currentHealth - 5).coerceIn(0, 100)
            currentHealth < 100 -> (currentHealth + 2).coerceIn(0, 100)
            else -> currentHealth
        }
    }
}
