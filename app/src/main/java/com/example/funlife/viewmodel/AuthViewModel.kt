// AuthViewModel.kt - 认证视图模型
package com.example.funlife.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.funlife.FunLifeApplication
import com.example.funlife.data.database.AppDatabase
import com.example.funlife.data.model.UserSession
import com.example.funlife.repository.SocialLinkRepository
import com.example.funlife.repository.UserRepository
import com.example.funlife.utils.UserSessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val session: UserSession) : AuthState()
    data class RegisterSuccess(val username: String) : AuthState()  // 🔥 新增：注册成功状态
    data class Banned(val reason: String) : AuthState()              // 🔥 新增：被封号
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

    // 🔒 token 健康度：true = 已登录且本地有有效 token；
    //   false = 已登录但 token 缺失（多见于：服务端旧版漏配 IDENTITY_SECRET 时
    //   注册过的老用户，需要重新登录一次以补领 token）
    private val _tokenHealthy = MutableStateFlow(true)
    val tokenHealthy: StateFlow<Boolean> = _tokenHealthy.asStateFlow()

    // 🔒 注册/登录单飞守卫：UI 双击或状态滞后时丢弃后续调用，
    //   避免两次 register 并发跑出"内测码标 used 两次 / 本地 insert 冲突"等竞态
    @Volatile private var registerInFlight: Boolean = false
    @Volatile private var loginInFlight: Boolean = false

    init {
        val database = AppDatabase.getDatabase(application)
        userRepository = UserRepository(database.userDao())
        
        // 检查是否已登录
        _isLoggedIn.value = sessionManager.isLoggedIn()
        // 启动时同步检查 token 健康度（不阻塞）
        refreshTokenHealth()
    }

    /** 检查当前 session 对应账号在本地是否有 device_token */
    private fun refreshTokenHealth() {
        val session = sessionManager.getSession()
        if (session == null) {
            _tokenHealthy.value = true // 未登录无需关心
            return
        }
        val tok = try {
            com.example.funlife.vip.DeviceTokenStore(getApplication()).load(session.username)
        } catch (e: Exception) { null }
        val healthy = !tok.isNullOrBlank()
        _tokenHealthy.value = healthy
        if (!healthy) {
            android.util.Log.w(
                "AuthViewModel",
                "device_token missing for ${session.username}; please re-login to refresh"
            )
        }
    }
    
    fun login(username: String, password: String) {
        if (loginInFlight) {
            android.util.Log.w("AuthViewModel", "login ignored: already in flight")
            return
        }
        loginInFlight = true
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
                
                // 🔒 安全修复：合并用户名/密码错误为统一信息，
                // 防止用户名枚举攻击（攻击者通过响应差异判断哪些账号已存在）
                val user = userRepository.login(trimmedUsername, trimmedPassword)

                if (user != null) {
                    // 🔒 登录前先调云端检查是否被封禁
                    val banStatus = withContext(Dispatchers.IO) {
                        com.example.funlife.vip.UserCloudRepository(getApplication())
                            .checkBanStatus(user.username)
                    }
                    if (banStatus is com.example.funlife.vip.UserCloudRepository.BanStatus.Banned) {
                        sessionManager.clearSession()
                        _isLoggedIn.value = false
                        _authState.value = AuthState.Banned(banStatus.reason)
                        return@launch
                    }

                    // 登录成功
                    userRepository.updateLastLogin(user.id)

                    val session = UserSession(
                        userId = user.id,
                        username = user.username,
                        nickname = user.nickname,
                        avatar = user.avatar
                    )
                    sessionManager.saveSession(session)

                    // 🔒 兜底领 device_token：兼容老用户/升级/token 丢失场景
                    //    必须在 login 函数内调用，因为只有这里能拿到明文密码计算 passwordProof
                    launch(Dispatchers.IO) {
                        try {
                            val store = com.example.funlife.vip.DeviceTokenStore(getApplication())
                            if (store.load(user.username).isNullOrBlank()) {
                                com.example.funlife.vip.UserCloudRepository(getApplication())
                                    .registerLog(
                                        user.username, user.nickname, "", trimmedPassword,
                                        mode = "refresh",  // 🔒 已登录补领 token，不走注册严格校验
                                    )
                            }
                        } catch (e: Exception) {
                            android.util.Log.w("AuthViewModel", "refresh token failed: ${e.message}")
                        }
                    }

                    _isLoggedIn.value = true
                    _authState.value = AuthState.Success(session)
                    com.example.funlife.social.SocialSessionManager.warmStartAsync(getApplication())
                    // 登录成功后稍后再刷一次 token 健康度（兜底 registerLog 完成后）
                    refreshTokenHealth()
                } else {
                    // 用户名不存在或密码错误一律返回相同提示，避免信息泄漏
                    _authState.value = AuthState.Error("用户名或密码错误", ErrorField.PASSWORD)
                }
            } catch (e: Exception) {
                // 系统错误处理
                val (errorMessage, errorField) = categorizeLoginError(e)
                _authState.value = AuthState.Error(errorMessage, errorField)
                
                // 记录详细错误到日志
                android.util.Log.e("AuthViewModel", "Login error: ${e.message}", e)
            } finally {
                loginInFlight = false
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
            else -> {
                // 🔒 兜底：只给用户显示中文友好提示，技术细节写日志供开发者排查
                android.util.Log.e("AuthViewModel", "Login unmapped error: ${e::class.java.simpleName}: ${e.message}", e)
                Pair("登录失败，请稍后重试", ErrorField.GENERAL)
            }
        }
    }
    
    fun register(username: String, password: String, nickname: String, betaCode: String) {
        // 🔒 单飞守卫：UI 双击或状态滞后导致的重复进入直接丢弃
        if (registerInFlight) {
            android.util.Log.w("AuthViewModel", "register ignored: already in flight")
            return
        }
        registerInFlight = true
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
                
                // 步骤4a：本地 username 冲突预检（必须在 beta_validate 之前做，
                //   否则本地有残留同名账号时会先标 used 再失败 → 内测码白白作废）
                val localExisting = withContext(Dispatchers.IO) {
                    try { userRepository.getUserByUsername(trimmedUsername) }
                    catch (e: Exception) { null }
                }
                if (localExisting != null) {
                    _authState.value = AuthState.Error("该用户名本地已存在，请直接登录或换用户名", ErrorField.USERNAME)
                    return@launch
                }

                // 步骤4b：云端用户名冲突 dryRun 预检（只查不写，避免无效注册留下孤儿云端记录）
                val cloudPre = withContext(Dispatchers.IO) {
                    try {
                        com.example.funlife.vip.UserCloudRepository(getApplication())
                            .registerLog(
                                trimmedUsername, trimmedNickname, trimmedBetaCode, trimmedPassword,
                                mode = "register", dryRun = true,
                            )
                    } catch (e: Exception) {
                        com.example.funlife.vip.UserCloudRepository.RegisterResult.NetworkError
                    }
                }
                when (cloudPre) {
                    is com.example.funlife.vip.UserCloudRepository.RegisterResult.Rejected -> {
                        val field = when (cloudPre.code) {
                            "ALREADY_REGISTERED", "DEVICE_CONFLICT" -> ErrorField.USERNAME
                            "WRONG_PASSWORD"                         -> ErrorField.PASSWORD
                            else                                     -> ErrorField.GENERAL
                        }
                        _authState.value = AuthState.Error(cloudPre.msg, field)
                        return@launch
                    }
                    is com.example.funlife.vip.UserCloudRepository.RegisterResult.NetworkError -> {
                        _authState.value = AuthState.Error("网络异常，请检查网络后重试", ErrorField.NETWORK)
                        return@launch
                    }
                    is com.example.funlife.vip.UserCloudRepository.RegisterResult.Ok -> {
                        // 预检通过，云端未写库
                    }
                }

                // 步骤5：验证内测码（云端原子标记 used；失败则本地账号也不创建）
                val betaResult = validateBetaCodeWithCloud(trimmedBetaCode, trimmedUsername)
                if (betaResult is BetaValidateResult.Rejected) {
                    _authState.value = AuthState.Error(betaResult.msg, ErrorField.BETA_CODE)
                    return@launch
                }

                // 步骤6：本地建账号
                val result = userRepository.register(trimmedUsername, trimmedPassword, trimmedNickname)

                result.onSuccess { userId ->
                    // 步骤7：本地建好后，正式发一次非 dryRun 调用，写云端 vip_users + 签发 device_token
                    withContext(Dispatchers.IO) {
                        try {
                            com.example.funlife.vip.UserCloudRepository(getApplication())
                                .registerLog(
                                    trimmedUsername, trimmedNickname, trimmedBetaCode, trimmedPassword,
                                    mode = "register", dryRun = false,
                                )
                        } catch (e: Exception) {
                            android.util.Log.w("AuthViewModel", "final registerLog failed: ${e.message}")
                        }
                    }
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
            } finally {
                registerInFlight = false  // 🔒 释放单飞守卫
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
            
            // 🔒 兜底：只给用户显示中文友好提示，技术细节写日志供开发者排查
            else -> {
                android.util.Log.e("AuthViewModel", "Register unmapped error: ${e::class.java.simpleName}: ${e.message}", e)
                Pair("注册失败，请稍后重试", ErrorField.GENERAL)
            }
        }
    }
    
    /**
     * App 启动时（已登录用户）调用：检查是否被远程封号。
     * 被封 → 清 session + 通知 UI 跳登录页。
     * 网络异常 → 维持原状（避免大面积错误登出）。
     */
    fun verifyCurrentSessionNotBanned() {
        val session = sessionManager.getSession() ?: return
        // 顺带刷新 token 健康度（async 上报路径若清掉过 token，启动时这里会标红）
        refreshTokenHealth()
        // 🔒 启动时把本地金币 + 积分余额上报到云端，便于对账与异常检测（fire-and-forget）
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val app = getApplication<Application>()
                val db = AppDatabase.getDatabase(app)
                val repo = com.example.funlife.repository.CoinRepository(db.coinDao(), app.applicationContext)
                repo.reportBalancesSnapshot(session.userId)
            } catch (_: Exception) { /* 静默 */ }
        }
        viewModelScope.launch {
            val banStatus = withContext(Dispatchers.IO) {
                com.example.funlife.vip.UserCloudRepository(getApplication())
                    .checkBanStatus(session.username)
            }
            if (banStatus is com.example.funlife.vip.UserCloudRepository.BanStatus.Banned) {
                sessionManager.clearSession()
                _isLoggedIn.value = false
                _authState.value = AuthState.Banned(banStatus.reason)
            }
        }
    }

    fun logout() {
        val uid = sessionManager.getCurrentUserId()
        com.example.funlife.social.SocialSessionManager.shutdown(getApplication())
        sessionManager.clearSession()
        _isLoggedIn.value = false
        _authState.value = AuthState.Idle
        if (uid > 0L) {
            com.example.funlife.utils.UserAvatarBitmapCache.clearUser(uid)
            viewModelScope.launch(Dispatchers.IO) {
                runCatching {
                    val app = getApplication<FunLifeApplication>()
                    SocialLinkRepository(app, app.database.socialDao()).clearLocal(uid)
                    com.example.funlife.social.SocialPushTokenRegistry.clearToken(app, uid)
                }
            }
        }
    }
    
    fun getCurrentSession(): UserSession? {
        return sessionManager.getSession()
    }
    
    fun resetAuthState() {
        _authState.value = AuthState.Idle
    }
    
    // 内测码校验结果（区分"明确拒绝"和"网络问题/通过"）
    private sealed class BetaValidateResult {
        object Ok : BetaValidateResult()
        data class Rejected(val msg: String) : BetaValidateResult()
    }

    /**
     * 内测码校验：完全走云端 /beta_validate，无任何本地兜底。
     * 网络异常 → 拒绝注册（提示用户检查网络后重试）。
     */
    private suspend fun validateBetaCodeWithCloud(code: String, username: String): BetaValidateResult {
        val repo = com.example.funlife.vip.BetaCodeRepository(getApplication())
        val r = withContext(Dispatchers.IO) { repo.validate(code, username) }
        return when (r) {
            is com.example.funlife.vip.BetaCodeRepository.Result.Ok -> BetaValidateResult.Ok
            is com.example.funlife.vip.BetaCodeRepository.Result.Failed -> BetaValidateResult.Rejected(when (r.code) {
                "USED"       -> "此内测码已被使用"
                "DISABLED"   -> "此内测码已禁用"
                "WRONG_TYPE" -> "请输入正确的内测邀请码"
                else         -> "内测码无效，请检查后重试"
            })
            is com.example.funlife.vip.BetaCodeRepository.Result.NetworkError ->
                BetaValidateResult.Rejected("网络异常，请检查网络后重试")
        }
    }
}
