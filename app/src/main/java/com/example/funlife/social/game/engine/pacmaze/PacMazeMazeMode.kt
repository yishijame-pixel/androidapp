package com.example.funlife.social.game.engine.pacmaze

import java.util.Calendar

/** 迷宫难度：探索 / 标准 / 深渊 */
enum class PacMazeMazeDifficulty(
    val id: String,
    val displayName: String,
    val mapSize: Int,
    val ghostCount: Int,
    val keyCount: Int,
    val timeLimitSeconds: Int,
    val dynamicWalls: Boolean,
    val fogRadius: Int,
) {
    SCOUT("scout", "探路", 11, 0, 1, 240, false, 3),
    STANDARD("standard", "标准", 15, 1, 2, 180, true, 2),
    ABYSS("abyss", "深渊", 19, 2, 3, 150, true, 1),
    ;

    companion object {
        fun fromId(id: String?): PacMazeMazeDifficulty =
            entries.firstOrNull { it.id == id } ?: STANDARD
    }
}

/** 开局契约：竞技速通向修饰规则 */
enum class PacMazeMazeContract(
    val id: String,
    val displayName: String,
    val tagline: String,
    val pillar: String = "竞技",
) {
    NONE("none", "无契约", "标准规则", "综合"),
    DEEP_FOG("deep_fog", "浓雾契约", "视野 -1 · 三星时限 +15s", "探索"),
    SILENT("silent", "静默契约", "无幽灵 · 必须集齐钥印", "探索"),
    RUSH("rush", "疾行契约", "时限 ×0.65 · 得分 ×1.5", "竞技"),
    LABYRINTH("labyrinth", "巨窟契约", "地图 +4 · 多 1 把钥印", "探索"),
    MIRROR("mirror", "镜像契约", "动态墙相位反转", "逃生"),
    HUNTER("hunter", "猎手契约", "幽灵 +1 · 雷达 CD 减半", "逃生"),
    BLIND_PATH("blind_path", "盲径契约", "无线索豆 · 末钥闪出口", "探索"),
    TWIN("twin", "双生契约", "生成一对短距传送门", "探索"),
    ;

    companion object {
        fun fromId(id: String?): PacMazeMazeContract =
            entries.firstOrNull { it.id == id } ?: NONE
    }
}

