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

    /**
     * 🆕 v51 VIP 配额：统计指定时间窗口内本用户收到的 AI 回复条数。
     * 用于聊天记账每日额度限制（VipQuota.chatAiDailyLimit）。
     * 仅统计 role='ai'，避免把 type=bill / system 的本地账单消息算入。
     */
    @Query("SELECT COUNT(*) FROM chat_messages WHERE userId = :userId AND role = 'ai' AND timestamp >= :startMs AND timestamp < :endMs")
    suspend fun countAiBetween(userId: Long, startMs: Long, endMs: Long): Int
}
