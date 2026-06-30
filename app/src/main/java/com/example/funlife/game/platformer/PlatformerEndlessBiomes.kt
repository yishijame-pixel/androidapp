package com.example.funlife.game.platformer

/**
 * 无尽跑酷生物群系：与闯关 1–16 关同款主题/地砖/天空轮换。
 */
object PlatformerEndlessBiomes {

    /** 每个群系持续片段数（28 格/段 → 4 段 ≈ 112 格换景）。 */
    const val SEGMENTS_PER_BIOME = 4

    data class Biome(
        val label: String,
        val theme: PlatformerTheme,
        val tilesetPack: PlatformerTilesetPack,
        val skyTop: Long,
        val skyBottom: Long,
        val parallaxHill: Long? = null,
        val favoredSegments: List<PlatformerSegmentLibrary.SegmentKind> = emptyList(),
    )

    /** 顺序对齐闯关：1–6 Goodly 手绘关 → 7–16 素材包关（去掉极简包）。 */
    val all: List<Biome> = listOf(
        Biome("翠野平原", PlatformerTheme.GRASS, PlatformerTilesetPack.GOODLY,
            0xFF87CEEB, 0xFFB0E0FF, 0xFF5DAD5D,
            listOf(PlatformerSegmentLibrary.SegmentKind.LOW_PLATEAU, PlatformerSegmentLibrary.SegmentKind.FORK, PlatformerSegmentLibrary.SegmentKind.SPRING_VAULT)),
        Biome("钢铁地牢", PlatformerTheme.METAL, PlatformerTilesetPack.GOODLY,
            0xFF2A2A35, 0xFF3D3D4A, null,
            listOf(PlatformerSegmentLibrary.SegmentKind.TRAP_LANE, PlatformerSegmentLibrary.SegmentKind.GAP, PlatformerSegmentLibrary.SegmentKind.ENEMY_ROOM)),
        Biome("赤岩峡谷", PlatformerTheme.DESERT, PlatformerTilesetPack.GOODLY,
            0xFFF4A460, 0xFFFFE4B5, 0xFF8B4513,
            listOf(PlatformerSegmentLibrary.SegmentKind.GAP, PlatformerSegmentLibrary.SegmentKind.STEPS, PlatformerSegmentLibrary.SegmentKind.MID_BRIDGE)),
        Biome("幽林古堡", PlatformerTheme.SPOOKY, PlatformerTilesetPack.GOODLY,
            0xFF0D1B2A, 0xFF1B263B, null,
            listOf(PlatformerSegmentLibrary.SegmentKind.TOWER, PlatformerSegmentLibrary.SegmentKind.TRAP_LANE, PlatformerSegmentLibrary.SegmentKind.ENEMY_ROOM)),
        Biome("冰封雪岭", PlatformerTheme.ICE, PlatformerTilesetPack.GOODLY,
            0xFFB0D4E8, 0xFFE8F4FC, 0xFF6B9DB8,
            listOf(PlatformerSegmentLibrary.SegmentKind.STEPS, PlatformerSegmentLibrary.SegmentKind.MID_BRIDGE, PlatformerSegmentLibrary.SegmentKind.GAP)),
        Biome("要塞高墙", PlatformerTheme.FORTRESS, PlatformerTilesetPack.GOODLY,
            0xFF87CEEB, 0xFFB0D4E8, 0xFF6B8E6B,
            listOf(PlatformerSegmentLibrary.SegmentKind.ENEMY_ROOM, PlatformerSegmentLibrary.SegmentKind.TIER_ASCENT, PlatformerSegmentLibrary.SegmentKind.SPRING_VAULT)),
        Biome("烈日遗迹", PlatformerTheme.PACK_DESERT, PlatformerTilesetPack.DESERT_PACK,
            0xFFE8B86D, 0xFFFFF0C8, null,
            listOf(PlatformerSegmentLibrary.SegmentKind.GAP, PlatformerSegmentLibrary.SegmentKind.LOW_PLATEAU, PlatformerSegmentLibrary.SegmentKind.TRAP_LANE)),
        Biome("雪原长廊", PlatformerTheme.PACK_WINTER, PlatformerTilesetPack.WINTER_PACK,
            0xFFB3D9F2, 0xFFE0F4FF, null,
            listOf(PlatformerSegmentLibrary.SegmentKind.STEPS, PlatformerSegmentLibrary.SegmentKind.MID_BRIDGE, PlatformerSegmentLibrary.SegmentKind.GAP)),
        Biome("密林探险", PlatformerTheme.PACK_FOREST, PlatformerTilesetPack.FOREST_PACK,
            0xFF87CEEB, 0xFFE0F4FF, null,
            listOf(PlatformerSegmentLibrary.SegmentKind.FORK, PlatformerSegmentLibrary.SegmentKind.LOW_PLATEAU, PlatformerSegmentLibrary.SegmentKind.TOWER)),
        Biome("墓园迷踪", PlatformerTheme.PACK_GRAVEYARD, PlatformerTilesetPack.GRAVEYARD_PACK,
            0xFF1A1A2E, 0xFF3D3D5C, null,
            listOf(PlatformerSegmentLibrary.SegmentKind.ENEMY_ROOM, PlatformerSegmentLibrary.SegmentKind.TRAP_LANE, PlatformerSegmentLibrary.SegmentKind.TOWER)),
        Biome("丛林遗迹", PlatformerTheme.PACK_JUNGLE, PlatformerTilesetPack.JUNGLE_PACK,
            0xFF2E7D32, 0xFFA5D6A7, null,
            listOf(PlatformerSegmentLibrary.SegmentKind.LOW_PLATEAU, PlatformerSegmentLibrary.SegmentKind.FORK, PlatformerSegmentLibrary.SegmentKind.SPRING_VAULT)),
        Biome("科幻基地", PlatformerTheme.PACK_SCIFI, PlatformerTilesetPack.SCIFI_PACK,
            0xFF0A0E27, 0xFF1A237E, null,
            listOf(PlatformerSegmentLibrary.SegmentKind.TRAP_LANE, PlatformerSegmentLibrary.SegmentKind.MID_BRIDGE, PlatformerSegmentLibrary.SegmentKind.HIGH_SKYWAY)),
        Biome("溶洞逃亡", PlatformerTheme.PACK_GROTTO, PlatformerTilesetPack.GROTTO_PACK,
            0xFF1B2838, 0xFF2C3E50, null,
            listOf(PlatformerSegmentLibrary.SegmentKind.HIGH_SKYWAY, PlatformerSegmentLibrary.SegmentKind.TOWER, PlatformerSegmentLibrary.SegmentKind.TIER_ASCENT)),
        Biome("英雄跑道", PlatformerTheme.PACK_FOREST, PlatformerTilesetPack.FOREST_PACK,
            0xFF558B2F, 0xFFC5E1A5, null,
            listOf(PlatformerSegmentLibrary.SegmentKind.LOW_PLATEAU, PlatformerSegmentLibrary.SegmentKind.GAP, PlatformerSegmentLibrary.SegmentKind.SPRING_VAULT)),
        Biome("机械走廊", PlatformerTheme.PACK_SCIFI, PlatformerTilesetPack.SCIFI_PACK,
            0xFF1A237E, 0xFF3949AB, null,
            listOf(PlatformerSegmentLibrary.SegmentKind.TRAP_LANE, PlatformerSegmentLibrary.SegmentKind.ENEMY_ROOM)),
        Biome("墓园夜行", PlatformerTheme.PACK_GRAVEYARD, PlatformerTilesetPack.GRAVEYARD_PACK,
            0xFF1A1A2E, 0xFF4527A0, null,
            listOf(PlatformerSegmentLibrary.SegmentKind.TOWER, PlatformerSegmentLibrary.SegmentKind.ENEMY_ROOM)),
        Biome("果冻湿地", PlatformerTheme.PACK_FOREST, PlatformerTilesetPack.FOREST_PACK,
            0xFF33691E, 0xFFAED581, null,
            listOf(PlatformerSegmentLibrary.SegmentKind.SPRING_VAULT, PlatformerSegmentLibrary.SegmentKind.MID_BRIDGE)),
        Biome("天空哨站", PlatformerTheme.PACK_WINTER, PlatformerTilesetPack.WINTER_PACK,
            0xFF0D47A1, 0xFF90CAF9, null,
            listOf(PlatformerSegmentLibrary.SegmentKind.HIGH_SKYWAY, PlatformerSegmentLibrary.SegmentKind.GAP)),
    )

    fun biomeIndexForSegment(segmentIndex: Int): Int =
        (segmentIndex / SEGMENTS_PER_BIOME) % all.size

    fun biomeForSegment(segmentIndex: Int): Biome = all[biomeIndexForSegment(segmentIndex)]

    fun applyToLevel(level: PlatformerLevelDef, segmentIndex: Int): PlatformerLevelDef {
        val biome = biomeForSegment(segmentIndex)
        return level.copy(
            theme = biome.theme,
            tilesetPack = biome.tilesetPack,
            skyTop = biome.skyTop,
            skyBottom = biome.skyBottom,
            parallaxHill = biome.parallaxHill,
            subtitle = "Endless · ${biome.label}",
        )
    }
}
