package com.example.funlife.game.platformer

import android.content.Context
import com.example.funlife.resource.ResourceStore

/** 从 SuperTux bundle 构建可玩世界。 */
object PlatformerSuperTuxLevelLoader {

    suspend fun ensureBundle(): Boolean =
        ResourceStore.ensureBundle("platformer_supertux")

    fun buildWorld(
        context: Context,
        level: PlatformerLevelDef,
        characterId: PlatformerCharacterId,
    ): PlatformerWorld {
        // 每次进关重新读 bundle，避免 lazy catalog 在 CDN 未就绪时缓存空 segments。
        val resolved = runCatching {
            PlatformerSuperTuxLevelCatalog.buildManifest(level.id, level.title)
        }.getOrElse { level }
        if (resolved.useCampaignScroll && !resolved.supertuxBakedSegments.isNullOrEmpty()) {
            return PlatformerSuperTuxScrollFactory.buildInitial(context, resolved, characterId)
        }
        if (resolved.rows.isNotEmpty()) {
            var world = PlatformerLevels.buildWorldFromRowsInternal(resolved, characterId)
            resolved.supertuxVisualRows?.let { rows ->
                val visual = IntArray(world.width * world.height)
                rows.forEachIndexed { y, row ->
                    row.forEachIndexed { x, tid ->
                        if (x < world.width && y < world.height) {
                            visual[y * world.width + x] = tid
                        }
                    }
                }
                world = world.copy(supertuxVisualTiles = visual)
            }
            return PlatformerSuperTuxScrollFactory.applySuperTuxObjects(world, resolved)
        }
        return PlatformerLevels.buildEmergencyWorld(resolved, characterId)
    }
}
