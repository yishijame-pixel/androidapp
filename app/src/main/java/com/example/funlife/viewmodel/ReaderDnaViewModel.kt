// ReaderDnaViewModel.kt — v53 阅光书房 · 读者 DNA
package com.example.funlife.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.funlife.FunLifeApplication
import com.example.funlife.data.model.ReaderDnaCard
import com.example.funlife.repository.ReaderDnaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ReaderDnaViewModel(
    application: Application,
    val userId: Long,
) : AndroidViewModel(application) {

    private val app = application as FunLifeApplication
    private val repo = ReaderDnaRepository(
        context = application,
        bookDao = app.database.bookDao(),
        quoteDao = app.database.quoteDao(),
        dnaDao = app.database.readerDnaCardDao(),
        userVipDao = app.database.userVipDao(),
    )

    val history: StateFlow<List<ReaderDnaCard>> =
        app.database.readerDnaCardDao().observeAll(userId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _generating = MutableStateFlow(false)
    val generating: StateFlow<Boolean> = _generating.asStateFlow()

    private val _toast = MutableStateFlow<String?>(null)
    val toast: StateFlow<String?> = _toast.asStateFlow()
    fun consumeToast() { _toast.value = null }

    private val _cooldownMs = MutableStateFlow(0L)
    val cooldownMs: StateFlow<Long> = _cooldownMs.asStateFlow()

    init {
        viewModelScope.launch {
            _cooldownMs.value = repo.cooldownRemainingMs(userId)
        }
    }

    fun generate() {
        if (_generating.value) return
        viewModelScope.launch {
            _generating.value = true
            when (val r = repo.generate(userId)) {
                is ReaderDnaRepository.GenResult.Success ->
                    _toast.value = "🧬 你的读者 DNA 已生成"
                is ReaderDnaRepository.GenResult.SuccessLocal ->
                    _toast.value = "🧬 已生成（本地估算）—— 云端 AI 暂不可用"
                is ReaderDnaRepository.GenResult.Cooldown -> {
                    _cooldownMs.value = r.remainingMs
                    _toast.value = "❄️ 距离下次生成还需 ${r.remainingMs / (24 * 3600 * 1000)} 天"
                }
                is ReaderDnaRepository.GenResult.NoData ->
                    _toast.value = "📚 至少读完 1 本书后再来吧"
                is ReaderDnaRepository.GenResult.QuotaExceeded ->
                    _toast.value = "今日 DNA 生成额度已用完"
            }
            _cooldownMs.value = repo.cooldownRemainingMs(userId)
            _generating.value = false
        }
    }
}
