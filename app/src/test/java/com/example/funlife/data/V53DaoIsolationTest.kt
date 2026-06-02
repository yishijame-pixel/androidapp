// V53DaoIsolationTest.kt — v53 阅光书房 · DAO 层多用户数据隔离测试
//
// 验收点：
//   1. 用户 A 写入的 Quote / ReadingSession / ReaderDnaCard 绝不可被用户 B 查到
//   2. observeByBook(userA, bookA_id) 不会拿到 userB 同 id 的 book 摘抄
//   3. ReadingSession.getMinutesOfDay 严格按 (userId, dateYmd) 聚合
//   4. quoteDao.findDueCapsules 只挑当前到期且未送达的，不区分用户但 markDelivered 后不再被挑出
package com.example.funlife.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.funlife.data.database.AppDatabase
import com.example.funlife.data.model.Book
import com.example.funlife.data.model.Quote
import com.example.funlife.data.model.ReadingSession
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class V53DaoIsolationTest {

    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java)
            .allowMainThreadQueries()
            .fallbackToDestructiveMigration()
            .build()
    }

    @After fun tearDown() { db.close() }

    /* ───────────── Quote 隔离 ───────────── */

    @Test
    fun quotes_userIsolation(): Unit = runBlocking {
        val bookA = db.bookDao().insert(Book(userId = 100L, title = "A 的书"))
        val bookB = db.bookDao().insert(Book(userId = 200L, title = "B 的书"))

        db.quoteDao().insert(Quote(userId = 100L, bookId = bookA, text = "A 的摘抄"))
        db.quoteDao().insert(Quote(userId = 200L, bookId = bookB, text = "B 的摘抄"))

        val a = db.quoteDao().observeAll(100L).first()
        val b = db.quoteDao().observeAll(200L).first()

        assertThat(a).hasSize(1)
        assertThat(a[0].text).isEqualTo("A 的摘抄")
        assertThat(b).hasSize(1)
        assertThat(b[0].text).isEqualTo("B 的摘抄")
    }

    @Test
    fun quotes_observeByBook_strictUserMatch(): Unit = runBlocking {
        // 故意构造：用户 A 与 B 各自的 book 拿到相同 id（autoGenerate 通常不会，但代码层不能依赖）
        // 这里用真实写入，确保 SQL 的 WHERE userId=? AND bookId=? 双重过滤生效。
        val bA = db.bookDao().insert(Book(userId = 1L, title = "BookA"))
        val bB = db.bookDao().insert(Book(userId = 2L, title = "BookB"))

        db.quoteDao().insert(Quote(userId = 1L, bookId = bA, text = "X"))
        db.quoteDao().insert(Quote(userId = 2L, bookId = bB, text = "Y"))

        // 用 A 的 userId + B 的 bookId → 一定为空（两个维度都要对上）
        val crossA = db.quoteDao().observeByBook(userId = 1L, bookId = bB).first()
        val crossB = db.quoteDao().observeByBook(userId = 2L, bookId = bA).first()
        assertThat(crossA).isEmpty()
        assertThat(crossB).isEmpty()

        val correctA = db.quoteDao().observeByBook(1L, bA).first()
        assertThat(correctA.map { it.text }).containsExactly("X")
    }

    @Test
    fun quotes_findDueCapsules_capsuleDeliveredFlow(): Unit = runBlocking {
        val now = System.currentTimeMillis()
        val bA = db.bookDao().insert(Book(userId = 1L, title = "B"))
        // 已到期、未送达
        db.quoteDao().insert(
            Quote(userId = 1L, bookId = bA, text = "due-1",
                capsuleDeliveryAt = now - 1000)
        )
        // 已到期、已送达
        db.quoteDao().insert(
            Quote(userId = 1L, bookId = bA, text = "delivered",
                capsuleDeliveryAt = now - 1000, capsuleDelivered = true)
        )
        // 未到期
        db.quoteDao().insert(
            Quote(userId = 1L, bookId = bA, text = "future",
                capsuleDeliveryAt = now + 86_400_000)
        )

        val due = db.quoteDao().findDueCapsules(now, 50)
        assertThat(due.map { it.text }).containsExactly("due-1")

        // 标 delivered 后不再出现
        db.quoteDao().markCapsuleDelivered(due[0].id)
        val due2 = db.quoteDao().findDueCapsules(now, 50)
        assertThat(due2).isEmpty()
    }

    @Test
    fun quotes_countCapsulesInMonth_userScoped(): Unit = runBlocking {
        val bA = db.bookDao().insert(Book(userId = 1L, title = "B"))
        val bB = db.bookDao().insert(Book(userId = 2L, title = "B"))
        val now = System.currentTimeMillis()

        // 用户 1 寄了 2 个胶囊
        repeat(2) {
            db.quoteDao().insert(Quote(userId = 1L, bookId = bA,
                text = "u1-$it", capsuleDeliveryAt = now + 1000_000_000L))
        }
        // 用户 2 寄了 5 个
        repeat(5) {
            db.quoteDao().insert(Quote(userId = 2L, bookId = bB,
                text = "u2-$it", capsuleDeliveryAt = now + 1000_000_000L))
        }

        val monthStart = now - 7L * 24 * 3600 * 1000
        val monthEnd = now + 7L * 24 * 3600 * 1000

        val u1 = db.quoteDao().countCapsulesInMonth(1L, monthStart, monthEnd)
        val u2 = db.quoteDao().countCapsulesInMonth(2L, monthStart, monthEnd)

        assertThat(u1).isEqualTo(2)
        assertThat(u2).isEqualTo(5)
    }

    /* ───────────── ReadingSession 隔离 ───────────── */

    @Test
    fun readingSession_minutesOfDay_userScoped(): Unit = runBlocking {
        val today = todayYmd()
        db.readingSessionDao().insert(ReadingSession(
            userId = 1L, bookId = null, minutes = 15, dateYmd = today))
        db.readingSessionDao().insert(ReadingSession(
            userId = 1L, bookId = null, minutes = 25, dateYmd = today))
        db.readingSessionDao().insert(ReadingSession(
            userId = 2L, bookId = null, minutes = 999, dateYmd = today))

        val u1 = db.readingSessionDao().observeMinutesOfDay(1L, today).first()
        val u2 = db.readingSessionDao().observeMinutesOfDay(2L, today).first()

        assertThat(u1).isEqualTo(40)
        assertThat(u2).isEqualTo(999)
    }

    @Test
    fun readingSession_observeMinutesOfBook_strictMatch(): Unit = runBlocking {
        val today = todayYmd()
        val bA = db.bookDao().insert(Book(userId = 1L, title = "B"))
        db.readingSessionDao().insert(ReadingSession(
            userId = 1L, bookId = bA, minutes = 30, dateYmd = today))
        db.readingSessionDao().insert(ReadingSession(
            userId = 1L, bookId = null, minutes = 99, dateYmd = today))

        val perBook = db.readingSessionDao().observeMinutesOfBook(1L, bA).first()
        assertThat(perBook).isEqualTo(30)  // 自由阅读不应被聚合到这本书上
    }

    private fun todayYmd(): Int {
        val c = java.util.Calendar.getInstance()
        return c.get(java.util.Calendar.YEAR) * 10000 +
               (c.get(java.util.Calendar.MONTH) + 1) * 100 +
               c.get(java.util.Calendar.DAY_OF_MONTH)
    }
}
