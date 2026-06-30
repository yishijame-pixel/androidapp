package com.example.funlife.game.platformer

/**
 * 用 [PlatformerSegmentLibrary] 片段脚本批量产出复杂长关卡。
 */
object PlatformerChunkLevelFactory {

    data class Config(
        val id: Int,
        val title: String,
        val subtitle: String,
        val theme: PlatformerTheme,
        val tilesetPack: PlatformerTilesetPack,
        val skyTop: Long,
        val skyBottom: Long,
        val seriesId: String,
        val seriesOrder: Int,
        val segments: List<PlatformerSegmentLibrary.SegmentSpec>,
        val hazardLevel: Int = 2,
        val trapLevel: Int = 2,
    )

    fun build(cfg: Config): PlatformerLevelDef {
        val h = 14
        val g = h - 1
        val segW = PlatformerSegmentLibrary.SEGMENT_W
        val w = segW * cfg.segments.size + 8
        val m = PlatformerMapCanvas(w, h)

        var x = 0
        cfg.segments.forEachIndexed { index, spec ->
            if (index > 0) {
                m.bridgeGap((x - 2).coerceAtLeast(0), g, 1)
            }
            val merged = spec.copy(
                hazardLevel = maxOf(spec.hazardLevel, cfg.hazardLevel),
                trapLevel = maxOf(spec.trapLevel, cfg.trapLevel),
            )
            PlatformerSegmentLibrary.paint(m, g, x, merged, index)
            x += segW
        }

        return PlatformerLevelEnhancer.finalize(
            PlatformerLevelDef(
                id = cfg.id,
                title = cfg.title,
                subtitle = cfg.subtitle,
                theme = cfg.theme,
                tilesetPack = cfg.tilesetPack,
                skyTop = cfg.skyTop,
                skyBottom = cfg.skyBottom,
                rows = m.toRows(),
                seriesId = cfg.seriesId,
                seriesOrder = cfg.seriesOrder,
            ),
            m, g, w,
        )
    }

    /** 23~34：片段拼接扩展关（每关 7~8 段 ≈ 200+ 格）。 */
    fun extendedLevels(): List<PlatformerLevelDef> = listOf(
        build(chunkGrassMarathon()),
        build(chunkMetalGauntlet()),
        build(chunkDesertRift()),
        build(chunkSpookyMaze()),
        build(chunkIceRhythm()),
        build(chunkForestDeep()),
        build(chunkGraveyardSiege()),
        build(chunkScifiRelay()),
        build(chunkGrottoDescent()),
        build(chunkMinimalSprint()),
        build(chunkFortressAssault()),
        build(chunkCanyonUltimate()),
    )

    /** 35~52：低/中/高层平台专题关。 */
    fun tieredLevels(): List<PlatformerLevelDef> = listOf(
        build(tierLowGrass()),
        build(tierLowMetal()),
        build(tierLowDesert()),
        build(tierMidSpooky()),
        build(tierMidIce()),
        build(tierMidForest()),
        build(tierMidGraveyard()),
        build(tierMidScifi()),
        build(tierHighGrotto()),
        build(tierHighMinimal()),
        build(tierHighFortress()),
        build(tierLowMidMix()),
        build(tierMidHighMix()),
        build(tierAscentGrass()),
        build(tierAscentGraveyard()),
        build(tierLowMarathon()),
        build(tierHighMarathon()),
        build(tierUltimate52()),
    )

    private fun tierScript(
        kinds: List<PlatformerSegmentLibrary.SegmentKind>,
        hazard: Int = 2,
        trap: Int = 2,
    ): List<PlatformerSegmentLibrary.SegmentSpec> =
        kinds.map { PlatformerSegmentLibrary.SegmentSpec(it, hazard, trap) }

    private fun tierLowGrass() = Config(
        id = 35, title = "低层翠野", subtitle = "Low · 地面踏板",
        theme = PlatformerTheme.GRASS, tilesetPack = PlatformerTilesetPack.GOODLY,
        skyTop = 0xFF87CEEB, skyBottom = 0xFFB0E0FF,
        seriesId = "tier_low", seriesOrder = 1,
        segments = tierScript(
            listOf(
                PlatformerSegmentLibrary.SegmentKind.ENTRY,
                PlatformerSegmentLibrary.SegmentKind.LOW_PLATEAU,
                PlatformerSegmentLibrary.SegmentKind.GAP,
                PlatformerSegmentLibrary.SegmentKind.LOW_PLATEAU,
                PlatformerSegmentLibrary.SegmentKind.STEPS,
                PlatformerSegmentLibrary.SegmentKind.LOW_PLATEAU,
                PlatformerSegmentLibrary.SegmentKind.SPRING_VAULT,
                PlatformerSegmentLibrary.SegmentKind.FINALE,
            ),
        ),
    )

