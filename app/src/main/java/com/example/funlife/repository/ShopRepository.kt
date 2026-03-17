// ShopRepository.kt - 商城仓库
package com.example.funlife.repository

import com.example.funlife.data.dao.ShopDao
import com.example.funlife.data.model.ShopItem
import com.example.funlife.data.model.PurchaseHistory
import kotlinx.coroutines.flow.Flow

class ShopRepository(private val shopDao: ShopDao) {
    
    val allShopItems: Flow<List<ShopItem>> = shopDao.getAllShopItems()
    
    fun getPurchaseHistory(userId: Long): Flow<List<PurchaseHistory>> = shopDao.getPurchaseHistory(userId)
    
    suspend fun getShopItem(itemId: Int) = shopDao.getShopItem(itemId)
    
    suspend fun insertPurchaseHistory(purchase: PurchaseHistory) = shopDao.insertPurchaseHistory(purchase)
    
    suspend fun getPurchaseCount(userId: Long, itemId: Int) = shopDao.getPurchaseCount(userId, itemId)
}
