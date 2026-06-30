package com.example.funlife.game.platformer

/**
 * 高空章节：10 关独立天路关卡（ID 53–62），一开局即站在浮岛高处。
 */
object PlatformerSkyLevelCatalog {

    data class LevelMeta(
        val id: Int,
        val title: String,
        val subtitle: String,
        val theme: PlatformerTheme,
        val tilesetPack: PlatformerTilesetPack,
        val skyTop: Long,
        val skyBottom: Long,
        val parallaxHill: Long? = null,
        val seriesId: String,
        val seriesOrder: Int,
    )

    val all: List<PlatformerLevelDef> by lazy {
        (PlatformerSkyLengthSpec.SKY_LEVEL_START..PlatformerSkyLengthSpec.SKY_LEVEL_END)
            .map(::buildManifest)
    }

    fun isSkyLevel(levelId: Int): Boolean = PlatformerSkyLengthSpec.isSkyLevel(levelId)

    fun buildManifest(levelId: Int): PlatformerLevelDef {
        val skyBudget = PlatformerSkyLengthSpec.budget(levelId)
        val script = PlatformerSkySegmentScripts.scriptFor(levelId)
        val meta = meta(levelId)
        return PlatformerLevelDef(
            id = meta.id,
            title = meta.title,
            subtitle = meta.subtitle,
            theme = meta.theme,
            rows = emptyList(),
            skyTop = meta.skyTop,
            skyBottom = meta.skyBottom,
            parallaxHill = meta.parallaxHill,
            tilesetPack = meta.tilesetPack,
            seriesId = meta.seriesId,
            seriesOrder = meta.seriesOrder,
            campaignSegmentScript = script,
            useCampaignScroll = skyBudget.useScrollBuffer,
            checkpointEverySegments = skyBudget.checkpointEverySegments,
            targetTiles = skyBudget.targetTiles,
        )
    }

    fun meta(levelId: Int): LevelMeta = when (levelId) {
        53 -> m(53, "浮云起点", "Sky · 天路教学", PlatformerTheme.ICE, PlatformerTilesetPack.GOODLY,
            0xFFB0D4E8, 0xFFE8F4FC, null, "sky", 1)
        54 -> m(54, "冰桥天路", "Sky · 冰原浮岛", PlatformerTheme.PACK_WINTER, PlatformerTilesetPack.WINTER_PACK,
            0xFFB0D4E8, 0xFFE8F4FC, null, "sky", 2)
        55 -> m(55, "激光云海", "Sky · 高空激光", PlatformerTheme.PACK_SCIFI, PlatformerTilesetPack.SCIFI_PACK,
            0xFF0A0E27, 0xFF1A237E, null, "sky", 3)
        56 -> m(56, "炮台浮岛", "Sky · 炮台围攻", PlatformerTheme.FORTRESS, PlatformerTilesetPack.GOODLY,
            0xFF87CEEB, 0xFFB0D4E8, null, "sky", 4)
        57 -> m(57, "弹簧天际", "Sky · 弹簧连跳", PlatformerTheme.GRASS, PlatformerTilesetPack.GOODLY,
            0xFF87CEEB, 0xFFB0E0FF, 0xFF5DAD5D, "sky", 5)
        58 -> m(58, "三界天阶", "Sky · 逐层攀登", PlatformerTheme.PACK_FOREST, PlatformerTilesetPack.FOREST_PACK,
            0xFF6BA3C7, 0xFFB8D9E8, null, "sky", 6)
        59 -> m(59, "深渊边缘", "Sky · 狭道险境", PlatformerTheme.SPOOKY, PlatformerTilesetPack.GOODLY,
            0xFF0D1B2A, 0xFF1B263B, null, "sky", 7)
        60 -> m(60, "机关天廊", "Sky · 机关走廊", PlatformerTheme.PACK_SCIFI, PlatformerTilesetPack.SCIFI_PACK,
            0xFF0A0E27, 0xFF283593, null, "sky", 8)
        61 -> m(61, "亡灵浮岛", "Sky · 浮岛混战", PlatformerTheme.PACK_GRAVEYARD, PlatformerTilesetPack.GRAVEYARD_PACK,
            0xFF1A1A2E, 0xFF3D3D5C, null, "sky", 9)
        62 -> m(62, "天路终章", "Sky · 终极天路", PlatformerTheme.DESERT, PlatformerTilesetPack.DESERT_PACK,
            0xFFE8B86D, 0xFFFFF0C8, null, "sky", 10)
        else -> m(levelId, "高空 $levelId", "Sky", PlatformerTheme.GRASS, PlatformerTilesetPack.GOODLY,
            0xFF87CEEB, 0xFFB0E0FF, null, "sky", levelId - 52)
    }

    private fun m(
        id: Int,
        title: String,
        subtitle: String,
        theme: PlatformerTheme,
        pack: PlatformerTilesetPack,
        skyTop: Long,
        skyBottom: Long,
        hill: Long?,
        seriesId: String,
        seriesOrder: Int,
    ) = LevelMeta(id, title, subtitle, theme, pack, skyTop, skyBottom, hill, seriesId, seriesOrder)
}
