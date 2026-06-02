// QuoteGalaxyViewModel.kt — v53 阅光书房 · 摘抄星河
package com.example.funlife.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.funlife.FunLifeApplication
import com.example.funlife.vip.QuoteGalaxyCloudRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class QuoteGalaxyViewModel(
    application: Application,
    val userId: Long,
) : AndroidViewModel(application) {

    private val cloud = QuoteGalaxyCloudRepository(application)
    private val app = application as FunLifeApplication

    private val _stars = MutableStateFlow<List<QuoteGalaxyCloudRepository.StarItem>>(emptyList())
    val stars: StateFlow<List<QuoteGalaxyCloudRepository.StarItem>> = _stars.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _err = MutableStateFlow<String?>(null)
    val err: StateFlow<String?> = _err.asStateFlow()
    fun consumeErr() { _err.value = null }

    private val _toast = MutableStateFlow<String?>(null)
    val toast: StateFlow<String?> = _toast.asStateFlow()
    fun consumeToast() { _toast.value = null }

    private val _vipLevel = MutableStateFlow(0)
    val vipLevel: StateFlow<Int> = _vipLevel.asStateFlow()

    private var nextCursor: String? = null

    init {
        viewModelScope.launch {
            _vipLevel.value = app.database.userVipDao().getUserVipSync(userId)?.vipLevel ?: 0
        }
        refresh()
    }

    fun refresh() {
        nextCursor = null
        viewModelScope.launch {
            _loading.value = true
            when (val r = cloud.feed(userId, cursor = null, limit = 30)) {
                is QuoteGalaxyCloudRepository.Result.Ok -> {
                    _stars.value = r.value.first
                    nextCursor = r.value.second
                }
                is QuoteGalaxyCloudRepository.Result.Err -> {
                    // NO_CERT / NO_BACKEND：普通用户尚未开通 VIP 凭证；不抛错误，展示空星空
                    if (r.code == "NO_CERT" || r.code == "NO_BACKEND") {
                        _stars.value = emptyList()
                    } else {
                        _err.value = r.msg
                    }
                }
            }
            _loading.value = false
        }
    }

    fun loadMore() {
        if (_loading.value || nextCursor == null) return
        viewModelScope.launch {
            _loading.value = true
            when (val r = cloud.feed(userId, cursor = nextCursor, limit = 30)) {
                is QuoteGalaxyCloudRepository.Result.Ok -> {
                    _stars.value = _stars.value + r.value.first
                    nextCursor = r.value.second
                }
                is QuoteGalaxyCloudRepository.Result.Err -> {
                    if (r.code != "NO_CERT" && r.code != "NO_BACKEND") _err.value = r.msg
                }
            }
            _loading.value = false
        }
    }

    fun publish(text: String, bookTitle: String) {
        viewModelScope.launch {
            when (val r = cloud.publish(userId, text, bookTitle)) {
                is QuoteGalaxyCloudRepository.Result.Ok -> {
                    _toast.value = "✨ 你的句子已升入星河"
                    refresh()
                }
                is QuoteGalaxyCloudRepository.Result.Err -> {
                    _toast.value = when (r.code) {
                        "NO_CERT" -> "成为月卡及以上会员，才能向星河发声 ✦"
                        "NO_BACKEND" -> "云端暂不可用，请稍后再试"
                        else -> r.msg
                    }
                }
            }
        }
    }

    fun light(starId: String) {
        viewModelScope.launch {
            when (val r = cloud.light(userId, starId)) {
                is QuoteGalaxyCloudRepository.Result.Ok -> {
                    val newCount = r.value
                    _stars.value = _stars.value.map {
                        if (it.id == starId && newCount >= 0) it.copy(lightCount = newCount) else it
                    }
                }
                is QuoteGalaxyCloudRepository.Result.Err -> {
                    if (r.code == "NO_CERT") _toast.value = "成为 VIP 后才能接住星辰 ✦"
                    else if (r.code != "NO_BACKEND") _toast.value = r.msg
                }
            }
        }
    }

    fun report(starId: String, reason: String = "") {
        viewModelScope.launch {
            when (val r = cloud.report(userId, starId, reason)) {
                is QuoteGalaxyCloudRepository.Result.Ok -> _toast.value = "已收到，感谢你"
                is QuoteGalaxyCloudRepository.Result.Err -> {
                    if (r.code != "NO_CERT" && r.code != "NO_BACKEND") _toast.value = r.msg
                }
            }
        }
    }
}