    private fun tierLowMetal() = Config(
        id = 36, title = "低层钢轨", subtitle = "Low · 金属踏板",
        theme = PlatformerTheme.METAL, tilesetPack = PlatformerTilesetPack.GOODLY,
        skyTop = 0xFF2A2A35, skyBottom = 0xFF3D3D4A,
        seriesId = "tier_low", seriesOrder = 2,
        segments = tierScript(
            listOf(
                PlatformerSegmentLibrary.SegmentKind.ENTRY,
                PlatformerSegmentLibrary.SegmentKind.LOW_PLATEAU,
                PlatformerSegmentLibrary.SegmentKind.TRAP_LANE,
                PlatformerSegmentLibrary.SegmentKind.LOW_PLATEAU,
                PlatformerSegmentLibrary.SegmentKind.GAP,
                PlatformerSegmentLibrary.SegmentKind.LOW_PLATEAU,
                PlatformerSegmentLibrary.SegmentKind.ENEMY_ROOM,
                PlatformerSegmentLibrary.SegmentKind.FINALE,
            ),
            trap = 2,
        ),
    )

    private fun tierLowDesert() = Config(
        id = 37, title = "低层赤道", subtitle = "Low · 沙漠踏板",
        theme = PlatformerTheme.DESERT, tilesetPack = PlatformerTilesetPack.DESERT_PACK,
        skyTop = 0xFFF4A460, skyBottom = 0xFFFFE4B5,
        seriesId = "tier_low", seriesOrder = 3,
        segments = tierScript(
            listOf(
                PlatformerSegmentLibrary.SegmentKind.ENTRY,
                PlatformerSegmentLibrary.SegmentKind.LOW_PLATEAU,
                PlatformerSegmentLibrary.SegmentKind.FORK,
                PlatformerSegmentLibrary.SegmentKind.LOW_PLATEAU,
                PlatformerSegmentLibrary.SegmentKind.GAP,
                PlatformerSegmentLibrary.SegmentKind.LOW_PLATEAU,
                PlatformerSegmentLibrary.SegmentKind.SPRING_VAULT,
                PlatformerSegmentLibrary.SegmentKind.FINALE,
            ),
        ),
    )

    private fun tierMidSpooky() = Config(
        id = 38, title = "中层幽廊", subtitle = "Mid · 廊桥浮岛",
        theme = PlatformerTheme.SPOOKY, tilesetPack = PlatformerTilesetPack.GOODLY,
        skyTop = 0xFF0D1B2A, skyBottom = 0xFF1B263B,
        seriesId = "tier_mid", seriesOrder = 1,
        segments = tierScript(
            listOf(
                PlatformerSegmentLibrary.SegmentKind.ENTRY,
                PlatformerSegmentLibrary.SegmentKind.MID_BRIDGE,
                PlatformerSegmentLibrary.SegmentKind.GAP,
                PlatformerSegmentLibrary.SegmentKind.MID_BRIDGE,
                PlatformerSegmentLibrary.SegmentKind.TRAP_LANE,
                PlatformerSegmentLibrary.SegmentKind.MID_BRIDGE,
                PlatformerSegmentLibrary.SegmentKind.ENEMY_ROOM,
                PlatformerSegmentLibrary.SegmentKind.FINALE,
            ),
            trap = 3,
        ),
    )

    private fun tierMidIce() = Config(
        id = 39, title = "中层冰桥", subtitle = "Mid · 冰原廊桥",
        theme = PlatformerTheme.ICE, tilesetPack = PlatformerTilesetPack.WINTER_PACK,
        skyTop = 0xFFB0D4E8, skyBottom = 0xFFE8F4FC,
        seriesId = "tier_mid", seriesOrder = 2,
        segments = tierScript(
            listOf(
                PlatformerSegmentLibrary.SegmentKind.ENTRY,
                PlatformerSegmentLibrary.SegmentKind.MID_BRIDGE,
                PlatformerSegmentLibrary.SegmentKind.STEPS,
                PlatformerSegmentLibrary.SegmentKind.MID_BRIDGE,
                PlatformerSegmentLibrary.SegmentKind.SPRING_VAULT,
                PlatformerSegmentLibrary.SegmentKind.MID_BRIDGE,
                PlatformerSegmentLibrary.SegmentKind.GAP,
                PlatformerSegmentLibrary.SegmentKind.FINALE,
            ),
        ),
    )

