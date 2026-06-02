// QuoteRepositoryV53Test.kt — v53 阅光书房 · 摘抄/胶囊配额逻辑测试
//
// 验收点：
//   1. 普通用户胶囊配额 1 条/月 — 第 2 条返回 QuotaExceeded
//   2. VIP3 视为无限 — 连寄 50 条都不应触发 QuotaExceeded
//   3. 投递时间 < letterMinDelayMs → NeedsLongerDelay
//   4. text 为空 / 超长 → Invalid
//   5. 仅普通摘抄（capsuleDeliveryAt = 0）不消耗配额，可无限写
//   6. 多用户隔离：用户 A 的胶囊计数不影响用户 B
package com.example.funlife.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.funlife.data.database.AppDatabase
import com.example.funlife.data.model.Book
import com.example.funlife.data.model.Quote
import com.example.funlife.data.model.UserVip
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
class QuoteRepositoryV53Test {

    private lateinit var db: AppDatabase
    private lateinit var repo: QuoteRepository
    private val USER_NORMAL = 100L
    private val USER_VIP3 = 300L
    private val USER_OTHER = 999L
    private var bookId: Long = 0

    @Before
    fun setUp(): Unit = runBlocking {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java)
            .allowMainThreadQueries()
            .fallbackToDestructiveMigration()
            .build()
        repo = QuoteRepository(db.quoteDao(), db.userVipDao())

        // 注入用户的 VIP 等级
        db.userVipDao().insertOrUpdate(UserVip(userId = USER_NORMAL, vipLevel = 0))
        db.userVipDao().insertOrUpdate(UserVip(userId = USER_VIP3, vipLevel = 3))
        db.userVipDao().insertOrUpdate(UserVip(userId = USER_OTHER, vipLevel = 0))

        // 一本通用 book
        bookId = db.bookDao().insert(Book(userId = USER_NORMAL, title = "Test"))
        // 用户 OTHER 的书
        db.bookDao().insert(Book(userId = USER_OTHER, title = "Other"))
    }

    @After fun tearDown() { db.close() }

    private fun futureCapsule(daysAhead: Int) =
        System.currentTimeMillis() + daysAhead * 24L * 3600 * 1000

    /* ───────────── 基础校验 ───────────── */

    @Test
    fun emptyText_returnsInvalid(): Unit = runBlocking {
        val r = repo.save(Quote(userId = USER_NORMAL, bookId = bookId, text = "   "))
        assertThat(r).isInstanceOf(QuoteRepository.SaveResult.Invalid::class.java)
    }

    @Test
    fun overLongText_returnsInvalid(): Unit = runBlocking {
        val r = repo.save(Quote(
            userId = USER_NORMAL, bookId = bookId, text = "啊".repeat(501)
        ))
        assertThat(r).isInstanceOf(QuoteRepository.SaveResult.Invalid::class.java)
    }

    @Test
    fun normalQuote_doesNotConsumeQuota(): Unit = runBlocking {
        // 普通用户连写 100 条非胶囊摘抄都应成功
        repeat(100) { i ->
            val r = repo.save(Quote(
                userId = USER_NORMAL, bookId = bookId, text = "ordinary-$i"
            ))
            assertThat(r).isInstanceOf(QuoteRepository.SaveResult.Success::class.java)
        }
        // 此时再寄 1 条胶囊应当成功（配额未被普通摘抄占用）
        val cap = repo.save(Quote(
            userId = USER_NORMAL, bookId = bookId, text = "capsule",
            capsuleDeliveryAt = futureCapsule(30)
        ))
        assertThat(cap).isInstanceOf(QuoteRepository.SaveResult.Success::class.java)
    }

    /* ───────────── 普通用户配额 ───────────── */

    @Test
    fun normalUser_capsule_quotaIsOne(): Unit = runBlocking {
        val first = repo.save(Quote(
            userId = USER_NORMAL, bookId = bookId, text = "1st",
            capsuleDeliveryAt = futureCapsule(7)
        ))
        assertThat(first).isInstanceOf(QuoteRepository.SaveResult.Success::class.java)

        val second = repo.save(Quote(
            userId = USER_NORMAL, bookId = bookId, text = "2nd",
            capsuleDeliveryAt = futureCapsule(7)
        ))
        assertThat(second).isInstanceOf(QuoteRepository.SaveResult.QuotaExceeded::class.java)
        val q = second as QuoteRepository.SaveResult.QuotaExceeded
        assertThat(q.limit).isEqualTo(1)
        assertThat(q.used).isEqualTo(1)
    }

    /* ───────────── VIP3 无限 ───────────── */

    @Test
    fun vip3_capsule_unlimited(): Unit = runBlocking {
        val bId = db.bookDao().insert(Book(userId = USER_VIP3, title = "VIP"))
        repeat(50) { i ->
            val r = repo.save(Quote(
                userId = USER_VIP3, bookId = bId, text = "vip-$i",
                capsuleDeliveryAt = futureCapsule(7)
            ))
            assertThat(r).isInstanceOf(QuoteRepository.SaveResult.Success::class.java)
        }
    }

    /* ───────────── 最短延迟 ───────────── */

    @Test
    fun capsule_immediateDelivery_returnsNeedsLongerDelay(): Unit = runBlocking {
        val r = repo.save(Quote(
            userId = USER_NORMAL, bookId = bookId, text = "now",
            capsuleDeliveryAt = System.currentTimeMillis() // delta = 0
        ))
        assertThat(r).isInstanceOf(QuoteRepository.SaveResult.NeedsLongerDelay::class.java)
    }

    /* ───────────── 多用户隔离 ───────────── */

    @Test
    fun capsuleQuota_isolatedBetweenUsers(): Unit = runBlocking {
        // 用户 NORMAL 用掉自己的 1 条
        repo.save(Quote(userId = USER_NORMAL, bookId = bookId, text = "n",
            capsuleDeliveryAt = futureCapsule(7)))

        // 用户 OTHER 仍可以寄 1 条
        val otherBook = db.bookDao().getById(USER_OTHER, 2L)
            ?: db.bookDao().insert(Book(userId = USER_OTHER, title = "O")).let {
                db.bookDao().getById(USER_OTHER, it)!!
            }
        val r = repo.save(Quote(
            userId = USER_OTHER, bookId = otherBook.id, text = "o",
            capsuleDeliveryAt = futureCapsule(7)
        ))
        assertThat(r).isInstanceOf(QuoteRepository.SaveResult.Success::class.java)
    }
}
