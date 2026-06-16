package com.example.funlife.social.game.engine.pacmaze

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PacMazeGhostRosterTest {

    @Test
    fun defaultKind_progressesWithLevel() {
        assertEquals(GhostKind.ARCADE_RED, PacMazeGhostRoster.defaultKind(1, 0))
        assertTrue(PacMazeGhostRoster.defaultKind(4, 1).silhouette != GhostKind.ARCADE_RED.silhouette)
        assertTrue(PacMazeGhostRoster.defaultKind(8, 2).silhouette != PacMazeGhostRoster.defaultKind(8, 1).silhouette)
    }

    @Test
    fun activeKindsForLevel_unlocksIncrementally() {
        assertTrue(PacMazeGhostRoster.activeKindsForLevel(1).contains(GhostKind.ARCADE_RED))
        assertEquals(GhostKind.entries.size, GhostKind.codexOrder.size)
        assertTrue(PacMazeGhostRoster.activeKindsForLevel(7).contains(GhostKind.FLANKER))
        assertTrue(PacMazeGhostRoster.activeKindsForLevel(18).contains(GhostKind.CACHE_BLOB))
        assertEquals(16, PacMazeGhostRoster.activeKindsForLevel(20).size)
    }

    @Test
    fun defaultSpecialty_assignsGateKeeperForFlankerAtHighLevels() {
        val specialty = PacMazeGhostRoster.defaultSpecialty(
            levelId = 16,
            kind = GhostKind.FLANKER,
            hasDynamicTiles = true,
            hasEnergyGates = true,
        )
        assertEquals(GhostSpecialty.GATE_KEEPER, specialty)
    }

    @Test
    fun resolveSpawns_preservesPositions() {
        val spawns = PacMazeGhostRoster.resolveSpawns(
            levelId = 10,
            positions = listOf(3 to 5, 8 to 5),
        )
        assertEquals(2, spawns.size)
        assertEquals(3, spawns[0].x)
        assertEquals(5, spawns[0].y)
        assertNotEquals(spawns[0].kind.silhouette, spawns[1].kind.silhouette)
    }

    @Test
    fun resolveSpawns_prefersUniqueSilhouettesOnHighLevels() {
        val positions = List(6) { it to 5 }
        val spawns = PacMazeGhostRoster.resolveSpawns(levelId = 20, positions = positions)
        val silhouettes = spawns.map { it.kind.silhouette }
        assertEquals(6, silhouettes.distinct().size)
    }
}

class PacMazeGhostAiTest {

    private fun level(id: Int = 10) = PacMazeLevelConfig(
        id = id,
        name = "ai-test",
        width = 15,
        height = 13,
        pacSpawn = 7 to 11,
        ghostSpawns = listOf(
            PacMazeGhostSpawnDef(6, 5, GhostKind.STRIKER),
            PacMazeGhostSpawnDef(8, 5, GhostKind.PREDICTOR),
        ),
        aiAggression = 1f,
    )

    private fun world(levelId: Int = 10) = PacMazeWorldState(
        tick = 0L,
        levelId = levelId,
        tiles = IntArray(15 * 13) { TileType.EMPTY.code },
        width = 15,
        height = 13,
        entities = emptyList(),
        score = 0,
        lives = 3,
        pelletsRemaining = 0,
        phase = PacMazePhase.PLAYING,
        rngSeed = 42L,
        ghostMode = GhostMode.CHASE,
    )

    private fun pac(x: Int, y: Int, dir: Direction = Direction.RIGHT) = PacMazeEntity(
        id = "pac1",
        role = "pac",
        x = x.toFloat(),
        y = y.toFloat(),
        direction = dir,
        facing = dir,
        speed = 1f,
    )

    private fun ghost(kind: GhostKind, x: Int, y: Int, dir: Direction? = null) = PacMazeEntity(
        id = "ghost_0",
        role = "ghost",
        x = x.toFloat(),
        y = y.toFloat(),
        direction = dir,
        facing = dir ?: Direction.LEFT,
        speed = 1f,
        ghostMode = GhostMode.CHASE,
        ghostKind = kind,
    )

    @Test
    fun striker_chasesPacTile() {
        val lv = level()
        val state = world()
        val g = ghost(GhostKind.STRIKER, 5, 5, dir = Direction.UP)
        val p = pac(9, 5)
        val rng = PacMazeDeterministicRng(1L)
        val dir = PacMazeGhostAi.pickDirection(state, g, p, rng, lv)!!
        assertEquals(Direction.RIGHT, dir)
    }

    @Test
    fun predictor_prefersForwardIntercept() {
        val lv = level()
        val state = world()
        val g = ghost(GhostKind.PREDICTOR, 5, 5, dir = Direction.UP)
        val p = pac(7, 5, Direction.RIGHT)
        val rng = PacMazeDeterministicRng(1L)
        val dir = PacMazeGhostAi.pickDirection(state, g, p, rng, lv)!!
        assertEquals(Direction.RIGHT, dir)
    }
}
