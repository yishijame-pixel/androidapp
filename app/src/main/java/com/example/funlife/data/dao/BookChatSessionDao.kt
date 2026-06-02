// BookChatSessionDao.kt — v54 阅光书房 · AI 长对话存档 DAO
package com.example.funlife.data.dao

import androidx.room.*
import com.example.funlife.data.model.BookChatSession
import kotlinx.coroutines.flow.Flow

@Dao
interface BookChatSessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: BookChatSession): Long

    @Update
    suspend fun update(session: BookChatSession)

    @Delete
    suspend fun delete(session: BookChatSession)

    @Query("SELECT * FROM book_chat_sessions WHERE userId = :userId AND id = :id LIMIT 1")
    suspend fun getById(userId: Long, id: Long): BookChatSession?

    /** 该用户全部对话档案（按最近一次消息时间倒序） */
    @Query("""
        SELECT * FROM book_chat_sessions
        WHERE userId = :userId
        ORDER BY lastMessageAt DESC
    """)
    fun observeAll(userId: Long): Flow<List<BookChatSession>>

    /** 单本书的对话档案（详情页"读书档案"卡片）*/
    @Query("""
        SELECT * FROM book_chat_sessions
        WHERE userId = :userId AND bookId = :bookId
        ORDER BY lastMessageAt DESC
    """)
    fun observeByBook(userId: Long, bookId: Long): Flow<List<BookChatSession>>

    @Query("""
        SELECT * FROM book_chat_sessions
        WHERE userId = :userId AND bookId = :bookId
        ORDER BY lastMessageAt DESC
        LIMIT 1
    """)
    suspend fun latestForBook(userId: Long, bookId: Long): BookChatSession?

    @Query("SELECT COUNT(*) FROM book_chat_sessions WHERE userId = :userId")
    fun countAll(userId: Long): Flow<Int>
}
