package com.example.funlife.resource

import android.content.Context
import com.example.funlife.data.database.AppDatabase
import com.example.funlife.data.model.PlatformerClipCacheEntity
import com.example.funlife.game.platformer.catalog.PlatformerAnimClip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Room 层横版 clip 元数据同步（decode 完成后写入，供跨进程/可观测查询）。 */
object PlatformerClipCacheIndex {

    suspend fun recordClip(
        context: Context,
        catalogId: String,
        clip: PlatformerAnimClip,
        frameCount: Int,
    ) = withContext(Dispatchers.IO) {
        if (frameCount <= 0) return@withContext
        val decodeTag = ResourceStore.platformerDecodeTag()
        val bundleVer = ResourceStore.readPlatformerCharactersBundleVersion() ?: 1
        val entity = PlatformerClipCacheEntity(
            id = PlatformerClipCacheEntity.makeId(catalogId, clip.folder, decodeTag),
            catalogId = catalogId,
            clipFolder = clip.folder,
            frameCount = frameCount,
            decodeTag = decodeTag,
            bundleVersion = bundleVer,
            format = DecodedClipDiskIndex.FORMAT_WEBP,
            updatedAtMs = System.currentTimeMillis(),
        )
        AppDatabase.getDatabase(context.applicationContext).platformerClipCacheDao().upsert(entity)
    }

    suspend fun recordChickClip(
        context: Context,
        clipFolder: String,
        frameCount: Int,
    ) = withContext(Dispatchers.IO) {
        if (frameCount <= 0) return@withContext
        val decodeTag = ResourceStore.pacMazeSkinsDecodeTag()
        val bundleVer = ResourceStore.readPacMazeSkinsBundleVersion()
            ?: ResourceStore.PAC_MAZE_SKINS_BUNDLE_VERSION
        val catalogId = "chick_pro_max"
        val entity = PlatformerClipCacheEntity(
            id = PlatformerClipCacheEntity.makeId(catalogId, clipFolder, decodeTag),
            catalogId = catalogId,
            clipFolder = clipFolder,
            frameCount = frameCount,
            decodeTag = decodeTag,
            bundleVersion = bundleVer,
            format = DecodedClipDiskIndex.FORMAT_WEBP,
            updatedAtMs = System.currentTimeMillis(),
        )
        AppDatabase.getDatabase(context.applicationContext).platformerClipCacheDao().upsert(entity)
    }

    suspend fun purgeStale(context: Context) = withContext(Dispatchers.IO) {
        val dao = AppDatabase.getDatabase(context.applicationContext).platformerClipCacheDao()
        val decodeTag = ResourceStore.platformerDecodeTag()
        val bundleVer = ResourceStore.readPlatformerCharactersBundleVersion() ?: 1
        dao.purgeStale(decodeTag, bundleVer)
    }

    suspend fun clearAll(context: Context) = withContext(Dispatchers.IO) {
        AppDatabase.getDatabase(context.applicationContext).platformerClipCacheDao().clearAll()
    }
}
