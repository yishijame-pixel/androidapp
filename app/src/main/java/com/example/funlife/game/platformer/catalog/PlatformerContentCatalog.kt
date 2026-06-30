package com.example.funlife.game.platformer.catalog

import com.example.funlife.game.platformer.PlatformerCharacterId
import com.example.funlife.resource.ResourceStore
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import java.io.InputStreamReader

/**
 * 企业级横版内容目录：角色 / 敌人 / 地砖 / 模式 / 英雄章节。
 * 数据源：APK assets/platformer/content_catalog.json 或 platformer_characters/content_catalog.json
 */
object PlatformerContentCatalog {

    private const val CATALOG_ASSET = "platformer/content_catalog.json"
    private const val BUNDLE_CATALOG = "platformer_characters/content_catalog.json"

    private val gson = Gson()

    data class Catalog(
        @SerializedName("schemaVersion") val schemaVersion: Int = 1,
        @SerializedName("bundleVersion") val bundleVersion: Int = 1,
        @SerializedName("characters") val characters: List<CharacterEntry> = emptyList(),
        @SerializedName("enemies") val enemies: List<EnemyEntry> = emptyList(),
        @SerializedName("tilesets") val tilesets: List<TilesetEntry> = emptyList(),
        @SerializedName("modes") val modes: List<ModeEntry> = emptyList(),
        @SerializedName("campaign") val campaign: CampaignSection? = null,
    )

    data class UnlockRule(
        @SerializedName("type") val type: String = "default",
        @SerializedName("value") val value: Int = 0,
    )

    data class RenderSpec(
        @SerializedName("heightCellFrac") val heightCellFrac: Float = 1.55f,
        @SerializedName("mirrorDefault") val mirrorDefault: Boolean = true,
    )

    data class CharacterEntry(
        @SerializedName("id") val id: String,
        @SerializedName("title") val title: String,
        @SerializedName("subtitle") val subtitle: String = "",
        @SerializedName("group") val group: String = "hero",
        @SerializedName("assetRoot") val assetRoot: String,
        @SerializedName("source") val source: String? = null,
        @SerializedName("sourceZip") val sourceZip: String? = null,
        @SerializedName("unlock") val unlock: UnlockRule = UnlockRule(),
        @SerializedName("abilities") val abilities: List<String> = emptyList(),
        @SerializedName("render") val render: RenderSpec = RenderSpec(),
        @SerializedName("requiredClips") val requiredClips: List<String> = emptyList(),
    ) {
        val isPacMazeSource: Boolean get() = source == "pac_maze" || assetRoot.startsWith("pac_maze_skins/")
        val isLocalApkSource: Boolean get() = source == "local_apk"
        val isRemoteBundle: Boolean get() = assetRoot.startsWith("platformer_characters/")
    }

    data class EnemyEntry(
        @SerializedName("id") val id: String,
        @SerializedName("title") val title: String,
        @SerializedName("group") val group: String = "generic",
        @SerializedName("assetRoot") val assetRoot: String,
        @SerializedName("sourceZip") val sourceZip: String? = null,
        @SerializedName("behavior") val behavior: String = "PATROL",
    )

    data class TilesetEntry(
        @SerializedName("id") val id: String,
        @SerializedName("assetRoot") val assetRoot: String,
        @SerializedName("theme") val theme: String,
        @SerializedName("tileCount") val tileCount: Int = 18,
        @SerializedName("sourceZip") val sourceZip: String? = null,
    )

    data class ModeEntry(
        @SerializedName("id") val id: String,
        @SerializedName("title") val title: String,
        @SerializedName("entry") val entry: String = "hub_default",
        @SerializedName("characterId") val characterId: String? = null,
        @SerializedName("sourceZip") val sourceZip: String? = null,
    )

    data class HeroLevelEntry(
        @SerializedName("id") val id: Int,
        @SerializedName("title") val title: String,
        @SerializedName("chapter") val chapter: String = "HEROES",
        @SerializedName("tilesetId") val tilesetId: String,
        @SerializedName("featuredCharacterId") val featuredCharacterId: String? = null,
        @SerializedName("featuredEnemyId") val featuredEnemyId: String? = null,
        @SerializedName("segmentProfile") val segmentProfile: String = "HERO_STANDARD",
    )

    data class HeroChapter(
        @SerializedName("id") val id: String,
        @SerializedName("title") val title: String,
        @SerializedName("filter") val filter: String,
        @SerializedName("levelRange") val levelRange: List<Int> = emptyList(),
        @SerializedName("levels") val levels: List<HeroLevelEntry> = emptyList(),
    )

    data class CampaignSection(
        @SerializedName("heroChapter") val heroChapter: HeroChapter? = null,
    )

    @Volatile
    private var cached: Catalog? = null

    fun load(force: Boolean = false): Catalog? {
        if (!force && cached != null) return cached
        val stream = openCatalogStream() ?: return null
        return stream.use {
            runCatching {
                gson.fromJson(InputStreamReader(it, Charsets.UTF_8), Catalog::class.java)
            }.getOrNull()?.also { catalog -> cached = catalog }
        }
    }

    fun requireLoaded(): Catalog = load() ?: emptyFallback()

    private fun openCatalogStream(): java.io.InputStream? =
        ResourceStore.openInputStream(BUNDLE_CATALOG)
            ?: ResourceStore.openInputStream(CATALOG_ASSET)

    private fun emptyFallback(): Catalog = Catalog()

    fun character(id: String): CharacterEntry? =
        requireLoaded().characters.find { it.id == id }

    fun characterForEnum(enumId: PlatformerCharacterId): CharacterEntry? =
        character(enumId.catalogId)

    fun enemies(): List<EnemyEntry> = requireLoaded().enemies

    fun modes(): List<ModeEntry> = requireLoaded().modes

    fun heroLevels(): List<HeroLevelEntry> =
        requireLoaded().campaign?.heroChapter?.levels.orEmpty()

    fun charactersByGroup(): Map<String, List<CharacterEntry>> =
        requireLoaded().characters.groupBy { it.group }

    fun invalidateCache() {
        cached = null
    }
}

/** 枚举与 catalog string id 的双向映射。 */
val PlatformerCharacterId.catalogId: String
    get() = when (this) {
        PlatformerCharacterId.CHICK_PRO_MAX -> "chick_pro_max"
        PlatformerCharacterId.TREASURE_HUNTER -> "treasure_hunter"
        PlatformerCharacterId.PIXEL_WALKER -> "pixel_walker"
        PlatformerCharacterId.TEMPLE_RUNNER -> "temple_runner"
        PlatformerCharacterId.ADVENTURE_GIRL -> "adventure_girl"
        PlatformerCharacterId.NINJA_GIRL -> "ninja_girl"
        PlatformerCharacterId.NINJA_BOY -> "ninja_boy"
        PlatformerCharacterId.JACK -> "jack"
        PlatformerCharacterId.RED_HAT -> "red_hat"
        PlatformerCharacterId.ROBOT -> "robot"
        PlatformerCharacterId.DINO -> "dino"
        PlatformerCharacterId.KNIGHT -> "knight"
        PlatformerCharacterId.SANTA -> "santa"
        PlatformerCharacterId.CAT -> "cat"
        PlatformerCharacterId.DOG -> "dog"
        PlatformerCharacterId.SUPERTUX_TUX -> "supertux_tux"
    }

fun catalogIdToCharacterId(id: String): PlatformerCharacterId? =
    PlatformerCharacterId.entries.find { it.catalogId == id }
