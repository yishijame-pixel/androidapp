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

    private val warmChunks = listOf("chunk_alpha", "chunk_beta", "chunk_gamma")
    private val hotChunks = listOf("chunk_delta", "chunk_zeta", "chunk_eta")
    private val infernoChunks = listOf("chunk_epsilon", "chunk_theta")
    private val allChunks = warmChunks + hotChunks + infernoChunks
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

    private fun chunkPoolForWave(wave: Int): List<String> = when {
        wave <= 2 -> warmChunks
        wave <= 4 -> warmChunks + hotChunks
        wave <= 7 -> allChunks
        else -> hotChunks + infernoChunks
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
        val pool = chunkPoolForWave(displayWave)
        val chunk = pool[rng.nextInt(pool.size)]
        val json = readAsset(context, "pac_maze/chunks/$chunk.json")
        val parsed = PacMazeMapLoader.parseLevelJson(json)
        val speedMul = parsed.ghostSpeedMul * (1f + (displayWave - 1) * 0.06f)
        val aggression = (parsed.aiAggression + (displayWave - 1) * 0.018f).coerceAtMost(0.95f)
        val level = parsed.copy(
            id = displayWave,
            name = if (displayWave >= 8) "无尽 · 第 $displayWave 波（${parsed.name.substringAfter("·")}）"
            else "无尽 · 第 $displayWave 波 · ${parsed.name.substringAfter("·")}",
            ghostSpeedMul = speedMul.coerceAtMost(2.2f),
            aiAggression = aggression,
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
        val themeLevelId = when (options.difficulty) {
            PacMazeMazeDifficulty.SCOUT -> 3
            PacMazeMazeDifficulty.STANDARD -> 7
            PacMazeMazeDifficulty.ABYSS -> 14
        }.let { base ->
            ((params.seed % 6).toInt() + base).coerceIn(1, PacMazeLevelProgression.TOTAL_LEVELS)
        }
        return PacMazeRunPayload(
            level = level,
            json = json,
            runMode = PacMazeRunMode.MAZE,
            themeLevelId = themeLevelId,
        )
    }
}

private fun readAsset(context: Context, path: String): String =
    context.assets.open(path).bufferedReader().use { it.readText() }
