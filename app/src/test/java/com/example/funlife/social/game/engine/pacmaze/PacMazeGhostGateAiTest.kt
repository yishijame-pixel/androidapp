package com.example.funlife.social.game.engine.pacmaze

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PacMazeGhostGateAiTest {

    private fun level(id: Int, markers: List<PacMazeMapMarker>) = PacMazeLevelConfig(
        id = id,
        name = "gate-test",
        width = 23,
        height = 21,
        pacSpawn = 11 to 18,
        ghostSpawns = emptyList(),
        markers = markers,
        aiAggression = 0.9f,
    )

    private fun world(levelId: Int = 14) = PacMazeWorldState(
        tick = 0L,
        levelId = levelId,
        tiles = IntArray(23 * 21) { TileType.EMPTY.code },
        width = 23,
        height = 21,
        entities = emptyList(),
        score = 0,
        lives = 3,
        pelletsRemaining = 0,
        phase = PacMazePhase.PLAYING,
        rngSeed = 1L,
    )

    private fun pac(x: Int, y: Int) = PacMazeEntity(
        id = "pac1",
        role = "pac",
        x = x.toFloat(),
        y = y.toFloat(),
        direction = Direction.RIGHT,
        speed = 1f,
    )

    private fun ghost(id: String, x: Int, y: Int) = PacMazeEntity(
        id = id,
        role = "ghost",
        x = x.toFloat(),
        y = y.toFloat(),
        direction = Direction.LEFT,
        speed = 1f,
    )

    @Test
    fun ambushTarget_returnsNullBeforeL14() {
        val markers = listOf(
            PacMazeMapMarker(PacMazeMarkerKind.CHECKPOINT, 7, 10, tag = "L13-W"),
        )
        val lv = level(13, markers)
        assertNull(PacMazeGhostGateAi.ambushTarget(world(13), lv, ghost("ghost_0", 5, 5), pac(15, 10)))
    }

    @Test
    fun ambushTarget_pointsAtWingWhenPacOnOppositeSide() {
        val markers = listOf(
            PacMazeMapMarker(PacMazeMarkerKind.CHECKPOINT, 7, 10, tag = "L14-W"),
            PacMazeMapMarker(PacMazeMarkerKind.CHECKPOINT, 15, 10, tag = "L14-E"),
        )
        val lv = level(14, markers)
        val target = PacMazeGhostGateAi.ambushTarget(world(), lv, ghost("ghost_0", 5, 5), pac(16, 10))
        assertNotNull(target)
    }

    @Test
    fun effectiveAmbushWeight_risesWhenGateOpen() {
        val markers = listOf(
            PacMazeMapMarker(PacMazeMarkerKind.CHECKPOINT, 7, 10, tag = "L14-W"),
        )
        val lv = level(14, markers)
        val g = ghost("ghost_0", 6, 10)
        val openWeight = PacMazeGhostGateAi.effectiveAmbushWeight(world(), lv, g)
        val closedState = world().copy(dynamicsTick = PacMazeMapDynamics.dynamicPhaseTicks(14))
        val closedWeight = PacMazeGhostGateAi.effectiveAmbushWeight(closedState, lv, g)
        assert(openWeight > closedWeight)
    }
}
