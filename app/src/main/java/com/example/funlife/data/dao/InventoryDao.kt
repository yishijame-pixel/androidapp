// InventoryDao.kt - 背包数据访问对象
package com.example.funlife.data.dao

import androidx.room.*
import com.example.funlife.data.model.InventoryItem
import com.example.funlife.data.model.InventoryItemType
import kotlinx.coroutines.flow.Flow

@Dao
interface InventoryDao {
    
    @Query("SELECT * FROM inventory_items WHERE userId = :userId ORDER BY obtainedTime DESC")
    fun getAllItems(userId: Long = 1): Flow<List<InventoryItem>>
    
    @Query("SELECT * FROM inventory_items WHERE userId = :userId AND itemType = :type ORDER BY obtainedTime DESC")
    fun getItemsByType(userId: Long = 1, type: InventoryItemType): Flow<List<InventoryItem>>
    
    @Query("SELECT * FROM inventory_items WHERE id = :itemId")
    suspend fun getItemById(itemId: Long): InventoryItem?
    
    @Query("SELECT * FROM inventory_items WHERE userId = :userId AND itemId = :itemId LIMIT 1")
    suspend fun getItemByItemId(userId: Long = 1, itemId: String): InventoryItem?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: InventoryItem): Long
    
    @Update
    suspend fun updateItem(item: InventoryItem)
    
    @Delete
    suspend fun deleteItem(item: InventoryItem)
    
    @Query("DELETE FROM inventory_items WHERE id = :itemId")
    suspend fun deleteItemById(itemId: Long)
    
    @Query("DELETE FROM inventory_items WHERE userId = :userId")
    suspend fun deleteAllItems(userId: Long = 1)
    
    @Query("SELECT COUNT(*) FROM inventory_items WHERE userId = :userId")
    fun getItemCount(userId: Long = 1): Flow<Int>
    
    @Query("SELECT SUM(quantity) FROM inventory_items WHERE userId = :userId")
    fun getTotalQuantity(userId: Long = 1): Flow<Int?>
    
    // 增加物品数量
    @Query("UPDATE inventory_items SET quantity = quantity + :amount WHERE id = :itemId")
    suspend fun increaseQuantity(itemId: Long, amount: Int)
    
    // 减少物品数量
    @Query("UPDATE inventory_items SET quantity = quantity - :amount WHERE id = :itemId")
    suspend fun decreaseQuantity(itemId: Long, amount: Int)
    
    // 🔥 获取所有物品列表（非Flow）
    @Query("SELECT * FROM inventory_items ORDER BY obtainedTime DESC")
    suspend fun getAllItemsList(): List<InventoryItem>
    
    // 🔥 更新物品名称
    @Query("UPDATE inventory_items SET itemName = :newName WHERE id = :itemId")
    suspend fun updateItemName(itemId: Long, newName: String)
}
