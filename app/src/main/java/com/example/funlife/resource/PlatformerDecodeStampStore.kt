package com.example.funlife.resource

import android.content.Context
import com.example.funlife.game.platformer.PlatformerCharacterId
import com.example.funlife.game.platformer.catalog.catalogId
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

/**
 * 横版解码完成指纹 v3：bundle 版本 + 全角色 clip 磁盘帧数 + 地图预热标记。
 * 向后兼容 v2 SharedPreferences 键，首次读取 v2 时迁移到 v3。
 */
object PlatformerDecodeStampStore {

    private const val PREFS_KEY_V3 = "platformer_decode_stamp_v3"
    private const val PREFS_KEY_V2 = "platformer_decode_stamp_v2"
    private val gson = Gson()

    data class CharacterClipStamp(
        @SerializedName("walk") val walk: Int = 0,
        @SerializedName("jump") val jump: Int = 0,
        @SerializedName("die") val die: Int = 0,
    )

    data class ChickClipStamp(
        @SerializedName("walk") val walk: Int = 0,
        @SerializedName("jump") val jump: Int = 0,
        @SerializedName("die") val die: Int = 0,
    )

    data class StampV3(
        @SerializedName("version") val version: Int = 3,
        @SerializedName("skinsBundleVer") val skinsBundleVer: Int,
        @SerializedName("platformerBundleVer") val platformerBundleVer: Int,
        @SerializedName("pacMazeDecodeTag") val pacMazeDecodeTag: String,
        @SerializedName("platformerDecodeTag") val platformerDecodeTag: String,
        @SerializedName("characters") val characters: Map<String, CharacterClipStamp> = emptyMap(),
        @SerializedName("chick") val chick: ChickClipStamp? = null,
        @SerializedName("mapAssetsLoaded") val mapAssetsLoaded: Boolean = false,
        @SerializedName("localCharactersLoaded") val localCharactersLoaded: Boolean = false,
        @SerializedName("completedAtMs") val completedAtMs: Long = System.currentTimeMillis(),
    )

    /** @deprecated v2 兼容读取 */
    data class StampV2(
        @SerializedName("skinsBundleVer") val skinsBundleVer: Int,
        @SerializedName("platformerBundleVer") val platformerBundleVer: Int,
        @SerializedName("pacMazeDecodeTag") val pacMazeDecodeTag: String,
        @SerializedName("platformerDecodeTag") val platformerDecodeTag: String,
        @SerializedName("chickWalkFrames") val chickWalkFrames: Int = 0,
        @SerializedName("chickJumpFrames") val chickJumpFrames: Int = 0,
        @SerializedName("catalogId") val catalogId: String? = null,
        @SerializedName("catalogWalkFrames") val catalogWalkFrames: Int = 0,
        @SerializedName("catalogJumpFrames") val catalogJumpFrames: Int = 0,
        @SerializedName("completedAtMs") val completedAtMs: Long = System.currentTimeMillis(),
    )

    fun currentExpectedBase(): StampV3 {
        val skins = ResourceStore.readPacMazeSkinsBundleVersion() ?: ResourceStore.PAC_MAZE_SKINS_BUNDLE_VERSION
        val platformer = ResourceStore.readPlatformerCharactersBundleVersion() ?: 1
        return StampV3(
            skinsBundleVer = skins,
            platformerBundleVer = platformer,
            pacMazeDecodeTag = ResourceStore.pacMazeSkinsDecodeTag(),
            platformerDecodeTag = ResourceStore.platformerDecodeTag(),
        )
    }

    fun loadV3(context: Context): StampV3? {
        val prefs = ResourceStore.prefsAccessor()
        prefs.getString(PREFS_KEY_V3, null)?.let { raw ->
            return runCatching { gson.fromJson(raw, StampV3::class.java) }.getOrNull()
        }
        return migrateFromV2(prefs.getString(PREFS_KEY_V2, null))
    }

    private fun migrateFromV2(raw: String?): StampV3? {
        if (raw.isNullOrBlank()) return null
        val v2 = runCatching { gson.fromJson(raw, StampV2::class.java) }.getOrNull() ?: return null
        val characters = buildMap {
            v2.catalogId?.let { id ->
                put(
                    id,
                    CharacterClipStamp(
                        walk = v2.catalogWalkFrames,
                        jump = v2.catalogJumpFrames,
                    ),
                )
            }
        }
        return StampV3(
            skinsBundleVer = v2.skinsBundleVer,
            platformerBundleVer = v2.platformerBundleVer,
            pacMazeDecodeTag = v2.pacMazeDecodeTag,
            platformerDecodeTag = v2.platformerDecodeTag,
            characters = characters,
            chick = ChickClipStamp(
                walk = v2.chickWalkFrames,
                jump = v2.chickJumpFrames,
            ),
            completedAtMs = v2.completedAtMs,
        )
    }