data class PacMazeMazeRunOptions(
    val seed: Long,
    val difficulty: PacMazeMazeDifficulty = PacMazeMazeDifficulty.STANDARD,
    val contract: PacMazeMazeContract = PacMazeMazeContract.NONE,
    val dailyChallenge: Boolean = false,
    val variant: PacMazeMazeVariant = PacMazeMazeVariant.STANDARD,
    val keyMode: PacMazeMazeKeyMode = PacMazeMazeKeyMode.FREE,
    val mutator: PacMazeMazeMutator = PacMazeMazeMutator.NONE,
) {
    val effectiveMapSize: Int = when {
        contract == PacMazeMazeContract.LABYRINTH -> (difficulty.mapSize + 4).coerceAtMost(23)
        variant == PacMazeMazeVariant.DUAL -> (difficulty.mapSize + 2).coerceAtMost(21)
        else -> difficulty.mapSize
    }

    val effectiveGhostCount: Int = when {
        contract == PacMazeMazeContract.SILENT -> 0
        variant == PacMazeMazeVariant.HUNT -> difficulty.ghostCount + 1
        contract == PacMazeMazeContract.HUNTER -> difficulty.ghostCount + 1
        mutator == PacMazeMazeMutator.EXTRA_GHOST -> difficulty.ghostCount + 1
        else -> difficulty.ghostCount
    }

    val effectiveKeyCount: Int = when (contract) {
        PacMazeMazeContract.LABYRINTH -> (difficulty.keyCount + 1).coerceAtMost(4)
        else -> difficulty.keyCount
    }

    val effectiveTimeLimitSeconds: Int
        get() {
            var limit = difficulty.timeLimitSeconds
            if (contract == PacMazeMazeContract.RUSH) limit = (limit * 0.65f).toInt().coerceAtLeast(60)
            if (mutator == PacMazeMazeMutator.TIME_CRUNCH) limit = (limit * 0.8f).toInt().coerceAtLeast(60)
            if (variant == PacMazeMazeVariant.HUNT) limit = (limit * 1.15f).toInt()
            return limit
        }

    val effectiveFogRadius: Int = when (contract) {
        PacMazeMazeContract.DEEP_FOG -> (difficulty.fogRadius - 1).coerceAtLeast(1)
        else -> difficulty.fogRadius
    }

    val scoreMultiplier: Float
        get() {
            var mul = 1f
            if (contract == PacMazeMazeContract.RUSH) mul *= 1.5f
            if (mutator == PacMazeMazeMutator.TIME_CRUNCH) mul *= 1.5f
            if (variant == PacMazeMazeVariant.HUNT) mul *= 1.25f
            if (keyMode == PacMazeMazeKeyMode.SEALED) mul *= 1.1f
            return mul
        }

    val starTimeBonusSeconds: Int = when (contract) {
        PacMazeMazeContract.DEEP_FOG -> 15
        else -> 0
    }

    val hintPelletsEnabled: Boolean =
        contract != PacMazeMazeContract.BLIND_PATH && variant != PacMazeMazeVariant.INTEL

    val intelPointsMax: Int = when (variant) {
        PacMazeMazeVariant.INTEL -> 3
        else -> 0
    }

    val huntEscalation: Boolean = variant == PacMazeMazeVariant.HUNT

    val mirrorDynamicWalls: Boolean = contract == PacMazeMazeContract.MIRROR

    val radarCooldownMultiplier: Float = when (contract) {
        PacMazeMazeContract.HUNTER -> 0.5f
        else -> 1f
    }

    val revealExitOnLastKey: Boolean = contract == PacMazeMazeContract.BLIND_PATH

    val dynamicWallSpeedMul: Float = when (mutator) {
        PacMazeMazeMutator.FAST_WALLS -> 1.5f
        else -> 1f
    }

    val placeTwinPortals: Boolean =
        contract == PacMazeMazeContract.TWIN || variant == PacMazeMazeVariant.DUAL

    val placeItemRooms: Boolean =
        variant != PacMazeMazeVariant.GHOST_REPLAY && contract != PacMazeMazeContract.SILENT

    companion object {
        fun fromParams(params: PacMazeLoadParams): PacMazeMazeRunOptions {
            params.mazeProfile?.let { return it.toRunOptions(params.seed, params.userId) }
            val seed = if (params.mazeDailyChallenge) dailySeed() else params.seed
            return PacMazeMazeRunOptions(
                seed = seed,
                difficulty = PacMazeMazeDifficulty.fromId(params.mazeDifficultyId),
                contract = PacMazeMazeContract.fromId(params.mazeContractId),
                dailyChallenge = params.mazeDailyChallenge,
                variant = PacMazeMazeVariant.fromId(params.mazeVariantId),
                keyMode = PacMazeMazeKeyMode.fromId(params.mazeKeyModeId),
                mutator = PacMazeMazeMutator.fromId(params.mazeMutatorId),
            )
        }

        fun dailySeed(): Long {
            val cal = Calendar.getInstance()
            val y = cal.get(Calendar.YEAR)
            val m = cal.get(Calendar.MONTH) + 1
            val d = cal.get(Calendar.DAY_OF_MONTH)
            return y * 10_000L + m * 100L + d
        }

        fun dailyLabel(): String {
            val cal = Calendar.getInstance()
            return "${cal.get(Calendar.MONTH) + 1}月${cal.get(Calendar.DAY_OF_MONTH)}日挑战"
        }

        fun todayDateString(): String {
            val cal = Calendar.getInstance()
            val y = cal.get(Calendar.YEAR)
            val m = cal.get(Calendar.MONTH) + 1
            val d = cal.get(Calendar.DAY_OF_MONTH)
            return "$y-$m-$d"
        }
    }
}
