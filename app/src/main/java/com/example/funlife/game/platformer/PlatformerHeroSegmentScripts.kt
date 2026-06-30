package com.example.funlife.game.platformer

import com.example.funlife.game.platformer.catalog.PlatformerContentCatalog

/** 英雄章节片段脚本：按 catalog segmentProfile 组合。 */
object PlatformerHeroSegmentScripts {

    fun scriptFor(hero: PlatformerContentCatalog.HeroLevelEntry): List<PlatformerSegmentLibrary.SegmentSpec> {
        val profile = hero.segmentProfile.uppercase()
        val base = listOf(
            PlatformerSegmentLibrary.SegmentSpec(PlatformerSegmentLibrary.SegmentKind.LOW_PLATEAU),
            PlatformerSegmentLibrary.SegmentSpec(PlatformerSegmentLibrary.SegmentKind.GAP),
            PlatformerSegmentLibrary.SegmentSpec(PlatformerSegmentLibrary.SegmentKind.MID_BRIDGE),
        )
        val extra = when {
            profile.contains("SLIDE") -> listOf(
                PlatformerSegmentLibrary.SegmentSpec(PlatformerSegmentLibrary.SegmentKind.TRAP_LANE),
                PlatformerSegmentLibrary.SegmentSpec(PlatformerSegmentLibrary.SegmentKind.LOW_PLATEAU),
            )
            profile.contains("CLIMB") -> listOf(
                PlatformerSegmentLibrary.SegmentSpec(PlatformerSegmentLibrary.SegmentKind.TOWER),
                PlatformerSegmentLibrary.SegmentSpec(PlatformerSegmentLibrary.SegmentKind.TIER_ASCENT),
            )
            profile.contains("SHOOT") || profile.contains("TURRET") -> listOf(
                PlatformerSegmentLibrary.SegmentSpec(PlatformerSegmentLibrary.SegmentKind.TRAP_LANE),
                PlatformerSegmentLibrary.SegmentSpec(PlatformerSegmentLibrary.SegmentKind.ENEMY_ROOM),
            )
            profile.contains("MELEE") || profile.contains("FINALE") -> listOf(
                PlatformerSegmentLibrary.SegmentSpec(PlatformerSegmentLibrary.SegmentKind.ENEMY_ROOM),
                PlatformerSegmentLibrary.SegmentSpec(PlatformerSegmentLibrary.SegmentKind.ENEMY_ROOM),
            )
            profile.contains("BOUNCE") -> listOf(
                PlatformerSegmentLibrary.SegmentSpec(PlatformerSegmentLibrary.SegmentKind.SPRING_VAULT),
                PlatformerSegmentLibrary.SegmentSpec(PlatformerSegmentLibrary.SegmentKind.SPRING_VAULT),
            )
            else -> listOf(
                PlatformerSegmentLibrary.SegmentSpec(PlatformerSegmentLibrary.SegmentKind.FORK),
                PlatformerSegmentLibrary.SegmentSpec(PlatformerSegmentLibrary.SegmentKind.STEPS),
            )
        }
        return (base + extra + base).take(14)
    }
}