    fun saveV3(context: Context, stamp: StampV3) {
        ResourceStore.prefsAccessor().edit()
            .putString(PREFS_KEY_V3, gson.toJson(stamp))
            .remove(PREFS_KEY_V2)
            .apply()
        ResourceStore.markPlatformerDecodeStampCompleteLegacy()
    }

    fun isCurrent(stamp: StampV3): Boolean {
        val expected = currentExpectedBase()
        if (stamp.skinsBundleVer != expected.skinsBundleVer) return false
        if (stamp.platformerBundleVer != expected.platformerBundleVer) return false
        if (stamp.pacMazeDecodeTag != expected.pacMazeDecodeTag) return false
        if (stamp.platformerDecodeTag != expected.platformerDecodeTag) return false
        return true
    }

    fun isFullyComplete(stamp: StampV3): Boolean {
        if (!isCurrent(stamp)) return false
        if (!stamp.mapAssetsLoaded || !stamp.localCharactersLoaded) return false
        val chick = stamp.chick ?: return false
        if (chick.walk <= 0 || chick.jump <= 0 || chick.die <= 0) return false
        val remoteIds = com.example.funlife.game.platformer.PlatformerCharacterId.entries
            .filter { it.isCatalogRemote }
            .map { it.catalogId }
        if (remoteIds.isEmpty()) return true
        return remoteIds.all { id ->
            val clip = stamp.characters[id] ?: return false
            clip.walk > 0 && clip.die > 0
        }
    }

    fun clear(context: Context) {
        ResourceStore.prefsAccessor().edit()
            .remove(PREFS_KEY_V3)
            .remove(PREFS_KEY_V2)
            .apply()
        ResourceStore.clearPlatformerDecodeStamp()
    }

    // --- v2 API 兼容（旧调用方） ---

    @Deprecated("Use StampV3", ReplaceWith("currentExpectedBase()"))
    fun currentExpected(): StampV2 {
        val base = currentExpectedBase()
        return StampV2(
            skinsBundleVer = base.skinsBundleVer,
            platformerBundleVer = base.platformerBundleVer,
            pacMazeDecodeTag = base.pacMazeDecodeTag,
            platformerDecodeTag = base.platformerDecodeTag,
        )
    }

    @Deprecated("Use loadV3", ReplaceWith("loadV3(context)"))
    fun load(context: Context): StampV2? {
        val v3 = loadV3(context) ?: return null
        val firstCatalog = v3.characters.entries.firstOrNull()
        return StampV2(
            skinsBundleVer = v3.skinsBundleVer,
            platformerBundleVer = v3.platformerBundleVer,
            pacMazeDecodeTag = v3.pacMazeDecodeTag,
            platformerDecodeTag = v3.platformerDecodeTag,
            chickWalkFrames = v3.chick?.walk ?: 0,
            chickJumpFrames = v3.chick?.jump ?: 0,
            catalogId = firstCatalog?.key,
            catalogWalkFrames = firstCatalog?.value?.walk ?: 0,
            catalogJumpFrames = firstCatalog?.value?.jump ?: 0,
            completedAtMs = v3.completedAtMs,
        )
    }

    @Deprecated("Use saveV3", ReplaceWith("saveV3(context, stamp)"))
    fun save(context: Context, stamp: StampV2) {
        saveV3(
            context,
            StampV3(
                skinsBundleVer = stamp.skinsBundleVer,
                platformerBundleVer = stamp.platformerBundleVer,
                pacMazeDecodeTag = stamp.pacMazeDecodeTag,
                platformerDecodeTag = stamp.platformerDecodeTag,
                characters = buildMap {
                    stamp.catalogId?.let {
                        put(it, CharacterClipStamp(walk = stamp.catalogWalkFrames, jump = stamp.catalogJumpFrames))
                    }
                },
                chick = ChickClipStamp(walk = stamp.chickWalkFrames, jump = stamp.chickJumpFrames),
                completedAtMs = stamp.completedAtMs,
            ),
        )
    }

    @Deprecated("Use isCurrent(StampV3)", ReplaceWith("isCurrent(stamp)"))
    fun isCurrent(context: Context, stamp: StampV2): Boolean {
        val v3 = StampV3(
            skinsBundleVer = stamp.skinsBundleVer,
            platformerBundleVer = stamp.platformerBundleVer,
            pacMazeDecodeTag = stamp.pacMazeDecodeTag,
            platformerDecodeTag = stamp.platformerDecodeTag,
        )
        return isCurrent(v3)
    }
}
