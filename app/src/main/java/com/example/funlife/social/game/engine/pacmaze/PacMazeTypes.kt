package com.example.funlife.social.game.engine.pacmaze

enum class Direction {
    UP, DOWN, LEFT, RIGHT;

    fun delta(): Pair<Int, Int> = when (this) {
        UP -> 0 to -1
        DOWN -> 0 to 1
        LEFT -> -1 to 0
        RIGHT -> 1 to 0
    }

    fun opposite(): Direction = when (this) {
        UP -> DOWN
        DOWN -> UP
        LEFT -> RIGHT
        RIGHT -> LEFT
    }
}

enum class TileType(val code: Int) {
    WALL(0),
    PATH(1),
    PELLET(2),
    POWER(3),
    DOOR(4),
    TUNNEL(5),
    EMPTY(6),
    /** 定时开合的墙体（逻辑格不变，阻挡由 [PacMazeMapDynamics] 决定）。 */
    DYNAMIC_WALL(7),
    /** 能量门：关闭时阻挡玩家，打开时可通行。 */
    ENERGY_GATE(8),
    /** 传送门：逻辑同隧道，主题渲染为虫洞/传送特效。 */
    PORTAL(9),
    /** 青砖墙（院落外围）。 */
    BRICK_WALL(10),
    /** 木质回廊（廊道隔断）。 */
    WOOD_WALL(11),
    /** 瓦片屋檐（建筑顶缘）。 */
    TILE_WALL(12),
    ;

    fun isPelletTile(): Boolean = this == PELLET || this == POWER

    fun isSolidWall(): Boolean = when (this) {
        WALL, BRICK_WALL, WOOD_WALL, TILE_WALL -> true
        else -> false
    }

    fun isWalkableFloor(): Boolean = when (this) {
        PATH, PELLET, POWER, EMPTY, TUNNEL, PORTAL, ENERGY_GATE -> true
        else -> false
    }

    companion object {
        fun isSolidWallCode(code: Int): Boolean =
            entries.any { it.code == code && it.isSolidWall() }
    }
}

enum class GhostMode {
    CHASE, SCATTER, FRIGHTENED, EATEN,
}

enum class PacMazePhase {
    MENU, PLAYING, LEVEL_CLEAR, GAME_OVER, PAUSED,
}

data class PacMazeEntity(
    val id: String,
    val role: String,
    val x: Float,
    val y: Float,
    val direction: Direction?,
    val speed: Float,
    val ghostMode: GhostMode = GhostMode.CHASE,
    /** 渲染用朝向。 */
    val facing: Direction = Direction.RIGHT,
    val inputActive: Boolean = false,
    /** 亚像素速度（格/秒）。 */
    val velX: Float = 0f,
    val velY: Float = 0f,
    /** 输入缓冲的目标方向（提前转向排队）。 */
    val nextDirection: Direction? = null,
    /** 被玩家子弹击中后的眩晕剩余帧数，结束后恢复原本颜色与行为。 */
    val hitStunTicksLeft: Int = 0,
    val ghostKind: GhostKind = GhostKind.STRIKER,
    val ghostSpecialty: GhostSpecialty = GhostSpecialty.NONE,
    /** 伺机者：玩家吃豆后的爆发剩余帧。 */
    val opportunistBurstTicksLeft: Int = 0,
    /** 相位通者：穿墙冷却剩余帧。 */
    val phaseWalkCooldownTicksLeft: Int = 0,
    /** 连续未换格计数，用于脱困。 */
    val ghostStuckTicks: Int = 0,
    /** 上次在三岔路口决策的格子键，避免在格心每帧重选方向导致颤抖。 */
    val ghostDecisionTileKey: Int = -1,
)

data class PacMazeProjectile(
    val id: String,
    val x: Float,
    val y: Float,
    val direction: Direction,
)

/** 敌方机关子弹（炮台发射）。 */
data class PacMazeEnemyBullet(
    val id: String,
    val x: Float,
    val y: Float,
    val direction: Direction,
    val hazardId: String = "",
)

enum class PacMazeHazardKind {
    /** 水平扫描激光（沿行 x 轴扫射）。 */
    LASER_ROW,
    /** 垂直扫描激光（沿列 y 轴扫射）。 */
    LASER_COL,
    /** 固定方向炮台。 */
    TURRET,
}

data class PacMazeHazardDef(
    val id: String,
    val kind: PacMazeHazardKind,
    val x: Int,
    val y: Int,
    val rangeStart: Int,
    val rangeEnd: Int,
    val direction: Direction = Direction.RIGHT,
)

