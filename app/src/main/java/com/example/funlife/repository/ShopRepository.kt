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
    
    suspend fun insertShopItem(item: ShopItem) = shopDao.insertShopItem(item)
    
    // 初始化商城商品
    suspend fun initializeShopItems() {
        // 使用一个简单的查询来检查是否已有商品
        val count = shopDao.getShopItemCount()
        
        if (count == 0) {
            // 添加默认商品
            val defaultItems = listOf(
                ShopItem(
                    name = "补卡卡片",
                    description = "可以补签一次错过的打卡",
                    icon = "🎫",
                    price = 50,
                    type = "makeup_card",
                    value = 1
                ),
                ShopItem(
                    name = "补卡礼包",
                    description = "一次性获得5张补卡卡片",
                    icon = "🎁",
                    price = 200,
                    type = "makeup_card",
                    value = 5
                ),
                ShopItem(
                    name = "金币袋",
                    description = "获得100金币",
                    icon = "💰",
                    price = 0,
                    type = "coins",
                    value = 100,
                    isAvailable = false
                ),
                ShopItem(
                    name = "转盘次数+1",
                    description = "额外获得1次转盘机会",
                    icon = "🎰",
                    price = 30,
                    type = "spin_chance",
                    value = 1
                ),
                ShopItem(
                    name = "转盘礼包",
                    description = "额外获得5次转盘机会",
                    icon = "🎲",
                    price = 120,
                    type = "spin_chance",
                    value = 5
                ),
                ShopItem(
                    name = "宠物食物",
                    description = "喂养宠物，增加亲密度",
                    icon = "🍖",
                    price = 20,
                    type = "pet_food",
                    value = 1
                ),
                ShopItem(
                    name = "宠物玩具",
                    description = "和宠物玩耍，增加快乐值",
                    icon = "🎾",
                    price = 35,
                    type = "pet_toy",
                    value = 1
                ),
                ShopItem(
                    name = "宠物零食礼包",
                    description = "包含5份宠物食物",
                    icon = "🍗",
                    price = 80,
                    type = "pet_food",
                    value = 5
                ),
                ShopItem(
                    name = "幸运符",
                    description = "提升转盘中奖概率",
                    icon = "🍀",
                    price = 100,
                    type = "lucky_charm",
                    value = 1
                ),
                ShopItem(
                    name = "经验加速卡",
                    description = "宠物经验获取速度翻倍",
                    icon = "⚡",
                    price = 150,
                    type = "exp_boost",
                    value = 1
                ),
                ShopItem(
                    name = "彩虹主题",
                    description = "解锁转盘彩虹主题",
                    icon = "🌈",
                    price = 300,
                    type = "theme",
                    value = 1
                ),
                ShopItem(
                    name = "星空主题",
                    description = "解锁转盘星空主题",
                    icon = "✨",
                    price = 300,
                    type = "theme",
                    value = 1
                ),
                ShopItem(
                    name = "成就徽章",
                    description = "展示你的成就",
                    icon = "🏆",
                    price = 500,
                    type = "badge",
                    value = 1
                ),
                ShopItem(
                    name = "VIP月卡",
                    description = "30天VIP特权",
                    icon = "👑",
                    price = 1000,
                    type = "vip",
                    value = 30
                )
            )
            
            defaultItems.forEach { item ->
                insertShopItem(item)
            }
        }
    }
}