    private fun tierMidForest() = Config(
        id = 40, title = "中层林海", subtitle = "Mid · 森林廊桥",
        theme = PlatformerTheme.PACK_FOREST, tilesetPack = PlatformerTilesetPack.FOREST_PACK,
        skyTop = 0xFF6BA3C7, skyBottom = 0xFFB8D9E8,
        seriesId = "tier_mid", seriesOrder = 3,
        segments = tierScript(
            listOf(
                PlatformerSegmentLibrary.SegmentKind.ENTRY,
                PlatformerSegmentLibrary.SegmentKind.MID_BRIDGE,
                PlatformerSegmentLibrary.SegmentKind.FORK,
                PlatformerSegmentLibrary.SegmentKind.MID_BRIDGE,
                PlatformerSegmentLibrary.SegmentKind.ENEMY_ROOM,
                PlatformerSegmentLibrary.SegmentKind.MID_BRIDGE,
                PlatformerSegmentLibrary.SegmentKind.TRAP_LANE,
                PlatformerSegmentLibrary.SegmentKind.FINALE,
            ),
        ),
    )

    private fun tierMidGraveyard() = Config(
        id = 41, title = "中层墓廊", subtitle = "Mid · 墓园廊桥",
        theme = PlatformerTheme.PACK_GRAVEYARD, tilesetPack = PlatformerTilesetPack.GRAVEYARD_PACK,
        skyTop = 0xFF1A1A2E, skyBottom = 0xFF3D3D5C,
        seriesId = "tier_mid", seriesOrder = 4,
        segments = tierScript(
            listOf(
                PlatformerSegmentLibrary.SegmentKind.ENTRY,
                PlatformerSegmentLibrary.SegmentKind.MID_BRIDGE,
                PlatformerSegmentLibrary.SegmentKind.ENEMY_ROOM,
                PlatformerSegmentLibrary.SegmentKind.MID_BRIDGE,
                PlatformerSegmentLibrary.SegmentKind.TRAP_LANE,
                PlatformerSegmentLibrary.SegmentKind.MID_BRIDGE,
                PlatformerSegmentLibrary.SegmentKind.GAP,
                PlatformerSegmentLibrary.SegmentKind.FINALE,
            ),
            trap = 3,
        ),
    )

    private fun tierMidScifi() = Config(
        id = 42, title = "中层量子桥", subtitle = "Mid · 科幻廊桥",
        theme = PlatformerTheme.PACK_SCIFI, tilesetPack = PlatformerTilesetPack.SCIFI_PACK,
        skyTop = 0xFF0A0E27, skyBottom = 0xFF1A237E,
        seriesId = "tier_mid", seriesOrder = 5,
        segments = tierScript(
            listOf(
                PlatformerSegmentLibrary.SegmentKind.ENTRY,
                PlatformerSegmentLibrary.SegmentKind.MID_BRIDGE,
                PlatformerSegmentLibrary.SegmentKind.TRAP_LANE,
                PlatformerSegmentLibrary.SegmentKind.MID_BRIDGE,
                PlatformerSegmentLibrary.SegmentKind.SPRING_VAULT,
                PlatformerSegmentLibrary.SegmentKind.MID_BRIDGE,
                PlatformerSegmentLibrary.SegmentKind.TRAP_LANE,
                PlatformerSegmentLibrary.SegmentKind.FINALE,
            ),
            trap = 3,
        ),
    )

    private fun tierHighGrotto() = Config(
        id = 43, title = "高层溶洞", subtitle = "High · 天路浮岛",
        theme = PlatformerTheme.PACK_GROTTO, tilesetPack = PlatformerTilesetPack.GROTTO_PACK,
        skyTop = 0xFF1B2838, skyBottom = 0xFF2C3E50,
        seriesId = "tier_high", seriesOrder = 1,
        segments = tierScript(
            listOf(
                PlatformerSegmentLibrary.SegmentKind.ENTRY,
                PlatformerSegmentLibrary.SegmentKind.HIGH_SKYWAY,
                PlatformerSegmentLibrary.SegmentKind.TOWER,
                PlatformerSegmentLibrary.SegmentKind.HIGH_SKYWAY,
                PlatformerSegmentLibrary.SegmentKind.SPRING_VAULT,
                PlatformerSegmentLibrary.SegmentKind.HIGH_SKYWAY,
                PlatformerSegmentLibrary.SegmentKind.TRAP_LANE,
                PlatformerSegmentLibrary.SegmentKind.FINALE,
            ),
            trap = 3,
        ),
    )

