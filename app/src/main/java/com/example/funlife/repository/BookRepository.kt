// BookRepository.kt — 方案 F · 人生书架
//
// 设计：
//   - 增删改查走本地 Room
//   - "AI 个人书评年鉴"是 VIP3+ 专享：调 chat_ai 云函数（KEY 在云端 + 服务端配额）
//     云函数失败时降级为本地规则模板（保证体验不中断，但提示用户云端不可用）
package com.example.funlife.repository

import android.content.Context
import com.example.funlife.data.dao.BookDao
import com.example.funlife.data.dao.UserVipDao
import com.example.funlife.data.model.Book
import com.example.funlife.data.model.BookYearStats
import com.example.funlife.data.model.VipLevel
import com.example.funlife.vip.ChatAiCloudRepository
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class BookRepository(
    private val context: Context,
    private val bookDao: BookDao,
    private val userVipDao: UserVipDao
) {

    fun observeAll(userId: Long): Flow<List<Book>> {
        require(userId > 0L)
        return bookDao.observeAll(userId)
    }

    fun observeCount(userId: Long): Flow<Int> {
        require(userId > 0L)
        return bookDao.countAll(userId)
    }

    suspend fun getById(userId: Long, id: Long): Book? {
        require(userId > 0L)
        return bookDao.getById(userId, id)
    }

    suspend fun save(book: Book): Long {
        require(book.userId > 0L) { "userId must be > 0" }
        require(book.title.isNotBlank()) { "title blank" }
        val now = System.currentTimeMillis()
        val cleaned = book.copy(
            title = book.title.trim().take(120),
            author = book.author.trim().take(80),
            note = book.note.take(4000),
            favoriteQuote = book.favoriteQuote.take(800),
            tags = book.tags.trim().take(200),
            rating = book.rating.coerceIn(0, 5),
            // v53 字段防御
            totalPages = book.totalPages.coerceAtLeast(0),
            currentPage = book.currentPage.coerceIn(0, book.totalPages.coerceAtLeast(0).coerceAtLeast(book.currentPage)),
            openingLetter = book.openingLetter.take(280),
            openingMood = book.openingMood.take(40),
            finishedMood = book.finishedMood.take(120),
            createdAt = if (book.createdAt == 0L) now else book.createdAt,
            updatedAt = now,
        )
        return bookDao.insert(cleaned)
    }

    suspend fun delete(book: Book) {
        require(book.userId > 0L)
        bookDao.delete(book)
    }

    /* ─────────── 年鉴 ─────────── */

    /** 是否解锁年鉴功能（VIP3 / PERMANENT 才能用） */
    suspend fun canGenerateYearbook(userId: Long): Boolean {
        val level = userVipDao.getUserVipSync(userId)?.vipLevel ?: 0
        return when (VipLevel.fromLevel(level)) {
            VipLevel.VIP3, VipLevel.PERMANENT -> true
            else -> false
        }
    }

    suspend fun loadYearStats(userId: Long, year: Int): BookYearStats {
        val (start, end) = yearRangeMs(year)
        val books = bookDao.getBooksOfYear(userId, start, end)
        val rated = books.filter { it.rating > 0 }
        val avg = if (rated.isNotEmpty()) rated.sumOf { it.rating } / rated.size.toDouble() else 0.0
        val tagCount = mutableMapOf<String, Int>()
        books.forEach { b ->
            b.tags.split(",", "，").map { it.trim() }.filter { it.isNotEmpty() }.forEach { t ->
                tagCount[t] = (tagCount[t] ?: 0) + 1
            }
        }
        val topTags = tagCount.entries.sortedByDescending { it.value }.take(5)
            .map { it.key to it.value }
        return BookYearStats(year, books.size, rated.size, avg, topTags)
    }

    /**
     * 调用 AI 云端生成年鉴。
     * @return 成功 → 长文；失败 → null（UI 自行兜底为规则模板）
     */
    suspend fun generateYearbookViaCloud(userId: Long, year: Int): String? {
        if (!canGenerateYearbook(userId)) return null
        val (start, end) = yearRangeMs(year)
        val books = bookDao.getBooksOfYear(userId, start, end)
        if (books.isEmpty()) return null

        val brief = buildString {
            append("以下是用户在 $year 年读完的 ${books.size} 本书：\n")
            books.forEachIndexed { i, b ->
                append("${i + 1}. 《${b.title}》")
                if (b.author.isNotEmpty()) append(" by ${b.author}")
                if (b.rating > 0) append(" ${"★".repeat(b.rating)}")
                if (b.tags.isNotEmpty()) append(" [标签:${b.tags}]")
                if (b.note.isNotBlank()) append("\n   心得：${b.note.take(120)}")
                if (b.favoriteQuote.isNotBlank()) append("\n   摘抄：${b.favoriteQuote.take(80)}")
                append('\n')
            }
        }
        val system = """
            你是用户的私人阅读知己，文笔温暖、克制、有洞察力。
            请基于用户提供的本年度书单，写一篇 400 字以内的"年度阅读年鉴"，要求：
            1. 开头一句点题，体现该用户这一年的阅读特征（关键词由你从书单中归纳）
            2. 中间挑 1-2 本最值得说的书展开点评（必要时引用用户的心得/摘抄）
            3. 结尾给一句对下一年的祝愿，温暖但不肉麻
            语气真诚、不堆砌辞藻、不假大空。
        """.trimIndent()

        return try {
            val cloud = ChatAiCloudRepository(context.applicationContext)
            val r = cloud.reply(
                userId = userId,
                body = ChatAiCloudRepository.Body(
                    mode = "chat",
                    personaSystem = system,
                    userText = brief
                )
            )
            when (r) {
                is ChatAiCloudRepository.CallResult.Success -> r.reply
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }

    /** 兜底：纯本地规则模板（云端不可用时仍可生成） */
    fun buildLocalYearbook(stats: BookYearStats, books: List<Book>): String {
        if (books.isEmpty()) return "今年还没有读完的书。新的一年，从一本翻开开始 ✨"
        val sb = StringBuilder()
        sb.append("📚 ${stats.year} 年阅读年鉴\n\n")
        sb.append("这一年，你读完了 ${stats.totalBooks} 本书")
        if (stats.totalRated > 0) {
            sb.append("，其中 ${stats.totalRated} 本被你认真打分")
            sb.append("，平均分 ${"%.1f".format(stats.avgRating)} 星")
        }
        sb.append("。\n\n")
        if (stats.topTags.isNotEmpty()) {
            sb.append("最常出现的标签是：")
            sb.append(stats.topTags.joinToString("、") { "${it.first}(${it.second})" })
            sb.append("。\n\n")
        }
        val top = books.maxByOrNull { it.rating }
        if (top != null && top.rating >= 4) {
            sb.append("🌟 最爱的一本：《${top.title}》")
            if (top.author.isNotEmpty()) sb.append(" / ${top.author}")
            if (top.note.isNotBlank()) {
                sb.append("\n   你写下的：${top.note.take(80)}")
            }
            sb.append("\n\n")
        }
        sb.append("愿下一年继续在书里遇见更好的自己。")
        return sb.toString()
    }

    /* ─────────── 内部 ─────────── */

    private fun yearRangeMs(year: Int): Pair<Long, Long> {
        val cal = Calendar.getInstance().apply {
            clear(); set(Calendar.YEAR, year)
            set(Calendar.MONTH, 0); set(Calendar.DAY_OF_MONTH, 1)
        }
        val start = cal.timeInMillis
        cal.add(Calendar.YEAR, 1)
        val end = cal.timeInMillis
        return start to end
    }
}
