package com.example.funlife.ui.screens.pacmaze.cosmetic

import com.example.funlife.ui.screens.pacmaze.character.PacMazeCharacterId

enum class BodyTier(val scaleMul: Float, val speedMul: Float, val label: String) {
    S(0.85f, 1.05f, "S"),
    M(1.0f, 1.0f, "M"),
    L(1.2f, 0.92f, "L"),
    XL(1.4f, 0.84f, "XL"),
}

enum class SkinStyleFamily(val label: String) {
    LINE_ART("线条"),
    CHIBI("可爱"),
    RETRO("复古"),
    CYBER("赛博"),
    INK("国风"),
    FOOD("美食"),
    STEAM("蒸汽"),
    OCEAN("海底"),
    IKUN("ikun"),
}

enum class PacMazeSkinId(
    val storageKey: String,
    val displayName: String,
    val subtitle: String,
    val emoji: String,
) {
    // —— 线条系列 ——
    LINE_PUPPY("line_puppy", "线条小狗", "手绘简笔 · 治愈闯关", "🐕"),
    LINE_KITTY("line_kitty", "线条小猫", "翘尾胡须 · 轻巧穿梭", "🐱"),
    LINE_BUNNY("line_bunny", "线条小兔", "长耳弹跳 · 软萌过关", "🐰"),
    LINE_PANDA("line_panda", "线条熊猫", "圆耳黑眼圈 · 憨态可掬", "🐼"),
    LINE_FOX("line_fox", "线条小狐", "蓬松尾尖 · 机灵探路", "🦊"),
    LINE_BEAR("line_bear", "线条小熊", "圆耳厚掌 · 稳重前行", "🐻"),
    LINE_PENGUIN("line_penguin", "线条企鹅", "圆肚短翅 · 摇摆滑行", "🐧"),
    LINE_OWL("line_owl", "线条猫头鹰", "大圆眼 · 夜行眨眼", "🦉"),
    LINE_HEDGEHOG("line_hedgehog", "线条刺猬", "背刺起伏 · 小短腿奔", "🦔"),
    LINE_SHIBA("line_shiba", "线条柴犬", "卷尾吐舌 · 元气摇尾", "🐕‍🦺"),
    LINE_OTTER("line_otter", "线条水獭", "抱鱼划水 · 流线摆尾", "🦦"),
    LINE_KOALA("line_koala", "线条考拉", "抱枝慢摇 · 大耳憨眠", "🐨"),

    // —— 深海系列 ——
    SEA_SHARK("sea_shark", "流线小鲨", "摆尾穿梭 · 深海猎手", "🦈"),
    SEA_CLOWNFISH("sea_clownfish", "小丑鱼", "橙白条纹 · 珊瑚游侠", "🐠"),
    SEA_JELLYFISH("sea_jellyfish", "梦幻水母", "伞体脉动 · 触须轻飘", "🪼"),
    SEA_OCTOPUS("sea_octopus", "小章鱼", "八足划波 · 软萌潜行", "🐙"),
    SEA_TURTLE("sea_turtle", "海龟游侠", "稳壳划水 · 慢游致远", "🐢"),
    SEA_MANTA("sea_manta", "魔鬼鱼", "宽翼滑翔 · 深海翱翔", "🐋"),
    SEA_SEAHORSE("sea_seahorse", "海马精灵", "S 形轻卷 · 随波摇曳", "🐴"),
    SEA_DOLPHIN("sea_dolphin", "微笑海豚", "尾鳍拍浪 · 灵动伴游", "🐬"),
    SEA_SQUID("sea_squid", "荧光乌贼", "伞体脉动 · 触须星点", "🦑"),
    SEA_ANGLER("sea_angler", "灯笼鱼", "柔光灯笼 · 深渊引路", "🔦"),
    SEA_HERMIT("sea_hermit", "寄居蟹", "螺壳护身 · 钳舞横行", "🦀"),
    SEA_STARFISH("sea_starfish", "海星精灵", "五腕旋转 · 吸盘爬行", "⭐"),
    SEA_EEL("sea_eel", "电鳗闪击", "S 形电纹 · 尾扫疾驰", "⚡"),
    SEA_SUNFISH("sea_sunfish", "翻车鱼", "扁圆呆萌 · 侧鳍慢摆", "🐟"),

    // —— 国风系列 ——
    INK_DROP_SPIRIT("ink_drop_spirit", "墨滴小妖", "墨汁甩须 · 留白点睛", "🖋️"),
    INK_PAPER_BIRD("ink_paper_bird", "剪纸雀", "折纸镂空 · 翅振轻盈", "🕊️"),
    INK_LION_DANCE("ink_lion_dance", "舞狮头豆", "绒球狮首 · 眨眼摆须", "🦁"),
    INK_PORCELAIN("ink_porcelain", "瓷娃灵", "青花裂纹 · 红晕瓷肌", "🏺"),
    INK_KYLIN("ink_kylin", "麒麟幼灵", "云纹角尖 · 瑞兽轻跃", "🦄"),
    INK_FAN_FAIRY("ink_fan_fairy", "团扇仙", "折扇开合 · 绢面飘舞", "🪭"),
    INK_LOTUS_BUD("ink_lotus_bud", "莲蕊童", "荷瓣层叠 · 莲蓬轻摇", "🪷"),
    INK_SHADOW_PUPPET("ink_shadow_puppet", "皮影戏偶", "镂空剪影 · 关节提线", "🎭"),
    SCHOLAR("scholar", "小书童", "青衫束发 · 庭院游侠", "📚"),
    LANTERN_FOX("lantern_fox", "提灯小狐", "宫灯引路 · 园林灵物", "🏮"),

    // —— 赛博系列 ——
    CYBER_HOLO_CAT("cyber_holo_cat", "全息猫", "半透明体 · 扫描闪烁", "🐱"),
    CYBER_GLITCH_CUBE("cyber_glitch_cube", "故障方块", "像素拼合 · glitch 抖动", "📦"),
    CYBER_MAGLEV_ORB("cyber_maglev_orb", "磁浮球", "悬浮环带 · 能量核心", "🔮"),
    CYBER_WIRE_WORM("cyber_wire_worm", "数据线虫", "插头尾梢 · 线缆缠绕", "🔌"),
    CYBER_DRONE_BEE("cyber_drone_bee", "巡逻蜂", "四旋翼闪 · 扫描复眼", "🐝"),
    CYBER_NEON_SNAKE("cyber_neon_snake", "霓虹蛇", "S 形光带 · 鳞片闪烁", "🐍"),
    CYBER_CHIP_MONKEY("cyber_chip_monkey", "芯片猿", "电路纹路 · 尾巴数据线", "🐒"),
    CYBER_LASER_BEETLE("cyber_laser_beetle", "镭射甲虫", "硬壳反光 · 激光触须", "🪲"),
    DATA_CORE("data_core", "数据核心", "六边芯片 · 霓虹拖尾", "💠"),

    // —— 怪趣零食 ——
    FOOD_MOCHI("food_mochi", "麻薯团子", "软糯团子 · 内馅微露", "🍡"),
    FOOD_CHILI("food_chili", "辣椒侠", "火焰眉睫 · 蹦跳火辣", "🌶️"),
    FOOD_SUSHI("food_sushi", "寿司卷精", "米粒圆卷 · 鱼生顶饰", "🍣"),
    FOOD_POPCORN("food_popcorn", "爆米花球", "蓬松米壳 · 米花蹦出", "🍿"),
    FOOD_TANGYUAN("food_tangyuan", "汤圆精", "滚圆白团 · 芝麻馅笑", "⚪"),
    FOOD_DUMPLING("food_dumpling", "饺子侠", "褶边元宝 · 热气腾腾", "🥟"),
    FOOD_MANGO_PUDDING("food_mango_pudding", "芒果布丁", "Q 弹橙黄 · 布丁轻颤", "🍮"),
    FOOD_DONUT("food_donut", "甜甜圈精", "糖霜彩针 · 中空蹦跳", "🍩"),
    FOOD_CHICK_DAZE("food_chick_daze", "呆脸小鸡", "中分巨眼 · 经典呆脸", "🐤"),
    FOOD_CHICK_BALLER("food_chick_baller", "篮球小鸡", "中分运球 · 球衣加身", "🏀"),
    FOOD_CHICK_WALKER("food_chick_walker", "行走小鸡", "四帧步态 · 运球向左", "🚶"),
    FOOD_CHICK_WALKER_PRO_MAX("food_chick_walker_pro_max", "行走小鸡 Pro Max", "云端多段动画 · 专属攻击技", "🐔"),
    FOOD_XIA_WALK("xia_walk", "小侠", "云端行走 · 61帧步态", "🦸"),
    FOOD_MOUSE_WALK("laoshu_walk", "行走老鼠", "云端行走 · 61帧步态", "🐭"),
    FOOD_QINGTING_WALK("qinting_walk", "倾听侠", "云端行走 · 61帧步态", "🎧"),
    FOOD_MOSQUITO_WALK("wenzi_walk", "蚊子精", "云端行走 · 61帧步态", "🦟"),
    FOOD_TOUSHI_WALK("toushi_walk", "投食侠", "云端行走 · 61帧步态", "🍱"),
    CANDY_SPIRIT("candy_spirit", "糖纸精灵", "彩虹糖纸 · 弹跳闯关", "🍬"),
    BUBBLE_SLIME("bubble_slime", "气泡史莱姆", "咕嘟冒泡 · 弹性过关", "🫧"),
    NOODLE_PHANTOM("noodle_phantom", "拉面精", "面条成精 · 晃晃悠悠", "🍜"),

    // —— 典藏 ——
    CLASSIC_PAC("classic_pac", "经典豆人", "街机原味 · 张嘴吃豆", "🟡"),
    GEAR_MOLE("gear_mole", "发条鼹鼠", "黄铜齿轮 · 挖地突进", "⚙️"),
    ;

    fun isLineArt(): Boolean = storageKey.startsWith("line_")

    fun isOcean(): Boolean = storageKey.startsWith("sea_")

    fun legacyCharacterId(): PacMazeCharacterId? = when (this) {
        CLASSIC_PAC -> PacMazeCharacterId.CLASSIC_PAC
        SCHOLAR -> PacMazeCharacterId.SCHOLAR
        LANTERN_FOX -> PacMazeCharacterId.LANTERN_FOX
        CANDY_SPIRIT -> PacMazeCharacterId.CANDY_SPIRIT
        DATA_CORE -> PacMazeCharacterId.DATA_CORE
        BUBBLE_SLIME -> PacMazeCharacterId.BUBBLE_SLIME
        NOODLE_PHANTOM -> PacMazeCharacterId.NOODLE_PHANTOM
        GEAR_MOLE -> PacMazeCharacterId.GEAR_MOLE
        else -> null
    }

    fun hasPowerAura(): Boolean = this == DATA_CORE || this == CYBER_MAGLEV_ORB

    fun bodyTierLabel(): String = PacMazeCosmeticCatalog.bodyTier(this).label

    companion object {
        val selectable: List<PacMazeSkinId> = entries.filter { it != CLASSIC_PAC }.let { list ->
            // 经典豆人置顶展示
            listOf(CLASSIC_PAC) + list.filter { it != CLASSIC_PAC }
        }

        fun fromStorage(raw: String): PacMazeSkinId =
            entries.firstOrNull { it.storageKey == raw } ?: CLASSIC_PAC

        fun fromLegacy(characterId: PacMazeCharacterId): PacMazeSkinId = when (characterId) {
            PacMazeCharacterId.CLASSIC_PAC -> CLASSIC_PAC
            PacMazeCharacterId.SCHOLAR -> SCHOLAR
            PacMazeCharacterId.LANTERN_FOX -> LANTERN_FOX
            PacMazeCharacterId.CANDY_SPIRIT -> CANDY_SPIRIT
            PacMazeCharacterId.DATA_CORE -> DATA_CORE
            PacMazeCharacterId.BUBBLE_SLIME -> BUBBLE_SLIME
            PacMazeCharacterId.NOODLE_PHANTOM -> NOODLE_PHANTOM
            PacMazeCharacterId.GEAR_MOLE -> GEAR_MOLE
        }
    }
}