    private fun tierHighMinimal() = Config(
        id = 44, title = "高层极简", subtitle = "High · 节奏天路",
        theme = PlatformerTheme.PACK_MINIMAL, tilesetPack = PlatformerTilesetPack.MINIMAL_PACK,
        skyTop = 0xFF1565C0, skyBottom = 0xFF42A5F5,
        seriesId = "tier_high", seriesOrder = 2,
        segments = tierScript(
            listOf(
                PlatformerSegmentLibrary.SegmentKind.ENTRY,
                PlatformerSegmentLibrary.SegmentKind.HIGH_SKYWAY,
                PlatformerSegmentLibrary.SegmentKind.GAP,
                PlatformerSegmentLibrary.SegmentKind.HIGH_SKYWAY,
                PlatformerSegmentLibrary.SegmentKind.STEPS,
                PlatformerSegmentLibrary.SegmentKind.HIGH_SKYWAY,
                PlatformerSegmentLibrary.SegmentKind.TRAP_LANE,
                PlatformerSegmentLibrary.SegmentKind.FINALE,
            ),
            hazard = 3,
        ),
    )

    private fun tierHighFortress() = Config(
        id = 45, title = "高层要塞", subtitle = "High · 尖塔天路",
        theme = PlatformerTheme.FORTRESS, tilesetPack = PlatformerTilesetPack.GOODLY,
        skyTop = 0xFF87CEEB, skyBottom = 0xFFB0D4E8,
        seriesId = "tier_high", seriesOrder = 3,
        segments = tierScript(
            listOf(
                PlatformerSegmentLibrary.SegmentKind.ENTRY,
                PlatformerSegmentLibrary.SegmentKind.TOWER,
                PlatformerSegmentLibrary.SegmentKind.HIGH_SKYWAY,
                PlatformerSegmentLibrary.SegmentKind.ENEMY_ROOM,
                PlatformerSegmentLibrary.SegmentKind.HIGH_SKYWAY,
                PlatformerSegmentLibrary.SegmentKind.TOWER,
                PlatformerSegmentLibrary.SegmentKind.SPRING_VAULT,
                PlatformerSegmentLibrary.SegmentKind.FINALE,
            ),
            hazard = 3, trap = 3,
        ),
    )

    private fun tierLowMidMix() = Config(
        id = 46, title = "低中混搭", subtitle = "Low+Mid · 赤岩",
        theme = PlatformerTheme.DESERT, tilesetPack = PlatformerTilesetPack.GOODLY,
        skyTop = 0xFFE8B86D, skyBottom = 0xFFFFF0C8,
        seriesId = "tier_mix", seriesOrder = 1,
        segments = tierScript(
            listOf(
                PlatformerSegmentLibrary.SegmentKind.ENTRY,
                PlatformerSegmentLibrary.SegmentKind.LOW_PLATEAU,
                PlatformerSegmentLibrary.SegmentKind.MID_BRIDGE,
                PlatformerSegmentLibrary.SegmentKind.LOW_PLATEAU,
                PlatformerSegmentLibrary.SegmentKind.MID_BRIDGE,
                PlatformerSegmentLibrary.SegmentKind.FORK,
                PlatformerSegmentLibrary.SegmentKind.MID_BRIDGE,
                PlatformerSegmentLibrary.SegmentKind.FINALE,
            ),
        ),
    )

    private fun tierMidHighMix() = Config(
        id = 47, title = "中高混搭", subtitle = "Mid+High · 量子",
        theme = PlatformerTheme.PACK_SCIFI, tilesetPack = PlatformerTilesetPack.SCIFI_PACK,
        skyTop = 0xFF0A0E27, skyBottom = 0xFF1A237E,
        seriesId = "tier_mix", seriesOrder = 2,
        segments = tierScript(
            listOf(
                PlatformerSegmentLibrary.SegmentKind.ENTRY,
                PlatformerSegmentLibrary.SegmentKind.MID_BRIDGE,
                PlatformerSegmentLibrary.SegmentKind.HIGH_SKYWAY,
                PlatformerSegmentLibrary.SegmentKind.MID_BRIDGE,
                PlatformerSegmentLibrary.SegmentKind.TOWER,
                PlatformerSegmentLibrary.SegmentKind.HIGH_SKYWAY,
                PlatformerSegmentLibrary.SegmentKind.TRAP_LANE,
                PlatformerSegmentLibrary.SegmentKind.FINALE,
            ),
            trap = 3,
        ),
    )

