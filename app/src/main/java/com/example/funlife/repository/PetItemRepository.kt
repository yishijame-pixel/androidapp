// PetItemRepository.kt - 宠物物品仓库
package com.example.funlife.repository

import com.example.funlife.data.dao.PetItemDao
import com.example.funlife.data.model.PetItem
import com.example.funlife.data.model.ItemType
import kotlinx.coroutines.flow.Flow

class PetItemRepository(private val petItemDao: PetItemDao) {
    
    fun getItemsByUserId(userId: Long): Flow<List<PetItem>> = 
        petItemDao.getItemsByUserId(userId)
    
    fun getItemsByType(userId: Long, type: ItemType): Flow<List<PetItem>> = 
        petItemDao.getItemsByType(userId, type)
    
    suspend fun getItemByItemId(userId: Long, itemId: Int): PetItem? = 
        petItemDao.getItemByItemId(userId, itemId)
    
    suspend fun addItem(userId: Long, itemId: Int, itemName: String, itemType: ItemType, quantity: Int = 1) {
        val existingItem = petItemDao.getItemByItemId(userId, itemId)
        if (existingItem != null) {
            petItemDao.addItemQuantity(userId, itemId, quantity)
        } else {
            val newItem = PetItem(
                userId = userId,
                itemId = itemId,
                itemType = itemType,
                itemName = itemName,
                quantity = quantity
            )
            petItemDao.insertItem(newItem)
        }
    }
    
    suspend fun useItem(userId: Long, itemId: Int, quantity: Int = 1): Boolean {
        val rowsAffected = petItemDao.reduceItemQuantity(userId, itemId, quantity)
        if (rowsAffected > 0) {
            petItemDao.deleteEmptyItems(userId, itemId)
            return true
        }
        return false
    }
    
    suspend fun deleteItem(item: PetItem) = petItemDao.deleteItem(item)
}
