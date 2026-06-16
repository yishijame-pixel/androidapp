package com.example.funlife.social.game.engine.pacmaze

import java.util.TreeMap

/**
 * tick → 该 tick 结束后的世界快照（用于回滚重放）。
 */
class PacMazeRollbackStateRing(
    private val maxEntries: Int = 128,
) {
    private val states = TreeMap<Long, PacMazeWorldState>()

    fun save(tick: Long, state: PacMazeWorldState) {
        if (tick < 0L) return
        states[tick] = state
        while (states.size > maxEntries) states.pollFirstEntry()
    }

    /** tick 结束后的世界；tick=0 为初始帧。 */
    fun getAfterTick(tick: Long): PacMazeWorldState? = states[tick]

    fun clear() {
        states.clear()
    }
}