    private fun tierAscentGrass() = Config(
        id = 48, title = "三界攀登·翠", subtitle = "Tier · 低→中→高",
        theme = PlatformerTheme.GRASS, tilesetPack = PlatformerTilesetPack.FOREST_PACK,
        skyTop = 0xFF87CEEB, skyBottom = 0xFFB0E0FF,
        seriesId = "tier_ascent", seriesOrder = 1,
        segments = tierScript(
            listOf(
                PlatformerSegmentLibrary.SegmentKind.ENTRY,
                PlatformerSegmentLibrary.SegmentKind.TIER_ASCENT,
                PlatformerSegmentLibrary.SegmentKind.LOW_PLATEAU,
                PlatformerSegmentLibrary.SegmentKind.TIER_ASCENT,
                PlatformerSegmentLibrary.SegmentKind.MID_BRIDGE,
                PlatformerSegmentLibrary.SegmentKind.HIGH_SKYWAY,
                PlatformerSegmentLibrary.SegmentKind.SPRING_VAULT,
                PlatformerSegmentLibrary.SegmentKind.FINALE,
            ),
        ),
    )

    private fun tierAscentGraveyard() = Config(
        id = 49, title = "三界攀登·墓", subtitle = "Tier · 亡灵天路",
        theme = PlatformerTheme.PACK_GRAVEYARD, tilesetPack = PlatformerTilesetPack.GRAVEYARD_PACK,
        skyTop = 0xFF1A1A2E, skyBottom = 0xFF3D3D5C,
        seriesId = "tier_ascent", seriesOrder = 2,
        segments = tierScript(
            listOf(
                PlatformerSegmentLibrary.SegmentKind.ENTRY,
                PlatformerSegmentLibrary.SegmentKind.TIER_ASCENT,
                PlatformerSegmentLibrary.SegmentKind.ENEMY_ROOM,
                PlatformerSegmentLibrary.SegmentKind.TIER_ASCENT,
                PlatformerSegmentLibrary.SegmentKind.TRAP_LANE,
                PlatformerSegmentLibrary.SegmentKind.HIGH_SKYWAY,
                PlatformerSegmentLibrary.SegmentKind.TOWER,
                PlatformerSegmentLibrary.SegmentKind.FINALE,
            ),
            trap = 3,
        ),
    )

    private fun tierLowMarathon() = Config(
        id = 50, title = "低层马拉松", subtitle = "Low · 长跑",
        theme = PlatformerTheme.PACK_JUNGLE, tilesetPack = PlatformerTilesetPack.JUNGLE_PACK,
        skyTop = 0xFF1B5E20, skyBottom = 0xFF81C784,
        seriesId = "tier_marathon", seriesOrder = 1,
        segments = tierScript(
            listOf(
                PlatformerSegmentLibrary.SegmentKind.ENTRY,
                PlatformerSegmentLibrary.SegmentKind.LOW_PLATEAU,
                PlatformerSegmentLibrary.SegmentKind.LOW_PLATEAU,
                PlatformerSegmentLibrary.SegmentKind.GAP,
                PlatformerSegmentLibrary.SegmentKind.LOW_PLATEAU,
                PlatformerSegmentLibrary.SegmentKind.STEPS,
                PlatformerSegmentLibrary.SegmentKind.LOW_PLATEAU,
                PlatformerSegmentLibrary.SegmentKind.FINALE,
            ),
        ),
    )

    private fun tierHighMarathon() = Config(
        id = 51, title = "高层马拉松", subtitle = "High · 天路长跑",
        theme = PlatformerTheme.PACK_GROTTO, tilesetPack = PlatformerTilesetPack.GROTTO_PACK,
        skyTop = 0xFF1B2838, skyBottom = 0xFF2C3E50,
        seriesId = "tier_marathon", seriesOrder = 2,
        segments = tierScript(
            listOf(
                PlatformerSegmentLibrary.SegmentKind.ENTRY,
                PlatformerSegmentLibrary.SegmentKind.HIGH_SKYWAY,
                PlatformerSegmentLibrary.SegmentKind.TOWER,
                PlatformerSegmentLibrary.SegmentKind.HIGH_SKYWAY,
                PlatformerSegmentLibrary.SegmentKind.TOWER,
                PlatformerSegmentLibrary.SegmentKind.HIGH_SKYWAY,
                PlatformerSegmentLibrary.SegmentKind.SPRING_VAULT,
                PlatformerSegmentLibrary.SegmentKind.FINALE,
            ),
            hazard = 3, trap = 3,
        ),
    )

    private fun tierUltimate52() = Config(
        id = 52, title = "三界终极", subtitle = "Ultimate · 52关终章",
        theme = PlatformerTheme.DESERT, tilesetPack = PlatformerTilesetPack.DESERT_PACK,
        skyTop = 0xFFE8B86D, skyBottom = 0xFFFFF0C8,
        seriesId = "tier_ultimate", seriesOrder = 1,
        segments = tierScript(
            listOf(
                PlatformerSegmentLibrary.SegmentKind.ENTRY,
                PlatformerSegmentLibrary.SegmentKind.LOW_PLATEAU,
                PlatformerSegmentLibrary.SegmentKind.MID_BRIDGE,
                PlatformerSegmentLibrary.SegmentKind.HIGH_SKYWAY,
                PlatformerSegmentLibrary.SegmentKind.TIER_ASCENT,
                PlatformerSegmentLibrary.SegmentKind.TRAP_LANE,
                PlatformerSegmentLibrary.SegmentKind.ENEMY_ROOM,
                PlatformerSegmentLibrary.SegmentKind.FINALE,
            ),
            hazard = 3, trap = 3,
        ),
    )

