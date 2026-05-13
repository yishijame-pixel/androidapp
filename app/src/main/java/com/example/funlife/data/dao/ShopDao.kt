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
    
    @Query("SELECT COUNT(*) FROM shop_items")
    suspend fun getShopItemCount(): Int
    
    @Query("SELECT * FROM shop_items WHERE id = :itemId")
    suspend fun getShopItem(itemId: Int): ShopItem?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShopItem(item: ShopItem)
    
    @Insert
    suspend fun insertPurchaseHistory(purchase: PurchaseHistory)
    
    @Query("SELECT * FROM purchase_history WHERE userId = :userId ORDER BY timestamp DESC")
    fun getPurchaseHistory(userId: Long): Flow<List<PurchaseHistory>>
    
    @Query("SELECT COUNT(*) FROM purchase_history WHERE userId = :userId AND itemId = :itemId")
    suspend fun getPurchaseCount(userId: Long, itemId: Int): Int
    
    @Query("SELECT * FROM shop_items WHERE type = :type")
    suspend fun getShopItemsByType(type: String): List<ShopItem>
    
    // 🔥 新增：头像框相关查询
    
    /**
     * 获取所有头像框商品
     */
    @Query("SELECT * FROM shop_items WHERE type = 'avatar_frame' ORDER BY sortOrder ASC, price ASC")
    fun getAvatarFrames(): Flow<List<ShopItem>>
    
    /**
     * 根据稀有度获取头像框
     */
    @Query("SELECT * FROM shop_items WHERE type = 'avatar_frame' AND rarity = :rarity ORDER BY sortOrder ASC")
    fun getAvatarFramesByRarity(rarity: String): Flow<List<ShopItem>>
    
    /**
     * 根据分类获取头像框
     */
    @Query("SELECT * FROM shop_items WHERE type = 'avatar_frame' AND category = :category ORDER BY sortOrder ASC")
    fun getAvatarFramesByCategory(category: String): Flow<List<ShopItem>>
    
    /**
     * 获取动态头像框
     */
    @Query("SELECT * FROM shop_items WHERE type = 'avatar_frame' AND isAnimated = 1 ORDER BY sortOrder ASC")
    fun getAnimatedFrames(): Flow<List<ShopItem>>
    
    /**
     * 获取静态头像框
     */
    @Query("SELECT * FROM shop_items WHERE type = 'avatar_frame' AND isAnimated = 0 ORDER BY sortOrder ASC")
    fun getStaticFrames(): Flow<List<ShopItem>>
    
    /**
     * 删除所有头像框商品（用于重新初始化）
     */
    @Query("DELETE FROM shop_items WHERE type = 'avatar_frame'")
    suspend fun deleteAllAvatarFrames()
}
