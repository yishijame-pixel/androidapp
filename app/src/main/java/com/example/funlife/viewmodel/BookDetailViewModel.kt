// BookDetailViewModel.kt — v53 单本书详情 VM
//
// 承载：
//   - 书的基本信息（流式订阅，自动刷新）
//   - 这本书的摘抄列表
//   - 阅读心电图聚合数据
//   - 阅读时长（针对此书）
//   - 开篇期待 / 完成宣言 编辑
//   - 阅读进度更新
package com.example.funlife.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.funlife.FunLifeApplication
import com.example.funlife.data.model.Book
import com.example.funlife.data.model.Quote
import com.example.funlife.repository.BookRepository
import com.example.funlife.repository.EcgPoint
import com.example.funlife.repository.QuoteRepository
import com.example.funlife.repository.ReadingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BookDetailViewModel(
    application: Application,
    val userId: Long,
    val bookId: Long,
) : AndroidViewModel(application) {

    private val app = application as FunLifeApplication
    private val db = app.database

    private val bookRepo = BookRepository(application, db.bookDao(), db.userVipDao())
    private val quoteRepo = QuoteRepository(db.quoteDao(), db.userVipDao())
    private val readingRepo = ReadingRepository(db.readingSessionDao(), db.quoteDao(), db.coinDao())

    private val _book = MutableStateFlow<Book?>(null)
    val book: StateFlow<Book?> = _book.asStateFlow()

    val quotes: StateFlow<List<Quote>> =
        quoteRepo.observeByBook(userId, bookId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _ecg = MutableStateFlow<List<EcgPoint>>(emptyList())
    val ecg: StateFlow<List<EcgPoint>> = _ecg.asStateFlow()

    val totalMinutes: StateFlow<Int> =
        readingRepo.observeBookMinutes(userId, bookId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    /** 🆕 v54 该用户该本书的所有 AI 对话档案（仅 VIP3 才会有数据） */
    val chatSessions: StateFlow<List<com.example.funlife.data.model.BookChatSession>> =
        db.bookChatSessionDao().observeByBook(userId, bookId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** 用户当前 VIP 是否解锁了 "读书档案" */
    private val _archiveUnlocked = MutableStateFlow(false)
    val archiveUnlocked: StateFlow<Boolean> = _archiveUnlocked.asStateFlow()

    private val _toast = MutableStateFlow<String?>(null)
    val toast: StateFlow<String?> = _toast.asStateFlow()
    fun consumeToast() { _toast.value = null }

    /** E2 双向胶囊触发事件：刚刚由"未读完 → 读完"的瞬间，UI 弹"穿越对话"对照卡。 */
    private val _justFinished = MutableStateFlow(false)
    val justFinished: StateFlow<Boolean> = _justFinished.asStateFlow()
    fun consumeJustFinished() { _justFinished.value = false }

    init {
        viewModelScope.launch {
            _book.value = bookRepo.getById(userId, bookId)
            _ecg.value = readingRepo.loadBookEcg(userId, bookId)
            val vipLevel = db.userVipDao().getUserVipSync(userId)?.vipLevel ?: 0
            _archiveUnlocked.value = com.example.funlife.vip.VipQuota.aiBookDeepChatUnlocked(vipLevel)
        }
    }

    fun reloadEcg() {
        viewModelScope.launch { _ecg.value = readingRepo.loadBookEcg(userId, bookId) }
    }

    /** 更新开篇期待 / 完成宣言 / 进度 / 评分 / 心情 */
    fun patchBook(transform: (Book) -> Book) {
        viewModelScope.launch {
            val cur = _book.value ?: return@launch
            val patched = transform(cur)
            bookRepo.save(patched)
            _book.value = bookRepo.getById(userId, bookId)
        }
    }

    /** 标记读完（finishedAt + finishedMood）。如果之前 finishedAt == 0，则触发穿越对话。 */
    fun markFinished(mood: String) {
        viewModelScope.launch {
            val cur = _book.value ?: return@launch
            val wasUnfinished = cur.finishedAt <= 0
            val patched = cur.copy(
                finishedAt = System.currentTimeMillis(),
                finishedMood = mood,
            )
            bookRepo.save(patched)
            _book.value = bookRepo.getById(userId, bookId)
            _toast.value = "🎉 你读完了这本书"
            // E2 双向胶囊：仅在"未读完 → 读完"的瞬间触发穿越对话
            if (wasUnfinished) _justFinished.value = true
        }
    }

    /** 阅读打卡（含 atPage） */
    fun checkIn(minutes: Int, atPage: Int) {
        viewModelScope.launch {
            readingRepo.checkIn(userId, minutes, bookId, atPage)
            _ecg.value = readingRepo.loadBookEcg(userId, bookId)
            _toast.value = "✓ 已打卡 $minutes 分钟"
        }
    }

    fun togglePinned(q: Quote) = viewModelScope.launch {
        quoteRepo.update(q.copy(pinned = !q.pinned))
    }

    fun deleteQuote(q: Quote) = viewModelScope.launch { quoteRepo.delete(q) }

    /** 加摘抄 / 寄胶囊 */
    fun saveQuote(
        text: String,
        page: Int = 0,
        rating: Int = 0,
        pinned: Boolean = false,
        capsuleDeliveryAt: Long = 0L,
    ) {
        viewModelScope.launch {
            val r = quoteRepo.save(
                Quote(
                    userId = userId, bookId = bookId,
                    text = text, page = page, rating = rating,
                    pinned = pinned, capsuleDeliveryAt = capsuleDeliveryAt
                )
            )
            _toast.value = when (r) {
                is QuoteRepository.SaveResult.Success ->
                    if (capsuleDeliveryAt > 0L) "✨ 胶囊已寄出"
                    else "📝 已记入摘抄本"
                is QuoteRepository.SaveResult.Invalid -> r.msg
                is QuoteRepository.SaveResult.NeedsLongerDelay ->
                    "胶囊投递至少 ${r.minDelayMs / 60000} 分钟之后"
                is QuoteRepository.SaveResult.QuotaExceeded ->
                    "本月胶囊额度已用完（${r.used}/${r.limit}）"
            }
        }
    }
}
