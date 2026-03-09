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
    
    val shopItems: StateFlow<List<ShopItem>> = shopRepository.allShopItems.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    
    val userCoins: StateFlow<UserCoins?> = coinRepository.userCoins.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )
    
    val purchaseHistory: StateFlow<List<PurchaseHistory>> = shopRepository.purchaseHistory.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    
    init {
        viewModelScope.launch {
            coinRepository.initializeCoins()
        }
    }
    
    fun purchaseItem(item: ShopItem, habitId: Int? = null) {
        viewModelScope.launch {
            val success = coinRepository.spendCoins(item.price)
            if (success) {
                // 记录购买历史
                val purchase = PurchaseHistory(
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
                            val currentCards = habitRepository.getMakeupCards(it)
                            habitRepository.updateMakeupCards(it, currentCards + item.value)
                        }
                    }
                    "coins" -> {
                        // 添加金币
                        coinRepository.addCoins(item.value)
                    }
                    // 其他类型可以在这里扩展
                }
            }
        }
    }
    
    suspend fun canAfford(price: Int): Boolean {
        return coinRepository.getCoinsAmount() >= price
    }
}
