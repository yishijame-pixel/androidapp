package com.example.funlife.game.platformer

import android.content.Context

/** SuperTux 烘焙片段滚动关：复用 [PlatformerCampaignScrollRunner] 缓冲逻辑。 */
object PlatformerSuperTuxScrollFactory {

    fun buildInitial(
        context: Context,
        level: PlatformerLevelDef,
        characterId: PlatformerCharacterId,
    ): PlatformerWorld {
        val segments = level.supertuxBakedSegments
            ?: error("SuperTux scroll level ${level.id} missing baked segments")
        val visualSegments = level.supertuxVisualSegments
        val buffer = PlatformerCampaignLengthSpec.INITIAL_BUFFER_SEGMENTS.coerceAtMost(segments.size)
        val h = PLATFORMER_LEVEL_ROWS
        val g = h - 1
        val segW = PlatformerSegmentLibrary.SEGMENT_W
        val width = buffer * segW + PlatformerCampaignLengthSpec.SEGMENT_TAIL_PAD
        val canvas = PlatformerMapCanvas(width, h)
        val visualTiles = IntArray(width * h)
        for (i in 0 until buffer) {
            paintBakedSegment(canvas, i * segW, segments[i])
            visualSegments?.getOrNull(i)?.let { seg ->
                paintVisualSegment(visualTiles, width, h, i * segW, seg)
            }
        }
        val def = PlatformerSegmentLevelFactory.bakeLevelDef(level, canvas, g, width)
        val base = PlatformerLevels.buildWorldFromRowsInternal(def, characterId)
            .let { applySuperTuxObjects(it, level) }
        val checkpoints = listOf(
            PlatformerCampaignCheckpoint(
                segmentIndex = 0,
                absoluteTile = 0,
                spawnX = base.player.x,
                spawnY = base.player.y,
            ),
        ).takeIf { level.checkpointEverySegments > 0 }.orEmpty()
        return base.copy(
            campaignScrollMode = true,
            campaignBakedSegments = segments,
            campaignBakedVisualSegments = visualSegments,
            supertuxVisualTiles = visualTiles.takeIf { visualSegments != null },
            campaignScriptIndex = buffer,
            campaignTotalSegments = segments.size,
            campaignTilesRun = 0,
            campaignCheckpoints = checkpoints,
            campaignLastCheckpointIndex = if (checkpoints.isNotEmpty()) 0 else -1,
            levelSpawnX = base.player.x,
            levelSpawnY = base.player.y,
            phase = PlatformerPhase.PLAYING,
            cameraX = 0f,
        )
    }

    fun paintBakedSegment(canvas: PlatformerMapCanvas, startX: Int, rows: List<String>) {
        rows.forEachIndexed { y, row ->
            row.forEachIndexed { x, ch ->
                if (ch != '.') canvas.set(startX + x, y, ch)
            }
        }
    }

    fun paintVisualSegment(
        buffer: IntArray,
        width: Int,
        height: Int,
        startX: Int,
        rows: List<List<Int>>,
    ) {
        rows.forEachIndexed { y, row ->
            if (y >= height) return@forEachIndexed
            row.forEachIndexed { x, tid ->
                val wx = startX + x
                if (wx in 0 until width) {
                    buffer[y * width + wx] = tid
                }
            }
        }
    }

    fun applySuperTuxObjects(world: PlatformerWorld, level: PlatformerLevelDef): PlatformerWorld {
        val tile = PLATFORMER_TILE_PX.toFloat()
        val gems = level.supertuxCoins.mapNotNull { coin ->
            if (coin.tx !in 0 until world.width || coin.ty !in 0 until world.height) return@mapNotNull null
            PlatformerGem(
                x = coin.tx * tile + tile / 2f,
                y = coin.ty * tile + tile / 2f,
            )
        }
        return if (gems.isEmpty()) world else world.copy(gems = world.gems + gems)
    }
}
