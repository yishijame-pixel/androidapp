// DiaryRepository.kt — 日记本仓库
package com.example.funlife.repository

import com.example.funlife.data.dao.DiaryDao
import com.example.funlife.data.model.DiaryEntry
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class DiaryRepository(private val dao: DiaryDao) {

    fun observeAll(userId: Long): Flow<List<DiaryEntry>> = dao.getAllAsc(userId)

    suspend fun getByDate(userId: Long, date: LocalDate): DiaryEntry? =
        dao.getByDate(userId, date.toString())

    /**
     * Upsert by (userId, date)：若该日已有日记则更新，否则插入。
     * @return 新建/更新后的 id
     */
    suspend fun saveOrUpdate(entry: DiaryEntry): Long {
        val exist = dao.getByDate(entry.userId, entry.date)
        return if (exist == null) {
            dao.insert(entry.copy(createdAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis()))
        } else {
            val merged = exist.copy(
                title = entry.title,
                content = entry.content,
                weather = entry.weather ?: exist.weather,
                temperature = entry.temperature ?: exist.temperature,
                moodEmoji = entry.moodEmoji ?: exist.moodEmoji,
                location = entry.location ?: exist.location,
                bookmarked = entry.bookmarked,
                updatedAt = System.currentTimeMillis()
            )
            dao.update(merged)
            merged.id
        }
    }

    suspend fun delete(entry: DiaryEntry) = dao.delete(entry)

    suspend fun count(userId: Long): Int = dao.count(userId)

    suspend fun getByMonth(userId: Long, ymPrefix: String): List<DiaryEntry> =
        dao.getByMonth(userId, ymPrefix)

    suspend fun getDistinctMonths(userId: Long): List<String> =
        dao.getDistinctMonths(userId)
}
