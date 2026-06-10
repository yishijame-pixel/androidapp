package com.example.funlife.social.game.engine.pacmaze

import java.util.concurrent.ConcurrentHashMap

/**
 * 商业级输入缓冲：保存当前方向与目标方向，支持提前输入、路口自动转向且不丢输入。
 */
class PacMazeInputBuffer {
    private data class Entry(
        var current: Direction? = null,
        var queued: Direction? = null,
        var active: Boolean = false,
    )

    private val entries = ConcurrentHashMap<String, Entry>()

    fun push(playerId: String, direction: Direction?) {
        if (direction != null) {
            val entry = entries.getOrPut(playerId) { Entry() }
            entry.current = direction
            entry.queued = direction
            entry.active = true
        } else {
            entries[playerId]?.active = false
        }
    }

    fun poll(playerId: String): PacMazeInputState {
        val entry = entries[playerId] ?: return PacMazeInputState.Inactive
        return PacMazeInputState(
            current = entry.current,
            queued = entry.queued,
            active = entry.active,
        )
    }

    /** 路口成功转向后同步当前方向，保留 queued 以便连续提前输入。 */
    fun commitTurn(playerId: String, direction: Direction) {
        val entry = entries.getOrPut(playerId) { Entry() }
        entry.current = direction
    }

    fun clear(playerId: String) {
        entries.remove(playerId)
    }
}