    private fun script(
        kinds: List<PlatformerSegmentLibrary.SegmentKind>,
        hazard: Int = 2,
        trap: Int = 2,
    ): List<PlatformerSegmentLibrary.SegmentSpec> =
        kinds.map { PlatformerSegmentLibrary.SegmentSpec(it, hazard, trap) }

    private fun chunkGrassMarathon() = Config(
        id = 23, title = "翠野长征", subtitle = "Grass · 片段马拉松",
        theme = PlatformerTheme.GRASS, tilesetPack = PlatformerTilesetPack.GOODLY,
        skyTop = 0xFF87CEEB, skyBottom = 0xFFB0E0FF,
        seriesId = "grass_ext", seriesOrder = 1,
        segments = script(
            listOf(
                PlatformerSegmentLibrary.SegmentKind.ENTRY,
                PlatformerSegmentLibrary.SegmentKind.GAP,
                PlatformerSegmentLibrary.SegmentKind.FORK,
                PlatformerSegmentLibrary.SegmentKind.ENEMY_ROOM,
                PlatformerSegmentLibrary.SegmentKind.SPRING_VAULT,
                PlatformerSegmentLibrary.SegmentKind.TRAP_LANE,
                PlatformerSegmentLibrary.SegmentKind.TOWER,
                PlatformerSegmentLibrary.SegmentKind.FINALE,
            ),
            hazard = 2, trap = 2,
        ),
    )

    private fun chunkMetalGauntlet() = Config(
        id = 24, title = "钢铁试炼", subtitle = "Metal · 机关走廊",
        theme = PlatformerTheme.METAL, tilesetPack = PlatformerTilesetPack.GOODLY,
        skyTop = 0xFF2A2A35, skyBottom = 0xFF3D3D4A,
        seriesId = "metal_ext", seriesOrder = 1,
        segments = script(
            listOf(
                PlatformerSegmentLibrary.SegmentKind.ENTRY,
                PlatformerSegmentLibrary.SegmentKind.TRAP_LANE,
                PlatformerSegmentLibrary.SegmentKind.GAP,
                PlatformerSegmentLibrary.SegmentKind.TOWER,
                PlatformerSegmentLibrary.SegmentKind.TRAP_LANE,
                PlatformerSegmentLibrary.SegmentKind.ENEMY_ROOM,
                PlatformerSegmentLibrary.SegmentKind.SPRING_VAULT,
                PlatformerSegmentLibrary.SegmentKind.FINALE,
            ),
            hazard = 3, trap = 3,
        ),
    )

    private fun chunkDesertRift() = Config(
        id = 25, title = "赤岩裂谷", subtitle = "Desert · 双路线",
        theme = PlatformerTheme.DESERT, tilesetPack = PlatformerTilesetPack.GOODLY,
        skyTop = 0xFFF4A460, skyBottom = 0xFFFFE4B5,
        seriesId = "desert_ext", seriesOrder = 1,
        segments = script(
            listOf(
                PlatformerSegmentLibrary.SegmentKind.ENTRY,
                PlatformerSegmentLibrary.SegmentKind.FORK,
                PlatformerSegmentLibrary.SegmentKind.GAP,
                PlatformerSegmentLibrary.SegmentKind.STEPS,
                PlatformerSegmentLibrary.SegmentKind.ENEMY_ROOM,
                PlatformerSegmentLibrary.SegmentKind.SPRING_VAULT,
                PlatformerSegmentLibrary.SegmentKind.FORK,
                PlatformerSegmentLibrary.SegmentKind.FINALE,
            ),
        ),
    )

    private fun chunkSpookyMaze() = Config(
        id = 26, title = "幽林迷径", subtitle = "Spooky · 迷宫段",
        theme = PlatformerTheme.SPOOKY, tilesetPack = PlatformerTilesetPack.GOODLY,
        skyTop = 0xFF0D1B2A, skyBottom = 0xFF1B263B,
        seriesId = "spooky_ext", seriesOrder = 1,
        segments = script(
            listOf(
                PlatformerSegmentLibrary.SegmentKind.ENTRY,
                PlatformerSegmentLibrary.SegmentKind.TOWER,
                PlatformerSegmentLibrary.SegmentKind.TRAP_LANE,
                PlatformerSegmentLibrary.SegmentKind.FORK,
                PlatformerSegmentLibrary.SegmentKind.ENEMY_ROOM,
                PlatformerSegmentLibrary.SegmentKind.GAP,
                PlatformerSegmentLibrary.SegmentKind.TOWER,
                PlatformerSegmentLibrary.SegmentKind.FINALE,
            ),
            trap = 3,
        ),
    )

