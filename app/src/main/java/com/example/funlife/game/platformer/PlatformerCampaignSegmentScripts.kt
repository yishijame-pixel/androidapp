package com.example.funlife.game.platformer

import com.example.funlife.game.platformer.PlatformerSegmentLibrary.SegmentKind
import com.example.funlife.game.platformer.PlatformerSegmentLibrary.SegmentSpec

/**
 * 52 关片段脚本：按章节池 + 关卡 id 生成节奏曲线（教学→挑战→喘息→高潮）。
 */
object PlatformerCampaignSegmentScripts {

    private val classicPool = listOf(
        SegmentKind.GAP, SegmentKind.STEPS, SegmentKind.FORK,
        SegmentKind.ENEMY_ROOM, SegmentKind.SPRING_VAULT, SegmentKind.TRAP_LANE,
    )
    private val packPool = listOf(
        SegmentKind.LOW_PLATEAU, SegmentKind.GAP, SegmentKind.FORK,
        SegmentKind.TRAP_LANE, SegmentKind.ENEMY_ROOM, SegmentKind.SPRING_VAULT,
    )
    private val storyPool = listOf(
        SegmentKind.MID_BRIDGE, SegmentKind.GAP, SegmentKind.TOWER,
        SegmentKind.TRAP_LANE, SegmentKind.ENEMY_ROOM, SegmentKind.FORK,
    )
    private val epicPool = listOf(
        SegmentKind.GAP, SegmentKind.FORK, SegmentKind.TOWER,
        SegmentKind.TRAP_LANE, SegmentKind.ENEMY_ROOM, SegmentKind.SPRING_VAULT,
        SegmentKind.HIGH_SKYWAY,
    )
    private val tierPool = listOf(
        SegmentKind.LOW_PLATEAU, SegmentKind.MID_BRIDGE, SegmentKind.HIGH_SKYWAY,
        SegmentKind.TIER_ASCENT, SegmentKind.TOWER, SegmentKind.TRAP_LANE,
        SegmentKind.ENEMY_ROOM, SegmentKind.SPRING_VAULT,
    )

    /** 剧情关 TMX 房间（32px 逻辑格，嵌入长关中段，非独立短竞技场）。 */
    fun storyTmxAsset(levelId: Int): String? = when (levelId) {
        17 -> "platformer/marianandrobin/levels/forest/forest.tmx"
        18 -> "platformer/marianandrobin/levels/castle/castle.tmx"
        19 -> "platformer/marianandrobin/levels/castle/castle2.tmx"
        20 -> "platformer/marianandrobin/levels/castle/castle3.tmx"
        21 -> "platformer/marianandrobin/levels/castle/castle4.tmx"
        22 -> "platformer/marianandrobin/levels/tournament/tournament.tmx"
        else -> null
    }

    fun scriptFor(levelId: Int): List<SegmentSpec> {
        val budget = PlatformerCampaignLengthSpec.budget(levelId)
        val hazard = hazardLevel(levelId)
        val trap = trapLevel(levelId)
        val pool = when (budget.chapter) {
            PlatformerCampaignLengthSpec.Chapter.CLASSIC -> classicPool
            PlatformerCampaignLengthSpec.Chapter.PACK -> packPool
            PlatformerCampaignLengthSpec.Chapter.STORY -> storyPool
            PlatformerCampaignLengthSpec.Chapter.EPIC -> epicPool
            PlatformerCampaignLengthSpec.Chapter.TIER -> tierPool
        }
        val bodyCount = budget.segmentCount - 2
        val kinds = mutableListOf<SegmentKind>()
        kinds += SegmentKind.ENTRY
        repeat(bodyCount) { i ->
            if (budget.chapter == PlatformerCampaignLengthSpec.Chapter.STORY &&
                i == bodyCount / 2
            ) {
                kinds += SegmentKind.STORY_ROOM
            } else {
                kinds += pool[(i * 7 + levelId) % pool.size]
            }
        }
        kinds += SegmentKind.FINALE
        val tmx = storyTmxAsset(levelId)
        return kinds.map { kind ->
            SegmentSpec(
                kind = kind,
                hazardLevel = hazard,
                trapLevel = trap,
                tmxAssetPath = if (kind == SegmentKind.STORY_ROOM) tmx else null,
                layoutVariant = levelId % 4,
            )
        }
    }

    private fun hazardLevel(levelId: Int): Int = when {
        levelId <= 6 -> 2
        levelId <= 16 -> 2
        levelId <= 22 -> 3
        levelId <= 34 -> 3
        else -> 3
    }

    private fun trapLevel(levelId: Int): Int = when {
        levelId <= 6 -> 2
        levelId <= 16 -> 2
        levelId <= 22 -> 3
        levelId <= 34 -> 3
        else -> 3
    }
}
