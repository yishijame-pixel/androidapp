// LetterDao.kt — 时光信箱：信件 DAO
package com.example.funlife.data.dao

import androidx.room.*
import com.example.funlife.data.model.Letter
import com.example.funlife.data.model.LetterDirection
import com.example.funlife.data.model.LetterStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface LetterDao {

    // ───── 信箱总览 ─────

    /** 全部信件（按 deliveryAt 倒序，已送达在前；未送达按 deliveryAt 升序在末尾） */
    @Query("""
        SELECT * FROM letters
        WHERE userId = :userId
        ORDER BY 
            CASE WHEN deliveredAt IS NULL THEN 1 ELSE 0 END ASC,
            deliveredAt DESC,
            deliveryAt ASC
    """)
    fun getAll(userId: Long): Flow<List<Letter>>

    /** 按收信人过滤（用于"我和爷爷的所有信"页） */
    @Query("""
        SELECT * FROM letters
        WHERE userId = :userId AND recipientId = :recipientId
        ORDER BY 
            CASE WHEN deliveredAt IS NULL THEN 1 ELSE 0 END ASC,
            deliveredAt DESC,
            deliveryAt ASC
    """)
    fun getByRecipient(userId: Long, recipientId: Long): Flow<List<Letter>>

    @Query("SELECT * FROM letters WHERE userId = :userId AND id = :id LIMIT 1")
    suspend fun getById(userId: Long, id: Long): Letter?

    /** 统计当前月用户已写的"to_recipient"信件数（用于 VIP 配额校验） */
    @Query("""
        SELECT COUNT(*) FROM letters
        WHERE userId = :userId AND direction = '${LetterDirection.TO_RECIPIENT}'
          AND sentAt >= :monthStart AND sentAt < :monthEnd
    """)
    suspend fun countSentInMonth(userId: Long, monthStart: Long, monthEnd: Long): Int

    // ───── Worker 用：批量取 due letters ─────

    /**
     * 取所有已到投递时间但还没生成回信的"用户信件"（待 AI 处理）。
     * 条件：direction=to_recipient AND deliveryAt <= now AND 没有对应的 from_recipient。
     */
    @Query("""
        SELECT * FROM letters
        WHERE direction = '${LetterDirection.TO_RECIPIENT}'
          AND deliveryAt <= :now
          AND id NOT IN (
              SELECT IFNULL(parentLetterId, -1) FROM letters
              WHERE direction = '${LetterDirection.FROM_RECIPIENT}' AND parentLetterId IS NOT NULL
          )
        ORDER BY deliveryAt ASC
        LIMIT :limit
    """)
    suspend fun getDueOutgoingLetters(now: Long, limit: Int = 20): List<Letter>

    /**
     * 取所有"已生成但还在路上的回信"（pending，且 deliveryAt 已到）。
     * 用于"延时投递"——AI 提前生成内容，但要等到投递时间才推通知 + 设 deliveredAt。
     */
    @Query("""
        SELECT * FROM letters
        WHERE direction = '${LetterDirection.FROM_RECIPIENT}'
          AND status = '${LetterStatus.PENDING}'
          AND deliveryAt <= :now
        ORDER BY deliveryAt ASC
        LIMIT :limit
    """)
    suspend fun getDueIncomingReplies(now: Long, limit: Int = 20): List<Letter>

    @Query("SELECT COUNT(*) FROM letters WHERE userId = :userId AND deliveredAt IS NOT NULL AND isRead = 0")
    fun unreadCount(userId: Long): Flow<Int>

    // ───── 写入 ─────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(letter: Letter): Long

    @Update
    suspend fun update(letter: Letter)

    @Delete
    suspend fun delete(letter: Letter)

    @Query("UPDATE letters SET isRead = 1, updatedAt = :now WHERE userId = :userId AND id = :id")
    suspend fun markRead(userId: Long, id: Long, now: Long = System.currentTimeMillis())

    @Query("DELETE FROM letters WHERE userId = :userId AND recipientId = :recipientId")
    suspend fun deleteAllByRecipient(userId: Long, recipientId: Long)
}
