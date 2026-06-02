// QuoteDao.kt — v53 阅光书房 · 摘抄 + 时光胶囊
package com.example.funlife.data.dao

import androidx.room.*
import com.example.funlife.data.model.Quote
import kotlinx.coroutines.flow.Flow

@Dao
interface QuoteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(quote: Quote): Long

    @Update
    suspend fun update(quote: Quote)

    @Delete
    suspend fun delete(quote: Quote)

    @Query("SELECT * FROM quotes WHERE userId = :userId AND id = :id LIMIT 1")
    suspend fun getById(userId: Long, id: Long): Quote?

    @Query("SELECT * FROM quotes WHERE userId = :userId ORDER BY createdAt DESC")
    fun observeAll(userId: Long): Flow<List<Quote>>

    @Query("SELECT * FROM quotes WHERE userId = :userId AND bookId = :bookId ORDER BY createdAt DESC")
    fun observeByBook(userId: Long, bookId: Long): Flow<List<Quote>>

    @Query("SELECT COUNT(*) FROM quotes WHERE userId = :userId AND bookId = :bookId")
    fun countByBook(userId: Long, bookId: Long): Flow<Int>

    /** 单本书最近 N 条摘抄（一次性拿，AI 上下文用） */
    @Query("""
        SELECT * FROM quotes
        WHERE userId = :userId AND bookId = :bookId
        ORDER BY createdAt DESC LIMIT :limit
    """)
    suspend fun getRecentByBook(userId: Long, bookId: Long, limit: Int): List<Quote>

    /** 时光胶囊：本月已使用条数（capsuleDeliveryAt > 0 视为已寄） */
    @Query("""
        SELECT COUNT(*) FROM quotes
        WHERE userId = :userId
          AND capsuleDeliveryAt > 0
          AND createdAt >= :monthStart AND createdAt < :monthEnd
    """)
    suspend fun countCapsulesInMonth(userId: Long, monthStart: Long, monthEnd: Long): Int

    /** 找到所有到期但未送达的胶囊（投递 Worker 用） */
    @Query("""
        SELECT * FROM quotes
        WHERE capsuleDeliveryAt > 0
          AND capsuleDelivered = 0
          AND capsuleDeliveryAt <= :nowMs
        ORDER BY capsuleDeliveryAt ASC LIMIT :limit
    """)
    suspend fun findDueCapsules(nowMs: Long, limit: Int = 50): List<Quote>

    @Query("UPDATE quotes SET capsuleDelivered = 1 WHERE id = :id")
    suspend fun markCapsuleDelivered(id: Long)

    @Query("UPDATE quotes SET publishedToGalaxy = 1 WHERE userId = :userId AND id = :id")
    suspend fun markPublishedToGalaxy(userId: Long, id: Long)

    /** 心情低谷召回：抽 1 条优质 quote */
    @Query("""
        SELECT * FROM quotes
        WHERE userId = :userId
          AND (rating >= 4 OR pinned = 1)
        ORDER BY RANDOM() LIMIT 1
    """)
    suspend fun pickRandomQuality(userId: Long): Quote?

    /** 昨日新增 quote（晨光信使用） */
    @Query("""
        SELECT * FROM quotes
        WHERE userId = :userId
          AND createdAt >= :startMs AND createdAt < :endMs
        ORDER BY RANDOM() LIMIT 1
    """)
    suspend fun pickRandomCreatedBetween(userId: Long, startMs: Long, endMs: Long): Quote?

    /** 全表去重 userId（Worker 跨用户扫描用，仅本地无网络隐私风险） */
    @Query("SELECT DISTINCT userId FROM quotes")
    suspend fun distinctUserIds(): List<Long>

    /** 即将到期的胶囊（晨光信使预告用，未来 7 天） */
    @Query("""
        SELECT * FROM quotes
        WHERE userId = :userId
          AND capsuleDeliveryAt > :nowMs AND capsuleDeliveryAt <= :nowMs + :windowMs
          AND capsuleDelivered = 0
        ORDER BY capsuleDeliveryAt ASC LIMIT 1
    """)
    suspend fun pickUpcomingCapsule(userId: Long, nowMs: Long, windowMs: Long): Quote?
}
