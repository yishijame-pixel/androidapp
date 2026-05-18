package com.example.funlife.data.dao

import androidx.room.*
import com.example.funlife.data.model.ChatMessage
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatMessageDao {
    @Insert
    suspend fun insert(message: ChatMessage): Long

    @Delete
    suspend fun delete(message: ChatMessage)

    @Query("SELECT * FROM chat_messages WHERE userId = :userId ORDER BY timestamp DESC")
    fun getAllMessages(userId: Long): Flow<List<ChatMessage>>

    @Query("SELECT * FROM chat_messages WHERE userId = :userId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentMessages(userId: Long, limit: Int = 50): List<ChatMessage>

    @Query("DELETE FROM chat_messages WHERE userId = :userId")
    suspend fun clearAll(userId: Long)

    @Query("SELECT COUNT(*) FROM chat_messages WHERE userId = :userId")
    suspend fun getMessageCount(userId: Long): Int

    @Query("DELETE FROM chat_messages WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM chat_messages WHERE userId = :userId AND content LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchMessages(userId: Long, query: String): Flow<List<ChatMessage>>
}