/** 运行时机关状态（扫描位置、开火冷却等）。 */
data class PacMazeHazardState(
    val id: String,
    val scanPos: Float,
    val scanDir: Int = 1,
    val fireCooldown: Int = 0,
    /** true = 激光处于致命相位。 */
    val lethal: Boolean = false,
)

data class PacMazeLevelConfig(
    val id: Int,
    val name: String,
    val width: Int,
    val height: Int,
    val pacSpawn: Pair<Int, Int>,
    val pacSpawnB: Pair<Int, Int>? = null,
    val ghostSpawns: List<PacMazeGhostSpawnDef>,
    val ghostSpeedMul: Float = 1f,
    val aiAggression: Float = 0.8f,
    val markers: List<PacMazeMapMarker> = emptyList(),
    val hazards: List<PacMazeHazardDef> = emptyList(),
    val itemSpawners: List<PacMazeItemSpawnerDef> = emptyList(),
    val starCriteria: PacMazeStarCriteria = PacMazeStarCriteria.defaults(),
    val modeRules: PacMazeModeRules = PacMazeModeRules(),
)

enum class PacMazeMarkerKind { START, CHECKPOINT, EXIT, ITEM_FACTORY }

/** 道具种类（由地图工厂随机产出）。 */
enum class PacMazeItemKind(val id: String, val displayName: String, val emoji: String) {
    MAGNET("magnet", "磁力", "🧲"),
    SHIELD("shield", "护盾", "🛡"),
    FROST("frost", "冰霜", "❄"),
    SPEED("speed", "迅捷", "⚡"),
    DOUBLE("double", "双倍", "✦"),
    CHARGE("charge", "充能", "💥"),
    ;

    companion object {
        fun fromId(raw: String): PacMazeItemKind? =
            entries.firstOrNull { it.id.equals(raw.trim(), ignoreCase = true) }

        val DEFAULT_POOL: List<PacMazeItemKind> = entries.toList()
    }
}

/** 地图上的道具生产装置（静态配置）。 */
data class PacMazeItemSpawnerDef(
    val id: String,
    val x: Int,
    val y: Int,
    val intervalTicks: Int = PacMazeItemConstants.SPAWNER_INTERVAL_TICKS,
    val pool: List<PacMazeItemKind> = PacMazeItemKind.DEFAULT_POOL,
)

/** 生产装置运行时（冷却、脉冲动画）。 */
data class PacMazeItemSpawnerState(
    val id: String,
    val cooldownTicks: Int = 0,
    val pulseTick: Int = 0,
)

/** 地面待拾取道具。 */
data class PacMazeFloorItem(
    val id: String,
    val kind: PacMazeItemKind,
    val x: Int,
    val y: Int,
    val spawnerId: String = "",
    val ticksLeft: Int = PacMazeItemConstants.FLOOR_LIFETIME_TICKS,
)

/** 磁力吸附中的豆子/能量豆（飞向玩家动画）。 */
data class PacMazeMagnetPull(
    val id: String,
    val x: Float,
    val y: Float,
    val sourceX: Int,
    val sourceY: Int,
    val isPower: Boolean,
)

data class PacMazeMapMarker(
    val kind: PacMazeMarkerKind,
    val x: Int,
    val y: Int,
    /** 面板主数字，如 001 */
    val label: String = "",
    /** 角标，如 CP-1 */
    val tag: String = "",
)

