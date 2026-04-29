// PetItemDao.kt - 宠物物品数据访问对象
package com.example.funlife.data.dao

import androidx.room.*
import com.example.funlife.data.model.PetItem
import com.example.funlife.data.model.ItemType
import kotlinx.coroutines.flow.Flow

@Dao
interface PetItemDao {
    @Query("SELECT * FROM pet_items WHERE userId = :userId")
    fun getItemsByUserId(userId: Long): Flow<List<PetItem>>
    
    @Query("SELECT * FROM pet_items WHERE userId = :userId AND itemType = :type")
    fun getItemsByType(userId: Long, type: ItemType): Flow<List<PetItem>>
    
    @Query("SELECT * FROM pet_items WHERE userId = :userId AND itemId = :itemId LIMIT 1")
    suspend fun getItemByItemId(userId: Long, itemId: Int): PetItem?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: PetItem): Long
    
    @Update
    suspend fun updateItem(item: PetItem)
    
    @Delete
    suspend fun deleteItem(item: PetItem)
    
    @Query("UPDATE pet_items SET quantity = quantity + :amount WHERE userId = :userId AND itemId = :itemId")
    suspend fun addItemQuantity(userId: Long, itemId: Int, amount: Int)
    
    @Query("UPDATE pet_items SET quantity = quantity - :amount WHERE userId = :userId AND itemId = :itemId AND quantity >= :amount")
    suspend fun reduceItemQuantity(userId: Long, itemId: Int, amount: Int): Int
    
    @Query("DELETE FROM pet_items WHERE userId = :userId AND itemId = :itemId AND quantity <= 0")
    suspend fun deleteEmptyItems(userId: Long, itemId: Int)
}
