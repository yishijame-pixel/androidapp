// ReadingRepository.kt — v53 阅光书房 · 阅读打卡
//
// 职责：
//   1. 写入 ReadingSession（阅读时长打卡）
//   2. 计算"今日已读分钟"、"连续阅读天数"
//   3. 自动联动 CoinDao 发放金币奖励（每日首次打卡 +5，连续 7 天 +20，连续 30 天 +50）
//   4. 暴露月度阅读时长曲线、单本书阅读心电图聚合数据
//
// 隔离：所有方法强制 userId > 0；DAO 全部带 userId 过滤。
package com.example.funlife.repository

import com.example.funlife.data.dao.CoinDao
import com.example.funlife.data.dao.DailyMinutes
import com.example.funlife.data.dao.QuoteDao
import com.example.funlife.data.dao.ReadingSessionDao
import com.example.funlife.data.model.ReadingSession
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class ReadingRepository(
    private val readingSessionDao: ReadingSessionDao,
    private val quoteDao: QuoteDao,
    private val coinDao: CoinDao,
) {

    /* =====================  打卡  ===================== */

    /**
     * 阅读打卡。返回值用于 UI 展示。
     * @param minutes 1..600 之间，越界自动 clamp
     * @param bookId  null = 自由阅读
     * @param atPage  当前页码（用于阅读心电图，可空 0）
     */
    suspend fun checkIn(
        userId: Long,
        minutes: Int,
        bookId: Long? = null,
        atPage: Int = 0,
    ): CheckInResult {
        require(userId > 0L) { "userId must be > 0" }
        val safeMin = minutes.coerceIn(1, 600)
        val today = todayYmd()
        val nowMs = System.currentTimeMillis()

        // 今天此前是否已打过卡（用于判断是否首次发金币）
        val before = readingSessionDao.minutesOfDay(userId, today)

        readingSessionDao.insert(
            ReadingSession(
                userId = userId,
                bookId = bookId,
                minutes = safeMin,
                dateYmd = today,
                atPage = atPage.coerceAtLeast(0),
                createdAt = nowMs,
            )
        )

        val streak = calcStreakDays(userId)

        // 金币奖励规则（仅当天首次打卡发放，避免一天疯狂刷）
        var coinAward = 0
        if (before == 0) {
            coinAward = 5
            if (streak == 7) coinAward += 20
            else if (streak == 30) coinAward += 50
            else if (streak > 0 && streak % 100 == 0) coinAward += 100
            runCatching { coinDao.addCoins(userId, coinAward) }
        }

        return CheckInResult(
            todayMinutes = before + safeMin,
            streakDays = streak,
            coinAward = coinAward,
        )
    }

    /** 实时观察今日已读分钟数 */
    fun observeTodayMinutes(userId: Long): Flow<Int> {
        require(userId > 0L)
        return readingSessionDao.observeMinutesOfDay(userId, todayYmd())
    }

    /**
     * 连续阅读天数。从今天往前数，直到遇到一个没打卡的日子为止。
     * 即使今天还没打过，但昨天打过，仍会返回 0（用户视角："今天断签"）。
     */
    suspend fun calcStreakDays(userId: Long): Int {
        require(userId > 0L)
        // 取最近 365 天的去重日期足够覆盖连续天数
        val sinceYmd = ymdMinusDays(365)
        val dates = readingSessionDao.distinctDatesSince(userId, sinceYmd).toHashSet()
        if (dates.isEmpty()) return 0
        // 从今天往前推；今天没打则从昨天起
        val today = todayYmd()
        var offset = if (today in dates) 0 else 1
        var streak = 0
        while (true) {
            val cursor = shiftYmd(today, -offset)
            if (cursor in dates) {
                streak++; offset++
            } else break
        }
        return streak
    }

    /** 月度阅读时长曲线（最近 N 天） */
    suspend fun loadDailyMinutes(userId: Long, sinceDays: Int = 30): List<DailyMinutes> {
        require(userId > 0L)
        return readingSessionDao.dailyMinutesSince(userId, ymdMinusDays(sinceDays))
    }

    /**
     * 单本书阅读心电图（v53.2：双因子权重）
     *
     * 公式：weight(page) = α · timeShare(page) + (1-α) · quoteShare(page)
     *   - timeShare:  该 page 阅读分钟数 / 全书总分钟
     *   - quoteShare: 该 page 摘抄条数  / 全书总摘抄
     *   - α 默认 0.5；若任一分量缺失则自动权重转移给另一个
     *
     * 取并集：sessions.atPage ∪ quotes.page —— 用户只摘抄不打卡的高峰也能浮现。
     */
    suspend fun loadBookEcg(
        userId: Long,
        bookId: Long,
        timeWeight: Float = 0.5f,
    ): List<EcgPoint> {
        require(userId > 0L && bookId > 0L)
        val a = timeWeight.coerceIn(0f, 1f)

        val sessions = readingSessionDao.getByBook(userId, bookId)
        val quotes = quoteDao.getRecentByBook(userId, bookId, limit = 5000)

        val pageMinutes = sessions.filter { it.atPage > 0 }
            .groupBy { it.atPage }
            .mapValues { (_, list) -> list.sumOf { it.minutes } }
        val pageQuotes = quotes.filter { it.page > 0 }
            .groupBy { it.page }
            .mapValues { (_, list) -> list.size }

        val pages = (pageMinutes.keys + pageQuotes.keys).sorted()
        if (pages.isEmpty()) return emptyList()

        val totalMin = pageMinutes.values.sum()
        val totalQ = pageQuotes.values.sum()

        // 缺失分量时把权重让给另一个，保证总和归一
        val (wT, wQ) = when {
            totalMin == 0 && totalQ == 0 -> 0f to 0f
            totalMin == 0 -> 0f to 1f
            totalQ == 0 -> 1f to 0f
            else -> a to (1f - a)
        }

        return pages.map { p ->
            val tShare = if (totalMin > 0) (pageMinutes[p] ?: 0).toFloat() / totalMin else 0f
            val qShare = if (totalQ > 0) (pageQuotes[p] ?: 0).toFloat() / totalQ else 0f
            EcgPoint(page = p, weight = wT * tShare + wQ * qShare)
        }
    }

    fun observeBookMinutes(userId: Long, bookId: Long): Flow<Int> {
        require(userId > 0L && bookId > 0L)
        return readingSessionDao.observeMinutesOfBook(userId, bookId)
    }

    /* =====================  辅助 / 时间  ===================== */

    private fun todayYmd(): Int = ymdOf(Calendar.getInstance())

    private fun ymdMinusDays(days: Int): Int {
        val c = Calendar.getInstance()
        c.add(Calendar.DAY_OF_YEAR, -days)
        return ymdOf(c)
    }

    private fun shiftYmd(baseYmd: Int, deltaDays: Int): Int {
        val y = baseYmd / 10000
        val m = (baseYmd / 100) % 100
        val d = baseYmd % 100
        val c = Calendar.getInstance().apply {
            clear(); set(y, m - 1, d)
            add(Calendar.DAY_OF_YEAR, deltaDays)
        }
        return ymdOf(c)
    }

    private fun ymdOf(c: Calendar): Int =
        c.get(Calendar.YEAR) * 10000 +
        (c.get(Calendar.MONTH) + 1) * 100 +
        c.get(Calendar.DAY_OF_MONTH)
}

data class CheckInResult(
    val todayMinutes: Int,
    val streakDays: Int,
    val coinAward: Int,
)

data class EcgPoint(
    val page: Int,
    /** 0..1 归一化能量 */
    val weight: Float,
)
