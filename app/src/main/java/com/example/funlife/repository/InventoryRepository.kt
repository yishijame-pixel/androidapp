// InventoryRepository.kt - 背包仓库
package com.example.funlife.repository

import com.example.funlife.data.dao.InventoryDao
import com.example.funlife.data.model.InventoryItem
import com.example.funlife.data.model.InventoryItemType
import kotlinx.coroutines.flow.Flow

class InventoryRepository(private val inventoryDao: InventoryDao) {
    
    // 🔒 安全修复：移除 userId=1 默认值，强制调用方传入当前用户 ID。
    fun getAllItems(userId: Long): Flow<List<InventoryItem>> {
        return inventoryDao.getAllItems(userId)
    }
    
    fun getItemsByType(userId: Long, type: InventoryItemType): Flow<List<InventoryItem>> {
        return inventoryDao.getItemsByType(userId, type)
    }
    
    suspend fun getItemById(itemId: Long): InventoryItem? {
        return inventoryDao.getItemById(itemId)
    }
    
    suspend fun getItemByItemId(userId: Long, itemId: String): InventoryItem? {
        return inventoryDao.getItemByItemId(userId, itemId)
    }
    
    suspend fun addItem(item: InventoryItem): Long {
        // 检查是否已存在相同物品
        val existingItem = inventoryDao.getItemByItemId(item.userId, item.itemId)
        return if (existingItem != null) {
            // 如果已存在，增加数量
            inventoryDao.increaseQuantity(existingItem.id, item.quantity)
            existingItem.id
        } else {
            // 如果不存在，插入新物品
            inventoryDao.insertItem(item)
        }
    }
    
    suspend fun updateItem(item: InventoryItem) {
        inventoryDao.updateItem(item)
    }
    
    suspend fun deleteItem(item: InventoryItem) {
        inventoryDao.deleteItem(item)
    }
    
    suspend fun deleteItemById(itemId: Long) {
        inventoryDao.deleteItemById(itemId)
    }
    
    suspend fun useItem(itemId: Long, amount: Int = 1): Boolean {
        val item = inventoryDao.getItemById(itemId) ?: return false
        
        if (!item.isUsable || item.quantity < amount) {
            return false
        }
        
        val newQuantity = item.quantity - amount
        if (newQuantity <= 0) {
            // 数量为0，删除物品
            inventoryDao.deleteItemById(itemId)
        } else {
            // 减少数量
            inventoryDao.decreaseQuantity(itemId, amount)
        }
        
        return true
    }
    
    fun getItemCount(userId: Long): Flow<Int> {
        return inventoryDao.getItemCount(userId)
    }
    
    fun getTotalQuantity(userId: Long): Flow<Int?> {
        return inventoryDao.getTotalQuantity(userId)
    }
    
    suspend fun clearInventory(userId: Long) {
        inventoryDao.deleteAllItems(userId)
    }
}
