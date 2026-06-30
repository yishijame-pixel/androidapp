package com.example.funlife.game.platformer

import com.example.funlife.game.platformer.tmx.PlatformerTmxMap

/** Goodly 2x 图集单格像素尺寸（16px 逻辑放大到 32px）。 */
const val PLATFORMER_TILE_PX = 32

enum class PlatformerTheme {
    GRASS,
    METAL,
    DESERT,
    SPOOKY,
    ICE,
    FORTRESS,
    /** 外部 DesertTileset 素材包 */
    PACK_DESERT,
    /** 外部 WinterTileset 素材包 */
    PACK_WINTER,
    PACK_FOREST,
    PACK_GRAVEYARD,
    PACK_JUNGLE,
    PACK_SCIFI,
    PACK_GROTTO,
    PACK_MINIMAL,
    /** SuperTux 南极冰雪 tileset（platformer_supertux bundle） */
    PACK_SUPERTUX,
}

enum class PlatformerTilesetPack {
    GOODLY,
    DESERT_PACK,
    WINTER_PACK,
    FOREST_PACK,
    GRAVEYARD_PACK,
    JUNGLE_PACK,
    SCIFI_PACK,
    GROTTO_PACK,
    MINIMAL_PACK,
    SUPERTUX,
}

enum class PlatformerCell(val ch: Char) {
    AIR('.'),
    SOLID('#'),
    PLATFORM('-'),
    SPAWN('@'),
    GEM('G'),
    GOAL('O'),
    /** 纯装饰（贴地，无碰撞） */
    DECO('+'),
    /** 可站立木箱/石箱（有碰撞） */
    CRATE('C'),
    /** 远景树木（贴地，无碰撞，绘制在背景层） */
    BACKDROP('T'),
    /** 地刺（无碰撞体积，触碰即死） */
    SPIKE('^'),
    /** 弹簧板（可站立并弹射） */
    SPRING('S'),
    ;

    companion object {
        fun fromChar(c: Char): PlatformerCell? = entries.find { it.ch == c }
    }
}

data class PlatformerLevelDef(
    val id: Int,
    val title: String,
    val subtitle: String,
    val theme: PlatformerTheme,
    val rows: List<String>,
    val skyTop: Long,
    val skyBottom: Long,
    val parallaxHill: Long? = null,
    val tilesetPack: PlatformerTilesetPack = PlatformerTilesetPack.GOODLY,
    val enemySpawns: List<PlatformerEnemySpawn> = emptyList(),
    val trapSpawns: List<PlatformerTrapSpawn> = emptyList(),
    val seriesId: String? = null,
    val seriesOrder: Int = 0,
    /** 非空时从 TMX 资源加载关卡（已废弃独立短关；剧情关请用 campaignSegmentScript + STORY_ROOM）。 */
    val tmxAsset: String? = null,
    /** 企业级片段脚本；非空时由 [PlatformerSegmentLevelFactory] 在运行时烘焙。 */
    val campaignSegmentScript: List<PlatformerSegmentLibrary.SegmentSpec>? = null,
    val useCampaignScroll: Boolean = false,
    val checkpointEverySegments: Int = 0,
    val targetTiles: Int = 0,
    /** SuperTux 滚动关：烘焙片段行（28×14），由 [PlatformerSuperTuxScrollFactory] 绘制。 */
    val supertuxBakedSegments: List<List<String>>? = null,
    /** SuperTux 视觉片段（tile id 矩阵，与 supertuxBakedSegments 对齐）。 */
    val supertuxVisualSegments: List<List<List<Int>>>? = null,
    /** 非滚动关 SuperTux 视觉 tile 行。 */
    val supertuxVisualRows: List<List<Int>>? = null,
    /** SuperTux 金币（像素坐标，来自 .stl）。 */
    val supertuxCoins: List<PlatformerSuperTuxObjectSpawn> = emptyList(),
    /** SuperTux 敌人（像素坐标 + 类型名，来自 .stl）。 */
    val supertuxBadguys: List<PlatformerSuperTuxObjectSpawn> = emptyList(),
    /** BGM 事件 ID，对应 `platformer_sfx/sfx_manifest.json`。 */
    val bgmEventId: String? = null,
)

data class PlatformerSuperTuxObjectSpawn(
    val name: String = "",
    val tx: Int,
    val ty: Int,
)

data class PlatformerCampaignCheckpoint(
    val segmentIndex: Int,
    val absoluteTile: Int,
    /** 沿关卡全长的绝对 X（像素），复活时换算为当前缓冲局部坐标。 */
    val spawnX: Float,
    val spawnY: Float,
)

data class PlatformerGem(
    val x: Float,
    val y: Float,
    var collected: Boolean = false,
)

data class PlatformerPlayer(
    val x: Float,
    val y: Float,
    val vx: Float = 0f,
    val vy: Float = 0f,
    val facingRight: Boolean = true,
    val grounded: Boolean = false,
    val animPhase: Float = 0f,
    /** 正在输入移动或仍有明显水平速度（用于行走动画，比 vx 阈值更可靠）。 */
    val locomoting: Boolean = false,
    /** 空中剩余跳跃次数（落地重置为 1 = 可二段跳）。 */
    val airJumpsLeft: Int = 0,
    val coyoteSec: Float = 0f,
    /** 本次跳跃是否还可被「松键」截断（仅首段跳）。 */
    val jumpActive: Boolean = false,
    val jumpCanCut: Boolean = false,
    val jumpBufferSec: Float = 0f,
    /** 播放死亡序列帧中（播完后再复活/结算）。 */
    val dying: Boolean = false,
    val deathAnimTime: Float = 0f,
    /** 近战攻击动画剩余时长（秒）。 */
    val attackAnimSecLeft: Float = 0f,
    val attackAnimTotalSec: Float = 0f,
    val attackJumpVariant: Boolean = false,
    val attackCooldownSecLeft: Float = 0f,
    /** 远程攻击动画剩余时长（秒）。 */
    val rangedAnimSecLeft: Float = 0f,
    val rangedAnimTotalSec: Float = 0f,
    val rangedJumpVariant: Boolean = false,
    val rangedRunVariant: Boolean = false,
    val rangedClip: PlatformerAnimClipRef? = null,
    val rangedProjectileSpawned: Boolean = false,
    val rangedCooldownSecLeft: Float = 0f,
)

