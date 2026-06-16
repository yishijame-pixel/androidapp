package com.example.funlife.social.game.engine.pacmaze

/** 追击 AI  archetype；多种幽灵 UI 可复用同一套逻辑。 */
enum class GhostBehaviorArchetype {
    STRIKER,
    PREDICTOR,
    FLANKER,
    OPPORTUNIST,
}

/** 幽灵外形轮廓；同关出场时优先不重复。 */
enum class GhostSilhouette {
    CLASSIC_ARCADE,
    PENDULUM,
    TALISMAN,
    TWIN_ECHO,
    WISP,
    ORIGAMI,
    ERROR_PANEL,
    HOURGLASS,
    GLITCH_BLOCKS,
    ABACUS,
    SPIDER_NODE,
    GATE_STATUE,
    CACHE_STACK,
}

/**
 * 幽灵角色池（经典吃豆人造型 + 器物/符咒/故障/机关，与玩家皮肤完全分离）。
 * 可继续 append 新条目，图鉴按 [codexBit] 扩展。
 */
enum class GhostKind(
    val id: String,
    val displayName: String,
    val emoji: String,
    val codexTitle: String,
    val behaviorHint: String,
    val behaviorArchetype: GhostBehaviorArchetype,
    val speedMul: Float,
    val unlockLevelMin: Int,
    val accentArgb: Long,
    val silhouette: GhostSilhouette,
) {
    STRIKER(
        id = "striker",
        displayName = "钟摆锤",
        emoji = "🔨",
        codexTitle = "钟摆锤 Pendulum",
        behaviorHint = "直道封路，直线追击",
        behaviorArchetype = GhostBehaviorArchetype.STRIKER,
        speedMul = 1.08f,
        unlockLevelMin = 1,
        accentArgb = 0xFFE53935,
        silhouette = GhostSilhouette.PENDULUM,
    ),
    PREDICTOR(
        id = "predictor",
        displayName = "符纸封灵",
        emoji = "📜",
        codexTitle = "符纸封灵 Talisman",
        behaviorHint = "符咒瞄准玩家前方路径",
        behaviorArchetype = GhostBehaviorArchetype.PREDICTOR,
        speedMul = 1.0f,
        unlockLevelMin = 3,
        accentArgb = 0xFF26A69A,
        silhouette = GhostSilhouette.TALISMAN,
    ),
    FLANKER(
        id = "flanker",
        displayName = "镜像残影",
        emoji = "👥",
        codexTitle = "镜像残影 Twin Echo",
        behaviorHint = "双影错位，包抄检查点",
        behaviorArchetype = GhostBehaviorArchetype.FLANKER,
        speedMul = 0.96f,
        unlockLevelMin = 6,
        accentArgb = 0xFF7E57C2,
        silhouette = GhostSilhouette.TWIN_ECHO,
    ),
    OPPORTUNIST(
        id = "opportunist",
        displayName = "磷火",
        emoji = "🧿",
        codexTitle = "磷火 Will-o-Wisp",
        behaviorHint = "冷火漂移；吃豆时猛然明亮加速",
        behaviorArchetype = GhostBehaviorArchetype.OPPORTUNIST,
        speedMul = 0.92f,
        unlockLevelMin = 9,
        accentArgb = 0xFF69F0AE,
        silhouette = GhostSilhouette.WISP,
    ),
    ORIGAMI(
        id = "origami",
        displayName = "折纸信标",
        emoji = "📐",
        codexTitle = "折纸信标 Origami",
        behaviorHint = "薄纸折痕，窄道急转",
        behaviorArchetype = GhostBehaviorArchetype.STRIKER,
        speedMul = 1.05f,
        unlockLevelMin = 4,
        accentArgb = 0xFFFFF176,
        silhouette = GhostSilhouette.ORIGAMI,
    ),
    ERROR_SPECTER(
        id = "error_specter",
        displayName = "404游魂",
        emoji = "⛔",
        codexTitle = "404 游魂 Error",
        behaviorHint = "乱码弹窗，直扑玩家",
        behaviorArchetype = GhostBehaviorArchetype.STRIKER,
        speedMul = 1.02f,
        unlockLevelMin = 11,
        accentArgb = 0xFFFF5252,
        silhouette = GhostSilhouette.ERROR_PANEL,
    ),
    HOURGLASS(
        id = "hourglass",
        displayName = "沙漏执令",
        emoji = "⏳",
        codexTitle = "沙漏执令 Hourglass",
        behaviorHint = "沙漏预判，封锁走廊",
        behaviorArchetype = GhostBehaviorArchetype.PREDICTOR,
        speedMul = 0.98f,
        unlockLevelMin = 13,
        accentArgb = 0xFFFFB74D,
        silhouette = GhostSilhouette.HOURGLASS,
    ),
    GLITCH(
        id = "glitch",
        displayName = "故障拼贴",
        emoji = "▧",
        codexTitle = "故障拼贴 Glitch",
        behaviorHint = "RGB 错位块，擅长走捷径",
        behaviorArchetype = GhostBehaviorArchetype.PREDICTOR,
        speedMul = 1.0f,
        unlockLevelMin = 15,
        accentArgb = 0xFF18FFFF,
        silhouette = GhostSilhouette.GLITCH_BLOCKS,
    ),
    ABACUS(
        id = "abacus",
        displayName = "算盘煞",
        emoji = "🧮",
        codexTitle = "算盘煞 Abacus",
        behaviorHint = "算珠巡点，包抄要道",
        behaviorArchetype = GhostBehaviorArchetype.FLANKER,
        speedMul = 0.94f,
        unlockLevelMin = 8,
        accentArgb = 0xFF8D6E63,
        silhouette = GhostSilhouette.ABACUS,
    ),
    ROUTER_SPIDER(
        id = "router_spider",
        displayName = "路由蜘蛛",
        emoji = "🕸",
        codexTitle = "路由蜘蛛 Router",
        behaviorHint = "节点六足，占三叉路口",
        behaviorArchetype = GhostBehaviorArchetype.FLANKER,
        speedMul = 0.97f,
        unlockLevelMin = 16,
        accentArgb = 0xFF536DFE,
        silhouette = GhostSilhouette.SPIDER_NODE,
    ),
    GATE_STATUE(
        id = "gate_statue",
        displayName = "门闩石兽",
        emoji = "🗿",
        codexTitle = "门闩石兽 Gate",
        behaviorHint = "石质镇物，守能量闸道",
        behaviorArchetype = GhostBehaviorArchetype.FLANKER,
        speedMul = 0.9f,
        unlockLevelMin = 14,
        accentArgb = 0xFF78909C,
        silhouette = GhostSilhouette.GATE_STATUE,
    ),
    CACHE_BLOB(
        id = "cache_blob",
        displayName = "缓存堆",
        emoji = "🗃",
        codexTitle = "缓存堆 Cache",
        behaviorHint = "方块堆叠，蹲补给点爆发",
        behaviorArchetype = GhostBehaviorArchetype.OPPORTUNIST,
        speedMul = 0.88f,
        unlockLevelMin = 18,
        accentArgb = 0xFFFF9100,
        silhouette = GhostSilhouette.CACHE_STACK,
    ),
    ARCADE_RED(
        id = "arcade_red",
        displayName = "赤灵",
        emoji = "👻",
        codexTitle = "经典赤灵 Blinky",
        behaviorHint = "经典圆顶裙边，直线猛追",
        behaviorArchetype = GhostBehaviorArchetype.STRIKER,
        speedMul = 1.08f,
        unlockLevelMin = 1,
        accentArgb = 0xFFFF1744,
        silhouette = GhostSilhouette.CLASSIC_ARCADE,
    ),
    ARCADE_PINK(
        id = "arcade_pink",
        displayName = "桃灵",
        emoji = "👻",
        codexTitle = "经典桃灵 Pinky",
        behaviorHint = "经典造型，预判玩家前方",
        behaviorArchetype = GhostBehaviorArchetype.PREDICTOR,
        speedMul = 1.0f,
        unlockLevelMin = 1,
        accentArgb = 0xFFFF80AB,
        silhouette = GhostSilhouette.CLASSIC_ARCADE,
    ),
    ARCADE_SKY(
        id = "arcade_sky",
        displayName = "青灵",
        emoji = "👻",
        codexTitle = "经典青灵 Inky",
        behaviorHint = "经典造型，侧翼包抄",
        behaviorArchetype = GhostBehaviorArchetype.FLANKER,
        speedMul = 0.96f,
        unlockLevelMin = 2,
        accentArgb = 0xFF40C4FF,
        silhouette = GhostSilhouette.CLASSIC_ARCADE,
    ),
    ARCADE_AMBER(
        id = "arcade_amber",
        displayName = "橙灵",
        emoji = "👻",
        codexTitle = "经典橙灵 Clyde",
        behaviorHint = "经典造型，吃豆时突然加速",
        behaviorArchetype = GhostBehaviorArchetype.OPPORTUNIST,
        speedMul = 0.92f,
        unlockLevelMin = 3,
        accentArgb = 0xFFFFAB40,
        silhouette = GhostSilhouette.CLASSIC_ARCADE,
    ),
    ;

    val codexBit: Int get() = 1 shl ordinal

    companion object {
        fun fromId(raw: String?): GhostKind? =
            entries.firstOrNull { it.id.equals(raw?.trim(), ignoreCase = true) }

        val codexOrder: List<GhostKind> = entries.sortedBy { it.unlockLevelMin }

        fun unlockedAtLevel(levelId: Int): List<GhostKind> =
            entries.filter { levelId >= it.unlockLevelMin }
    }
}

