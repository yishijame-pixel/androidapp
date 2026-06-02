// BookViewModel.kt — 方案 F · 人生书架
package com.example.funlife.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.funlife.FunLifeApplication
import com.example.funlife.data.model.Book
import com.example.funlife.data.model.BookYearStats
import com.example.funlife.repository.BookRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

sealed class YearbookUiState {
    object Idle : YearbookUiState()
    object Generating : YearbookUiState()
    data class Ready(val text: String, val fromCloud: Boolean, val stats: BookYearStats) : YearbookUiState()
    data class NotVip3(val message: String) : YearbookUiState()
    data class Empty(val year: Int) : YearbookUiState()
}

class BookViewModel(application: Application, val userId: Long) : AndroidViewModel(application) {

    private val db = (application as FunLifeApplication).database
    private val repo = BookRepository(
        application.applicationContext,
        db.bookDao(),
        db.userVipDao()
    )

    val books: StateFlow<List<Book>> = repo.observeAll(userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalCount: StateFlow<Int> = repo.observeCount(userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _yearbook = MutableStateFlow<YearbookUiState>(YearbookUiState.Idle)
    val yearbook: StateFlow<YearbookUiState> = _yearbook.asStateFlow()

    fun saveBook(
        existing: Book?,
        title: String,
        author: String,
        rating: Int,
        finishedAt: Long,
        note: String,
        favoriteQuote: String,
        tags: String
    ) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val book = (existing ?: Book(userId = userId, title = title)).copy(
                userId = userId,
                title = title,
                author = author,
                rating = rating,
                finishedAt = finishedAt,
                note = note,
                favoriteQuote = favoriteQuote,
                tags = tags,
                createdAt = existing?.createdAt ?: now,
                updatedAt = now
            )
            repo.save(book)
        }
    }

    fun deleteBook(book: Book) {
        viewModelScope.launch { repo.delete(book) }
    }

    suspend fun getById(id: Long): Book? = repo.getById(userId, id)

    /** 触发"AI 生成年鉴"。VIP3+ 才能用；非 VIP3 给提示。云端失败回退本地模板。 */
    fun generateYearbook(year: Int = currentYear()) {
        _yearbook.value = YearbookUiState.Generating
        viewModelScope.launch {
            val stats = repo.loadYearStats(userId, year)
            if (stats.totalBooks == 0) {
                _yearbook.value = YearbookUiState.Empty(year)
                return@launch
            }
            if (!repo.canGenerateYearbook(userId)) {
                _yearbook.value = YearbookUiState.NotVip3(
                    "AI 个人书评年鉴是 VIP3 / 终身会员专享 ✨"
                )
                return@launch
            }
            val cloudReply = repo.generateYearbookViaCloud(userId, year)
            if (cloudReply != null) {
                _yearbook.value = YearbookUiState.Ready(cloudReply, true, stats)
            } else {
                // 云端失败 → 本地兜底（仍然生成可读年鉴，但提示文案标记为本地）
                val booksOfYear = books.value.filter {
                    val cal = Calendar.getInstance().apply { timeInMillis = it.finishedAt }
                    cal.get(Calendar.YEAR) == year && it.finishedAt > 0L
                }
                val local = repo.buildLocalYearbook(stats, booksOfYear)
                _yearbook.value = YearbookUiState.Ready(local, false, stats)
            }
        }
    }

    fun resetYearbook() { _yearbook.value = YearbookUiState.Idle }

    companion object {
        fun currentYear(): Int =
            Calendar.getInstance().get(Calendar.YEAR)
    }
}
