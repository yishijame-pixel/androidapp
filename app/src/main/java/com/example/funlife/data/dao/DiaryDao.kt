// DiaryDao.kt — 日记本数据访问（DB v56：按皮肤分册 + 页码槽位）
package com.example.funlife.data.dao

import androidx.room.*
import com.example.funlife.data.model.DiaryEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface DiaryDao {

    /** 指定皮肤册全部日记，按页码正序 */
    @Query(
        "SELECT * FROM diary_entries WHERE userId = :userId AND bookSkinId = :bookSkinId ORDER BY pageSlot ASC"
    )
    fun observeByBook(userId: Long, bookSkinId: String): Flow<List<DiaryEntry>>

    @Query(
        "SELECT * FROM diary_entries WHERE userId = :userId AND bookSkinId = :bookSkinId AND pageSlot = :pageSlot LIMIT 1"
    )
    suspend fun getByPageSlot(userId: Long, bookSkinId: String, pageSlot: Int): DiaryEntry?

    @Query("SELECT * FROM diary_entries WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): DiaryEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: DiaryEntry): Long

    @Update
    suspend fun update(entry: DiaryEntry)

    @Delete
    suspend fun delete(entry: DiaryEntry)

    @Query("SELECT COUNT(*) FROM diary_entries WHERE userId = :userId AND bookSkinId = :bookSkinId")
    suspend fun countByBook(userId: Long, bookSkinId: String): Int

    /** 按月份索引：返回某册某月的所有日记 */
    @Query(
        "SELECT * FROM diary_entries WHERE userId = :userId AND bookSkinId = :bookSkinId AND date LIKE :ymPrefix || '%' ORDER BY pageSlot ASC"
    )
    suspend fun getByMonth(userId: Long, bookSkinId: String, ymPrefix: String): List<DiaryEntry>

    @Query(
        "SELECT DISTINCT substr(date, 1, 7) AS ym FROM diary_entries WHERE userId = :userId AND bookSkinId = :bookSkinId ORDER BY ym DESC"
    )
    suspend fun getDistinctMonths(userId: Long, bookSkinId: String): List<String>
}
