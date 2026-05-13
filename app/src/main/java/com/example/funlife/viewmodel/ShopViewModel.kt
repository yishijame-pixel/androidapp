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
    private val shopRepository = ShopRepository(
        database.shopDao(),
        database.userAvatarFrameDao()  // 🔥 添加UserAvatarFrameDao
    )
    private val coinRepository = CoinRepository(database.coinDao())
    private val habitRepository = HabitRepository(database.habitDao())
    private val dailyRewardDao = database.dailyRewardDao()
    private val userDao = database.userDao()  // 🔥 添加UserDao
    private val sessionManager = com.example.funlife.utils.UserSessionManager(application)
    
    // 🔥 新增：获取当前用户ID
    private fun getCurrentUserId(): Long {
        return sessionManager.getCurrentUserId().takeIf { it > 0 } ?: 0L
    }
    
    // 🔥 新增：检查今天是否已经领取免费金币
    private val _canClaimFreeCoins = MutableStateFlow(true)
    val canClaimFreeCoins: StateFlow<Boolean> = _canClaimFreeCoins.asStateFlow()
    
    // 🔥 新增：消息提示
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()
    
    init {
        viewModelScope.launch {
            coinRepository.initializeCoins(getCurrentUserId())
            shopRepository.initializeShopItems()
            // 🔥 初始化头像框
            shopRepository.initializeAvatarFrames(application)
            checkDailyRewardStatus()
            // 🔥 更新旧的按钮皮肤名称
            updateOldButtonSkinNames()
        }
    }
    
    // 🔥 更新旧的按钮皮肤名称（从"按钮皮肤 X"更新为诗意名称）
    private suspend fun updateOldButtonSkinNames() {
        val buttonSkinNameMap = mapOf(
            1 to "初心如故",
            2 to "粉黛流年",
            3 to "碧海青天",
            4 to "翠竹凝烟",
            5 to "紫气东来",
            6 to "橙黄橘绿",
            7 to "丹霞映日",
            8 to "金风玉露",
            9 to "青山绿水",
            10 to "金碧辉煌",
            11 to "银装素裹",
            12 to "霓虹幻彩",
            13 to "星河璀璨",
            14 to "烈焰焚天",
            15 to "冰清玉洁",
            16 to "雷霆万钧",
            17 to "林深见鹿",
            18 to "沧海桑田",
            19 to "大漠孤烟",
            20 to "极光流转",
            21 to "樱花烂漫",
            22 to "枫叶如丹",
            23 to "雪舞轻扬",
            24 to "星辰大海",
            25 to "月华如水",
            26 to "传世经典"
        )
        
        // 从数据库获取所有按钮皮肤商品
        val allButtonSkins = database.inventoryDao().getAllItemsList()
            .filter { it.itemId.startsWith("button_pf_") }
        
        allButtonSkins.forEach { item ->
            // 提取按钮编号
            val buttonNumber = item.itemId.removePrefix("button_pf_").toIntOrNull()
            if (buttonNumber != null && buttonNumber in buttonSkinNameMap) {
                val newName = buttonSkinNameMap[buttonNumber]!!
                // 如果名称是旧格式，更新为新名称
                if (item.itemName.startsWith("按钮皮肤") || item.itemName == "按钮皮肤 $buttonNumber") {
                    database.inventoryDao().updateItemName(item.id, newName)
                    android.util.Log.d("ShopViewModel", "Updated button skin ${item.itemId} name to: $newName")
                }
            }
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
    
    // 🔥 ========== 头像框相关方法 ==========
    
    /**
     * 获取所有头像框
     */
    val avatarFrames: StateFlow<List<ShopItem>> = shopRepository.getAvatarFrames()
        .catch { e ->
            android.util.Log.e("ShopViewModel", "Error loading avatar frames", e)
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    /**
     * 获取用户拥有的头像框
     */
    val userOwnedFrames: StateFlow<List<com.example.funlife.data.model.UserAvatarFrame>> = 
        shopRepository.getUserOwnedFrames(getCurrentUserId())
            .catch { e ->
                android.util.Log.e("ShopViewModel", "Error loading owned frames", e)
                emit(emptyList())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
    
    /**
     * 检查用户是否为VIP
     */
    val isUserVip: StateFlow<Boolean> = userDao.isUserVipFlow(getCurrentUserId())
        .catch { e ->
            android.util.Log.e("ShopViewModel", "Error checking VIP status", e)
            emit(false)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )
    
    /**
     * 购买头像框
     */
    fun purchaseAvatarFrame(frame: ShopItem) {
        viewModelScope.launch {
            try {
                val userId = getCurrentUserId()
                val isVip = isUserVip.value
                val price = if (isVip) frame.vipPrice else frame.price
                
                // 检查是否已拥有
                val owned = shopRepository.isFrameOwned(userId, frame.id)
                if (owned) {
                    _message.value = "您已拥有此头像框"
                    return@launch
                }
                
                // 检查金币是否足够
                val canAfford = canAfford(price)
                if (!canAfford) {
                    _message.value = "金币不足，需要 $price 金币"
                    return@launch
                }
                
                // 扣除金币
                val success = coinRepository.spendCoins(userId, price)
                if (success) {
                    // 添加到用户背包
                    shopRepository.addUserFrame(userId, frame.id)
                    
                    // 记录购买历史
                    val purchase = PurchaseHistory(
                        userId = userId,
                        itemId = frame.id,
                        itemName = frame.name,
                        price = price,
                        timestamp = LocalDateTime.now().toString()
                    )
                    shopRepository.insertPurchaseHistory(purchase)
                    
                    _message.value = "购买成功！"
                    android.util.Log.d("ShopViewModel", "Purchased frame: ${frame.name}")
                } else {
                    _message.value = "购买失败"
                }
            } catch (e: Exception) {
                android.util.Log.e("ShopViewModel", "Error purchasing frame", e)
                _message.value = "购买失败: ${e.message}"
            }
        }
    }
    
    /**
     * 装备头像框
     */
    fun equipAvatarFrame(frameId: Int) {
        viewModelScope.launch {
            try {
                val userId = getCurrentUserId()
                
                // 检查是否拥有
                val owned = shopRepository.isFrameOwned(userId, frameId)
                if (!owned) {
                    _message.value = "您还未拥有此头像框"
                    return@launch
                }
                
                // 装备头像框
                shopRepository.equipFrame(userId, frameId)
                userDao.updateEquippedFrame(userId, frameId)
                
                _message.value = "装备成功！"
                android.util.Log.d("ShopViewModel", "Equipped frame: $frameId")
            } catch (e: Exception) {
                android.util.Log.e("ShopViewModel", "Error equipping frame", e)
                _message.value = "装备失败: ${e.message}"
            }
        }
    }
    
    /**
     * 卸下头像框
     */
    fun unequipAvatarFrame() {
        viewModelScope.launch {
            try {
                val userId = getCurrentUserId()
                shopRepository.unequipFrame(userId)
                userDao.updateEquippedFrame(userId, null)
                
                _message.value = "已卸下头像框"
                android.util.Log.d("ShopViewModel", "Unequipped frame")
            } catch (e: Exception) {
                android.util.Log.e("ShopViewModel", "Error unequipping frame", e)
                _message.value = "操作失败: ${e.message}"
            }
        }
    }
    
    /**
     * 清除消息
     */
    fun clearMessage() {
        _message.value = null
    }
}