/** 高关专长：与机关/闸道联动。 */
enum class GhostSpecialty(
    val id: String,
    val displayName: String,
    val emoji: String,
    val hint: String,
) {
    NONE("", "", "", ""),
    PHASE_WALKER(
        id = "phase_walker",
        displayName = "相位通者",
        emoji = "⟳",
        hint = "冷却就绪时可穿过关闭的移动墙条纹",
    ),
    GATE_KEEPER(
        id = "gate_keeper",
        displayName = "门控者",
        emoji = "⚡",
        hint = "蹲守翼舱闸道，门开瞬间冲入",
    ),
    ;

    val isActive: Boolean get() = this != NONE

    companion object {
        fun fromId(raw: String?): GhostSpecialty? =
            entries.firstOrNull { it.id.equals(raw?.trim(), ignoreCase = true) }
    }
}

data class PacMazeGhostSpawnDef(
    val x: Int,
    val y: Int,
    val kind: GhostKind,
    val specialty: GhostSpecialty = GhostSpecialty.NONE,
) {
    val position: Pair<Int, Int> get() = x to y
}

object PacMazeGhostRoster {

    const val OPPORTUNIST_BURST_TICKS = 90
    const val OPPORTUNIST_BURST_SPEED_MUL = 1.28f
    const val PHASE_WALK_COOLDOWN_TICKS = 240
    const val PREDICTOR_LOOKAHEAD_TILES = 4

