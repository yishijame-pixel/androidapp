// ReadingRepositoryV53Test.kt — v53 阅光书房 · 阅读打卡核心逻辑测试
//
// 验收点：
//   1. 单次打卡：返回 todayMinutes / streakDays / coinAward
//   2. 同一天多次打卡：minutes 累加；金币只发放一次（首次打卡）
//   3. 阅读心电图：按 page 桶聚合，weight 归一化到 0..1
//   4. 多用户：streak 与 todayMinutes 完全隔离
package com.example.funlife.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.funlife.data.database.AppDatabase
import com.example.funlife.data.model.Book
import com.example.funlife.data.model.Quote
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ReadingRepositoryV53Test {

    private lateinit var db: AppDatabase
    private lateinit var repo: ReadingRepository
    private val USER_A = 11L
    private val USER_B = 22L

    @Before
    fun setUp(): Unit = runBlocking {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java)
            .allowMainThreadQueries()
            .fallbackToDestructiveMigration()
            .build()
        repo = ReadingRepository(
            db.readingSessionDao(), db.quoteDao(), db.coinDao()
        )
    }

    @After fun tearDown() { db.close() }

    /* ───────────── 单次打卡 ───────────── */

    @Test
    fun firstCheckIn_returnsExpectedFields(): Unit = runBlocking {
        val r = repo.checkIn(USER_A, minutes = 25, bookId = null, atPage = 0)
        assertThat(r.todayMinutes).isEqualTo(25)
        assertThat(r.streakDays).isAtLeast(1)
        // 首次打卡至少 +5 金币（按实现规则）
        assertThat(r.coinAward).isAtLeast(5)
    }

    @Test
    fun secondCheckInSameDay_minutesAccumulate_butNoCoinAgain(): Unit = runBlocking {
        val first = repo.checkIn(USER_A, 10)
        val second = repo.checkIn(USER_A, 20)
        assertThat(first.coinAward).isAtLeast(5)
        assertThat(second.todayMinutes).isEqualTo(30)
        assertThat(second.coinAward).isEqualTo(0)  // 同日第二次不再发金币
    }

    /* ───────────── 多用户隔离 ───────────── */

    @Test
    fun checkIn_isolated_betweenUsers(): Unit = runBlocking {
        val a = repo.checkIn(USER_A, 15)
        val b = repo.checkIn(USER_B, 88)
        assertThat(a.todayMinutes).isEqualTo(15)
        assertThat(b.todayMinutes).isEqualTo(88)
        // 双方都属于"今天首次打卡"
        assertThat(a.coinAward).isAtLeast(5)
        assertThat(b.coinAward).isAtLeast(5)
    }

    /* ───────────── 阅读心电图 ───────────── */

    @Test
    fun loadBookEcg_aggregatesByPage_byProportion(): Unit = runBlocking {
        val bId = db.bookDao().insert(Book(userId = USER_A, title = "ECG"))
        // 三次打卡，分别在 page 10 / 50 / 100；总分钟 = 5+30+10 = 45
        repo.checkIn(USER_A, 5, bookId = bId, atPage = 10)
        repo.checkIn(USER_A, 30, bookId = bId, atPage = 50)
        repo.checkIn(USER_A, 10, bookId = bId, atPage = 100)

        val pts = repo.loadBookEcg(USER_A, bId)
        assertThat(pts).hasSize(3)

        // weight 是占比 mins/total，必须在 [0,1]，sum ≈ 1
        pts.forEach {
            assertThat(it.weight).isAtLeast(0f)
            assertThat(it.weight).isAtMost(1f)
        }
        val sum = pts.sumOf { it.weight.toDouble() }.toFloat()
        assertThat(sum).isWithin(0.0001f).of(1f)

        // page=50 的桶应当是占比最大那个（30/45 ≈ 0.667）
        val byPage = pts.associateBy { it.page }
        assertThat(byPage[50]!!.weight).isWithin(0.0001f).of(30f / 45f)
        assertThat(byPage[10]!!.weight).isWithin(0.0001f).of(5f / 45f)
        assertThat(byPage[100]!!.weight).isWithin(0.0001f).of(10f / 45f)

        // 必须按 page 升序
        for (i in 1 until pts.size) {
            assertThat(pts[i].page).isAtLeast(pts[i - 1].page)
        }
    }

    @Test
    fun ecg_includesQuoteDensity_withMixedFactors(): Unit = runBlocking {
        // 🆕 v53.2 双因子：weight = 0.5 * timeShare + 0.5 * quoteShare
        // 设计：page 10 阅读但无摘抄；page 50 仅有摘抄无打卡；page 100 两者都有。
        val bId = db.bookDao().insert(Book(userId = USER_A, title = "MixedECG"))
        repo.checkIn(USER_A, 5,  bookId = bId, atPage = 10)
        repo.checkIn(USER_A, 15, bookId = bId, atPage = 100)
        // 在 page 50 加 3 条摘抄；page 100 加 1 条
        repeat(3) { db.quoteDao().insert(
            com.example.funlife.data.model.Quote(userId = USER_A, bookId = bId, text = "q-50-$it", page = 50)
        ) }
        db.quoteDao().insert(
            com.example.funlife.data.model.Quote(userId = USER_A, bookId = bId, text = "q-100", page = 100)
        )

        val pts = repo.loadBookEcg(USER_A, bId)
        // 必须含三个 page 的并集
        val byPage = pts.associateBy { it.page }
        assertThat(byPage.keys).containsExactly(10, 50, 100)

        // 总分钟 = 5 + 15 = 20；总摘抄 = 4
        // page 10:  0.5*(5/20)  + 0.5*(0/4)  = 0.125
        // page 50:  0.5*(0/20)  + 0.5*(3/4)  = 0.375
        // page 100: 0.5*(15/20) + 0.5*(1/4)  = 0.5
        assertThat(byPage[10]!!.weight).isWithin(0.0001f).of(0.125f)
        assertThat(byPage[50]!!.weight).isWithin(0.0001f).of(0.375f)
        assertThat(byPage[100]!!.weight).isWithin(0.0001f).of(0.5f)

        // sum 必须 ≈ 1
        val sum = pts.sumOf { it.weight.toDouble() }.toFloat()
        assertThat(sum).isWithin(0.0001f).of(1f)
    }

    @Test
    fun ecg_onlyQuotes_noSessions(): Unit = runBlocking {
        // 完全没打过卡，但摘抄密集 → 应当退化为 100% 摘抄占比
        val bId = db.bookDao().insert(Book(userId = USER_A, title = "QuoteOnly"))
        db.quoteDao().insert(
            com.example.funlife.data.model.Quote(userId = USER_A, bookId = bId, text = "q1", page = 30)
        )
        db.quoteDao().insert(
            com.example.funlife.data.model.Quote(userId = USER_A, bookId = bId, text = "q2", page = 90)
        )
        val pts = repo.loadBookEcg(USER_A, bId)
        assertThat(pts).hasSize(2)
        // 每条占比 0.5
        pts.forEach { assertThat(it.weight).isWithin(0.0001f).of(0.5f) }
    }

    @Test
    fun ecg_emptyForBookWithoutSessions(): Unit = runBlocking {
        val bId = db.bookDao().insert(Book(userId = USER_A, title = "Empty"))
        val pts = repo.loadBookEcg(USER_A, bId)
        assertThat(pts).isEmpty()
    }

    /* ───────────── 月度曲线 ───────────── */

    @Test
    fun loadDailyMinutes_includesTodaysCheckIn(): Unit = runBlocking {
        repo.checkIn(USER_A, 30)
        val curve = repo.loadDailyMinutes(USER_A, 30)
        assertThat(curve).isNotEmpty()
        // 必须能找到一项 minutes >= 30（今天）
        assertThat(curve.maxOf { it.minutes }).isAtLeast(30)
    }
}
