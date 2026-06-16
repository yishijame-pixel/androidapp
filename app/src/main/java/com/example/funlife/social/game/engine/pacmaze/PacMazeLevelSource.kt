package com.example.funlife.social.game.engine.pacmaze

import android.content.Context

data class PacMazeRunPayload(
    val level: PacMazeLevelConfig,
    val json: String,
    val runMode: PacMazeRunMode,
    val themeLevelId: Int = level.id,
)

interface PacMazeLevelSource {
    suspend fun load(params: PacMazeLoadParams): PacMazeRunPayload
}

data class PacMazeLoadParams(
    val runMode: PacMazeRunMode,
    val levelId: Int = 1,
    val seed: Long,
    val userId: Long = 0L,
    val endlessWave: Int = 1,
    val maxLevelReached: Int = 1,
    val mazeDifficultyId: String? = null,
    val mazeContractId: String? = null,
    val mazeDailyChallenge: Boolean = false,
    val mazeVariantId: String? = null,
    val mazeKeyModeId: String? = null,
    val mazeMutatorId: String? = null,
    val mazeProfile: PacMazeMazeRunProfile? = null,
)

class CampaignLevelSource(
    private val context: Context,
) : PacMazeLevelSource {

    override suspend fun load(params: PacMazeLoadParams): PacMazeRunPayload {
        val id = params.levelId.coerceIn(1, 999)
        val json = readAsset(context, "pac_maze/levels/level_%03d.json".format(id))
        val level = PacMazeMapLoader.parseLevelJson(json)
        return PacMazeRunPayload(level = level, json = json, runMode = params.runMode, themeLevelId = id)
    }
}

class EndlessLevelSource(
    private val context: Context,
) : PacMazeLevelSource {

    private val chunkIds = listOf("chunk_alpha", "chunk_beta", "chunk_gamma")
    private val moltenLevelStart = 14
    private val moltenLevelCount = 10

    override suspend fun load(params: PacMazeLoadParams): PacMazeRunPayload {
        val wave = params.endlessWave.coerceAtLeast(1)
        val moltenUnlocked = params.maxLevelReached >= PacMazeLevelProgression.TOTAL_LEVELS
        return if (wave >= 8 && moltenUnlocked) {
            loadMoltenWave(wave, params.seed)
        } else {
            val chunkWave = if (wave >= 8) ((wave - 1) % 7) + 1 else wave
            loadChunkWave(chunkWave, params.seed, displayWave = wave)
        }
    }

    private fun loadMoltenWave(wave: Int, seed: Long): PacMazeRunPayload {
        val levelId = moltenLevelStart + ((wave - 8) % moltenLevelCount)
        val json = readAsset(context, "pac_maze/levels/level_%03d.json".format(levelId))
        val parsed = PacMazeMapLoader.parseLevelJson(json)
        val speedMul = parsed.ghostSpeedMul * (1f + (wave - 1) * 0.08f)
        val aggressionBoost = (wave - 8) * 0.025f
        val level = parsed.copy(
            id = wave,
            name = "熔炉无尽 · 第 $wave 波",
            ghostSpeedMul = speedMul.coerceAtMost(2.4f),
            aiAggression = (parsed.aiAggression + aggressionBoost).coerceAtMost(1f),
        )
        return PacMazeRunPayload(
            level = level,
            json = json,
            runMode = PacMazeRunMode.ENDLESS,
            themeLevelId = levelId,
        )
    }

    private fun loadChunkWave(wave: Int, seed: Long, displayWave: Int = wave): PacMazeRunPayload {
        val rng = PacMazeDeterministicRng(seed + displayWave)
        val chunk = chunkIds[rng.nextInt(chunkIds.size)]
        val json = readAsset(context, "pac_maze/chunks/$chunk.json")
        val parsed = PacMazeMapLoader.parseLevelJson(json)
        val speedMul = parsed.ghostSpeedMul * (1f + (displayWave - 1) * 0.06f)
        val level = parsed.copy(
            id = displayWave,
            name = if (displayWave >= 8) "无尽 · 第 $displayWave 波（预热）" else "无尽 · 第 $displayWave 波",
            ghostSpeedMul = speedMul.coerceAtMost(2.2f),
        )
        return PacMazeRunPayload(
            level = level,
            json = json,
            runMode = PacMazeRunMode.ENDLESS,
            themeLevelId = ((displayWave - 1) % 13) + 1,
        )
    }
}

class MazeLevelSource : PacMazeLevelSource {

    override suspend fun load(params: PacMazeLoadParams): PacMazeRunPayload {
        val options = PacMazeMazeRunOptions.fromParams(params)
        val json = PacMazeMazeGenerator.buildLevelJson(options)
        val level = PacMazeMapLoader.parseLevelJson(json)
        return PacMazeRunPayload(
            level = level,
            json = json,
            runMode = PacMazeRunMode.MAZE,
            themeLevelId = 3,
        )
    }
}

private fun readAsset(context: Context, path: String): String =
    context.assets.open(path).bufferedReader().use { it.readText() }
