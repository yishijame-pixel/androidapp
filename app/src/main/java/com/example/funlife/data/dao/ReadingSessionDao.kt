// ReadingSessionDao.kt — v53 阅光书房
package com.example.funlife.data.dao

import androidx.room.*
import com.example.funlife.data.model.ReadingSession
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadingSessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: ReadingSession): Long

    @Delete
    suspend fun delete(session: ReadingSession)

    /** 当天总分钟数（用于"今日已读 X 分钟" 展示） */
    @Query("SELECT IFNULL(SUM(minutes),0) FROM reading_sessions WHERE userId = :userId AND dateYmd = :dateYmd")
    fun observeMinutesOfDay(userId: Long, dateYmd: Int): Flow<Int>

    @Query("SELECT IFNULL(SUM(minutes),0) FROM reading_sessions WHERE userId = :userId AND dateYmd = :dateYmd")
    suspend fun minutesOfDay(userId: Long, dateYmd: Int): Int

    /** 最近 N 天，每天各自总分钟数（用于月度曲线） */
    @Query("""
        SELECT dateYmd AS dateYmd, SUM(minutes) AS minutes
        FROM reading_sessions
        WHERE userId = :userId AND dateYmd >= :sinceYmd
        GROUP BY dateYmd ORDER BY dateYmd ASC
    """)
    suspend fun dailyMinutesSince(userId: Long, sinceYmd: Int): List<DailyMinutes>

    /** 最近 N 天有打卡的去重日期集合（用于连续天数计算） */
    @Query("""
        SELECT DISTINCT dateYmd FROM reading_sessions
        WHERE userId = :userId AND dateYmd >= :sinceYmd
        ORDER BY dateYmd DESC
    """)
    suspend fun distinctDatesSince(userId: Long, sinceYmd: Int): List<Int>

    /** 单本书所有打卡记录（用于阅读心电图）*/
    @Query("""
        SELECT * FROM reading_sessions
        WHERE userId = :userId AND bookId = :bookId
        ORDER BY createdAt ASC
    """)
    suspend fun getByBook(userId: Long, bookId: Long): List<ReadingSession>

    /** 单本书累计阅读时长 */
    @Query("SELECT IFNULL(SUM(minutes),0) FROM reading_sessions WHERE userId = :userId AND bookId = :bookId")
    fun observeMinutesOfBook(userId: Long, bookId: Long): Flow<Int>

    @Query("SELECT * FROM reading_sessions WHERE userId = :userId ORDER BY createdAt DESC LIMIT :limit")
    fun observeRecent(userId: Long, limit: Int = 50): Flow<List<ReadingSession>>
}

data class DailyMinutes(val dateYmd: Int, val minutes: Int)
