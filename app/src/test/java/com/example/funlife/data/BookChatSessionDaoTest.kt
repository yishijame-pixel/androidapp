// BookChatSessionDaoTest.kt — v54 阅光书房 · 长对话存档 DAO 数据隔离测试
//
// 验收：
//   1. 用户 A 写入的 session 在 user B 视角下完全不可见
//   2. observeByBook 严格按 (userId, bookId) 双过滤
//   3. update 不能跨 userId 修改（getById WHERE userId=? 兜底）
//   4. lastMessageAt 决定排序：最近的对话在前
package com.example.funlife.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.funlife.data.database.AppDatabase
import com.example.funlife.data.model.Book
import com.example.funlife.data.model.BookChatSession
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
class BookChatSessionDaoTest {

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

    @Test
    fun sessions_isolated_betweenUsers(): Unit = runBlocking {
        val bookA = db.bookDao().insert(Book(userId = 1L, title = "A 的书"))
        val bookB = db.bookDao().insert(Book(userId = 2L, title = "B 的书"))

        db.bookChatSessionDao().insert(BookChatSession(
            userId = 1L, bookId = bookA, title = "A 的对话",
            messagesJson = "[]", turnCount = 1
        ))
        db.bookChatSessionDao().insert(BookChatSession(
            userId = 2L, bookId = bookB, title = "B 的对话",
            messagesJson = "[]", turnCount = 5
        ))

        val a = db.bookChatSessionDao().observeAll(1L).first()
        val b = db.bookChatSessionDao().observeAll(2L).first()

        assertThat(a).hasSize(1)
        assertThat(a[0].title).isEqualTo("A 的对话")
        assertThat(b).hasSize(1)
        assertThat(b[0].title).isEqualTo("B 的对话")
    }

    @Test
    fun observeByBook_strictDoubleFilter(): Unit = runBlocking {
        val bA = db.bookDao().insert(Book(userId = 1L, title = "BookA"))
        val bB = db.bookDao().insert(Book(userId = 2L, title = "BookB"))

        db.bookChatSessionDao().insert(BookChatSession(
            userId = 1L, bookId = bA, messagesJson = "[]"
        ))
        db.bookChatSessionDao().insert(BookChatSession(
            userId = 2L, bookId = bB, messagesJson = "[]"
        ))

        // 跨用户查询应当返回空（即使 bookId 数字相同）
        val cross = db.bookChatSessionDao().observeByBook(userId = 1L, bookId = bB).first()
        assertThat(cross).isEmpty()

        val correct = db.bookChatSessionDao().observeByBook(userId = 1L, bookId = bA).first()
        assertThat(correct).hasSize(1)
    }

    @Test
    fun getById_doesNotLeakAcrossUsers(): Unit = runBlocking {
        val bA = db.bookDao().insert(Book(userId = 1L, title = "B"))
        val sid = db.bookChatSessionDao().insert(BookChatSession(
            userId = 1L, bookId = bA, messagesJson = "[]"
        ))

        // 同 id 但 userId 不对 → 必须返回 null（DAO 自带 userId 过滤）
        val asUser2 = db.bookChatSessionDao().getById(userId = 2L, id = sid)
        assertThat(asUser2).isNull()

        val asUser1 = db.bookChatSessionDao().getById(userId = 1L, id = sid)
        assertThat(asUser1).isNotNull()
    }

    @Test
    fun observeAll_orderedByLastMessageAtDesc(): Unit = runBlocking {
        val bA = db.bookDao().insert(Book(userId = 1L, title = "B"))
        val now = System.currentTimeMillis()
        // 故意乱序插入
        db.bookChatSessionDao().insert(BookChatSession(
            userId = 1L, bookId = bA, title = "old", messagesJson = "[]",
            createdAt = now - 1000, lastMessageAt = now - 1000
        ))
        db.bookChatSessionDao().insert(BookChatSession(
            userId = 1L, bookId = bA, title = "newest", messagesJson = "[]",
            createdAt = now, lastMessageAt = now
        ))
        db.bookChatSessionDao().insert(BookChatSession(
            userId = 1L, bookId = bA, title = "middle", messagesJson = "[]",
            createdAt = now - 500, lastMessageAt = now - 500
        ))

        val list = db.bookChatSessionDao().observeAll(1L).first()
        assertThat(list.map { it.title }).containsExactly("newest", "middle", "old").inOrder()
    }

    @Test
    fun latestForBook_returnsMostRecent(): Unit = runBlocking {
        val bA = db.bookDao().insert(Book(userId = 1L, title = "B"))
        val now = System.currentTimeMillis()
        db.bookChatSessionDao().insert(BookChatSession(
            userId = 1L, bookId = bA, title = "x", messagesJson = "[]",
            lastMessageAt = now - 1000
        ))
        db.bookChatSessionDao().insert(BookChatSession(
            userId = 1L, bookId = bA, title = "y", messagesJson = "[]",
            lastMessageAt = now
        ))
        val latest = db.bookChatSessionDao().latestForBook(1L, bA)
        assertThat(latest?.title).isEqualTo("y")
    }

    @Test
    fun update_preservesUserId(): Unit = runBlocking {
        val bA = db.bookDao().insert(Book(userId = 1L, title = "B"))
        val sid = db.bookChatSessionDao().insert(BookChatSession(
            userId = 1L, bookId = bA, title = "orig", messagesJson = "[]"
        ))
        val cur = db.bookChatSessionDao().getById(1L, sid)!!
        // 改 title + 添加内容
        db.bookChatSessionDao().update(cur.copy(
            title = "updated", messagesJson = """[{"role":"user","text":"hi","ts":1}]"""
        ))
        val after = db.bookChatSessionDao().getById(1L, sid)!!
        assertThat(after.title).isEqualTo("updated")
        assertThat(after.userId).isEqualTo(1L)
        assertThat(after.bookId).isEqualTo(bA)
    }
}
