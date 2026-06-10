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
    val endlessWave: Int = 1,
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

    override suspend fun load(params: PacMazeLoadParams): PacMazeRunPayload {
        val wave = params.endlessWave.coerceAtLeast(1)
        val rng = PacMazeDeterministicRng(params.seed + wave)
        val chunk = chunkIds[rng.nextInt(chunkIds.size)]
        val json = readAsset(context, "pac_maze/chunks/$chunk.json")
        val parsed = PacMazeMapLoader.parseLevelJson(json)
        val speedMul = parsed.ghostSpeedMul * (1f + (wave - 1) * 0.06f)
        val level = parsed.copy(
            id = wave,
            name = "无尽 · 第 $wave 波",
            ghostSpeedMul = speedMul.coerceAtMost(2.2f),
        )
        return PacMazeRunPayload(
            level = level,
            json = json,
            runMode = PacMazeRunMode.ENDLESS,
            themeLevelId = ((wave - 1) % 13) + 1,
        )
    }
}

class MazeLevelSource : PacMazeLevelSource {

    override suspend fun load(params: PacMazeLoadParams): PacMazeRunPayload {
        val json = PacMazeMazeGenerator.buildLevelJson(params.seed)
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
