// VipViewModel.kt - VIP视图模型（增强安全版）
package com.example.funlife.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.funlife.data.database.AppDatabase
import com.example.funlife.data.model.UserVip
import com.example.funlife.data.model.VipLevel
import com.example.funlife.repository.VipRepository
import com.example.funlife.security.SecurityManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class VipViewModel(application: Application) : AndroidViewModel(application) {
    
    private val database = AppDatabase.getDatabase(application)
    private val repository = VipRepository(
        database.userVipDao(),
        database.redeemCodeDao(),
        database.coinDao(),
        application.applicationContext  // 传递 Context
    )
    
    init {
        // 初始化安全系统
        SecurityManager.initialize(application.applicationContext)
    }
    
    private val _userId = MutableStateFlow(0L)
    
    val userVip: StateFlow<UserVip?> = _userId.flatMapLatest { userId ->
        if (userId > 0) {
            repository.getUserVip(userId)
        } else {
            flowOf(null)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )
    
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    private val _loadingMessage = MutableStateFlow<String?>(null)
    val loadingMessage: StateFlow<String?> = _loadingMessage
    
    fun setUserId(userId: Long) {
        _userId.value = userId
        viewModelScope.launch {
            repository.initializeUserVip(userId)
        }
    }
    
    fun claimDailyCoins() {
        viewModelScope.launch {
            _isLoading.value = true
            _loadingMessage.value = "领取中..."
            val result = repository.claimDailyCoins(_userId.value)
            _isLoading.value = false
            _loadingMessage.value = null
            
            result.onSuccess { coins ->
                _message.value = "成功领取 $coins 金币！"
            }.onFailure { error ->
                _message.value = error.message
            }
        }
    }
    
    fun redeemCode(code: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _loadingMessage.value = "后台验证中..."
            val result = repository.redeemCode(_userId.value, code)
            _isLoading.value = false
            _loadingMessage.value = null
            
            result.onSuccess { reward ->
                _message.value = reward
            }.onFailure { error ->
                _message.value = error.message
            }
        }
    }
    
    fun purchaseVip(vipLevel: Int, days: Int, cost: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _loadingMessage.value = "购买中..."
            val result = repository.purchaseVip(_userId.value, vipLevel, days, cost)
            _isLoading.value = false
            _loadingMessage.value = null
            
            result.onSuccess { message ->
                _message.value = message
            }.onFailure { error ->
                _message.value = error.message
            }
        }
    }
    
    fun clearMessage() {
        _message.value = null
    }
    
    fun getVipLevel(): VipLevel {
        return userVip.value?.getCurrentVipLevel() ?: VipLevel.NORMAL
    }
}
