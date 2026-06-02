// LetterRecipientDao.kt — 时光信箱：收信人 DAO
package com.example.funlife.data.dao

import androidx.room.*
import com.example.funlife.data.model.LetterRecipient
import kotlinx.coroutines.flow.Flow

@Dao
interface LetterRecipientDao {

    @Query("SELECT * FROM letter_recipients WHERE userId = :userId ORDER BY sortOrder ASC, createdAt DESC")
    fun getAll(userId: Long): Flow<List<LetterRecipient>>

    @Query("SELECT * FROM letter_recipients WHERE userId = :userId ORDER BY sortOrder ASC, createdAt DESC")
    suspend fun getAllOnce(userId: Long): List<LetterRecipient>

    @Query("SELECT * FROM letter_recipients WHERE userId = :userId AND id = :id LIMIT 1")
    suspend fun getById(userId: Long, id: Long): LetterRecipient?

    @Query("SELECT COUNT(*) FROM letter_recipients WHERE userId = :userId")
    suspend fun countOf(userId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(recipient: LetterRecipient): Long

    @Update
    suspend fun update(recipient: LetterRecipient)

    @Delete
    suspend fun delete(recipient: LetterRecipient)

    @Query("DELETE FROM letter_recipients WHERE userId = :userId AND id = :id")
    suspend fun deleteById(userId: Long, id: Long)
}