    private fun chunkIceRhythm() = Config(
        id = 27, title = "冰原节拍", subtitle = "Ice · 节奏跳跃",
        theme = PlatformerTheme.ICE, tilesetPack = PlatformerTilesetPack.GOODLY,
        skyTop = 0xFFB0D4E8, skyBottom = 0xFFE8F4FC,
        seriesId = "ice_ext", seriesOrder = 1,
        segments = script(
            listOf(
                PlatformerSegmentLibrary.SegmentKind.ENTRY,
                PlatformerSegmentLibrary.SegmentKind.STEPS,
                PlatformerSegmentLibrary.SegmentKind.GAP,
                PlatformerSegmentLibrary.SegmentKind.STEPS,
                PlatformerSegmentLibrary.SegmentKind.SPRING_VAULT,
                PlatformerSegmentLibrary.SegmentKind.TRAP_LANE,
                PlatformerSegmentLibrary.SegmentKind.GAP,
                PlatformerSegmentLibrary.SegmentKind.FINALE,
            ),
            hazard = 3,
        ),
    )

    private fun chunkForestDeep() = Config(
        id = 28, title = "林海深处", subtitle = "Forest Pack · 片段关",
        theme = PlatformerTheme.PACK_FOREST, tilesetPack = PlatformerTilesetPack.FOREST_PACK,
        skyTop = 0xFF6BA3C7, skyBottom = 0xFFB8D9E8,
        seriesId = "forest_ext", seriesOrder = 1,
        segments = script(
            listOf(
                PlatformerSegmentLibrary.SegmentKind.ENTRY,
                PlatformerSegmentLibrary.SegmentKind.FORK,
                PlatformerSegmentLibrary.SegmentKind.ENEMY_ROOM,
                PlatformerSegmentLibrary.SegmentKind.GAP,
                PlatformerSegmentLibrary.SegmentKind.TOWER,
                PlatformerSegmentLibrary.SegmentKind.TRAP_LANE,
                PlatformerSegmentLibrary.SegmentKind.SPRING_VAULT,
                PlatformerSegmentLibrary.SegmentKind.FINALE,
            ),
        ),
    )

    private fun chunkGraveyardSiege() = Config(
        id = 29, title = "墓园围攻", subtitle = "Graveyard · 亡灵段",
        theme = PlatformerTheme.PACK_GRAVEYARD, tilesetPack = PlatformerTilesetPack.GRAVEYARD_PACK,
        skyTop = 0xFF1A1A2E, skyBottom = 0xFF3D3D5C,
        seriesId = "graveyard_ext", seriesOrder = 1,
        segments = script(
            listOf(
                PlatformerSegmentLibrary.SegmentKind.ENTRY,
                PlatformerSegmentLibrary.SegmentKind.ENEMY_ROOM,
                PlatformerSegmentLibrary.SegmentKind.TRAP_LANE,
                PlatformerSegmentLibrary.SegmentKind.GAP,
                PlatformerSegmentLibrary.SegmentKind.TOWER,
                PlatformerSegmentLibrary.SegmentKind.ENEMY_ROOM,
                PlatformerSegmentLibrary.SegmentKind.TRAP_LANE,
                PlatformerSegmentLibrary.SegmentKind.FINALE,
            ),
            trap = 3,
        ),
    )

    private fun chunkScifiRelay() = Config(
        id = 30, title = "量子中继", subtitle = "Sci-Fi · 激光链",
        theme = PlatformerTheme.PACK_SCIFI, tilesetPack = PlatformerTilesetPack.SCIFI_PACK,
        skyTop = 0xFF0A0E27, skyBottom = 0xFF1A237E,
        seriesId = "scifi_ext", seriesOrder = 1,
        segments = script(
            listOf(
                PlatformerSegmentLibrary.SegmentKind.ENTRY,
                PlatformerSegmentLibrary.SegmentKind.TRAP_LANE,
                PlatformerSegmentLibrary.SegmentKind.SPRING_VAULT,
                PlatformerSegmentLibrary.SegmentKind.TRAP_LANE,
                PlatformerSegmentLibrary.SegmentKind.GAP,
                PlatformerSegmentLibrary.SegmentKind.ENEMY_ROOM,
                PlatformerSegmentLibrary.SegmentKind.TRAP_LANE,
                PlatformerSegmentLibrary.SegmentKind.FINALE,
            ),
            trap = 3,
        ),
    )

