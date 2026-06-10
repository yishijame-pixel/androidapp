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
    val ghostSpawns: List<Pair<Int, Int>>,
    val ghostSpeedMul: Float = 1f,
    val aiAggression: Float = 0.8f,
    val markers: List<PacMazeMapMarker> = emptyList(),
    val hazards: List<PacMazeHazardDef> = emptyList(),
    val starCriteria: PacMazeStarCriteria = PacMazeStarCriteria.defaults(),
    val modeRules: PacMazeModeRules = PacMazeModeRules(),
)

enum class PacMazeMarkerKind { START, CHECKPOINT, EXIT }

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
    const val CENTER_SNAP_EPS = 0.08f
    /** 中心吸附力度（0~1）。 */
    const val CENTER_SNAP_PULL = 0.68f
    /** 路口转向对齐阈值。 */
    const val TURN_ALIGN_EPS = 0.14f
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
        val mul = ghostSpeedMul.coerceIn(0.72f, 1.25f)
        val modeScale = when (mode) {
            GhostMode.FRIGHTENED -> 0.78f
            GhostMode.EATEN -> 1.08f
            else -> 1f
        }
        return GHOST_SPEED_CELLS_PER_SEC * mul * modeScale
    }
}