enum class PacMazeTrailId(
    val storageKey: String,
    val displayName: String,
    val emoji: String,
) {
    // 丝带系
    RIBBON_FLOW("ribbon_flow", "流光丝带", "🌊"),
    RIBBON_SAKURA("ribbon_sakura", "樱落流光", "🌸"),
    RIBBON_AURORA("ribbon_aurora", "极光飘带", "🌌"),
    RIBBON_PHOENIX("ribbon_phoenix", "炽焰流羽", "🔥"),
    RIBBON_SOUL("ribbon_soul", "灵魄丝带", "💫"),
    RIBBON_JADE("ribbon_jade", "翠玉流光", "🍃"),
    RIBBON_CINNABAR("ribbon_cinnabar", "朱砂流焰", "🧨"),
    RIBBON_CELADON("ribbon_celadon", "青瓷雨丝", "🏺"),
    RIBBON_VIOLET("ribbon_violet", "紫电蔓藤", "🌿"),
    RIBBON_GINKGO("ribbon_ginkgo", "银杏落叶", "🍂"),
    RIBBON_MINT_BUBBLE("ribbon_mint_bubble", "薄荷气泡", "🫧"),
    RIBBON_NIGHT_INK("ribbon_night_ink", "暗夜墨痕", "🌑"),

    // 粒子系
    PETAL_SHOWER("petal_shower", "樱花瓣雨", "🌸"),
    NOTE_HOP("note_hop", "音符跳跃", "🎵"),
    CANDY_CRUMB("candy_crumb", "糖果碎屑", "🍬"),
    SNOW_SWIRL("snow_swirl", "雪花旋舞", "❄️"),

    // 几何科技系
    HEX_HONEY("hex_honey", "六边蜂巢", "⬡"),
    DATA_CASCADE("data_cascade", "数据瀑布", "💾"),
    RADAR_SWEEP("radar_sweep", "雷达扫描", "📡"),
    CUBE_SHATTER("cube_shatter", "立方碎裂", "🧊"),

    // 足迹系
    PAW_PRINT("paw_print", "爪印留痕", "🐾"),
    RIPPLE_STEP("ripple_step", "涟漪踏步", "💧"),

    // 经典特效
    NEON_PIXEL("neon_pixel", "霓虹方格", "▦"),
    ION_WAKE("ion_wake", "离子尾焰", "⚡"),
    GHOST_ECHO("ghost_echo", "残影 Echo", "👤"),
    STAR_COMET("star_comet", "星彗尾", "✨"),
    NONE("none", "无拖尾", "—"),
    ;

    companion object {
        val selectable: List<PacMazeTrailId> = listOf(
            RIBBON_FLOW, RIBBON_SAKURA, RIBBON_AURORA, RIBBON_PHOENIX, RIBBON_SOUL, RIBBON_JADE,
            RIBBON_CINNABAR, RIBBON_CELADON, RIBBON_VIOLET, RIBBON_GINKGO, RIBBON_MINT_BUBBLE, RIBBON_NIGHT_INK,
            PETAL_SHOWER, NOTE_HOP, CANDY_CRUMB, SNOW_SWIRL,
            HEX_HONEY, DATA_CASCADE, RADAR_SWEEP, CUBE_SHATTER,
            PAW_PRINT, RIPPLE_STEP,
            GHOST_ECHO, ION_WAKE, STAR_COMET, NEON_PIXEL,
            NONE,
        )

        fun fromStorage(raw: String?): PacMazeTrailId =
            entries.firstOrNull { it.storageKey == raw } ?: NONE
    }
}

data class PacMazeSkinDefinition(
    val id: PacMazeSkinId,
    val styleFamily: SkinStyleFamily,
    val bodyTier: BodyTier,
    val recommendedTrailId: PacMazeTrailId,
)
