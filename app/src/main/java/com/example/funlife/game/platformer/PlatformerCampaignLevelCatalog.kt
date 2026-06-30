package com.example.funlife.game.platformer

/**
 * 52 关主线目录：元数据 + [PlatformerCampaignLengthSpec] + 片段脚本 → 关卡 manifest。
 */
object PlatformerCampaignLevelCatalog {

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
        (1..PLATFORMER_CAMPAIGN_LEVEL_COUNT).map(::buildManifest)
    }

    fun buildManifest(levelId: Int): PlatformerLevelDef {
        val budget = PlatformerCampaignLengthSpec.budget(levelId)
        val script = PlatformerCampaignSegmentScripts.scriptFor(levelId)
        return PlatformerSegmentLevelFactory.manifest(
            PlatformerSegmentLevelFactory.BuildConfig(
                meta = meta(levelId),
                budget = budget,
                script = script,
            ),
        )
    }

    fun meta(levelId: Int): LevelMeta = when (levelId) {
        1 -> m(1, "欢迎公园", "Grassland · 浮岛探索", PlatformerTheme.GRASS, PlatformerTilesetPack.GOODLY, 0xFF87CEEB, 0xFFB0E0FF, 0xFF5DAD5D, "classic", 1)
        2 -> m(2, "石牢迷城", "Dungeon · 竖井攀登", PlatformerTheme.METAL, PlatformerTilesetPack.GOODLY, 0xFF2A2A35, 0xFF3D3D4A, null, "classic", 2)
        3 -> m(3, "赤岩峡谷", "Canyon · 台地跳跃", PlatformerTheme.DESERT, PlatformerTilesetPack.GOODLY, 0xFFF4A460, 0xFFFFE4B5, null, "classic", 3)
        4 -> m(4, "幽夜密林", "Spooky · 狭道迷宫", PlatformerTheme.SPOOKY, PlatformerTilesetPack.GOODLY, 0xFF0D1B2A, 0xFF1B263B, null, "classic", 4)
        5 -> m(5, "冰封绝壁", "Ice · 极限连跳", PlatformerTheme.ICE, PlatformerTilesetPack.GOODLY, 0xFFB0D4E8, 0xFFE8F4FC, null, "classic", 5)
        6 -> m(6, "边境要塞", "Fortress · 综合挑战", PlatformerTheme.FORTRESS, PlatformerTilesetPack.GOODLY, 0xFF87CEEB, 0xFFB0D4E8, null, "classic", 6)
        7 -> m(7, "烈日遗迹", "Desert Pack · 机关挑战", PlatformerTheme.PACK_DESERT, PlatformerTilesetPack.DESERT_PACK, 0xFFE8B86D, 0xFFFFF0C8, null, "pack", 1)
        8 -> m(8, "雪原长廊", "Winter Pack · 冰桥", PlatformerTheme.PACK_WINTER, PlatformerTilesetPack.WINTER_PACK, 0xFFB0D4E8, 0xFFE8F4FC, null, "pack", 2)
        9 -> m(9, "翡翠林海", "Forest · 初探", PlatformerTheme.PACK_FOREST, PlatformerTilesetPack.FOREST_PACK, 0xFF87CEEB, 0xFFE0F4FF, null, "pack", 3)
        10 -> m(10, "幽林迷踪", "Forest · 机关强化", PlatformerTheme.PACK_FOREST, PlatformerTilesetPack.FOREST_PACK, 0xFF6BA3C7, 0xFFB8D9E8, null, "pack", 4)
        11 -> m(11, "墓园夜行", "Graveyard · 亡灵", PlatformerTheme.PACK_GRAVEYARD, PlatformerTilesetPack.GRAVEYARD_PACK, 0xFF1A1A2E, 0xFF3D3D5C, null, "pack", 5)
        12 -> m(12, "深渊墓穴", "Graveyard · 高密度", PlatformerTheme.PACK_GRAVEYARD, PlatformerTilesetPack.GRAVEYARD_PACK, 0xFF0D0D18, 0xFF2A2A40, null, "pack", 6)
        13 -> m(13, "丛林遗迹", "Jungle · 藤蔓", PlatformerTheme.PACK_JUNGLE, PlatformerTilesetPack.JUNGLE_PACK, 0xFF2E7D32, 0xFFA5D6A7, null, "pack", 7)
        14 -> m(14, "科幻站台", "Sci-Fi · 激光", PlatformerTheme.PACK_SCIFI, PlatformerTilesetPack.SCIFI_PACK, 0xFF0A0E27, 0xFF1A237E, null, "pack", 8)
        15 -> m(15, "溶洞逃亡", "Grotto · 洞穴", PlatformerTheme.PACK_GROTTO, PlatformerTilesetPack.GROTTO_PACK, 0xFF1B2838, 0xFF2C3E50, null, "pack", 9)
        16 -> m(16, "极简冲刺", "Minimal · 节奏", PlatformerTheme.PACK_MINIMAL, PlatformerTilesetPack.MINIMAL_PACK, 0xFF1565C0, 0xFF42A5F5, null, "pack", 10)
        17 -> m(17, "绿林侠影", "Story · 森林幕", PlatformerTheme.PACK_FOREST, PlatformerTilesetPack.FOREST_PACK, 0xFF1B5E20, 0xFF81C784, null, "story", 1)
        18 -> m(18, "古堡征途 I", "Story · 城堡第一幕", PlatformerTheme.PACK_GRAVEYARD, PlatformerTilesetPack.GRAVEYARD_PACK, 0xFF263238, 0xFF546E7A, null, "story", 2)
        19 -> m(19, "古堡征途 II", "Story · 城堡第二幕", PlatformerTheme.PACK_GRAVEYARD, PlatformerTilesetPack.GRAVEYARD_PACK, 0xFF263238, 0xFF455A64, null, "story", 3)
        20 -> m(20, "古堡征途 III", "Story · 城堡第三幕", PlatformerTheme.PACK_GRAVEYARD, PlatformerTilesetPack.GRAVEYARD_PACK, 0xFF1A1A2E, 0xFF37474F, null, "story", 4)
        21 -> m(21, "古堡终章", "Story · 城堡终幕", PlatformerTheme.PACK_GRAVEYARD, PlatformerTilesetPack.GRAVEYARD_PACK, 0xFF0D0D18, 0xFF263238, null, "story", 5)
        22 -> m(22, "比武大会", "Story · 决赛场", PlatformerTheme.FORTRESS, PlatformerTilesetPack.GOODLY, 0xFF4E342E, 0xFF8D6E63, null, "story", 6)
        23 -> m(23, "翠野长征", "Epic · 片段马拉松", PlatformerTheme.GRASS, PlatformerTilesetPack.GOODLY, 0xFF87CEEB, 0xFFB0E0FF, null, "epic", 1)
        24 -> m(24, "钢铁试炼", "Epic · 机关走廊", PlatformerTheme.METAL, PlatformerTilesetPack.GOODLY, 0xFF2A2A35, 0xFF3D3D4A, null, "epic", 2)
        25 -> m(25, "赤岩裂谷", "Epic · 双路线", PlatformerTheme.DESERT, PlatformerTilesetPack.GOODLY, 0xFFF4A460, 0xFFFFE4B5, null, "epic", 3)
        26 -> m(26, "幽林迷径", "Epic · 迷宫段", PlatformerTheme.SPOOKY, PlatformerTilesetPack.GOODLY, 0xFF0D1B2A, 0xFF1B263B, null, "epic", 4)
        27 -> m(27, "冰原节拍", "Epic · 节奏跳跃", PlatformerTheme.ICE, PlatformerTilesetPack.GOODLY, 0xFFB0D4E8, 0xFFE8F4FC, null, "epic", 5)
        28 -> m(28, "林海深处", "Epic · 森林段", PlatformerTheme.PACK_FOREST, PlatformerTilesetPack.FOREST_PACK, 0xFF6BA3C7, 0xFFB8D9E8, null, "epic", 6)
        29 -> m(29, "墓园围攻", "Epic · 亡灵段", PlatformerTheme.PACK_GRAVEYARD, PlatformerTilesetPack.GRAVEYARD_PACK, 0xFF1A1A2E, 0xFF3D3D5C, null, "epic", 7)
        30 -> m(30, "量子中继", "Epic · 激光链", PlatformerTheme.PACK_SCIFI, PlatformerTilesetPack.SCIFI_PACK, 0xFF0A0E27, 0xFF1A237E, null, "epic", 8)
        31 -> m(31, "溶洞下潜", "Epic · 竖井段", PlatformerTheme.PACK_GROTTO, PlatformerTilesetPack.GROTTO_PACK, 0xFF1B2838, 0xFF2C3E50, null, "epic", 9)
        32 -> m(32, "极简狂飙", "Epic · 高速段", PlatformerTheme.PACK_MINIMAL, PlatformerTilesetPack.MINIMAL_PACK, 0xFF1565C0, 0xFF42A5F5, null, "epic", 10)
        33 -> m(33, "要塞突击", "Epic · 综合段", PlatformerTheme.FORTRESS, PlatformerTilesetPack.GOODLY, 0xFF87CEEB, 0xFFB0D4E8, null, "epic", 11)
        34 -> m(34, "峡谷终极", "Epic · 全片段", PlatformerTheme.DESERT, PlatformerTilesetPack.DESERT_PACK, 0xFFE8B86D, 0xFFFFF0C8, null, "epic", 12)
        35 -> m(35, "低层翠野", "Tier · 地面踏板", PlatformerTheme.GRASS, PlatformerTilesetPack.GOODLY, 0xFF87CEEB, 0xFFB0E0FF, null, "tier_low", 1)
        36 -> m(36, "低层钢轨", "Tier · 金属踏板", PlatformerTheme.METAL, PlatformerTilesetPack.GOODLY, 0xFF2A2A35, 0xFF3D3D4A, null, "tier_low", 2)
        37 -> m(37, "低层赤道", "Tier · 沙漠踏板", PlatformerTheme.DESERT, PlatformerTilesetPack.DESERT_PACK, 0xFFF4A460, 0xFFFFE4B5, null, "tier_low", 3)
        38 -> m(38, "中层幽廊", "Tier · 廊桥浮岛", PlatformerTheme.SPOOKY, PlatformerTilesetPack.GOODLY, 0xFF0D1B2A, 0xFF1B263B, null, "tier_mid", 1)
        39 -> m(39, "中层冰桥", "Tier · 冰原廊桥", PlatformerTheme.ICE, PlatformerTilesetPack.WINTER_PACK, 0xFFB0D4E8, 0xFFE8F4FC, null, "tier_mid", 2)
        40 -> m(40, "中层林海", "Tier · 森林廊桥", PlatformerTheme.PACK_FOREST, PlatformerTilesetPack.FOREST_PACK, 0xFF6BA3C7, 0xFFB8D9E8, null, "tier_mid", 3)
        41 -> m(41, "中层墓廊", "Tier · 墓园廊桥", PlatformerTheme.PACK_GRAVEYARD, PlatformerTilesetPack.GRAVEYARD_PACK, 0xFF1A1A2E, 0xFF3D3D5C, null, "tier_mid", 4)
        42 -> m(42, "中层量子桥", "Tier · 科幻廊桥", PlatformerTheme.PACK_SCIFI, PlatformerTilesetPack.SCIFI_PACK, 0xFF0A0E27, 0xFF1A237E, null, "tier_mid", 5)
        43 -> m(43, "高层溶洞", "Tier · 天路浮岛", PlatformerTheme.PACK_GROTTO, PlatformerTilesetPack.GROTTO_PACK, 0xFF1B2838, 0xFF2C3E50, null, "tier_high", 1)
        44 -> m(44, "高层极简", "Tier · 节奏天路", PlatformerTheme.PACK_MINIMAL, PlatformerTilesetPack.MINIMAL_PACK, 0xFF1565C0, 0xFF42A5F5, null, "tier_high", 2)
        45 -> m(45, "高层要塞", "Tier · 尖塔天路", PlatformerTheme.FORTRESS, PlatformerTilesetPack.GOODLY, 0xFF87CEEB, 0xFFB0D4E8, null, "tier_high", 3)
        46 -> m(46, "低中混搭", "Tier · 低+中", PlatformerTheme.DESERT, PlatformerTilesetPack.GOODLY, 0xFFE8B86D, 0xFFFFF0C8, null, "tier_mix", 1)
        47 -> m(47, "中高混搭", "Tier · 中+高", PlatformerTheme.PACK_SCIFI, PlatformerTilesetPack.SCIFI_PACK, 0xFF0A0E27, 0xFF1A237E, null, "tier_mix", 2)
        48 -> m(48, "三界攀登·翠", "Tier · 低→中→高", PlatformerTheme.GRASS, PlatformerTilesetPack.FOREST_PACK, 0xFF87CEEB, 0xFFB0E0FF, null, "tier_ascent", 1)
        49 -> m(49, "三界攀登·墓", "Tier · 亡灵天路", PlatformerTheme.PACK_GRAVEYARD, PlatformerTilesetPack.GRAVEYARD_PACK, 0xFF1A1A2E, 0xFF3D3D5C, null, "tier_ascent", 2)
        50 -> m(50, "低层马拉松", "Tier · 长跑", PlatformerTheme.PACK_JUNGLE, PlatformerTilesetPack.JUNGLE_PACK, 0xFF1B5E20, 0xFF81C784, null, "tier_marathon", 1)
        51 -> m(51, "高层马拉松", "Tier · 天路长跑", PlatformerTheme.PACK_GROTTO, PlatformerTilesetPack.GROTTO_PACK, 0xFF1B2838, 0xFF2C3E50, null, "tier_marathon", 2)
        52 -> m(52, "三界终极", "Ultimate · 终章", PlatformerTheme.DESERT, PlatformerTilesetPack.DESERT_PACK, 0xFFE8B86D, 0xFFFFF0C8, null, "tier_ultimate", 1)
        else -> m(levelId, "关卡 $levelId", "Campaign", PlatformerTheme.GRASS, PlatformerTilesetPack.GOODLY, 0xFF87CEEB, 0xFFB0E0FF, null, "misc", levelId)
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
