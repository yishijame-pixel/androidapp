// VipProfileViewModel.kt - VIP个人主页ViewModel
package com.example.funlife.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.funlife.data.model.*
import com.example.funlife.repository.ProfileRepository
import com.example.funlife.utils.AvatarStorageHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VipProfileViewModel(
    private val profileRepository: ProfileRepository,
    private val userId: Long,
    private val context: Context
) : ViewModel() {
    
    // 用户头像信息
    val userAvatar: StateFlow<UserAvatar?> = profileRepository.getUserAvatar(userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    
    // 用户统计信息
    private val _userStatistics = MutableStateFlow<UserStatistics?>(null)
    val userStatistics: StateFlow<UserStatistics?> = _userStatistics.asStateFlow()
    
    // 所有头像框
    val allFrames: StateFlow<List<AvatarFrame>> = profileRepository.getAllFrames()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    // 所有背�?
    val allBackgrounds: StateFlow<List<ProfileBackground>> = profileRepository.getAllBackgrounds()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    // 用户可用的头像框（根据VIP等级�?
    private val _availableFrames = MutableStateFlow<List<AvatarFrame>>(emptyList())
    val availableFrames: StateFlow<List<AvatarFrame>> = _availableFrames.asStateFlow()
    
    // 用户可用的背景（根据VIP等级�?
    private val _availableBackgrounds = MutableStateFlow<List<ProfileBackground>>(emptyList())
    val availableBackgrounds: StateFlow<List<ProfileBackground>> = _availableBackgrounds.asStateFlow()
    
    // 当前VIP等级（暂时硬编码，后续从VIP系统获取�?
    private val _vipLevel = MutableStateFlow(0)
    val vipLevel: StateFlow<Int> = _vipLevel.asStateFlow()
    
    // 操作结果消息
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()
    
    init {
        loadUserStatistics()
        loadAvailableItems()
    }
    
    /**
     * 加载用户统计信息
     */
    private fun loadUserStatistics() {
        viewModelScope.launch {
            try {
                val stats = profileRepository.getUserStatistics(userId)
                _userStatistics.value = stats
            } catch (e: Exception) {
                android.util.Log.e("VipProfileViewModel", "加载统计信息失败", e)
                _message.value = "加载统计信息失败，请重试"
            }
        }
    }
    
    /**
     * 加载用户可用的头像框和背�?
     */
    private fun loadAvailableItems() {
        viewModelScope.launch {
            profileRepository.getUserAvailableFrames(userId, _vipLevel.value)
                .collect { frames ->
                    _availableFrames.value = frames
                }
        }
        
        viewModelScope.launch {
            profileRepository.getUserAvailableBackgrounds(userId, _vipLevel.value)
                .collect { backgrounds ->
                    _availableBackgrounds.value = backgrounds
                }
        }
    }
    
    /**
     * 更新头像URI
     * 将外部URI的图片复制到内部存储
     */
    fun updateAvatarUri(uri: Uri?) {
        viewModelScope.launch {
            try {
                if (uri == null) {
                    // 清除头像
                    profileRepository.updateAvatarUri(userId, null)
                    _message.value = "头像已清除"
                    return@launch
                }
                
                // 在IO线程中保存头像到内部存储
                val internalUri = withContext(Dispatchers.IO) {
                    AvatarStorageHelper.saveAvatarToInternalStorage(
                        context = context,
                        sourceUri = uri,
                        userId = userId
                    )
                }
                
                if (internalUri != null) {
                    // 更新数据库中的URI
                    profileRepository.updateAvatarUri(userId, internalUri)
                    runCatching {
                        com.example.funlife.repository.SocialLinkRepository(
                            context.applicationContext,
                            (context.applicationContext as com.example.funlife.FunLifeApplication).database.socialDao(),
                        ).syncAvatarToPocketBase(userId)
                    }
                    _message.value = "头像更新成功"
                } else {
                    _message.value = "头像保存失败，请重试"
                }
            } catch (e: Exception) {
                android.util.Log.e("VipProfileViewModel", "头像更新失败", e)
                _message.value = "头像更新失败，请重试"
            }
        }
    }
    
    /**
     * 更新头像框
     */
    fun updateFrame(frameId: String?) {
        viewModelScope.launch {
            try {
                profileRepository.updateFrameId(userId, frameId)
                _message.value = "头像框更新成功"
            } catch (e: Exception) {
                android.util.Log.e("VipProfileViewModel", "头像框更新失败", e)
                _message.value = "头像框更新失败，请重试"
            }
        }
    }
    
    /**
     * 更新背景
     */
    fun updateBackground(backgroundId: String?) {
        viewModelScope.launch {
            try {
                profileRepository.updateBackgroundId(userId, backgroundId)
                _message.value = "背景更新成功"
            } catch (e: Exception) {
                android.util.Log.e("VipProfileViewModel", "背景更新失败", e)
                _message.value = "背景更新失败，请重试"
            }
        }
    }
    
    /**
     * 购买头像框
     */
    fun purchaseFrame(frame: AvatarFrame) {
        viewModelScope.launch {
            try {
                val result = profileRepository.purchaseFrame(userId, frame.id, frame.price)
                if (result.isSuccess) {
                    _message.value = "购买成功！"
                    loadAvailableItems()  // 刷新可用列表
                } else {
                    android.util.Log.e("VipProfileViewModel", "购买失败", result.exceptionOrNull())
                    _message.value = "购买失败，请重试"
                }
            } catch (e: Exception) {
                android.util.Log.e("VipProfileViewModel", "购买失败", e)
                _message.value = "购买失败，请重试"
            }
        }
    }
    
    /**
     * 购买背景
     */
    fun purchaseBackground(background: ProfileBackground) {
        viewModelScope.launch {
            try {
                val result = profileRepository.purchaseBackground(userId, background.id, background.price)
                if (result.isSuccess) {
                    _message.value = "购买成功！"
                    loadAvailableItems()  // 刷新可用列表
                } else {
                    android.util.Log.e("VipProfileViewModel", "购买失败", result.exceptionOrNull())
                    _message.value = "购买失败，请重试"
                }
            } catch (e: Exception) {
                android.util.Log.e("VipProfileViewModel", "购买失败", e)
                _message.value = "购买失败，请重试"
            }
        }
    }
    
    /**
     * 设置VIP等级（用于测试）
     */
    fun setVipLevel(level: Int) {
        _vipLevel.value = level
        loadAvailableItems()
    }
    
    /**
     * 清除消息
     */
    fun clearMessage() {
        _message.value = null
    }
    
    /**
     * 初始化默认数据
     */
    fun initializeDefaultData() {
        viewModelScope.launch {
            try {
                profileRepository.initializeDefaultFramesAndBackgrounds()
                _message.value = "默认数据初始化成功"
                loadAvailableItems()
            } catch (e: Exception) {
                android.util.Log.e("VipProfileViewModel", "初始化失败", e)
                _message.value = "初始化失败，请重试"
            }
        }
    }
}

