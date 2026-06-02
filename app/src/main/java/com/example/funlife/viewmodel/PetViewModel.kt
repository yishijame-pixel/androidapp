// PetViewModel.kt - 宠物视图模型
package com.example.funlife.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.funlife.data.model.*
import com.example.funlife.repository.PetRepository
import com.example.funlife.repository.PetItemRepository
import com.example.funlife.repository.CoinRepository
import com.example.funlife.utils.PetMissionHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class PetViewModel(
    private val petRepository: PetRepository,
    private val petItemRepository: PetItemRepository,
    private val coinRepository: CoinRepository,
    private val userId: Long,
    private val appContext: Context
) : ViewModel() {
    
    // ━━━━━━━━━━━━━━━━━ 每日任务 ━━━━━━━━━━━━━━━━━
    private val _missions = MutableStateFlow<List<PetMissionHelper.MissionState>>(emptyList())
    val missions: StateFlow<List<PetMissionHelper.MissionState>> = _missions.asStateFlow()
    
    // 操作反馈
    private val _toast = MutableStateFlow<String?>(null)
    val toast: StateFlow<String?> = _toast.asStateFlow()
    fun consumeToast() { _toast.value = null }
    
    private fun refreshMissions() {
        // 🔒 按当前用户读取任务状态
        _missions.value = PetMissionHelper.getMissions(appContext, userId)
    }
    
    private fun trackMission(type: PetMissionHelper.MissionType) {
        PetMissionHelper.increment(appContext, userId, type)
        refreshMissions()
    }

    // 🔒 防刷金币：仅当每日任务进度还没满时，互动小奖励才发放（满了仍可继续互动）
    //    互动后再 increment，所以这里要在 increment 之前判断当前进度
    private fun shouldAwardInteractionCoin(type: PetMissionHelper.MissionType): Boolean {
        val now = PetMissionHelper.getMissions(appContext, userId).firstOrNull { it.type == type }
        return (now?.progress ?: 0) < type.target
    }
    
    /** 领取任务奖励 */
    fun claimMission(type: PetMissionHelper.MissionType) {
        viewModelScope.launch {
            if (PetMissionHelper.claim(appContext, userId, type)) {
                coinRepository.addCoins(userId, type.reward)
                _toast.value = "🎉 领取了 ${type.reward} 金币！"
                refreshMissions()
            }
        }
    }
    
    // 宠物状态
    val pet: StateFlow<Pet?> = petRepository.getPetByUserId(userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    
    // 物品列表
    val items: StateFlow<List<PetItem>> = petItemRepository.getItemsByUserId(userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    // 用户金币
    val userCoins: StateFlow<UserCoins?> = coinRepository.getUserCoins(userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    
    // UI 状态
    private val _uiState = MutableStateFlow<PetUiState>(PetUiState.Loading)
    val uiState: StateFlow<PetUiState> = _uiState.asStateFlow()
    
    // 动画状态
    private val _animationState = MutableStateFlow<AnimationState>(AnimationState.Idle)
    val animationState: StateFlow<AnimationState> = _animationState.asStateFlow()
    
    init {
        loadPet()
        refreshMissions()
        startStatusTicker()
    }
    
    private fun loadPet() {
        viewModelScope.launch {
            val existingPet = petRepository.getPetByUserIdSync(userId)
            if (existingPet == null) {
                _uiState.value = PetUiState.NoPet
            } else {
                // 更新宠物状态（时间衰减）
                petRepository.updatePetStatus(existingPet.id)
                _uiState.value = PetUiState.Success
            }
        }
    }
    
    /** 每 30 秒触发一次状态衰减计算（基于真实分钟数，所以不会过快） */
    private fun startStatusTicker() {
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(30_000L)
                pet.value?.let { petRepository.updatePetStatus(it.id) }
            }
        }
    }
    
    // 创建宠物
    fun createPet(name: String, type: PetType) {
        viewModelScope.launch {
            val newPet = Pet(
                userId = userId,
                name = name,
                type = type
            )
            petRepository.createPet(newPet)
            _uiState.value = PetUiState.Success
        }
    }
    
    // 喂食（可选择不同食物 itemId，仅 itemId in [2,3] 时消耗库存）
    fun feedPet(itemId: Int = PetItems.BASIC_FOOD.id) {
        viewModelScope.launch {
            val currentPet = pet.value ?: return@launch
            val item = PetItems.getItemById(itemId) ?: return@launch
            
            // 高级/零食需要消耗库存，普通食物免费
            if (itemId != PetItems.BASIC_FOOD.id) {
                val ok = petItemRepository.useItem(userId, itemId)
                if (!ok) {
                    _toast.value = "背包里没有这个食物"
                    return@launch
                }
            }
            
            _animationState.value = AnimationState.Feeding
            petRepository.feed(currentPet.id, item.hungerBonus.coerceAtLeast(20))
            if (item.moodBonus > 0) petRepository.play(currentPet.id, item.moodBonus)
            petRepository.addExperience(currentPet.id, 5)
            petRepository.addIntimacy(currentPet.id, 2)
            
            // 奖励 + 任务（仅在今日任务 target 内发金币）
            val award = shouldAwardInteractionCoin(PetMissionHelper.MissionType.FEED)
            if (award) coinRepository.addCoins(userId, 1)
            trackMission(PetMissionHelper.MissionType.FEED)
            _toast.value = if (award) "${item.name} 好好吃 ＋1 💰" else "${item.name} 好好吃"
            
            kotlinx.coroutines.delay(2000)
            _animationState.value = AnimationState.Idle
        }
    }
    
    // 洗澡
    fun cleanPet() {
        viewModelScope.launch {
            val currentPet = pet.value ?: return@launch
            _animationState.value = AnimationState.Cleaning
            petRepository.clean(currentPet.id, 50)
            petRepository.addExperience(currentPet.id, 5)
            petRepository.addIntimacy(currentPet.id, 3)
            
            val award = shouldAwardInteractionCoin(PetMissionHelper.MissionType.CLEAN)
            if (award) coinRepository.addCoins(userId, 2)
            trackMission(PetMissionHelper.MissionType.CLEAN)
            _toast.value = if (award) "清爽满满！＋2 💰" else "清爽满满！"
            
            kotlinx.coroutines.delay(2500)
            _animationState.value = AnimationState.Idle
        }
    }
    
    // 玩耍
    fun playWithPet() {
        viewModelScope.launch {
            val currentPet = pet.value ?: return@launch
            _animationState.value = AnimationState.Playing
            petRepository.play(currentPet.id, 20)
            petRepository.addExperience(currentPet.id, 10)
            petRepository.addIntimacy(currentPet.id, 5)
            
            val award = shouldAwardInteractionCoin(PetMissionHelper.MissionType.PLAY)
            if (award) coinRepository.addCoins(userId, 3)
            trackMission(PetMissionHelper.MissionType.PLAY)
            _toast.value = if (award) "玩得开心！＋3 💰" else "玩得开心！"
            
            kotlinx.coroutines.delay(3000)
            _animationState.value = AnimationState.Idle
        }
    }
    
    // 抚摸
    fun petPet() {
        viewModelScope.launch {
            val currentPet = pet.value ?: return@launch
            _animationState.value = AnimationState.Petting
            petRepository.play(currentPet.id, 5)
            petRepository.addIntimacy(currentPet.id, 1)
            trackMission(PetMissionHelper.MissionType.PET)
            
            kotlinx.coroutines.delay(500)
            _animationState.value = AnimationState.Idle
        }
    }
    
    /**
     * 通用商城购买接口 (供 PetShopDialog 使用)
     * 根据价格扣金币，按 type 映射到 ItemType 存入库存。
     */
    fun purchaseShopItem(itemKey: String, name: String, type: String, price: Int) {
        viewModelScope.launch {
            val coins = coinRepository.getCoinsAmount(userId)
            if (coins < price) {
                _toast.value = "金币不足！还差 ${price - coins} 💰"
                return@launch
            }
            val ok = coinRepository.spendCoins(userId, price)
            if (!ok) {
                _toast.value = "购买失败"
                return@launch
            }
            val itemType = when (type) {
                "food" -> ItemType.FOOD
                "toy" -> ItemType.TOY
                "medicine" -> ItemType.MEDICINE
                else -> ItemType.DECORATION
            }
            // 使用名字 hash 作为 itemId (保证同名不重复)
            val stableId = (itemKey.hashCode() and 0x7fffffff) or 0x10000  // 避免与 PetItems 冲突
            petItemRepository.addItem(userId, stableId, name, itemType)
            _toast.value = "购买成功：$name"
        }
    }
    
    // 购买物品
    fun buyItem(itemId: Int) {
        viewModelScope.launch {
            val item = PetItems.getItemById(itemId) ?: return@launch
            val coinsAmount = coinRepository.getCoinsAmount(userId)
            
            if (coinsAmount < item.price) {
                _uiState.value = PetUiState.Error("金币不足")
                return@launch
            }
            
            // 扣除金币
            val success = coinRepository.spendCoins(userId, item.price)
            if (!success) {
                _uiState.value = PetUiState.Error("购买失败")
                return@launch
            }
            
            // 添加物品
            petItemRepository.addItem(userId, item.id, item.name, item.type)
            
            _uiState.value = PetUiState.Success
        }
    }
    
    // 使用药品
    fun useMedicine(itemId: Int) {
        viewModelScope.launch {
            val currentPet = pet.value ?: return@launch
            val item = PetItems.getItemById(itemId) ?: return@launch
            
            // 检查是否有物品
            val hasItem = petItemRepository.useItem(userId, itemId)
            if (!hasItem) {
                _uiState.value = PetUiState.Error("没有足够的物品")
                return@launch
            }
            
            // 更新宠物状态
            if (item.healthBonus > 0) {
                petRepository.heal(currentPet.id, item.healthBonus)
            }
            if (item.allBonus > 0) {
                petRepository.feed(currentPet.id, item.allBonus)
                petRepository.clean(currentPet.id, item.allBonus)
                petRepository.play(currentPet.id, item.allBonus)
                petRepository.heal(currentPet.id, item.allBonus)
            }
        }
    }
    
    // 重命名宠物
    fun renamePet(newName: String) {
        viewModelScope.launch {
            val currentPet = pet.value ?: return@launch
            petRepository.updatePetName(currentPet.id, newName)
        }
    }
}

// UI 状态
sealed class PetUiState {
    object Loading : PetUiState()
    object Success : PetUiState()
    object NoPet : PetUiState()
    data class Error(val message: String) : PetUiState()
}

// 动画状态
sealed class AnimationState {
    object Idle : AnimationState()
    object Feeding : AnimationState()
    object Cleaning : AnimationState()
    object Playing : AnimationState()
    object Petting : AnimationState()
    object LevelUp : AnimationState()
}
