// ShopViewModel.kt - 商城视图模型
package com.example.funlife.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.funlife.data.database.AppDatabase
import com.example.funlife.data.model.ShopItem
import com.example.funlife.data.model.PurchaseHistory
import com.example.funlife.data.model.UserCoins
import com.example.funlife.repository.CoinRepository
import com.example.funlife.repository.ShopRepository
import com.example.funlife.repository.HabitRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class ShopViewModel(application: Application) : AndroidViewModel(application) {
    
    private val database = AppDatabase.getDatabase(application)
    private val shopRepository = ShopRepository(database.shopDao())
    private val coinRepository = CoinRepository(database.coinDao())
    private val habitRepository = HabitRepository(database.habitDao())
    private val dailyRewardDao = database.dailyRewardDao()
    private val sessionManager = com.example.funlife.utils.UserSessionManager(application)
    
    // 🔥 新增：获取当前用户ID
    private fun getCurrentUserId(): Long {
        return sessionManager.getCurrentUserId().takeIf { it > 0 } ?: 0L
    }
    
    // 🔥 新增：检查今天是否已经领取免费金币
    private val _canClaimFreeCoins = MutableStateFlow(true)
    val canClaimFreeCoins: StateFlow<Boolean> = _canClaimFreeCoins.asStateFlow()
    
    init {
        viewModelScope.launch {
            coinRepository.initializeCoins(getCurrentUserId())
            shopRepository.initializeShopItems()
            checkDailyRewardStatus()
        }
    }
    
    // 检查每日奖励状态
    private suspend fun checkDailyRewardStatus() {
        val userId = getCurrentUserId()
        val today = java.time.LocalDate.now().toString()
        val reward = dailyRewardDao.getDailyReward(userId, "free_coins")
        
        _canClaimFreeCoins.value = reward == null || reward.lastClaimDate != today
    }
    
    // 领取免费金币
    suspend fun claimFreeCoins(): Boolean {
        val userId = getCurrentUserId()
        val today = java.time.LocalDate.now().toString()
        
        // 再次检查是否可以领取
        val reward = dailyRewardDao.getDailyReward(userId, "free_coins")
        if (reward != null && reward.lastClaimDate == today) {
            return false // 今天已经领取过了
        }
        
        // 添加金币
        coinRepository.addCoins(userId, 100)
        
        // 更新或创建领取记录
        if (reward == null) {
            dailyRewardDao.insertDailyReward(
                com.example.funlife.data.model.DailyReward(
                    userId = userId,
                    rewardType = "free_coins",
                    lastClaimDate = today,
                    claimCount = 1
                )
            )
        } else {
            dailyRewardDao.updateClaimDate(userId, "free_coins", today)
        }
        
        // 更新状态
        _canClaimFreeCoins.value = false
        return true
    }
    
    val shopItems: StateFlow<List<ShopItem>> = shopRepository.allShopItems
        .catch { e ->
            android.util.Log.e("ShopViewModel", "Error loading shop items", e)
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    val userCoins: StateFlow<UserCoins?> = coinRepository.getUserCoins(getCurrentUserId())
        .catch { e ->
            android.util.Log.e("ShopViewModel", "Error loading user coins", e)
            emit(null)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )
    
    val purchaseHistory: StateFlow<List<PurchaseHistory>> = shopRepository.getPurchaseHistory(getCurrentUserId())
        .catch { e ->
            android.util.Log.e("ShopViewModel", "Error loading purchase history", e)
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    fun purchaseItem(item: ShopItem, habitId: Int? = null) {
        viewModelScope.launch {
            val userId = getCurrentUserId()
            val success = coinRepository.spendCoins(userId, item.price)
            if (success) {
                // 记录购买历史
                val purchase = PurchaseHistory(
                    userId = userId,
                    itemId = item.id,
                    itemName = item.name,
                    price = item.price,
                    timestamp = LocalDateTime.now().toString()
                )
                shopRepository.insertPurchaseHistory(purchase)
                
                // 根据商品类型执行相应操作
                when (item.type) {
                    "makeup_card" -> {
                        // 给指定习惯添加补卡卡片
                        habitId?.let {
                            val currentCards = habitRepository.getMakeupCards(userId, it)
                            habitRepository.updateMakeupCards(userId, it, currentCards + item.value)
                        }
                    }
                    "coins" -> {
                        // 添加金币
                        coinRepository.addCoins(userId, item.value)
                    }
                    // 其他类型可以在这里扩展
                }
            }
        }
    }
    
    suspend fun canAfford(price: Int): Boolean {
        return coinRepository.getCoinsAmount(getCurrentUserId()) >= price
    }
    
    // 简单的购买方法（仅扣除金币）
    suspend fun purchaseItem(price: Int): Boolean {
        val userId = getCurrentUserId()
        return coinRepository.spendCoins(userId, price)
    }
}