data class PacMazeWorldState(
    val tick: Long,
    val levelId: Int,
    val tiles: IntArray,
    val width: Int,
    val height: Int,
    val entities: List<PacMazeEntity>,
    val score: Int,
    val lives: Int,
    val pelletsRemaining: Int,
    val phase: PacMazePhase,
    val rngSeed: Long,
    /** 玩家移动模式（自动滑行 / 完全手控）。 */
    val movementMode: PacMazeMovementMode = PacMazeMovementMode.Default,
    val powerTicksLeft: Int = 0,
    val ghostModeTicksLeft: Int = 0,
    val ghostMode: GhostMode = GhostMode.SCATTER,
    /** 开局倒计时，>0 时幽灵不移动，给玩家辨认角色与熟悉摇杆。 */
    val ghostReleaseTicksLeft: Int = 0,
    /** 吃大能量豆获得的攻击次数。 */
    val attackCharges: Int = 0,
    val projectiles: List<PacMazeProjectile> = emptyList(),
    val attackCooldownTicksLeft: Int = 0,
    /** 动态地图相位（移动墙、主题特效同步）。 */
    val dynamicsTick: Int = 0,
    /** 能量门是否开启（全局门同步，便于主题演出）。 */
    val energyGateOpen: Boolean = true,
    /** 关卡机关定义（静态）。 */
    val hazards: List<PacMazeHazardDef> = emptyList(),
    /** 机关运行时状态。 */
    val hazardStates: List<PacMazeHazardState> = emptyList(),
    /** 敌方机关子弹。 */
    val enemyBullets: List<PacMazeEnemyBullet> = emptyList(),
    /** 道具工厂定义（静态，来自关卡）。 */
    val itemSpawners: List<PacMazeItemSpawnerDef> = emptyList(),
    /** 道具工厂运行时。 */
    val itemSpawnerStates: List<PacMazeItemSpawnerState> = emptyList(),
    /** 地面道具。 */
    val floorItems: List<PacMazeFloorItem> = emptyList(),
    /** 磁力：吸引附近豆子。 */
    val magnetTicksLeft: Int = 0,
    /** 护盾层数（抵挡致命伤害）。 */
    val shieldCharges: Int = 0,
    /** 冰霜：全屏冻结幽灵。 */
    val frostTicksLeft: Int = 0,
    /** 迅捷：移动加速。 */
    val speedBoostTicksLeft: Int = 0,
    /** 双倍得分。 */
    val scoreBoostTicksLeft: Int = 0,
    /** 下一个 floor item 自增 id。 */
    val nextFloorItemId: Int = 0,
    /** 磁力吸附动画中的豆子。 */
    val magnetPulls: List<PacMazeMagnetPull> = emptyList(),
    val nextMagnetPullId: Int = 0,
    /** 已抵达的 checkpoint tag（闯关目标）。 */
    val visitedCheckpointTags: Set<String> = emptySet(),
    /** 迷宫模式：已探索格子索引（y * width + x）。 */
    val exploredTiles: Set<Int> = emptySet(),
    /** 回声雷达脉冲剩余帧。 */
    val radarRevealTicksLeft: Int = 0,
    /** 回声雷达冷却剩余帧。 */
    val radarCooldownTicksLeft: Int = 0,
    /** 回声豆指引剩余帧。 */
    val echoHintTicksLeft: Int = 0,
    val echoHintDirection: Direction? = null,
    val echoTargetKeyTag: String = "",
    /** 情报拍卖剩余点数。 */
    val intelPointsRemaining: Int = 0,
    val intelQuadrantsRevealed: Set<Int> = emptySet(),
    /** 追猎变体阶段。 */
    val huntPhase: Int = 0,
    /** 错序钥印拒绝提示。 */
    val sealedKeyRejectFlashTicks: Int = 0,
    /** 动态墙镜像（契约）。 */
    val mirrorDynamicWalls: Boolean = false,
    val dynamicWallSpeedMul: Float = 1f,
    /** 在线：match mode id（solo 为空）。 */
    val matchModeId: String = "",
    val teamLives: Int = 0,
    val playerLivesA: Int = 0,
    val playerLivesB: Int = 0,
    val playerScoreA: Int = 0,
    val playerScoreB: Int = 0,
    val hostEntityId: String = PacMazeConstants.PLAYER_ID,
    val guestEntityId: String = "pac_b",
    val pelletZoneA: Set<Int> = emptySet(),
    val pelletZoneB: Set<Int> = emptySet(),
    val pelletZoneAInitial: Int = 0,
    val pelletZoneBInitial: Int = 0,
    val onlineElapsedSeconds: Int = 0,
    val onlineWinnerEntityId: String? = null,
    val onlineEndReason: String? = null,
) {
    fun tileAt(x: Int, y: Int): TileType {
        if (x !in 0 until width || y !in 0 until height) return TileType.WALL
        return TileType.entries.firstOrNull { it.code == tiles[y * width + x] } ?: TileType.WALL
    }

    fun copyTiles(mutator: (IntArray) -> Unit): PacMazeWorldState {
        val copy = tiles.copyOf()
        mutator(copy)
        return copy(tiles = copy)
    }
}

