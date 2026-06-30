package com.example.funlife.game.platformer

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import com.example.funlife.resource.ResourceStore

/**
 * SuperTux 全关卡目录（901–1018，107 关）。
 * 关卡数据来自 `platformer_supertux/levels/level_XXX/level.json` bundle。
 */
object PlatformerSuperTuxLevelCatalog {

    private const val TAG = "PlatformerSuperTux"
    /** Reject stale v3 cloud catalog that only listed 10 demo levels. */
    private const val SUPERTUX_MIN_CATALOG_LEVELS = 100

    private val gson = Gson()

    data class LevelMeta(
        val id: Int,
        val title: String,
        val subtitle: String,
        val theme: PlatformerTheme = PlatformerTheme.PACK_SUPERTUX,
        val tilesetPack: PlatformerTilesetPack = PlatformerTilesetPack.SUPERTUX,
        val skyTop: Long = 0xFFB0D4E8,
        val skyBottom: Long = 0xFFE8F4FC,
        val seriesId: String = "supertux_antarctic",
        val seriesOrder: Int = 0,
        val bgmEventId: String = "bgm_supertux_antarctic",
    )

    val orderedLevelIds: List<Int> by lazy {
        val fromCatalog = loadCatalog()?.levels?.map { it.id }?.sorted()
        if (fromCatalog != null && fromCatalog.size >= SUPERTUX_MIN_CATALOG_LEVELS) {
            fromCatalog
        } else {
            android.util.Log.w(
                TAG,
                "SuperTux catalog incomplete (count=${fromCatalog?.size ?: 0}), using fallback 107 ids",
            )
            fallbackLevelIds()
        }
    }

    val all: List<PlatformerLevelDef> by lazy {
        orderedLevelIds.mapNotNull { id ->
            runCatching { buildManifest(id) }.getOrNull()
        }
    }

    fun isSuperTuxLevel(levelId: Int): Boolean =
        PlatformerSuperTuxLengthSpec.isSuperTuxLevel(levelId) &&
            orderedLevelIds.contains(levelId)

    /** 经典引擎 `.stl` 相对路径，如 `world1/welcome_antarctica.stl`。 */
    fun sourceStl(levelId: Int): String? {
        loadLevelJson(levelId)?.sourceStl?.takeIf { it.isNotBlank() }?.let { return it }
        return loadCatalog()?.levels
            ?.firstOrNull { it.id == levelId }
            ?.sourceStl
            ?.takeIf { it.isNotBlank() }
    }

    fun nextLevelId(currentId: Int): Int? {
        val ids = orderedLevelIds
        val idx = ids.indexOf(currentId)
        return if (idx >= 0 && idx < ids.lastIndex) ids[idx + 1] else null
    }

    fun hasNextLevel(currentId: Int): Boolean = nextLevelId(currentId) != null

    fun buildManifest(levelId: Int, titleOverride: String? = null): PlatformerLevelDef {
        val json = loadLevelJson(levelId) ?: error("Missing SuperTux level bundle: $levelId")
        val meta = meta(levelId, titleOverride ?: json.title)
        val segments = if (json.useCampaignScroll) loadSegments(levelId) else emptyList()
        val visualSegments = if (json.useCampaignScroll) loadVisualSegments(levelId) else emptyList()
        val visualRows = if (!json.useCampaignScroll) loadVisualRows(levelId) else emptyList()
        return PlatformerLevelDef(
            id = meta.id,
            title = meta.title,
            subtitle = json.subtitle.ifBlank { meta.subtitle },
            theme = meta.theme,
            rows = if (json.useCampaignScroll) emptyList() else json.rows,
            skyTop = meta.skyTop,
            skyBottom = meta.skyBottom,
            tilesetPack = meta.tilesetPack,
            seriesId = meta.seriesId,
            seriesOrder = meta.seriesOrder,
            useCampaignScroll = json.useCampaignScroll,
            checkpointEverySegments = if (json.useCampaignScroll) {
                PlatformerCampaignLengthSpec.CHECKPOINT_EVERY_SEGMENTS
            } else {
                0
            },
            targetTiles = json.width,
            supertuxBakedSegments = segments.takeIf { it.isNotEmpty() },
            supertuxVisualSegments = visualSegments.takeIf { it.isNotEmpty() },
            supertuxVisualRows = visualRows.takeIf { it.isNotEmpty() },
            supertuxCoins = json.coinTiles.map { PlatformerSuperTuxObjectSpawn(tx = it.tx, ty = it.ty) },
            supertuxBadguys = json.badguyTiles.map {
                PlatformerSuperTuxObjectSpawn(name = it.name, tx = it.tx, ty = it.ty)
            },
            bgmEventId = meta.bgmEventId,
        )
    }

    fun meta(levelId: Int, title: String? = null): LevelMeta {
        val json = loadLevelJson(levelId)
        val chapter = chapterForLevel(levelId, json?.chapterId)
        val range = chapter?.levelRange ?: listOf(SUPERTUX_LEVEL_START, SUPERTUX_LEVEL_END)
        val orderBase = range.firstOrNull() ?: SUPERTUX_LEVEL_START
        val order = (levelId - orderBase + 1).coerceAtLeast(1)
        val resolvedTitle = catalogTitle(levelId) ?: title ?: json?.title ?: "SuperTux $levelId"
        val subtitle = chapter?.subtitle ?: "SuperTux · 改编"
        return LevelMeta(
            id = levelId,
            title = resolvedTitle,
            subtitle = subtitle,
            skyTop = parseColor(chapter?.skyTop) ?: parseColor(json?.skyTop) ?: 0xFFB0D4E8,
            skyBottom = parseColor(chapter?.skyBottom) ?: parseColor(json?.skyBottom) ?: 0xFFE8F4FC,
            seriesId = json?.seriesId ?: chapter?.id ?: "supertux_antarctic",
            seriesOrder = order,
            bgmEventId = json?.bgmEvent ?: chapter?.bgmEvent ?: "bgm_supertux_antarctic",
        )
    }

