// LetterViewModel.kt — 时光信箱 · ViewModel
//
// 🔒 数据隔离：构造时绑定 userId；所有 Flow / suspend 都按该 userId 调用 Repository。
package com.example.funlife.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.funlife.FunLifeApplication
import com.example.funlife.data.model.LetterRecipient
import com.example.funlife.repository.LetterDraft
import com.example.funlife.repository.LetterRepository
import com.example.funlife.repository.LetterSendResult
import com.example.funlife.repository.LetterView
import com.example.funlife.utils.LetterDeliveryWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 发送结果对外暴露的简化态 */
sealed class LetterSendUiState {
    object Idle : LetterSendUiState()
    object Sending : LetterSendUiState()
    data class Sent(val letterId: Long) : LetterSendUiState()
    data class QuotaExceeded(val monthlyQuota: Int) : LetterSendUiState()
    data class DeliveryTooSoon(val minDelayMs: Long) : LetterSendUiState()
    data class Error(val msg: String) : LetterSendUiState()
}

class LetterViewModel(application: Application, val userId: Long) : AndroidViewModel(application) {

    private val db = (application as FunLifeApplication).database
    private val repo = LetterRepository(
        application.applicationContext,
        db.letterRecipientDao(),
        db.letterDao(),
        db.userVipDao()
    )

    val recipients: StateFlow<List<LetterRecipient>> = repo.observeRecipients(userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val letters: StateFlow<List<LetterView>> = repo.observeLetters(userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unreadCount: StateFlow<Int> = repo.observeUnreadCount(userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    /** 🆕 本月已寄出条数（响应式派生于 letters）—— 配额弹窗与升级提示统一来源 */
    val sentThisMonth: StateFlow<Int> = repo.observeLetters(userId)
        .map { list ->
            val start = monthStartMs()
            list.count {
                it.direction == com.example.funlife.data.model.LetterDirection.TO_RECIPIENT &&
                    it.sentAt >= start
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _vipLevel = MutableStateFlow(0)
    val vipLevel: StateFlow<Int> = _vipLevel.asStateFlow()

    private val _sendState = MutableStateFlow<LetterSendUiState>(LetterSendUiState.Idle)
    val sendState: StateFlow<LetterSendUiState> = _sendState.asStateFlow()

    init {
        viewModelScope.launch {
            _vipLevel.value = db.userVipDao().getUserVipSync(userId)?.vipLevel ?: 0
        }
    }

    fun resetSendState() { _sendState.value = LetterSendUiState.Idle }

    fun saveRecipient(
        existing: LetterRecipient?,
        name: String,
        avatar: String,
        customAvatarUri: String?,
        relation: String,
        persona: String,
        timeAnchor: Long?
    ) {
        viewModelScope.launch {
            val draft = (existing ?: LetterRecipient(userId = userId, name = name)).copy(
                userId = userId,
                name = name,
                avatar = avatar.ifBlank { "✉️" },
                customAvatarUri = customAvatarUri,
                relation = relation,
                persona = persona,
                timeAnchor = timeAnchor
            )
            repo.saveRecipient(draft)
        }
    }

    fun deleteRecipient(recipient: LetterRecipient) {
        viewModelScope.launch { repo.deleteRecipient(recipient) }
    }

    suspend fun getLetterById(id: Long): LetterView? = repo.getLetter(userId, id)

    fun markLetterRead(id: Long) {
        viewModelScope.launch { repo.markRead(userId, id) }
    }

    fun observeLettersOfRecipient(recipientId: Long) =
        repo.observeLettersByRecipient(userId, recipientId)

    /**
     * 发送信件。校验失败时 sendState 设为对应错误态，UI 据此弹 toast / 升级提示。
     */
    fun sendLetter(recipientId: Long, content: String, mood: String?, desiredDeliveryAt: Long) {
        viewModelScope.launch {
            _sendState.value = LetterSendUiState.Sending
            val result = repo.sendLetter(
                LetterDraft(
                    userId = userId,
                    recipientId = recipientId,
                    content = content,
                    mood = mood,
                    desiredDeliveryAt = desiredDeliveryAt
                )
            )
            _sendState.value = when (result) {
                is LetterSendResult.Ok -> {
                    // 立即触发一次 Worker，VIP2+ "立即"投递时可秒收
                    LetterDeliveryWorker.triggerOnce(getApplication())
                    LetterSendUiState.Sent(result.letterId)
                }
                LetterSendResult.QuotaExceeded -> LetterSendUiState.QuotaExceeded(
                    LetterRepository.monthlyQuota(_vipLevel.value).coerceAtLeast(0)
                )
                is LetterSendResult.DeliveryTooSoon -> LetterSendUiState.DeliveryTooSoon(result.minDelayMs)
                LetterSendResult.EmptyContent -> LetterSendUiState.Error("信件内容不能为空")
                is LetterSendResult.Error -> LetterSendUiState.Error(result.reason)
            }
        }
    }

    /** VIP 配额简表，供 UI 显示 "本月还能写 X 封" */
    suspend fun remainingThisMonth(): Int {
        val quota = LetterRepository.monthlyQuota(_vipLevel.value)
        if (quota == LetterRepository.UNLIMITED) return Int.MAX_VALUE
        // 简化：通过 letters 当前列表数粗估（按 to_recipient + 本月）
        val now = System.currentTimeMillis()
        val cal = java.util.Calendar.getInstance().apply {
            timeInMillis = now
            set(java.util.Calendar.DAY_OF_MONTH, 1)
            set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0)
        }
        val start = cal.timeInMillis
        val used = letters.value.count {
            it.direction == com.example.funlife.data.model.LetterDirection.TO_RECIPIENT && it.sentAt >= start
        }
        return (quota - used).coerceAtLeast(0)
    }

    private fun monthStartMs(): Long {
        val cal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.DAY_OF_MONTH, 1)
            set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }
}
