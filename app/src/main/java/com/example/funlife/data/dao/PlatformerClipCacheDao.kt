package com.example.funlife.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.funlife.data.model.PlatformerClipCacheEntity

@Dao
interface PlatformerClipCacheDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PlatformerClipCacheEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<PlatformerClipCacheEntity>)

    @Query("SELECT * FROM platformer_clip_cache WHERE catalogId = :catalogId AND decodeTag = :decodeTag")
    suspend fun clipsForCharacter(catalogId: String, decodeTag: String): List<PlatformerClipCacheEntity>

    @Query(
        "SELECT COUNT(*) FROM platformer_clip_cache WHERE decodeTag = :decodeTag AND bundleVersion = :bundleVersion",
    )
    suspend fun countForVersion(decodeTag: String, bundleVersion: Int): Int

    @Query("DELETE FROM platformer_clip_cache WHERE decodeTag != :decodeTag OR bundleVersion != :bundleVersion")
    suspend fun purgeStale(decodeTag: String, bundleVersion: Int)

    @Query("DELETE FROM platformer_clip_cache")
    suspend fun clearAll()
}
