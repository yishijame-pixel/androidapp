package com.example.funlife.social.game.engine.pacmaze

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PacMazeMapDynamicsTest {

    @Test
    fun energyGate_blocksWhenClosed() {
        val state = baseState(energyGateOpen = false)
        assertTrue(PacMazeMapDynamics.isTileBlocking(state, TileType.ENERGY_GATE, 2, 2, forGhost = false))
    }

    @Test
    fun energyGate_allowsWhenOpen() {
        val state = baseState(energyGateOpen = true)
        assertFalse(PacMazeMapDynamics.isTileBlocking(state, TileType.ENERGY_GATE, 2, 2, forGhost = false))
    }

    @Test
    fun gate_togglesEveryCycle() {
        var state = baseState().copy(dynamicsTick = PacMazeMapDynamics.GATE_CYCLE_TICKS - 1)
        val toggled = PacMazeMapDynamics.tick(state)
        assertFalse(toggled.energyGateOpen)
    }

    private fun baseState(energyGateOpen: Boolean = true): PacMazeWorldState {
        val tiles = IntArray(25) { TileType.PATH.code }
        tiles[12] = TileType.ENERGY_GATE.code
        return PacMazeWorldState(
            tick = 0,
            levelId = 1,
            tiles = tiles,
            width = 5,
            height = 5,
            entities = emptyList(),
            score = 0,
            lives = 3,
            pelletsRemaining = 0,
            phase = PacMazePhase.PLAYING,
            rngSeed = 1L,
            energyGateOpen = energyGateOpen,
        )
    }
}
