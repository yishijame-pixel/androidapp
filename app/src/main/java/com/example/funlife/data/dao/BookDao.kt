// BookDao.kt — 方案 F · 人生书架
package com.example.funlife.data.dao

import androidx.room.*
import com.example.funlife.data.model.Book
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {

    @Query("SELECT * FROM books WHERE userId = :userId ORDER BY finishedAt DESC, updatedAt DESC")
    fun observeAll(userId: Long): Flow<List<Book>>

    @Query("SELECT * FROM books WHERE userId = :userId AND id = :id LIMIT 1")
    suspend fun getById(userId: Long, id: Long): Book?

    /** 当年所有读完的书，用于年鉴 */
    @Query("""
        SELECT * FROM books
        WHERE userId = :userId
          AND finishedAt >= :yearStart AND finishedAt < :yearEnd
        ORDER BY finishedAt ASC
    """)
    suspend fun getBooksOfYear(userId: Long, yearStart: Long, yearEnd: Long): List<Book>

    @Query("SELECT COUNT(*) FROM books WHERE userId = :userId")
    fun countAll(userId: Long): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(book: Book): Long

    @Update
    suspend fun update(book: Book)

    @Delete
    suspend fun delete(book: Book)

    @Query("DELETE FROM books WHERE userId = :userId AND id = :id")
    suspend fun deleteById(userId: Long, id: Long)
}
