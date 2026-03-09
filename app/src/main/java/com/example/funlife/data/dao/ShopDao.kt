// ShopDao.kt - 商城数据访问对象
package com.example.funlife.data.dao

import androidx.room.*
import com.example.funlife.data.model.ShopItem
import com.example.funlife.data.model.PurchaseHistory
import kotlinx.coroutines.flow.Flow

@Dao
interface ShopDao {
    
    @Query("SELECT * FROM shop_items WHERE isAvailable = 1 ORDER BY price ASC")
    fun getAllShopItems(): Flow<List<ShopItem>>
    
    @Query("SELECT * FROM shop_items WHERE id = :itemId")
    suspend fun getShopItem(itemId: Int): ShopItem?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShopItem(item: ShopItem)
    
    @Insert
    suspend fun insertPurchaseHistory(purchase: PurchaseHistory)
    
    @Query("SELECT * FROM purchase_history ORDER BY timestamp DESC")
    fun getPurchaseHistory(): Flow<List<PurchaseHistory>>
    
    @Query("SELECT COUNT(*) FROM purchase_history WHERE itemId = :itemId")
    suspend fun getPurchaseCount(itemId: Int): Int
}
