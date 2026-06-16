package com.example.funlife.social.game.engine.pacmaze

/** 地图种子来源 */
enum class PacMazeMazeSeedMode(val id: String, val displayName: String) {
    DAILY("daily", "每日挑战"),
    RANDOM("random", "自由随机"),
    ;

    companion object {
        fun fromId(id: String?): PacMazeMazeSeedMode =
            entries.firstOrNull { it.id == id } ?: DAILY
    }
}

/** 钥印收集模式 */
enum class PacMazeMazeKeyMode(val id: String, val displayName: String, val summary: String) {
    FREE("free", "自由钥印", "任意顺序收集"),
    SEALED("sealed", "封印钥印", "必须按 1→2→3 顺序"),
    ;

    companion object {
        fun fromId(id: String?): PacMazeMazeKeyMode =
            entries.firstOrNull { it.id == id } ?: FREE

        fun resolve(seed: Long, seedMode: PacMazeMazeSeedMode): PacMazeMazeKeyMode = when (seedMode) {
            PacMazeMazeSeedMode.DAILY -> SEALED
            PacMazeMazeSeedMode.RANDOM -> if ((seed and 1L) == 0L) SEALED else FREE
        }
    }
}

/** 招牌变体玩法 */
enum class PacMazeMazeVariant(
    val id: String,
    val displayName: String,
    val emoji: String,
    val tagline: String,
    val pillar: String,
) {
    STANDARD("standard", "标准迷雾", "🌫", "探索 + 钥印 + 出口", "综合"),
    HUNT("hunt", "追猎倒计时", "⏳", "鬼随时间进化 · 抢在深渊化前逃出", "逃生"),
    DUAL("dual", "双迷宫", "🌀", "A/B 两面 + 传送门切换", "探索"),
    INTEL("intel", "情报拍卖", "🎯", "3 点情报换地图信息", "探索"),
    GHOST_REPLAY("ghost_replay", "异步 Ghost", "👻", "挑战今日最快残影轨迹", "竞技"),
    ;

    companion object {
        fun fromId(id: String?): PacMazeMazeVariant =
            entries.firstOrNull { it.id == id } ?: STANDARD
    }
}

/** 深渊周赛 Mutator */
enum class PacMazeMazeMutator(
    val id: String,
    val displayName: String,
    val summary: String,
) {
    NONE("none", "无修饰", "标准规则"),
    FAST_WALLS("fast_walls", "墙速 ×1.5", "动态墙切换更快"),
    EXTRA_GHOST("extra_ghost", "额外幽灵", "幽灵 +1 · 更具压迫感"),
    TIME_CRUNCH("time_crunch", "时限 -20%", "得分 ×1.5 补偿"),
    ;

    companion object {
        fun weekly(seed: Long): PacMazeMazeMutator {
            val idx = ((seed / 7) % (entries.size - 1)).toInt() + 1
            return entries[idx.coerceIn(1, entries.lastIndex)]
        }

        fun fromId(id: String?): PacMazeMazeMutator =
            entries.firstOrNull { it.id == id } ?: NONE
    }
}

/** 每日幽灵签名 */
enum class PacMazeMazeGhostSignature(
    val id: String,
    val displayName: String,
    val ghostKind: GhostKind,
    val summary: String,
) {
    STALKER("stalker", "追击者", GhostKind.OPPORTUNIST, "发现玩家后持续追击"),
    AMBUSHER("ambusher", "蹲守者", GhostKind.FLANKER, "在钥印附近蹲守"),
    STRIKER("striker", "突袭者", GhostKind.STRIKER, "直线冲刺压迫"),
    ;

    companion object {
        fun forDailySeed(seed: Long, ghostCount: Int): List<PacMazeMazeGhostSignature> {
            if (ghostCount <= 0) return emptyList()
            val pool = entries.toList()
            val rng = PacMazeDeterministicRng(seed xor 0x60505123456789L)
            return (0 until ghostCount).map { pool[rng.nextInt(pool.size)] }
        }

        fun fromId(id: String?): PacMazeMazeGhostSignature? =
            entries.firstOrNull { it.id == id }
    }
}

/** 一局迷宫的完整配置 */
data class PacMazeMazeRunProfile(
    val seedMode: PacMazeMazeSeedMode = PacMazeMazeSeedMode.DAILY,
    val difficulty: PacMazeMazeDifficulty = PacMazeMazeDifficulty.STANDARD,
    val contract: PacMazeMazeContract = PacMazeMazeContract.NONE,
    val variant: PacMazeMazeVariant = PacMazeMazeVariant.STANDARD,
    val keyMode: PacMazeMazeKeyMode? = null,
    val mutator: PacMazeMazeMutator? = null,
    val customSeed: Long? = null,
    val randomPreviewSeed: Long? = null,
) {
    fun resolvedSeed(userId: Long, runtimeSeed: Long): Long = when (seedMode) {
        PacMazeMazeSeedMode.DAILY -> PacMazeMazeRunOptions.dailySeed()
        PacMazeMazeSeedMode.RANDOM -> customSeed ?: runtimeSeed
    }

    fun resolvedKeyMode(seed: Long): PacMazeMazeKeyMode =
        keyMode ?: PacMazeMazeKeyMode.resolve(seed, seedMode)

    fun resolvedMutator(seed: Long): PacMazeMazeMutator = when {
        mutator != null -> mutator
        seedMode == PacMazeMazeSeedMode.DAILY -> PacMazeMazeMutator.weekly(seed)
        else -> PacMazeMazeMutator.NONE
    }

    fun toRunOptions(seed: Long, userId: Long = 0L): PacMazeMazeRunOptions {
        val resolved = resolvedSeed(userId, seed)
        return PacMazeMazeRunOptions(
            seed = resolved,
            difficulty = difficulty,
            contract = contract,
            dailyChallenge = seedMode == PacMazeMazeSeedMode.DAILY,
            variant = variant,
            keyMode = resolvedKeyMode(resolved),
            mutator = resolvedMutator(resolved),
        )
    }

    companion object {
        fun fromLegacy(
            difficulty: PacMazeMazeDifficulty,
            contract: PacMazeMazeContract,
            dailyChallenge: Boolean,
        ): PacMazeMazeRunProfile = PacMazeMazeRunProfile(
            seedMode = if (dailyChallenge) PacMazeMazeSeedMode.DAILY else PacMazeMazeSeedMode.RANDOM,
            difficulty = difficulty,
            contract = contract,
        )
    }
}
