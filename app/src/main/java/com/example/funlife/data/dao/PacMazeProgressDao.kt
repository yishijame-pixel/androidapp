package com.example.funlife.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.funlife.data.model.PacMazeProgress
import kotlinx.coroutines.flow.Flow

@Dao
interface PacMazeProgressDao {
    @Query("SELECT * FROM pac_maze_progress WHERE userId = :userId LIMIT 1")
    fun observeByUserId(userId: Long): Flow<PacMazeProgress?>

    @Query("SELECT * FROM pac_maze_progress WHERE userId = :userId LIMIT 1")
    suspend fun getByUserId(userId: Long): PacMazeProgress?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(progress: PacMazeProgress)
}
