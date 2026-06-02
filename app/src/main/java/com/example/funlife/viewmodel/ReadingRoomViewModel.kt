// ReadingRoomViewModel.kt — v53 阅光书房 总入口 ViewModel
//
// 一个 VM 承载阅光书房 4 个 Tab 的核心状态：
//   1. 阅读 Tab：今日已读分钟、连续天数、月度曲线
//   2. 书架 Tab：（直接复用 BookViewModel 的 books 流）
//   3. 摘抄 Tab：所有摘抄 + 待寄胶囊数 + 配额
//   4. 星河 Tab：云端 feed（懒加载到子 VM）
//
// 隔离：所有方法都按 userId 过滤；ViewModel 在 NavGraph 里按 userId 注入。
package com.example.funlife.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.funlife.FunLifeApplication
import com.example.funlife.data.dao.DailyMinutes
import com.example.funlife.data.model.Book
import com.example.funlife.data.model.Quote
import com.example.funlife.repository.CheckInResult
import com.example.funlife.repository.QuoteRepository
import com.example.funlife.repository.ReadingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ReadingRoomViewModel(
    application: Application,
    val userId: Long,
) : AndroidViewModel(application) {

    private val app = application as FunLifeApplication
    private val db = app.database

    private val readingRepo = ReadingRepository(
        readingSessionDao = db.readingSessionDao(),
        quoteDao = db.quoteDao(),
        coinDao = db.coinDao(),
    )
    private val quoteRepo = QuoteRepository(
        quoteDao = db.quoteDao(),
        userVipDao = db.userVipDao(),
    )

    /* ── 阅读 Tab 状态流 ── */
    val todayMinutes: StateFlow<Int> = readingRepo.observeTodayMinutes(userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _streakDays = MutableStateFlow(0)
    val streakDays: StateFlow<Int> = _streakDays.asStateFlow()

    private val _monthlyCurve = MutableStateFlow<List<DailyMinutes>>(emptyList())
    val monthlyCurve: StateFlow<List<DailyMinutes>> = _monthlyCurve.asStateFlow()

    private val _lastCheckIn = MutableStateFlow<CheckInResult?>(null)
    val lastCheckIn: StateFlow<CheckInResult?> = _lastCheckIn.asStateFlow()

    /* ── 摘抄 Tab 状态 ── */
    val allQuotes: StateFlow<List<Quote>> = quoteRepo.observeAll(userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _capsuleQuotaUsed = MutableStateFlow(0)
    val capsuleQuotaUsed: StateFlow<Int> = _capsuleQuotaUsed.asStateFlow()

    private val _vipLevel = MutableStateFlow(0)
    val vipLevel: StateFlow<Int> = _vipLevel.asStateFlow()

    /* ── 一次性消息 ── */
    private val _toast = MutableStateFlow<String?>(null)
    val toast: StateFlow<String?> = _toast.asStateFlow()
    fun consumeToast() { _toast.value = null }

    init {
        refreshStats()
        viewModelScope.launch {
            _vipLevel.value = db.userVipDao().getUserVipSync(userId)?.vipLevel ?: 0
        }
    }

    fun refreshStats() {
        viewModelScope.launch {
            _streakDays.value = readingRepo.calcStreakDays(userId)
            _monthlyCurve.value = readingRepo.loadDailyMinutes(userId, sinceDays = 30)
            _capsuleQuotaUsed.value = quoteRepo.countCapsulesThisMonth(userId)
        }
    }

    /* ── 阅读打卡 ── */
    fun checkIn(minutes: Int, bookId: Long? = null, atPage: Int = 0) {
        viewModelScope.launch {
            val r = readingRepo.checkIn(userId, minutes, bookId, atPage)
            _lastCheckIn.value = r
            _streakDays.value = r.streakDays
            _monthlyCurve.value = readingRepo.loadDailyMinutes(userId, sinceDays = 30)
            if (r.coinAward > 0) {
                _toast.value = "今日打卡 +${r.coinAward} 金币 · 连续 ${r.streakDays} 天 ✨"
            }
        }
    }

    fun consumeCheckIn() { _lastCheckIn.value = null }

    /* ── 摘抄保存（含可选胶囊） ── */
    fun saveQuote(
        bookId: Long,
        text: String,
        page: Int = 0,
        rating: Int = 0,
        pinned: Boolean = false,
        capsuleDeliveryAt: Long = 0L,
        onResult: (QuoteRepository.SaveResult) -> Unit = {},
    ) {
        viewModelScope.launch {
            val r = quoteRepo.save(
                Quote(
                    userId = userId,
                    bookId = bookId,
                    text = text,
                    page = page,
                    rating = rating,
                    pinned = pinned,
                    capsuleDeliveryAt = capsuleDeliveryAt,
                )
            )
            when (r) {
                is QuoteRepository.SaveResult.Success -> {
                    if (capsuleDeliveryAt > 0L) {
                        _toast.value = "✨ 胶囊已寄出，等待相遇之日"
                        _capsuleQuotaUsed.value = quoteRepo.countCapsulesThisMonth(userId)
                    } else {
                        _toast.value = "📝 已记入摘抄本"
                    }
                }
                is QuoteRepository.SaveResult.Invalid ->
                    _toast.value = r.msg
                is QuoteRepository.SaveResult.NeedsLongerDelay ->
                    _toast.value = "胶囊投递时间至少 ${r.minDelayMs / 60000} 分钟之后"
                is QuoteRepository.SaveResult.QuotaExceeded ->
                    _toast.value = "本月胶囊额度已用完（${r.used}/${r.limit}）"
            }
            onResult(r)
        }
    }

    fun deleteQuote(q: Quote) = viewModelScope.launch { quoteRepo.delete(q) }

    fun togglePinned(q: Quote) = viewModelScope.launch {
        quoteRepo.update(q.copy(pinned = !q.pinned))
    }
}