    private fun chapterForLevel(levelId: Int, chapterId: String?): ChapterEntry? {
        val catalog = loadCatalog() ?: return null
        chapterId?.let { id ->
            catalog.chapters.firstOrNull { it.id == id }?.let { return it }
        }
        return catalog.chapters.firstOrNull { ch ->
            val r = ch.levelRange
            r.size >= 2 && levelId in r[0]..r[1]
        }
    }

    private fun parseColor(hex: String?): Long? {
        if (hex.isNullOrBlank()) return null
        val clean = hex.removePrefix("#")
        return when (clean.length) {
            6 -> ("FF$clean").toLong(16)
            8 -> clean.toLong(16)
            else -> null
        }
    }

    private fun catalogTitle(levelId: Int): String? =
        loadCatalog()?.levels?.firstOrNull { it.id == levelId }?.title

    private fun fallbackLevelIds(): List<Int> =
        (PlatformerSuperTuxLengthSpec.SUPERTUX_LEVEL_START..931).toList() +
            (941..978).toList() +
            (981..1010).toList() +
            (1011..PlatformerSuperTuxLengthSpec.SUPERTUX_LEVEL_END).toList()

    private fun loadCatalog(): ContentCatalog? {
        val text = readBundleText("platformer_supertux/content_catalog.json") ?: return null
        return runCatching {
            gson.fromJson(text, ContentCatalog::class.java)
        }.getOrNull()
    }

    private fun loadLevelJson(levelId: Int): LevelJson? {
        val text = readBundleText("platformer_supertux/levels/level_${levelId}/level.json")
            ?: return null
        return runCatching {
            gson.fromJson(text, LevelJson::class.java)
        }.getOrNull()
    }

    private fun loadVisualSegments(levelId: Int): List<List<List<Int>>> {
        val text = readBundleText("platformer_supertux/levels/level_${levelId}/visual_segments.json")
            ?: return emptyList()
        return runCatching {
            val type = object : TypeToken<List<List<List<Int>>>>() {}.type
            @Suppress("UNCHECKED_CAST")
            gson.fromJson<List<List<List<Int>>>>(text, type)
        }.getOrDefault(emptyList())
    }

    private fun loadVisualRows(levelId: Int): List<List<Int>> {
        val text = readBundleText("platformer_supertux/levels/level_${levelId}/visual_rows.json")
            ?: return emptyList()
        return runCatching {
            val type = object : TypeToken<List<List<Int>>>() {}.type
            @Suppress("UNCHECKED_CAST")
            gson.fromJson<List<List<Int>>>(text, type)
        }.getOrDefault(emptyList())
    }

    private fun loadSegments(levelId: Int): List<List<String>> {
        val text = readBundleText("platformer_supertux/levels/level_${levelId}/segments.json")
            ?: return emptyList()
        return runCatching {
            val type = object : TypeToken<List<List<String>>>() {}.type
            @Suppress("UNCHECKED_CAST")
            gson.fromJson<List<List<String>>>(text, type)
        }.getOrDefault(emptyList())
    }

    private fun readBundleText(relativePath: String): String? = runCatching {
        ResourceStore.resolveFile(relativePath)?.readText()
            ?: ResourceStore.openInputStream(relativePath)?.bufferedReader()?.use { it.readText() }
    }.getOrNull()

    private const val SUPERTUX_LEVEL_START = PlatformerSuperTuxLengthSpec.SUPERTUX_LEVEL_START
    private const val SUPERTUX_LEVEL_END = PlatformerSuperTuxLengthSpec.SUPERTUX_LEVEL_END

    private data class ContentCatalog(
        @SerializedName("levels") val levels: List<CatalogLevel> = emptyList(),
        @SerializedName("chapters") val chapters: List<ChapterEntry> = emptyList(),
    )

    private data class CatalogLevel(
        val id: Int,
        val title: String,
        val chapterId: String? = null,
        @SerializedName("sourceStl") val sourceStl: String? = null,
    )

    private data class ChapterEntry(
        val id: String,
        val subtitle: String? = null,
        val levelRange: List<Int> = emptyList(),
        val bgmEvent: String? = null,
        val skyTop: String? = null,
        val skyBottom: String? = null,
    )

    private data class LevelJson(
        val id: Int = 0,
        val title: String = "",
        val subtitle: String = "",
        @SerializedName("sourceStl") val sourceStl: String? = null,
        @SerializedName("chapterId") val chapterId: String? = null,
        @SerializedName("seriesId") val seriesId: String? = null,
        @SerializedName("bgmEvent") val bgmEvent: String? = null,
        @SerializedName("skyTop") val skyTop: String? = null,
        @SerializedName("skyBottom") val skyBottom: String? = null,
        @SerializedName("useCampaignScroll") val useCampaignScroll: Boolean = false,
        val rows: List<String> = emptyList(),
        val width: Int = 0,
        val coinTiles: List<CoinTileJson> = emptyList(),
        val badguyTiles: List<BadguyTileJson> = emptyList(),
    )

    private data class CoinTileJson(val tx: Int = 0, val ty: Int = 0)

    private data class BadguyTileJson(val name: String = "", val tx: Int = 0, val ty: Int = 0)
}
