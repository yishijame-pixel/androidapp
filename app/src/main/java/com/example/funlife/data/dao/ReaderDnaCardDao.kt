// ReaderDnaCardDao.kt — v53 阅光书房 · 读者 DNA
package com.example.funlife.data.dao

import androidx.room.*
import com.example.funlife.data.model.ReaderDnaCard
import kotlinx.coroutines.flow.Flow

@Dao
interface ReaderDnaCardDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(card: ReaderDnaCard): Long

    @Delete
    suspend fun delete(card: ReaderDnaCard)

    @Query("SELECT * FROM reader_dna_cards WHERE userId = :userId ORDER BY generatedAt DESC")
    fun observeAll(userId: Long): Flow<List<ReaderDnaCard>>

    @Query("SELECT * FROM reader_dna_cards WHERE userId = :userId ORDER BY generatedAt DESC LIMIT 1")
    suspend fun latest(userId: Long): ReaderDnaCard?

    @Query("SELECT * FROM reader_dna_cards WHERE userId = :userId AND id = :id LIMIT 1")
    suspend fun getById(userId: Long, id: Long): ReaderDnaCard?
}
