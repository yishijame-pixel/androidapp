// BookChatSessionConcurrencyTest.kt — v54 并发竞态模拟测试
//
// 场景：模拟用户在 UI 不防抖时连点 send，并发触发 persistSession。
// 期望：即便多个协程同时写 session，最终只会创建 **1 条** session（互斥锁保证）。
//
// 注：直接 unit-test ViewModel 的 send 需要 mock 整个云端调用栈成本高。
// 这里抽象成"并发 insert OR update"语义验证 DAO + Mutex 在仓库层的正确性。
package com.example.funlife.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.funlife.data.database.AppDatabase
import com.example.funlife.data.model.Book
import com.example.funlife.data.model.BookChatSession
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class BookChatSessionConcurrencyTest {

    private lateinit var db: AppDatabase

    @Before fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java)
            .allowMainThreadQueries()
            .fallbackToDestructiveMigration()
            .build()
    }

    @After fun tearDown() { db.close() }

    /** 复制 BookChatViewModel.persistSession 的核心语义为一个独立测试装置。 */
    private class FakeChatPersistence(private val db: AppDatabase, val userId: Long, val bookId: Long) {
        @Volatile private var currentSessionId: Long = 0L
        private val mutex = Mutex()

        suspend fun persist(turn: Int) {
            mutex.withLock {
                if (currentSessionId > 0L) {
                    val cur = db.bookChatSessionDao().getById(userId, currentSessionId) ?: return@withLock
                    db.bookChatSessionDao().update(cur.copy(turnCount = turn, lastMessageAt = System.currentTimeMillis()))
                } else {
                    currentSessionId = db.bookChatSessionDao().insert(
                        BookChatSession(
                            userId = userId, bookId = bookId,
                            title = "concurrent-$turn", messagesJson = "[]",
                            turnCount = turn,
                        )
                    )
                }
            }
        }
    }

    @Test
    fun concurrentPersist_createsOnlyOneSession(): Unit = runBlocking {
        val bA = db.bookDao().insert(Book(userId = 1L, title = "B"))
        val persist = FakeChatPersistence(db, userId = 1L, bookId = bA)

        // 模拟 20 个并发"send"同时触发持久化
        val jobs = (1..20).map { i ->
            async { persist.persist(i) }
        }
        jobs.awaitAll()

        val all = db.bookChatSessionDao().observeAll(1L).first()
        assertThat(all).hasSize(1)
        // turnCount 应当 = 最后一次写入的值（可以是 1..20 中的任意，关键是只创建了 1 条）
        assertThat(all[0].turnCount).isAtLeast(1)
        assertThat(all[0].turnCount).isAtMost(20)
    }

    @Test
    fun concurrentPersist_acrossUsers_oneSessionPerUser(): Unit = runBlocking {
        val bA = db.bookDao().insert(Book(userId = 1L, title = "B1"))
        val bB = db.bookDao().insert(Book(userId = 2L, title = "B2"))
        val pA = FakeChatPersistence(db, userId = 1L, bookId = bA)
        val pB = FakeChatPersistence(db, userId = 2L, bookId = bB)

        val jobs = mutableListOf<kotlinx.coroutines.Deferred<Unit>>()
        repeat(15) { i -> jobs += async { pA.persist(i + 1) } }
        repeat(15) { i -> jobs += async { pB.persist(i + 1) } }
        jobs.awaitAll()

        val u1 = db.bookChatSessionDao().observeAll(1L).first()
        val u2 = db.bookChatSessionDao().observeAll(2L).first()
        assertThat(u1).hasSize(1)
        assertThat(u2).hasSize(1)
        // 多用户的 session 严格隔离
        assertThat(u1[0].userId).isEqualTo(1L)
        assertThat(u2[0].userId).isEqualTo(2L)
    }
}
