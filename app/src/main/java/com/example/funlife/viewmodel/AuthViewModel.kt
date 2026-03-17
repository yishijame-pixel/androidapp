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
    data class RegisterSuccess(val username: String) : AuthState()  // 🔥 新增：注册成功状态
    data class Error(val message: String, val field: ErrorField = ErrorField.GENERAL) : AuthState()
}

// 错误字段类型，用于UI精确显示错误位置
enum class ErrorField {
    GENERAL,        // 通用错误
    USERNAME,       // 用户名错误
    PASSWORD,       // 密码错误
    NICKNAME,       // 昵称错误
    BETA_CODE,      // 内测码错误
    NETWORK,        // 网络错误
    DATABASE        // 数据库错误
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
                // 步骤1：去除首尾空格
                val trimmedUsername = username.trim()
                val trimmedPassword = password.trim()
                
                // 步骤2：基本输入检查
                if (trimmedUsername.isEmpty()) {
                    _authState.value = AuthState.Error("请输入用户名", ErrorField.USERNAME)
                    return@launch
                }
                
                if (trimmedPassword.isEmpty()) {
                    _authState.value = AuthState.Error("请输入密码", ErrorField.PASSWORD)
                    return@launch
                }
                
                // 步骤3：格式验证
                val usernameValidation = com.example.funlife.utils.ValidationUtils.validateUsername(trimmedUsername)
                if (usernameValidation is com.example.funlife.utils.ValidationResult.Error) {
                    _authState.value = AuthState.Error(usernameValidation.message, ErrorField.USERNAME)
                    return@launch
                }
                
                val passwordValidation = com.example.funlife.utils.ValidationUtils.validatePassword(trimmedPassword)
                if (passwordValidation is com.example.funlife.utils.ValidationResult.Error) {
                    _authState.value = AuthState.Error(passwordValidation.message, ErrorField.PASSWORD)
                    return@launch
                }
                
                // 步骤4：检查用户是否存在
                val existingUser = userRepository.getUserByUsername(trimmedUsername)
                
                if (existingUser == null) {
                    // 用户名不存在
                    _authState.value = AuthState.Error("该用户名不存在", ErrorField.USERNAME)
                    return@launch
                }
                
                // 步骤5：验证密码
                val user = userRepository.login(trimmedUsername, trimmedPassword)
                