    private fun chunkGrottoDescent() = Config(
        id = 31, title = "溶洞下潜", subtitle = "Grotto · 竖井段",
        theme = PlatformerTheme.PACK_GROTTO, tilesetPack = PlatformerTilesetPack.GROTTO_PACK,
        skyTop = 0xFF1B2838, skyBottom = 0xFF2C3E50,
        seriesId = "grotto_ext", seriesOrder = 1,
        segments = script(
            listOf(
                PlatformerSegmentLibrary.SegmentKind.ENTRY,
                PlatformerSegmentLibrary.SegmentKind.TOWER,
                PlatformerSegmentLibrary.SegmentKind.GAP,
                PlatformerSegmentLibrary.SegmentKind.TOWER,
                PlatformerSegmentLibrary.SegmentKind.ENEMY_ROOM,
                PlatformerSegmentLibrary.SegmentKind.SPRING_VAULT,
                PlatformerSegmentLibrary.SegmentKind.TOWER,
                PlatformerSegmentLibrary.SegmentKind.FINALE,
            ),
        ),
    )

    private fun chunkMinimalSprint() = Config(
        id = 32, title = "极简狂飙", subtitle = "Minimal · 高速段",
        theme = PlatformerTheme.PACK_MINIMAL, tilesetPack = PlatformerTilesetPack.MINIMAL_PACK,
        skyTop = 0xFF1565C0, skyBottom = 0xFF42A5F5,
        seriesId = "minimal_ext", seriesOrder = 1,
        segments = script(
            listOf(
                PlatformerSegmentLibrary.SegmentKind.ENTRY,
                PlatformerSegmentLibrary.SegmentKind.STEPS,
                PlatformerSegmentLibrary.SegmentKind.GAP,
                PlatformerSegmentLibrary.SegmentKind.STEPS,
                PlatformerSegmentLibrary.SegmentKind.GAP,
                PlatformerSegmentLibrary.SegmentKind.SPRING_VAULT,
                PlatformerSegmentLibrary.SegmentKind.TRAP_LANE,
                PlatformerSegmentLibrary.SegmentKind.FINALE,
            ),
            hazard = 3, trap = 2,
        ),
    )

    private fun chunkFortressAssault() = Config(
        id = 33, title = "要塞突击", subtitle = "Fortress · 综合段",
        theme = PlatformerTheme.FORTRESS, tilesetPack = PlatformerTilesetPack.GOODLY,
        skyTop = 0xFF87CEEB, skyBottom = 0xFFB0D4E8,
        seriesId = "fortress_ext", seriesOrder = 1,
        segments = script(
            listOf(
                PlatformerSegmentLibrary.SegmentKind.ENTRY,
                PlatformerSegmentLibrary.SegmentKind.ENEMY_ROOM,
                PlatformerSegmentLibrary.SegmentKind.FORK,
                PlatformerSegmentLibrary.SegmentKind.TRAP_LANE,
                PlatformerSegmentLibrary.SegmentKind.TOWER,
                PlatformerSegmentLibrary.SegmentKind.ENEMY_ROOM,
                PlatformerSegmentLibrary.SegmentKind.SPRING_VAULT,
                PlatformerSegmentLibrary.SegmentKind.FINALE,
            ),
            hazard = 3, trap = 3,
        ),
    )

    private fun chunkCanyonUltimate() = Config(
        id = 34, title = "峡谷终极", subtitle = "Ultimate · 全片段",
        theme = PlatformerTheme.DESERT, tilesetPack = PlatformerTilesetPack.DESERT_PACK,
        skyTop = 0xFFE8B86D, skyBottom = 0xFFFFF0C8,
        seriesId = "ultimate", seriesOrder = 1,
        segments = script(
            listOf(
                PlatformerSegmentLibrary.SegmentKind.ENTRY,
                PlatformerSegmentLibrary.SegmentKind.GAP,
                PlatformerSegmentLibrary.SegmentKind.FORK,
                PlatformerSegmentLibrary.SegmentKind.TOWER,
                PlatformerSegmentLibrary.SegmentKind.TRAP_LANE,
                PlatformerSegmentLibrary.SegmentKind.ENEMY_ROOM,
                PlatformerSegmentLibrary.SegmentKind.SPRING_VAULT,
                PlatformerSegmentLibrary.SegmentKind.FINALE,
            ),
            hazard = 3, trap = 3,
        ),
    )
}