    fun resolveSpawns(
        levelId: Int,
        positions: List<Pair<Int, Int>>,
        hasDynamicTiles: Boolean = levelId >= 14,
        hasEnergyGates: Boolean = levelId >= 16,
        themeKey: PacMazeLevelThemeKey = PacMazeLevelThemeAssignment.forLevel(levelId),
    ): List<PacMazeGhostSpawnDef> {
        val kinds = pickKindsForSpawns(levelId, themeKey, positions.size)
        return positions.mapIndexed { index, (x, y) ->
            val kind = kinds[index]
            val specialty = defaultSpecialty(levelId, kind, hasDynamicTiles, hasEnergyGates)
            PacMazeGhostSpawnDef(x = x, y = y, kind = kind, specialty = specialty)
        }
    }

    fun defaultKind(levelId: Int, index: Int): GhostKind {
        val kinds = pickKindsForSpawns(
            levelId = levelId,
            themeKey = PacMazeLevelThemeAssignment.forLevel(levelId),
            count = index + 1,
        )
        return kinds[index.coerceAtMost(kinds.lastIndex)]
    }

    /** 按轮廓去重优先，保证同关尽量不出现重复外形；经典造型优先出场。 */
    fun pickKindsForSpawns(
        levelId: Int,
        themeKey: PacMazeLevelThemeKey,
        count: Int,
    ): List<GhostKind> {
        if (count <= 0) return emptyList()
        val unlocked = activeKindsForLevel(levelId)
        val featured = if (levelId >= 14) {
            PacMazeGhostThemeAffinity.featuredKinds(themeKey, levelId).filter { it in unlocked }
        } else {
            emptyList()
        }
        val classics = unlocked.filter { it.silhouette == GhostSilhouette.CLASSIC_ARCADE }
        val priority = buildList {
            addAll(featured)
            addAll(classics)
            addAll(unlocked.sortedBy { it.unlockLevelMin })
        }.distinctBy { it.silhouette }
        if (priority.isEmpty()) return List(count) { GhostKind.ARCADE_RED }

        val result = mutableListOf<GhostKind>()
        for (kind in priority) {
            if (result.size >= count) break
            result.add(kind)
        }
        while (result.size < count) {
            val next = priority.minBy { kind -> result.count { it.silhouette == kind.silhouette } }
            result.add(next)
        }
        return result
    }

