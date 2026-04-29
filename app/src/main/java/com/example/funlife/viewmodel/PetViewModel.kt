// PetViewModel.kt - 宠物视图模型
package com.example.funlife.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.funlife.data.model.*
import com.example.funlife.repository.PetRepository
import com.example.funlife.repository.PetItemRepository
import com.example.funlife.repository.CoinRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class PetViewModel(
    private val petRepository: PetRepository,
    private val petItemRepository: PetItemRepository,
    private val coinRepository: CoinRepository,
    private val userId: Long
) : ViewModel() {
    
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
    
    // 喂食
    fun feedPet(itemId: Int) {
        viewModelScope.launch {
            val currentPet = pet.value ?: return@launch
            val item = PetItems.getItemById(itemId) ?: return@launch
            
            // 播放喂食动画
            _animationState.value = AnimationState.Feeding
            
            // 更新宠物状态
            petRepository.feed(currentPet.id, item.hungerBonus)
            petRepository.addExperience(currentPet.id, 5)
            petRepository.addIntimacy(currentPet.id, 2)
            
            // 重置动画状态
            kotlinx.coroutines.delay(2000)
            _animationState.value = AnimationState.Idle
        }
    }
    
    // 洗澡
    fun cleanPet() {
        viewModelScope.launch {
            val currentPet = pet.value ?: return@launch
            
            // 播放洗澡动画
            _animationState.value = AnimationState.Cleaning
            
            // 更新宠物状态
            petRepository.clean(currentPet.id, 50)
            petRepository.addExperience(currentPet.id, 5)
            petRepository.addIntimacy(currentPet.id, 3)
            
            // 重置动画状态
            kotlinx.coroutines.delay(2500)
            _animationState.value = AnimationState.Idle
        }
    }
    
    // 玩耍
    fun playWithPet() {
        viewModelScope.launch {
            val currentPet = pet.value ?: return@launch
            
            // 播放玩耍动画
            _animationState.value = AnimationState.Playing
            
            // 更新宠物状态
            petRepository.play(currentPet.id, 20)
            petRepository.addExperience(currentPet.id, 10)
            petRepository.addIntimacy(currentPet.id, 5)
            
            // 重置动画状态
            kotlinx.coroutines.delay(3000)
            _animationState.value = AnimationState.Idle
        }
    }
    
    // 抚摸
    fun petPet() {
        viewModelScope.launch {
            val currentPet = pet.value ?: return@launch
            
            // 播放抚摸动画
            _animationState.value = AnimationState.Petting
            
            // 更新宠物状态
            petRepository.play(currentPet.id, 5)
            petRepository.addIntimacy(currentPet.id, 1)
            
            // 重置动画状态
            kotlinx.coroutines.delay(500)
            _animationState.value = AnimationState.Idle
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
