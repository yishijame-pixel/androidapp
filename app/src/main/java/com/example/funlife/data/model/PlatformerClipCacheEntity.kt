package com.example.funlife.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** 横版 clip 磁盘解码元数据（Room 可观测索引，替代纯 SP stamp 查询）。 */
@Entity(
    tableName = "platformer_clip_cache",
    indices = [
        Index(value = ["catalogId"]),
        Index(value = ["decodeTag", "bundleVersion"]),
    ],
)
data class PlatformerClipCacheEntity(
    @PrimaryKey val id: String,
    val catalogId: String,
    val clipFolder: String,
    val frameCount: Int,
    val decodeTag: String,
    val bundleVersion: Int,
    val format: String,
    val updatedAtMs: Long,
) {
    companion object {
        fun makeId(catalogId: String, clipFolder: String, decodeTag: String): String =
            "$catalogId:$clipFolder:$decodeTag"
    }
}
