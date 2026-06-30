package com.example.funlife.game.platformer

import com.example.funlife.game.platformer.PlatformerSegmentLibrary.SegmentKind
import com.example.funlife.game.platformer.PlatformerSegmentLibrary.SegmentSpec

/** 高空章节 10 关片段脚本：全程以浮岛/天路为主，出生即在高处。 */
object PlatformerSkySegmentScripts {

    private val skyLanePool = listOf(
        SegmentKind.SKY_LANE,
        SegmentKind.HIGH_SKYWAY,
        SegmentKind.SPRING_VAULT,
        SegmentKind.MID_BRIDGE,
    )
    private val trapPool = listOf(
        SegmentKind.SKY_CHASE,
        SegmentKind.TRAP_LANE,
        SegmentKind.SKY_LANE,
        SegmentKind.TOWER,
    )
    private val enemyPool = listOf(
        SegmentKind.ENEMY_ROOM,
        SegmentKind.SKY_CHASE,
        SegmentKind.SKY_LANE,
        SegmentKind.FORK,
    )
    private val ascentPool = listOf(
        SegmentKind.TIER_ASCENT,
        SegmentKind.SKY_LANE,
        SegmentKind.TOWER,
        SegmentKind.HIGH_SKYWAY,
        SegmentKind.SKY_DESCENT,
    )

    fun scriptFor(levelId: Int): List<SegmentSpec> {
        val budget = PlatformerSkyLengthSpec.budget(levelId)
        val hazard = hazardLevel(levelId)
        val trap = trapLevel(levelId)
        val spawnLift = spawnLift(levelId)
        val pool = poolFor(levelId)
        val bodyCount = budget.segmentCount - 2
        val kinds = mutableListOf<SegmentKind>()
        kinds += SegmentKind.SKY_ENTRY
        repeat(bodyCount) { i ->
            kinds += pool[(i * 5 + levelId) % pool.size]
        }
        kinds += SegmentKind.SKY_FINALE
        return kinds.map { kind ->
            SegmentSpec(
                kind = kind,
                hazardLevel = hazard,
                trapLevel = trap,
                skySpawnLift = if (kind == SegmentKind.SKY_ENTRY) spawnLift else 0,
                skyFinaleLift = if (kind == SegmentKind.SKY_FINALE) finaleLift(levelId) else 0,
                layoutVariant = levelId % 4,
            )
        }
    }

    private fun poolFor(levelId: Int): List<SegmentKind> = when (levelId) {
        53, 54 -> skyLanePool
        55, 56, 60 -> trapPool
        57 -> listOf(SegmentKind.SPRING_VAULT, SegmentKind.SKY_LANE, SegmentKind.HIGH_SKYWAY)
        58 -> ascentPool
        59 -> listOf(SegmentKind.SKY_CHASE, SegmentKind.SKY_DESCENT, SegmentKind.TRAP_LANE)
        61 -> enemyPool
        62 -> buildList {
            addAll(ascentPool)
            addAll(trapPool)
            add(SegmentKind.ENEMY_ROOM)
        }
        else -> skyLanePool
    }

    private fun spawnLift(levelId: Int): Int = when (levelId) {
        53 -> 4
        54 -> 4
        in 55..57 -> 5
        in 58..61 -> 5
        62 -> 6
        else -> 4
    }

    private fun finaleLift(levelId: Int): Int = when (levelId) {
        53 -> 4
        in 54..57 -> 5
        in 58..61 -> 5
        62 -> 6
        else -> 5
    }

    private fun hazardLevel(levelId: Int): Int = when (levelId) {
        53 -> 1
        in 54..56 -> 2
        in 57..59 -> 2
        else -> 3
    }

    private fun trapLevel(levelId: Int): Int = when (levelId) {
        53 -> 1
        in 54..56 -> 2
        in 57..59 -> 2
        else -> 3
    }
}
