package com.example.funlife.game.platformer

import android.content.Context
import com.example.funlife.game.platformer.PlatformerSegmentLibrary.SegmentSpec

/**
 * 统一片段化关卡烘焙：从 [PlatformerCampaignLengthSpec] + 脚本生成可玩世界。
 */
object PlatformerSegmentLevelFactory {

    data class BuildConfig(
        val meta: PlatformerCampaignLevelCatalog.LevelMeta,
        val budget: PlatformerCampaignLengthSpec.LevelBudget,
        val script: List<SegmentSpec>,
    )

    fun manifest(config: BuildConfig): PlatformerLevelDef =
        PlatformerLevelDef(
            id = config.meta.id,
            title = config.meta.title,
            subtitle = config.meta.subtitle,
            theme = config.meta.theme,
            rows = emptyList(),
            skyTop = config.meta.skyTop,
            skyBottom = config.meta.skyBottom,
            parallaxHill = config.meta.parallaxHill,
            tilesetPack = config.meta.tilesetPack,
            seriesId = config.meta.seriesId,
            seriesOrder = config.meta.seriesOrder,
            campaignSegmentScript = config.script,
            useCampaignScroll = config.budget.useScrollBuffer,
            checkpointEverySegments = config.budget.checkpointEverySegments,
            targetTiles = config.budget.targetTiles,
        )

    fun buildWorld(
        context: Context,
        level: PlatformerLevelDef,
        characterId: PlatformerCharacterId,
    ): PlatformerWorld {
        val script = level.campaignSegmentScript
            ?: error("Level ${level.id} has no campaignSegmentScript")
        return if (level.useCampaignScroll) {
            PlatformerCampaignScrollRunner.buildInitial(context, level, script, characterId)
        } else {
            buildFullWorld(context, level, script, characterId)
        }
    }

    fun buildFullWorld(
        context: Context?,
        level: PlatformerLevelDef,
        script: List<SegmentSpec>,
        characterId: PlatformerCharacterId,
    ): PlatformerWorld {
        val h = PLATFORMER_LEVEL_ROWS
        val g = h - 1
        val segW = PlatformerSegmentLibrary.SEGMENT_W
        prebakeStoryRooms(context, script, g)
        val width = script.size * segW + PlatformerCampaignLengthSpec.SEGMENT_TAIL_PAD
        val canvas = PlatformerMapCanvas(width, h)
        paintScript(context, canvas, g, script, 0, script.size)
        val def = bakeLevelDef(level, canvas, g, width)
        return PlatformerLevels.buildWorldFromRowsInternal(def, characterId)
    }

    /** 将 canvas 烘焙为 rows，否则 [PlatformerLevels.buildWorldFromRowsInternal] 会回退应急短关。 */
    internal fun bakeLevelDef(
        level: PlatformerLevelDef,
        canvas: PlatformerMapCanvas,
        groundY: Int,
        width: Int,
    ): PlatformerLevelDef = PlatformerLevelEnhancer.finalize(
        level.copy(rows = canvas.toRows()),
        canvas,
        groundY,
        width,
    )

    fun paintScript(
        context: Context?,
        canvas: PlatformerMapCanvas,
        groundY: Int,
        script: List<SegmentSpec>,
        fromIndex: Int,
        toIndexExclusive: Int,
    ) {
        val segW = PlatformerSegmentLibrary.SEGMENT_W
        for (i in fromIndex until toIndexExclusive.coerceAtMost(script.size)) {
            if (i > 0) canvas.bridgeGap((i * segW - 2).coerceAtLeast(0), groundY, 1)
            val x = i * segW
            paintSegment(context, canvas, groundY, x, script[i], i)
        }
    }

    fun paintSegment(
        context: Context?,
        canvas: PlatformerMapCanvas,
        groundY: Int,
        startX: Int,
        spec: SegmentSpec,
        sectionIndex: Int,
    ): Int {
        if (spec.kind == PlatformerSegmentLibrary.SegmentKind.STORY_ROOM &&
            context != null &&
            !spec.tmxAssetPath.isNullOrBlank()
        ) {
            return PlatformerTmxRoomPainter.paintFromTmx(context, canvas, groundY, startX, spec.tmxAssetPath)
        }
        if (spec.kind == PlatformerSegmentLibrary.SegmentKind.STORY_ROOM) {
            return PlatformerTmxRoomPainter.paintBakedOrPlaceholder(canvas, groundY, startX, spec.tmxAssetPath)
        }
        return PlatformerSegmentLibrary.paint(canvas, groundY, startX, spec, sectionIndex)
    }

    internal fun prebakeStoryRooms(context: Context?, script: List<SegmentSpec>, groundY: Int) {
        if (context == null) return
        val h = PLATFORMER_LEVEL_ROWS
        val tmp = PlatformerMapCanvas(PlatformerSegmentLibrary.SEGMENT_W + 8, h)
        script.filter {
            it.kind == PlatformerSegmentLibrary.SegmentKind.STORY_ROOM &&
                !it.tmxAssetPath.isNullOrBlank()
        }.forEach { spec ->
            PlatformerTmxRoomPainter.paintFromTmx(context, tmp, groundY, 0, spec.tmxAssetPath!!)
        }
    }
}
