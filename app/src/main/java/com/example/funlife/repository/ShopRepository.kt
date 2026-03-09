// ShopRepository.kt - 商城仓库
package com.example.funlife.repository

import com.example.funlife.data.dao.ShopDao
import com.example.funlife.data.model.ShopItem
import com.example.funlife.data.model.PurchaseHistory
import kotlinx.coroutines.flow.Flow

class ShopRepository(private val shopDao: ShopDao) {
    
    val allShopItems: Flow<List<ShopItem>> = shopDao.getAllShopItems()
    
    val purchaseHistory: Flow<List<PurchaseHistory>> = shopDao.getPurchaseHistory()
    
    suspend fun getShopItem(itemId: Int) = shopDao.getShopItem(itemId)
    
    suspend fun insertPurchaseHistory(purchase: PurchaseHistory) = shopDao.insertPurchaseHistory(purchase)
    
    suspend fun getPurchaseCount(itemId: Int) = shopDao.getPurchaseCount(itemId)
}