/** 避免 PlatformerTypes 依赖 catalog 包，远程 clip 用 name 存。 */
enum class PlatformerAnimClipRef {
    SHOOT, RUN_SHOOT, JUMP_SHOOT, THROW, BASKETBALL,
    ;

    fun toAnimClip(): com.example.funlife.game.platformer.catalog.PlatformerAnimClip? =
        when (this) {
            SHOOT -> com.example.funlife.game.platformer.catalog.PlatformerAnimClip.SHOOT
            RUN_SHOOT -> com.example.funlife.game.platformer.catalog.PlatformerAnimClip.RUN_SHOOT
            JUMP_SHOOT -> com.example.funlife.game.platformer.catalog.PlatformerAnimClip.JUMP_SHOOT
            THROW -> com.example.funlife.game.platformer.catalog.PlatformerAnimClip.THROW
            BASKETBALL -> null
        }

    companion object {
        fun from(clip: com.example.funlife.game.platformer.catalog.PlatformerAnimClip): PlatformerAnimClipRef? =
            when (clip) {
                com.example.funlife.game.platformer.catalog.PlatformerAnimClip.SHOOT -> SHOOT
                com.example.funlife.game.platformer.catalog.PlatformerAnimClip.RUN_SHOOT -> RUN_SHOOT
                com.example.funlife.game.platformer.catalog.PlatformerAnimClip.JUMP_SHOOT -> JUMP_SHOOT
                com.example.funlife.game.platformer.catalog.PlatformerAnimClip.THROW -> THROW
                else -> null
            }
    }
}

enum class PlatformerPhase {
    PLAYING,
    LEVEL_CLEAR,
    /** 无尽跑酷：死亡结算，不回卷起点。 */
    GAME_OVER,
}

data class PlatformerWorld(
    val level: PlatformerLevelDef,
    val width: Int,
    val height: Int,
    val cells: Array<PlatformerCell>,
    val gems: List<PlatformerGem>,
    val enemies: List<PlatformerEnemy> = emptyList(),
    val traps: List<PlatformerTrap> = emptyList(),
    val projectiles: List<PlatformerProjectile> = emptyList(),
    /** 本帧玩家被弹丸命中（弹丸已从列表移除，供死亡判定）。 */
    val lethalProjectileHit: Boolean = false,
    val player: PlatformerPlayer,
    val characterId: PlatformerCharacterId = PlatformerCharacterId.CHICK_PRO_MAX,
    val cameraX: Float = 0f,
    val phase: PlatformerPhase = PlatformerPhase.PLAYING,
    val gemsCollected: Int = 0,
    val tmx: PlatformerTmxMap? = null,
    val tilePx: Int = PLATFORMER_TILE_PX,
    val goalX: Float? = null,
    val goalY: Float? = null,
    val endlessMode: Boolean = false,
    /** 无尽模式已跑过的地图宽度（格）。 */
    val endlessTilesRun: Int = 0,
    val endlessSegmentIndex: Int = 0,
    val endlessSeed: Long = 0L,
    val endlessScrollSpeed: Float = PlatformerEndlessRunner.BASE_SCROLL_SPEED,
    val endlessBiomeIndex: Int = 0,
    /** 主线滚动缓冲（非无尽）：按脚本追加片段。 */
    val campaignScrollMode: Boolean = false,
    val campaignScript: List<PlatformerSegmentLibrary.SegmentSpec>? = null,
    /** SuperTux 滚动关：烘焙片段（与 [campaignScript] 二选一）。 */
    val campaignBakedSegments: List<List<String>>? = null,
    /** SuperTux 视觉 tile 缓冲（与 cells 同尺寸，0=无贴图）。 */
    val supertuxVisualTiles: IntArray? = null,
    val campaignBakedVisualSegments: List<List<List<Int>>>? = null,
    val campaignScriptIndex: Int = 0,
    val campaignTotalSegments: Int = 0,
    val campaignTilesRun: Int = 0,
    val campaignCheckpoints: List<PlatformerCampaignCheckpoint> = emptyList(),
    val campaignLastCheckpointIndex: Int = -1,
    /** 关卡出生点（局部像素），用于非滚动关死亡回起点。 */
    val levelSpawnX: Float = 0f,
    val levelSpawnY: Float = 0f,
    val hitSparks: List<PlatformerHitSpark> = emptyList(),
    /** 每关天空跟随的下蛋小鸡；null 表示本局未启用。 */
    val skyChick: PlatformerSkyChick? = null,
    val skyEggs: List<PlatformerSkyEgg> = emptyList(),
    val lethalSkyEggHit: Boolean = false,
) {
    val tileF: Float get() = tilePx.toFloat()

    fun cellAt(tx: Int, ty: Int): PlatformerCell {
        if (tx !in 0 until width || ty !in 0 until height) return PlatformerCell.AIR
        return cells[ty * width + tx]
    }

    fun index(tx: Int, ty: Int) = ty * width + tx
}

data class PlatformerInput(
    val left: Boolean = false,
    val right: Boolean = false,
    val jumpPressed: Boolean = false,
    val jumpHeld: Boolean = false,
    val attackPressed: Boolean = false,
    val rangedPressed: Boolean = false,
)
