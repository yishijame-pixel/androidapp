// DiaryRepository.kt — 日记本仓库（按皮肤分册）
package com.example.funlife.repository

import com.example.funlife.data.dao.DiaryDao
import com.example.funlife.data.model.DiaryEntry
import kotlinx.coroutines.flow.Flow

class DiaryRepository(private val dao: DiaryDao) {

    fun observeByBook(userId: Long, bookSkinId: String): Flow<List<DiaryEntry>> =
        dao.observeByBook(userId, bookSkinId)

    /**
     * Upsert by (userId, bookSkinId, pageSlot)：同一页再次保存则更新。
     * @return 新建/更新后的 id
     */
    suspend fun saveOrUpdate(entry: DiaryEntry): Long {
        val exist = dao.getByPageSlot(entry.userId, entry.bookSkinId, entry.pageSlot)
        return if (exist == null) {
            dao.insert(
                entry.copy(
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                )
            )
        } else {
            val merged = exist.copy(
                date = entry.date,
                title = entry.title,
                content = entry.content,
                weather = entry.weather ?: exist.weather,
                temperature = entry.temperature ?: exist.temperature,
                moodEmoji = entry.moodEmoji ?: exist.moodEmoji,
                location = entry.location ?: exist.location,
                bookmarked = entry.bookmarked,
                updatedAt = System.currentTimeMillis(),
            )
            dao.update(merged)
            merged.id
        }
    }

    suspend fun delete(entry: DiaryEntry) = dao.delete(entry)

    suspend fun countByBook(userId: Long, bookSkinId: String): Int =
        dao.countByBook(userId, bookSkinId)

    suspend fun getByMonth(userId: Long, bookSkinId: String, ymPrefix: String): List<DiaryEntry> =
        dao.getByMonth(userId, bookSkinId, ymPrefix)

    suspend fun getDistinctMonths(userId: Long, bookSkinId: String): List<String> =
        dao.getDistinctMonths(userId, bookSkinId)
}
