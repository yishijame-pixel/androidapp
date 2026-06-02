// VipViewModel.kt - VIP视图模型（增强安全版）
package com.example.funlife.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.funlife.data.database.AppDatabase
import com.example.funlife.data.model.UserVip
import com.example.funlife.data.model.VipLevel
import com.example.funlife.BuildConfig
import com.example.funlife.repository.VipRepository
import com.example.funlife.security.SecurityManager
import com.example.funlife.vip.VipManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class VipViewModel(application: Application) : AndroidViewModel(application) {
    
    private val database = AppDatabase.getDatabase(application)
    private val repository = VipRepository(
        database.userVipDao(),
        database.redeemCodeDao(),
        database.coinDao(),
        application.applicationContext,
        database
    )

    // ☁️ 云端 VIP 管理器（卡密兑换/迁移/验签都走这里）
    private val cloudVip = VipManager(application.applicationContext)

    /** 是否已配置云端后端（BuildConfig 中 VIP_BACKEND_URL 不为空） */
    private val cloudEnabled: Boolean
        get() = BuildConfig.VIP_BACKEND_URL.isNotBlank() &&
                // 🔒 release 必须 HTTPS；debug 包允许 http 方便本地联调
                (BuildConfig.VIP_BACKEND_URL.startsWith("https://") ||
                 (BuildConfig.DEBUG && BuildConfig.VIP_BACKEND_URL.startsWith("http://")))

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
    
    /**
     * 兑换卡密入口。
     *
     * 路由策略：
     *   - 已配置 VIP_BACKEND_URL → 走云端 VipManager（推荐）
     *   - 未配置 → 走旧本地 VipRepository（开发态兼容 / 演示码）
     *
     * 返回 message 兼容现有 UI 格式：
     *   成功："成功激活 终身VIP|1000"  (UI 解析"|"取金币数)
     *   失败：错误描述字符串
     */
    fun redeemCode(code: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _loadingMessage.value = "云端验证中..."

            val msg: String = if (cloudEnabled) {
                redeemViaCloud(code)
            } else {
                _loadingMessage.value = "本地验证中..."
                redeemLocally(code)
            }

            _isLoading.value = false
            _loadingMessage.value = null
            _message.value = msg
        }
    }

    /** 设备迁移：用户在新设备输入"迁移凭证" 恢复 VIP
     *  迁移凭证格式: "CODE:OLD_DEVICE_ID"（在原设备的"导出迁移凭证"按钮获取）
     */
    fun migrateVip(input: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _loadingMessage.value = "正在迁移设备..."

            val msg: String = if (!cloudEnabled) {
                "云端未配置，无法迁移"
            } else {
                // 解析输入：支持 "CODE:OLDDEVICEID" 格式
                val trimmed = input.trim()
                val parts = trimmed.split(":", limit = 2)
                if (parts.size != 2 || parts[0].isBlank() || parts[1].isBlank()) {
                    "迁移凭证格式错误｜请在原设备点「我的-VIP-导出迁移凭证」获取完整凭证"
                } else {
                    val code = parts[0].trim()
                    val oldDeviceId = parts[1].trim()
                    when (val r = cloudVip.migrate(_userId.value, code, oldDeviceId)) {
                        is VipManager.Outcome.Success -> {
                            val name = VipLevel.fromLevel(r.cert.vipLevel).displayName
                            "迁移成功 已恢复 $name|0"
                        }
                        is VipManager.Outcome.Failure -> friendlyError(r.code, r.msg)
                    }
                }
            }

            _isLoading.value = false
            _loadingMessage.value = null
            _message.value = msg
        }
    }

    /** 导出本机迁移凭证 = "CODE:DEVICE_ID"
     *  用户在原设备点击"导出迁移凭证" → 复制到剪贴板 → 在新设备粘贴 → 调 migrateVip
     *  @return 完整凭证字符串；本机没 VIP 时返回 null
     */
    fun exportMigrationToken(): String? {
        val cert = cloudVip.getCurrentCertOrNull(_userId.value) ?: return null
        // 用 skuCode 反查 code 比较麻烦，这里直接用凭证里的 deviceId + 让用户自己输 code 不方便
        // 我们改用：把"code"也存在 VipManager.getCurrentCode 里
        val code = cloudVip.getCurrentRedeemCodeOrNull(_userId.value) ?: return null
        return "$code:${cert.deviceId}"
    }

    private suspend fun redeemViaCloud(code: String): String {
        return when (val r = cloudVip.redeem(_userId.value, code)) {
            is VipManager.Outcome.Success -> {
                val name = VipLevel.fromLevel(r.cert.vipLevel).displayName
                if (r.isReissue) {
                    "已重新激活 $name|0"
                } else {
                    "成功激活 $name|${r.coinsGranted}"
                }
            }
            is VipManager.Outcome.Failure -> friendlyError(r.code, r.msg)
        }
    }

    private suspend fun redeemLocally(code: String): String {
        val result = repository.redeemCode(_userId.value, code)
        return result.fold(
            onSuccess = { it },
            onFailure = {
                android.util.Log.e("VipViewModel", "本地兑换失败", it)
                "兑换失败，请稍后重试"
            }
        )
    }

    /** 把后端错误码翻译成用户友好提示 */
    private fun friendlyError(code: String, fallback: String?): String = when (code) {
        "INVALID" -> "兑换码无效或格式错误"
        "USED" -> "此兑换码已被其他设备使用"
        "DISABLED" -> "兑换码已被禁用，请联系客服"
        "EXPIRED" -> "兑换码已过期"
        "BLOCKED" -> "迁移次数已达上限，请联系客服"
        "NOT_REDEEMED" -> "此卡密尚未激活，请直接兑换"
        "RATE_LIMITED" -> "请求过于频繁，请稍后再试"
        "UNAUTHENTICATED", "TOKEN_INVALID", "TOKEN_EXPIRED" -> "登录已失效，请重新登录"
        "DEVICE_CONFLICT" -> "该卡密已绑定其他设备"
        "CERT_EXPIRED" -> "凭证已过期，请联网激活"
        "BAD_SIGNATURE" -> "凭证已损坏，请联系客服"
        "DB_ERROR" -> "服务繁忙，请稍后再试"
        "BAD_REQUEST" -> "请求格式错误，请重试"
        "NETWORK_ERROR", "HTTP_500", "HTTP_502", "HTTP_503", "HTTP_504" ->
            "网络异常，请检查网络后重试"
        "NO_BACKEND" -> "兑换服务暂未开通"
        "VALIDATE_FAILED" -> "凭证验签失败，请重试或联系客服"
        else -> {
            // 🔒 不把未映射的英文 code 显示给用户，记日志即可
            android.util.Log.w("VipViewModel", "未映射的错误码: $code, fallback=$fallback")
            // 服务端 msg 通常已经是中文（云函数返回时会写中文），优先用 fallback
            // 但若 fallback 形如 "[ResourceNotFound]..." 这种英文，就用通用提示
            val safe = fallback?.takeIf { !it.contains(Regex("[A-Za-z]{6,}")) }
            safe ?: "操作失败，请稍后重试"
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
