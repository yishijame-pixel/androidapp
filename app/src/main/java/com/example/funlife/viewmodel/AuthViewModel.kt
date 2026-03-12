// AuthViewModel.kt - 认证视图模型
package com.example.funlife.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.funlife.data.database.AppDatabase
import com.example.funlife.data.model.UserSession
import com.example.funlife.repository.UserRepository
import com.example.funlife.utils.UserSessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val session: UserSession) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    
    private val userRepository: UserRepository
    private val sessionManager: UserSessionManager = UserSessionManager(application)
    
    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()
    
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()
    
    init {
        val database = AppDatabase.getDatabase(application)
        userRepository = UserRepository(database.userDao())
        
        // 检查是否已登录
        _isLoggedIn.value = sessionManager.isLoggedIn()
    }
    
    fun login(username: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            
            try {
                val user = userRepository.login(username, password)
                
                if (user != null) {
                    // 更新最后登录时间
                    userRepository.updateLastLogin(user.id)
                    
                    // 保存会话
                    val session = UserSession(
                        userId = user.id,
                        username = user.username,
                        nickname = user.nickname,
                        avatar = user.avatar
                    )
                    sessionManager.saveSession(session)
                    
                    _isLoggedIn.value = true
                    _authState.value = AuthState.Success(session)
                } else {
                    _authState.value = AuthState.Error("用户名或密码错误")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error("登录失败: ${e.message}")
            }
        }
    }
    
    fun register(username: String, password: String, nickname: String, betaCode: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            
            try {
                // 验证内测码
                if (betaCode != "223498") {
                    _authState.value = AuthState.Error("内测码错误")
                    return@launch
                }
                
                // 验证输入
                if (username.length < 3) {
                    _authState.value = AuthState.Error("用户名至少3个字符")
                    return@launch
                }
                
                if (password.length < 6) {
                    _authState.value = AuthState.Error("密码至少6个字符")
                    return@launch
                }
                
                val result = userRepository.register(username, password, nickname)
                
                result.onSuccess { userId ->
                    // 自动登录
                    val session = UserSession(
                        userId = userId,
                        username = username,
                        nickname = nickname.ifEmpty { username },
                        avatar = ""
                    )
                    sessionManager.saveSession(session)
                    
                    _isLoggedIn.value = true
                    _authState.value = AuthState.Success(session)
                }.onFailure { error ->
                    _authState.value = AuthState.Error(error.message ?: "注册失败")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error("注册失败: ${e.message}")
            }
        }
    }
    
    fun logout() {
        sessionManager.clearSession()
        _isLoggedIn.value = false
        _authState.value = AuthState.Idle
    }
    
    fun getCurrentSession(): UserSession? {
        return sessionManager.getSession()
    }
    
    fun resetAuthState() {
        _authState.value = AuthState.Idle
    }
}
