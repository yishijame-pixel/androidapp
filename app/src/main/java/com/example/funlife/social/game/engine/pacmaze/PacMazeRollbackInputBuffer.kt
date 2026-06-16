package com.example.funlife.social.game.engine.pacmaze

import java.util.TreeMap

/** tick → 该逻辑帧输入（本地 / 远端分离存储）。 */
class PacMazeRollbackInputBuffer {
    private val inputs = TreeMap<Long, PacMazeTickInput>()
    private val attacks = TreeMap<Long, Boolean>()

    fun put(tick: Long, input: PacMazeTickInput) {
        inputs[tick] = input
        trim()
    }

    fun markAttack(tick: Long) {
        attacks[tick] = true
        trim()
    }

    fun get(tick: Long): PacMazeTickInput? = inputs[tick]

    fun hasAttack(tick: Long): Boolean = attacks[tick] == true

    fun clear() {
        inputs.clear()
        attacks.clear()
    }

    private fun trim() {
        while (inputs.size > MAX_ENTRIES) inputs.pollFirstEntry()
        while (attacks.size > MAX_ENTRIES) attacks.pollFirstEntry()
    }

    companion object {
        private const val MAX_ENTRIES = 256
    }
}
