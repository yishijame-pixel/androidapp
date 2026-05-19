// InventoryDao.kt - 背包数据访问对象
package com.example.funlife.data.dao

import androidx.room.*
import com.example.funlife.data.model.InventoryItem
import com.example.funlife.data.model.InventoryItemType
import kotlinx.coroutines.flow.Flow

@Dao
interface InventoryDao {
    
    // 🔒 安全修复：所有查询强制传入 userId（移除默认值 1），
    // 防止多账号场景下错误读取 user 1 的数据导致数据泄漏。
    @Query("SELECT * FROM inventory_items WHERE userId = :userId ORDER BY obtainedTime DESC")
    fun getAllItems(userId: Long): Flow<List<InventoryItem>>
    
    @Query("SELECT * FROM inventory_items WHERE userId = :userId AND itemType = :type ORDER BY obtainedTime DESC")
    fun getItemsByType(userId: Long, type: InventoryItemType): Flow<List<InventoryItem>>
    
    @Query("SELECT * FROM inventory_items WHERE id = :itemId")
    suspend fun getItemById(itemId: Long): InventoryItem?
    
    @Query("SELECT * FROM inventory_items WHERE userId = :userId AND itemId = :itemId LIMIT 1")
    suspend fun getItemByItemId(userId: Long, itemId: String): InventoryItem?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: InventoryItem): Long
    
    @Update
    suspend fun updateItem(item: InventoryItem)
    
    @Delete
    suspend fun deleteItem(item: InventoryItem)
    
    @Query("DELETE FROM inventory_items WHERE id = :itemId")
    suspend fun deleteItemById(itemId: Long)
    
    @Query("DELETE FROM inventory_items WHERE userId = :userId")
    suspend fun deleteAllItems(userId: Long)
    
    @Query("SELECT COUNT(*) FROM inventory_items WHERE userId = :userId")
    fun getItemCount(userId: Long): Flow<Int>
    
    @Query("SELECT SUM(quantity) FROM inventory_items WHERE userId = :userId")
    fun getTotalQuantity(userId: Long): Flow<Int?>
    
    // 增加物品数量
    @Query("UPDATE inventory_items SET quantity = quantity + :amount WHERE id = :itemId")
    suspend fun increaseQuantity(itemId: Long, amount: Int)
    
    // 减少物品数量
    @Query("UPDATE inventory_items SET quantity = quantity - :amount WHERE id = :itemId")
    suspend fun decreaseQuantity(itemId: Long, amount: Int)
    
    // � 安全修复：原本无 userId 过滤会读出所有用户的物品（隐私泄漏 + 跨账户错乱）
    @Query("SELECT * FROM inventory_items WHERE userId = :userId ORDER BY obtainedTime DESC")
    suspend fun getAllItemsList(userId: Long): List<InventoryItem>
    
    // 🔥 更新物品名称
    @Query("UPDATE inventory_items SET itemName = :newName WHERE id = :itemId")
    suspend fun updateItemName(itemId: Long, newName: String)
}
