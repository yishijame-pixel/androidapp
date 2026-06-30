package com.example.funlife.game.platformer

import com.example.funlife.game.platformer.catalog.PlatformerContentCatalog

/** 群英荟萃章节：关卡 63–74，catalog 驱动元数据。 */
object PlatformerHeroLevelCatalog {

    const val HERO_LEVEL_START = 63
    const val HERO_LEVEL_COUNT = 12

    private fun tilesetPackFor(id: String): PlatformerTilesetPack = when (id) {
        "desert" -> PlatformerTilesetPack.DESERT_PACK
        "winter" -> PlatformerTilesetPack.WINTER_PACK
        "forest" -> PlatformerTilesetPack.FOREST_PACK
        "graveyard" -> PlatformerTilesetPack.GRAVEYARD_PACK
        "scifi" -> PlatformerTilesetPack.SCIFI_PACK
        else -> PlatformerTilesetPack.FOREST_PACK
    }

    private fun themeFor(id: String): PlatformerTheme = when (id) {
        "desert" -> PlatformerTheme.PACK_DESERT
        "winter" -> PlatformerTheme.PACK_WINTER
        "forest" -> PlatformerTheme.PACK_FOREST
        "graveyard" -> PlatformerTheme.PACK_GRAVEYARD
        "scifi" -> PlatformerTheme.PACK_SCIFI
        else -> PlatformerTheme.PACK_FOREST
    }

    private fun skyFor(tilesetId: String): Pair<Long, Long> = when (tilesetId) {
        "desert" -> 0xFFE8B86D to 0xFFFFF0C8
        "winter" -> 0xFFB3D9F2 to 0xFFE0F4FF
        "graveyard" -> 0xFF1A1A2E to 0xFF3D3D5C
        "scifi" -> 0xFF0A0E27 to 0xFF1A237E
        else -> 0xFF87CEEB to 0xFFE0F4FF
    }

    val all: List<PlatformerLevelDef> by lazy {
        PlatformerContentCatalog.heroLevels().map { hero ->
            val pack = tilesetPackFor(hero.tilesetId)
            val theme = themeFor(hero.tilesetId)
            val (top, bottom) = skyFor(hero.tilesetId)
            val budget = PlatformerCampaignLengthSpec.LevelBudget(
                levelId = hero.id,
                chapter = PlatformerCampaignLengthSpec.Chapter.PACK,
                segmentCount = 14,
                targetTiles = 14 * PlatformerSegmentLibrary.SEGMENT_W + PlatformerCampaignLengthSpec.SEGMENT_TAIL_PAD,
                targetMinutesMin = 4f,
                targetMinutesMax = 6f,
                useScrollBuffer = false,
                checkpointEverySegments = 0,
            )
            val script = PlatformerHeroSegmentScripts.scriptFor(hero)
            PlatformerSegmentLevelFactory.manifest(
                PlatformerSegmentLevelFactory.BuildConfig(
                    meta = PlatformerCampaignLevelCatalog.LevelMeta(
                        id = hero.id,
                        title = hero.title,
                        subtitle = "Heroes · ${hero.segmentProfile}",
                        theme = theme,
                        tilesetPack = pack,
                        skyTop = top,
                        skyBottom = bottom,
                        parallaxHill = null,
                        seriesId = "heroes",
                        seriesOrder = hero.id - HERO_LEVEL_START + 1,
                    ),
                    budget = budget,
                    script = script,
                ),
            )
        }.ifEmpty { fallbackLevels() }
    }

    private fun fallbackLevels(): List<PlatformerLevelDef> =
        (HERO_LEVEL_START until HERO_LEVEL_START + HERO_LEVEL_COUNT).map { id ->
            PlatformerCampaignLevelCatalog.buildManifest(id.coerceAtMost(PLATFORMER_CAMPAIGN_LEVEL_COUNT))
                .copy(
                    id = id,
                    title = "英雄关卡 $id",
                    subtitle = "Heroes",
                    seriesId = "heroes",
                )
        }
}
