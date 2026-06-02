// DiaryDao.kt — 日记本数据访问（DB v55）
package com.example.funlife.data.dao

import androidx.room.*
import com.example.funlife.data.model.DiaryEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface DiaryDao {

    /** 全部日记倒序（最新一篇在前），用于翻页浏览 */
    @Query("SELECT * FROM diary_entries WHERE userId = :userId ORDER BY date DESC")
    fun getAllDesc(userId: Long): Flow<List<DiaryEntry>>

    /** 全部日记正序（最早一篇在前），用于像真书一样从头翻 */
    @Query("SELECT * FROM diary_entries WHERE userId = :userId ORDER BY date ASC")
    fun getAllAsc(userId: Long): Flow<List<DiaryEntry>>

    @Query("SELECT * FROM diary_entries WHERE userId = :userId AND date = :date LIMIT 1")
    suspend fun getByDate(userId: Long, date: String): DiaryEntry?

    @Query("SELECT * FROM diary_entries WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): DiaryEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: DiaryEntry): Long

    @Update
    suspend fun update(entry: DiaryEntry)

    @Delete
    suspend fun delete(entry: DiaryEntry)

    @Query("SELECT COUNT(*) FROM diary_entries WHERE userId = :userId")
    suspend fun count(userId: Long): Int

    /** 按月份索引：返回某月的所有日记（YYYY-MM 前缀匹配） */
    @Query("SELECT * FROM diary_entries WHERE userId = :userId AND date LIKE :ymPrefix || '%' ORDER BY date ASC")
    suspend fun getByMonth(userId: Long, ymPrefix: String): List<DiaryEntry>

    /** 所有有日记的月份（用于目录页） */
    @Query("SELECT DISTINCT substr(date, 1, 7) AS ym FROM diary_entries WHERE userId = :userId ORDER BY ym DESC")
    suspend fun getDistinctMonths(userId: Long): List<String>
}
