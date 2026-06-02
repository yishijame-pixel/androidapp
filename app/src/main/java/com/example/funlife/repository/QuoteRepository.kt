// QuoteRepository.kt — v53 阅光书房 · 摘抄 + 时光胶囊
//
// 职责：
//   1. 摘抄 CRUD（带 userId 隔离 + 字段长度防御）
//   2. 时光胶囊投递时间约束（最短延迟 = VipQuota.letterMinDelayMs；月度配额 = VipQuota.readingCapsuleMonthlyLimit）
//   3. 提供 LetterDeliveryWorker / MorningHeraldWorker 用的扫描接口
//
// 注：胶囊投递与时光信箱 LetterDeliveryWorker 共享底层扫描机制（同 Worker 多分支）。
package com.example.funlife.repository

import com.example.funlife.data.dao.QuoteDao
import com.example.funlife.data.dao.UserVipDao
import com.example.funlife.data.model.Quote
import com.example.funlife.vip.VipQuota
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class QuoteRepository(
    private val quoteDao: QuoteDao,
    private val userVipDao: UserVipDao,
) {

    /* =====================  CRUD  ===================== */

    fun observeAll(userId: Long): Flow<List<Quote>> {
        require(userId > 0L); return quoteDao.observeAll(userId)
    }

    fun observeByBook(userId: Long, bookId: Long): Flow<List<Quote>> {
        require(userId > 0L && bookId > 0L); return quoteDao.observeByBook(userId, bookId)
    }

    fun countByBook(userId: Long, bookId: Long): Flow<Int> {
        require(userId > 0L && bookId > 0L); return quoteDao.countByBook(userId, bookId)
    }

    suspend fun getById(userId: Long, id: Long): Quote? {
        require(userId > 0L); return quoteDao.getById(userId, id)
    }

    /** 写入摘抄；如附带 capsuleDeliveryAt 则会做 VIP 配额 / 最短延迟校验 */
    suspend fun save(quote: Quote): SaveResult {
        require(quote.userId > 0L) { "userId must be > 0" }
        require(quote.bookId > 0L) { "bookId must be > 0" }
        val text = quote.text.trim()
        if (text.isEmpty()) return SaveResult.Invalid("摘抄不能为空")
        if (text.length > 500) return SaveResult.Invalid("摘抄最长 500 字")

        val cleaned = quote.copy(
            text = text,
            page = quote.page.coerceAtLeast(0),
            rating = quote.rating.coerceIn(0, 5),
        )

        // 时光胶囊校验
        if (cleaned.capsuleDeliveryAt > 0L) {
            val vipLevel = userVipDao.getUserVipSync(cleaned.userId)?.vipLevel ?: 0

            // 最短延迟（与时光信箱共享 letterMinDelayMs）
            val minDelay = VipQuota.letterMinDelayMs(vipLevel)
            val now = System.currentTimeMillis()
            if (cleaned.capsuleDeliveryAt - now < minDelay) {
                return SaveResult.NeedsLongerDelay(minDelayMs = minDelay)
            }

            // 月度配额（仅当 capsuleDeliveryAt > 0 才计数）
            val limit = VipQuota.readingCapsuleMonthlyLimit(vipLevel)
            if (limit != VipQuota.UNLIMITED) {
                val used = countCapsulesThisMonth(cleaned.userId)
                if (used >= limit) {
                    return SaveResult.QuotaExceeded(used = used, limit = limit, vipLevel = vipLevel)
                }
            }
        }

        val id = quoteDao.insert(cleaned)
        return SaveResult.Success(id)
    }

    suspend fun update(quote: Quote) {
        require(quote.userId > 0L); quoteDao.update(quote)
    }

    suspend fun delete(quote: Quote) {
        require(quote.userId > 0L); quoteDao.delete(quote)
    }

    suspend fun markPublishedToGalaxy(userId: Long, id: Long) {
        require(userId > 0L); quoteDao.markPublishedToGalaxy(userId, id)
    }

    /* =====================  时光胶囊扫描（Worker 用）  ===================== */

    suspend fun findDueCapsules(limit: Int = 50): List<Quote> =
        quoteDao.findDueCapsules(System.currentTimeMillis(), limit)

    suspend fun markCapsuleDelivered(id: Long) = quoteDao.markCapsuleDelivered(id)

    /* =====================  晨光信使取材  ===================== */

    suspend fun pickRandomQuality(userId: Long): Quote? =
        quoteDao.pickRandomQuality(userId)

    suspend fun pickYesterdayCreated(userId: Long): Quote? {
        val (start, end) = yesterdayMsRange()
        return quoteDao.pickRandomCreatedBetween(userId, start, end)
    }

    suspend fun pickUpcomingCapsule(userId: Long, windowDays: Int = 7): Quote? {
        val window = windowDays * 24L * 3600L * 1000L
        return quoteDao.pickUpcomingCapsule(userId, System.currentTimeMillis(), window)
    }

    /* =====================  配额查询  ===================== */

    suspend fun countCapsulesThisMonth(userId: Long): Int {
        val (start, end) = thisMonthMsRange()
        return quoteDao.countCapsulesInMonth(userId, start, end)
    }

    suspend fun remainingCapsulesThisMonth(userId: Long): Int {
        val vipLevel = userVipDao.getUserVipSync(userId)?.vipLevel ?: 0
        val limit = VipQuota.readingCapsuleMonthlyLimit(vipLevel)
        if (limit == VipQuota.UNLIMITED) return Int.MAX_VALUE
        val used = countCapsulesThisMonth(userId)
        return (limit - used).coerceAtLeast(0)
    }

    /* =====================  时间辅助  ===================== */

    private fun thisMonthMsRange(): Pair<Long, Long> {
        val c = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val start = c.timeInMillis
        c.add(Calendar.MONTH, 1)
        return start to c.timeInMillis
    }

    private fun yesterdayMsRange(): Pair<Long, Long> {
        val c = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, -1)
        }
        val start = c.timeInMillis
        c.add(Calendar.DAY_OF_YEAR, 1)
        return start to c.timeInMillis
    }

    sealed class SaveResult {
        data class Success(val id: Long) : SaveResult()
        data class Invalid(val msg: String) : SaveResult()
        data class NeedsLongerDelay(val minDelayMs: Long) : SaveResult()
        data class QuotaExceeded(val used: Int, val limit: Int, val vipLevel: Int) : SaveResult()
    }
}
