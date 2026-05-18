// InventoryViewModel.kt - 背包视图模型
package com.example.funlife.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.funlife.data.model.InventoryItem
import com.example.funlife.data.model.InventoryItemType
import com.example.funlife.repository.InventoryRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class InventoryViewModel(
    private val repository: InventoryRepository,
    private val userPreferencesDao: com.example.funlife.data.dao.UserPreferencesDao,
    private val userVipDao: com.example.funlife.data.dao.UserVipDao, // 🔥 新增VIP DAO
    private val userAvatarDao: com.example.funlife.data.dao.UserAvatarDao // 🔥 新增UserAvatar DAO用于检查头像
) : ViewModel() {
    
    // 🔥 当前用户ID
    private val _userId = MutableStateFlow(1L)
    
    // 🔥 消息提示
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()
    
    // 🔥 用户VIP状态
    val userVip = _userId.flatMapLatest { userId ->
        userVipDao.getUserVip(userId)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )
    
    // 🔥 背包容量限制
    val inventoryCapacity: StateFlow<Int> = userVip.map { vip ->
        val isVip = vip?.isVip() == true && vip.isExpired() == false
        if (isVip) {
            // 根据VIP等级设置容量
            when (vip?.getCurrentVipLevel()) {
                com.example.funlife.data.model.VipLevel.VIP3 -> Int.MAX_VALUE  // 🔥 终身VIP无限容量
                com.example.funlife.data.model.VipLevel.VIP2 -> 5000  // VIP2: 5000
                com.example.funlife.data.model.VipLevel.VIP1 -> 1000  // VIP1: 1000
                else -> 100  // 普通用户: 100
            }
        } else {
            100  // 普通用户: 100
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 100
    )
    
    // 当前选中的物品类型过滤
    private val _selectedType = MutableStateFlow<InventoryItemType?>(null)
    val selectedType: StateFlow<InventoryItemType?> = _selectedType.asStateFlow()
    
    // 当前装备的面板皮肤
    val equippedPanelSkin: StateFlow<String> = userPreferencesDao.getPreferences(1L)
        .map { prefs -> prefs?.spinResultPanelSkin ?: "js_1" }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "js_1"
        )
    
    // 🔥 新增：当前装备的按钮皮肤
    val equippedButtonSkin: StateFlow<String> = userPreferencesDao.getPreferences(1L)
        .map { prefs -> prefs?.spinButtonSkin ?: "pf_1" }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "pf_1"
        )
    
    // 所有物品
    private val allItems = repository.getAllItems()
    
    // 根据类型过滤的物品
    val items: StateFlow<List<InventoryItem>> = combine(
        allItems,
        _selectedType
    ) { items, type ->
        if (type == null) {
            items
        } else {
            items.filter { it.itemType == type }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    
    // 物品总数
    val itemCount: StateFlow<Int> = repository.getItemCount()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )
    
    // 物品总数量
    val totalQuantity: StateFlow<Int> = repository.getTotalQuantity()
        .map { it ?: 0 }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )
    
    // 按类型统计
    val itemsByType: StateFlow<Map<InventoryItemType, Int>> = allItems
        .map { items ->
            items.groupBy { it.itemType }
                .mapValues { (_, list) -> list.sumOf { it.quantity } }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )
    
    // 选中的物品详情
    private val _selectedItem = MutableStateFlow<InventoryItem?>(null)
    val selectedItem: StateFlow<InventoryItem?> = _selectedItem.asStateFlow()
    
    /**
     * 🔥 设置用户ID
     */
    fun setUserId(userId: Long) {
        _userId.value = userId
    }
    
    /**
     * 🔥 检查是否可以添加物品（容量检查）
     */
    suspend fun canAddItem(quantity: Int = 1): Boolean {
        val currentQuantity = repository.getTotalQuantity().first() ?: 0
        val capacity = inventoryCapacity.value
        return (currentQuantity + quantity) <= capacity
    }
    
    /**
     * 🔥 获取剩余容量
     */
    fun getRemainingCapacity(): StateFlow<Int> {
        return combine(totalQuantity, inventoryCapacity) { current, max ->
            max - current
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )
    }
    
    /**
     * 设置类型过滤
     */
    fun setTypeFilter(type: InventoryItemType?) {
        _selectedType.value = type
    }
    
    /**
     * 添加物品到背包（带容量检查）
     */
    fun addItem(item: InventoryItem, onCapacityExceeded: () -> Unit = {}) {
        viewModelScope.launch {
            if (canAddItem(item.quantity)) {
                repository.addItem(item)
            } else {
                onCapacityExceeded()
            }
        }
    }
    
    /**
     * 使用物品
     */
    fun useItem(item: InventoryItem, amount: Int = 1, onSuccess: (InventoryItem) -> Unit = {}) {
        viewModelScope.launch {
            val success = repository.useItem(item.id, amount)
            if (success) {
                onSuccess(item)
            }
        }
    }
    
    /**
     * 删除物品
     */
    fun deleteItem(item: InventoryItem) {
        viewModelScope.launch {
            repository.deleteItem(item)
        }
    }
    
    /**
     * 选中物品
     */
    fun selectItem(item: InventoryItem?) {
        _selectedItem.value = item
    }
    
    /**
     * 装备面板皮肤
     */
    fun equipPanelSkin(skinName: String) {
        viewModelScope.launch {
            try {
                // 先检查用户偏好是否存在
                val prefs = userPreferencesDao.getPreferencesSync(1L)
                
                if (prefs == null) {
                    // 如果不存在，创建默认偏好
                    val defaultPrefs = com.example.funlife.data.model.UserPreferences(
                        userId = 1L,
                        spinResultPanelSkin = skinName
                    )
                    userPreferencesDao.insertPreferences(defaultPrefs)
                } else {
                    // 如果存在，更新皮肤
                    userPreferencesDao.updateSpinResultPanelSkin(1L, skinName)
                }
            } catch (e: Exception) {
                android.util.Log.e("InventoryViewModel", "Error equipping panel skin", e)
            }
        }
    }
    
    /**
     * 🔥 新增：装备按钮皮肤
     */
    fun equipButtonSkin(skinName: String) {
        viewModelScope.launch {
            try {
                // 先检查用户偏好是否存在
                val prefs = userPreferencesDao.getPreferencesSync(1L)
                
                if (prefs == null) {
                    // 如果不存在，创建默认偏好
                    val defaultPrefs = com.example.funlife.data.model.UserPreferences(
                        userId = 1L,
                        spinButtonSkin = skinName
                    )
                    userPreferencesDao.insertPreferences(defaultPrefs)
                } else {
                    // 如果存在，更新皮肤
                    userPreferencesDao.updateSpinButtonSkin(1L, skinName)
                }
            } catch (e: Exception) {
                android.util.Log.e("InventoryViewModel", "Error equipping button skin", e)
            }
        }
    }
    
    /**
     * 清空背包
     */
    fun clearInventory() {
        viewModelScope.launch {
            repository.clearInventory()
        }
    }
    
    /**
     * 🔥 获取用户拥有的纪念日相框
     */
    fun getAnniversaryFrames(): StateFlow<List<InventoryItem>> {
        return allItems
            .map { items ->
                items.filter { it.itemType == InventoryItemType.ANNIVERSARY_FRAME }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
    }
    
    /**
     * 🔥 装备头像框
     */
    fun equipAvatarFrame(frameAssetPath: String) {
        viewModelScope.launch {
            try {
                val userId = _userId.value
                
                // 🔥 检查用户是否上传了头像（从user_avatars表检查）
                val userAvatar = userAvatarDao.getUserAvatar(userId).first()
                if (userAvatar?.avatarUri.isNullOrEmpty()) {
                    android.util.Log.w("InventoryViewModel", "Cannot equip frame: user has no avatar")
                    _message.value = "请先上传头像后再装备头像框"
                    return@launch
                }
                
                // 先检查用户偏好是否存在
                val prefs = userPreferencesDao.getPreferencesSync(userId)
                
                if (prefs == null) {
                    // 如果不存在，创建默认偏好
                    val defaultPrefs = com.example.funlife.data.model.UserPreferences(
                        userId = userId,
                        equippedAvatarFrame = frameAssetPath
                    )
                    userPreferencesDao.insertPreferences(defaultPrefs)
                    android.util.Log.d("InventoryViewModel", "Created preferences and equipped frame: $frameAssetPath")
                } else {
                    // 如果存在，更新头像框
                    userPreferencesDao.updateEquippedAvatarFrame(userId, frameAssetPath)
                    android.util.Log.d("InventoryViewModel", "Updated equipped frame: $frameAssetPath")
                }
                
                _message.value = "装备成功！"
            } catch (e: Exception) {
                android.util.Log.e("InventoryViewModel", "Error equipping avatar frame", e)
                _message.value = "装备失败: ${e.message}"
            }
        }
    }
    
    /**
     * 清除消息
     */
    fun clearMessage() {
        _message.value = null
    }
    
    /**
     * 🔥 获取当前装备的头像框
     */
    val equippedAvatarFrame: StateFlow<String?> = userPreferencesDao.getPreferences(1L)
        .map { prefs -> prefs?.equippedAvatarFrame }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )
}
