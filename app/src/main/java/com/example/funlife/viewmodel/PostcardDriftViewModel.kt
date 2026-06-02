// PostcardDriftViewModel.kt — v53 阅光书房 · 明信片漂流
package com.example.funlife.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.funlife.FunLifeApplication
import com.example.funlife.vip.PostcardDriftCloudRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PostcardDriftViewModel(
    application: Application,
    val userId: Long,
) : AndroidViewModel(application) {

    private val cloud = PostcardDriftCloudRepository(application)
    private val app = application as FunLifeApplication

    private val _inbox = MutableStateFlow<List<PostcardDriftCloudRepository.Postcard>>(emptyList())
    val inbox: StateFlow<List<PostcardDriftCloudRepository.Postcard>> = _inbox.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _toast = MutableStateFlow<String?>(null)
    val toast: StateFlow<String?> = _toast.asStateFlow()
    fun consumeToast() { _toast.value = null }

    private val _vipLevel = MutableStateFlow(0)
    val vipLevel: StateFlow<Int> = _vipLevel.asStateFlow()

    /** (used, limit, vipLevel) — limit==0 表示当前 VIP 不能寄 */
    private val _quota = MutableStateFlow(Triple(0, 0, 0))
    val quota: StateFlow<Triple<Int, Int, Int>> = _quota.asStateFlow()

    init {
        viewModelScope.launch {
            _vipLevel.value = app.database.userVipDao().getUserVipSync(userId)?.vipLevel ?: 0
            // 拿到 VIP 等级后再决定是否调云端：普通用户没 cert，调了也是 NO_CERT
            // 服务端 send 也只会在 VIP2+ 用户池中匹配收件人 → 普通用户 inbox 永远为空
            refresh()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            // 没买过 VIP 的用户：本地直接显示空收件箱，不去打云端、也不抛凭证错误
            val vipLevel = app.database.userVipDao().getUserVipSync(userId)?.vipLevel ?: 0
            if (vipLevel <= 0) {
                _inbox.value = emptyList()
                _loading.value = false
                return@launch
            }
            _loading.value = true
            when (val r = cloud.inbox(userId)) {
                is PostcardDriftCloudRepository.Result.Ok -> _inbox.value = r.value
                is PostcardDriftCloudRepository.Result.Err -> {
                    // NO_CERT 是"未登录或无 VIP 凭证"的内部错误码，对用户毫无意义 → 静默
                    if (r.code != "NO_CERT") _toast.value = r.msg
                }
                else -> {}
            }
            _loading.value = false
        }
    }

    fun send(text: String, bookTitle: String) {
        viewModelScope.launch {
            when (val r = cloud.send(userId, text, bookTitle)) {
                is PostcardDriftCloudRepository.Result.Ok ->
                    _toast.value = "✉️ 明信片已寄出，去往陌生人的城市"
                is PostcardDriftCloudRepository.Result.QuotaExceeded -> {
                    _quota.value = Triple(r.used, r.limit, r.vipLevel)
                    _toast.value = "本月配额已用完（${r.used}/${r.limit}）"
                }
                is PostcardDriftCloudRepository.Result.Err -> {
                    _toast.value = when (r.code) {
                        "NO_CERT" -> "季卡及以上会员才能寄出明信片"
                        "NO_BACKEND" -> "云端暂不可用，请稍后再试"
                        else -> r.msg
                    }
                }
            }
        }
    }

    fun react(id: String) {
        viewModelScope.launch {
            when (cloud.react(userId, id)) {
                is PostcardDriftCloudRepository.Result.Ok -> {
                    _inbox.value = _inbox.value.map {
                        if (it.id == id) it.copy(reactedHeart = true) else it
                    }
                }
                is PostcardDriftCloudRepository.Result.Err -> {}
                else -> {}
            }
        }
    }
}