object PacMazeConstants {
    const val TICKS_PER_SECOND = 60
    /** 豆人速度：格/秒（Float 亚像素积分）。 */
    const val PAC_SPEED_CELLS_PER_SEC = 6.0f
    /** 幽灵基础速度：格/秒（略慢于豆人，模式乘数叠加）。 */
    const val GHOST_SPEED_CELLS_PER_SEC = 5.4f
    /** @deprecated 幽灵已改为每帧速度积分，不再按间隔节流移动。 */
    const val PAC_MOVE_INTERVAL_TICKS = 12
    /** 接近格子中心时吸附阈值。 */
    const val CENTER_SNAP_EPS = 0.06f
    /** 中心吸附力度（0~1），仅用于路口转向动画。 */
    const val CENTER_SNAP_PULL = 0.55f
    /** 路口转向对齐阈值。 */
    const val TURN_ALIGN_EPS = 0.18f
    /** 提前转向窗口：略宽于 [TURN_ALIGN_EPS]，在接近中心时允许缓冲转向。 */
    const val TURN_PREEMPT_EPS = 0.20f
    /** 单帧最多追赶的逻辑 tick 数（略提高以减少掉帧时「一顿一顿」）。 */
    const val MAX_SIM_TICKS_PER_FRAME = 5
    /** 渲染插值速度外推系数（0~1，越大越“超前”）。 */
    const val RENDER_VEL_EXTRAP = 0.22f
    /** 角色/幽灵装饰动画相位增速（每逻辑 tick）；越小摆动越慢。 */
    const val ANIM_PHASE_PER_TICK = 0.08f
    /** 摇杆死区（strength 0~1）。 */
    const val JOYSTICK_DEAD_ZONE = 0.08f
    /** 推杆超过此阈值立即提交方向。 */
    const val JOYSTICK_COMMIT_THRESHOLD = 0.30f
    /** 轻推提交阈值（需与 stableTicks 配合）。 */
    const val JOYSTICK_SOFT_COMMIT_THRESHOLD = 0.20f
    /** 同一扇区连续多少 tick 后提交方向（旋转锁定解除后）。 */
    const val JOYSTICK_STABLE_TICKS = 1
    /** 滑动窗口内扇区变化次数达到此值 → 进入旋转锁定。 */
    const val JOYSTICK_SPIN_SECTOR_CHANGES = 4
    /** Spin 检测窗口（tick 数）。 */
    const val JOYSTICK_SPIN_WINDOW_TICKS = 15
    /** 窗口内累计转角超过此值（度）→ 进入旋转锁定。 */
    const val JOYSTICK_SPIN_ANGLE_DEG = 90f
    /** 旋转锁定解除：同一扇区稳定停留 tick 数。 */
    const val JOYSTICK_SPIN_RELEASE_STABLE_TICKS = 10
    /** 旋转锁定中用力 breakout 提交的最小力度。 */
    const val JOYSTICK_SPIN_BREAKOUT_STRENGTH = 0.80f
    /** breakout 需在同一扇区稳定的 tick 数。 */
    const val JOYSTICK_SPIN_BREAKOUT_STABLE_TICKS = 4
    /** 能量弹速度（格/秒）。 */
    const val PROJECTILE_SPEED_CELLS_PER_SEC = 11f
    /** 每次攻击冷却（逻辑帧）。 */
    const val ATTACK_COOLDOWN_TICKS = 20
    /** @deprecated 幽灵每帧决策，保留仅供旧测试引用。 */
    const val GHOST_MOVE_INTERVAL_TICKS = 6
    /** 开局幽灵静止时间（帧）；240 ≈ 4 秒。 */
    const val GHOST_RELEASE_TICKS = 240
    const val INITIAL_LIVES = 3
    const val POWER_DURATION_TICKS = 300
    const val GHOST_MODE_CYCLE_TICKS = 420
    const val PELLET_SCORE = 10
    const val POWER_SCORE = 50
    const val GHOST_SCORE = 200
    /** 子弹击中幽灵后的眩晕时长（帧）；结束后恢复原本颜色。 */
    const val GHOST_HIT_STUN_TICKS = 180
    const val GHOST_HIT_SCORE = 80
    /** 激光扫描速度（格/秒）。 */
    const val LASER_SCAN_SPEED_CELLS_PER_SEC = 4.2f
    /** 激光致命相位长度（帧）。 */
    const val LASER_LETHAL_TICKS = 90
    /** 激光预警相位长度（帧）。 */
    const val LASER_WARN_TICKS = 60
    /** 炮台开火间隔（帧）。 */
    const val TURRET_FIRE_INTERVAL_TICKS = 95
    /** 敌方子弹速度（格/秒）。 */
    const val ENEMY_BULLET_SPEED_CELLS_PER_SEC = 8.5f
    const val PAC_SPEED = 1f
    const val GHOST_SPEED = 0.72f
    const val FRIGHTENED_GHOST_SPEED = 0.48f
    const val EATEN_GHOST_SPEED = 0.95f
    const val PLAYER_ID = "pac1"

    @Suppress("UNUSED_PARAMETER")
    fun ghostMoveIntervalTicks(ghostSpeedMul: Float): Long = 1L

    fun ghostSpeedCellsPerSec(mode: GhostMode, ghostSpeedMul: Float): Float {
        val mul = ghostSpeedMul.coerceIn(0.38f, 1.35f)
        val modeScale = when (mode) {
            GhostMode.FRIGHTENED -> 0.78f
            GhostMode.EATEN -> 1.08f
            else -> 1f
        }
        return GHOST_SPEED_CELLS_PER_SEC * mul * modeScale
    }
}