    fun activeKindsForLevel(levelId: Int): List<GhostKind> =
        GhostKind.unlockedAtLevel(levelId).ifEmpty { listOf(GhostKind.STRIKER) }

    fun defaultSpecialty(
        levelId: Int,
        kind: GhostKind,
        hasDynamicTiles: Boolean,
        hasEnergyGates: Boolean,
    ): GhostSpecialty {
        if (levelId < 14) return GhostSpecialty.NONE
        return when (kind) {
            GhostKind.GATE_STATUE -> GhostSpecialty.GATE_KEEPER
            GhostKind.GLITCH -> if (hasDynamicTiles) GhostSpecialty.PHASE_WALKER else GhostSpecialty.NONE
            GhostKind.ROUTER_SPIDER -> if (hasEnergyGates) GhostSpecialty.GATE_KEEPER else GhostSpecialty.NONE
            GhostKind.FLANKER, GhostKind.ABACUS -> GhostSpecialty.GATE_KEEPER
            GhostKind.OPPORTUNIST, GhostKind.CACHE_BLOB ->
                if (hasEnergyGates) GhostSpecialty.GATE_KEEPER else GhostSpecialty.NONE
            GhostKind.PREDICTOR, GhostKind.HOURGLASS, GhostKind.ARCADE_PINK ->
                if (hasDynamicTiles) GhostSpecialty.PHASE_WALKER else GhostSpecialty.NONE
            GhostKind.STRIKER, GhostKind.ORIGAMI, GhostKind.ERROR_SPECTER, GhostKind.ARCADE_RED ->
                if (levelId >= 20 && hasDynamicTiles) GhostSpecialty.PHASE_WALKER else GhostSpecialty.NONE
            GhostKind.ARCADE_SKY -> GhostSpecialty.GATE_KEEPER
            GhostKind.ARCADE_AMBER ->
                if (hasEnergyGates) GhostSpecialty.GATE_KEEPER else GhostSpecialty.NONE
        }
    }

    fun scatterCorner(kind: GhostKind, width: Int, height: Int): Pair<Int, Int> =
        when (kind.ordinal % 4) {
            0 -> width - 2 to 1
            1 -> 1 to 1
            2 -> 1 to height - 2
            else -> width - 2 to height - 2
        }

    fun prefersWestWing(kind: GhostKind, specialty: GhostSpecialty): Boolean =
        kind == GhostKind.GATE_STATUE ||
            kind == GhostKind.ABACUS ||
            kind == GhostKind.FLANKER ||
            (specialty == GhostSpecialty.GATE_KEEPER && kind != GhostKind.CACHE_BLOB)

    fun prefersEastWing(kind: GhostKind, specialty: GhostSpecialty): Boolean =
        kind == GhostKind.ROUTER_SPIDER ||
            kind == GhostKind.OPPORTUNIST ||
            (specialty == GhostSpecialty.GATE_KEEPER && kind == GhostKind.STRIKER)
}