                if (user != null) {
                    // 登录成功
                    userRepository.updateLastLogin(user.id)
                    
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
                    // 密码错误
                    _authState.value = AuthState.Error("密码错误", ErrorField.PASSWORD)
                }
            } catch (e: Exception) {
                // 系统错误处理
                val (errorMessage, errorField) = categorizeLoginError(e)
                _authState.value = AuthState.Error(errorMessage, errorField)
                
                // 记录详细错误到日志
                android.util.Log.e("AuthViewModel", "Login error: ${e.message}", e)
            }
        }
    }
    
    // 登录错误分类
    private fun categorizeLoginError(e: Exception): Pair<String, ErrorField> {
        return when {
            e.message?.contains("SQLITE_ERROR") == true -> 
                Pair("系统初始化中，请稍后重试", ErrorField.DATABASE)
            e.message?.contains("no such column") == true -> 
                Pair("应用数据需要更新，请重启应用", ErrorField.DATABASE)
            e.message?.contains("database is locked") == true -> 
                Pair("系统繁忙，请稍后重试", ErrorField.DATABASE)
            e.message?.contains("disk I/O error") == true -> 
                Pair("存储空间不足或读写错误", ErrorField.DATABASE)
            e.message?.contains("constraint") == true -> 
                Pair("数据验证失败", ErrorField.DATABASE)
            e is java.net.UnknownHostException || e is java.net.SocketTimeoutException -> 
                Pair("网络连接失败，请检查网络", ErrorField.NETWORK)
            else -> 
                Pair("登录失败，请稍后重试", ErrorField.GENERAL)
        }
    }
    
    fun register(username: String, password: String, nickname: String, betaCode: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            
            try {
                // 步骤1：去除首尾空格
                val trimmedUsername = username.trim()
                val trimmedPassword = password.trim()
                val trimmedNickname = nickname.trim()
                val trimmedBetaCode = betaCode.trim()
                
                // 步骤2：基本输入检查
                if (trimmedUsername.isEmpty()) {
                    _authState.value = AuthState.Error("请输入用户名", ErrorField.USERNAME)
                    return@launch
                }
                
                if (trimmedPassword.isEmpty()) {
                    _authState.value = AuthState.Error("请输入密码", ErrorField.PASSWORD)
                    return@launch
                }
                
                if (trimmedNickname.isEmpty()) {
                    _authState.value = AuthState.Error("请输入昵称", ErrorField.NICKNAME)
                    return@launch
                }
                
                if (trimmedBetaCode.isEmpty()) {
                    _authState.value = AuthState.Error("请输入内测码", ErrorField.BETA_CODE)
                    return@launch
                }
                
                // 步骤3：格式验证（按顺序验证，一次只显示一个错误）
                val usernameValidation = com.example.funlife.utils.ValidationUtils.validateUsername(trimmedUsername)
                if (usernameValidation is com.example.funlife.utils.ValidationResult.Error) {
                    _authState.value = AuthState.Error(usernameValidation.message, ErrorField.USERNAME)
                    return@launch
                }
                
                val passwordValidation = com.example.funlife.utils.ValidationUtils.validatePassword(trimmedPassword)
                if (passwordValidation is com.example.funlife.utils.ValidationResult.Error) {
                    _authState.value = AuthState.Error(passwordValidation.message, ErrorField.PASSWORD)
                    return@launch
                }
                
                val nicknameValidation = com.example.funlife.utils.ValidationUtils.validateNickname(trimmedNickname)
                if (nicknameValidation is com.example.funlife.utils.ValidationResult.Error) {
                    _authState.value = AuthState.Error(nicknameValidation.message, ErrorField.NICKNAME)
                    return@launch
                }
                
                // 步骤4：验证内测码
                if (!isValidBetaCode(trimmedBetaCode)) {
                    _authState.value = AuthState.Error("内测码无效，请检查后重试", ErrorField.BETA_CODE)
                    return@launch
                }
                
                // 步骤5：尝试注册
                val result = userRepository.register(trimmedUsername, trimmedPassword, trimmedNickname)
                
                result.onSuccess { userId ->
                    // 🔥 修改：注册成功后不自动登录，返回RegisterSuccess状态
                    _authState.value = AuthState.RegisterSuccess(trimmedUsername)
                }.onFailure { error ->
                    // 注册失败处理
                    val (errorMessage, errorField) = categorizeRegisterError(error)
                    _authState.value = AuthState.Error(errorMessage, errorField)
                    
                    // 记录详细错误到日志
                    android.util.Log.e("AuthViewModel", "Register error: ${error.message}", error)
                }
            } catch (e: Exception) {
                // 系统错误处理
                val (errorMessage, errorField) = categorizeRegisterError(e)
                _authState.value = AuthState.Error(errorMessage, errorField)
                
                // 记录详细错误到日志
                android.util.Log.e("AuthViewModel", "Register error: ${e.message}", e)
            }
        }
    }
    
    // 注册错误分类
    private fun categorizeRegisterError(e: Throwable): Pair<String, ErrorField> {
        return when {
            // 用户名已存在
            e.message?.contains("已存在") == true || 
            e.message?.contains("UNIQUE constraint") == true ||
            e.message?.contains("username") == true -> 
                Pair("该用户名已被使用，请换一个", ErrorField.USERNAME)
            
            // 数据库错误
            e.message?.contains("SQLITE_ERROR") == true -> 
                Pair("系统初始化中，请稍后重试", ErrorField.DATABASE)
            e.message?.contains("no such column") == true -> 
                Pair("应用数据需要更新，请重启应用", ErrorField.DATABASE)
            e.message?.contains("database is locked") == true -> 
                Pair("系统繁忙，请稍后重试", ErrorField.DATABASE)
            e.message?.contains("disk I/O error") == true -> 
                Pair("存储空间不足或读写错误", ErrorField.DATABASE)
            e.message?.contains("constraint") == true -> 
                Pair("数据验证失败，请检查输入", ErrorField.GENERAL)
            
            // 网络错误
            e is java.net.UnknownHostException || e is java.net.SocketTimeoutException -> 
                Pair("网络连接失败，请检查网络", ErrorField.NETWORK)
            
            // 其他错误
            else -> 
                Pair("注册失败，请稍后重试", ErrorField.GENERAL)
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
    
    // 🔥 新增：内测码验证逻辑（可配置）
    private fun isValidBetaCode(code: String): Boolean {
        // 方案1：支持多个内测码
        val validCodes = listOf(
            "223498",  // 原始内测码
            "FUNLIFE2026",  // 新内测码
            "BETA001"  // 备用内测码
        )
        
        // 方案2：简单的算法验证（示例）
        // 内测码格式：6位数字，前3位 + 后3位 = 1000
        if (code.length == 6 && code.all { it.isDigit() }) {
            val first3 = code.substring(0, 3).toIntOrNull() ?: 0
            val last3 = code.substring(3, 6).toIntOrNull() ?: 0
            if (first3 + last3 == 1000) {
                return true
            }
        }
        
        return validCodes.contains(code)
    }
}
