package com.example.funlife.social.pacmazews

import android.util.Log
import com.example.funlife.social.game.engine.pacmaze.PacMazeConstants
import com.example.funlife.social.game.engine.pacmaze.PacMazeWorldState

/**
 * 在线对战诊断日志（logcat 过滤：PacMazeOnlineVM / PacMazeWs）
 *
 * adb logcat -s PacMazeOnlineVM PacMazeWs
 */
object PacMazeOnlineDiagnostics {
    const val VM_TAG = "PacMazeOnlineVM"
    const val WS_TAG = "PacMazeWs"

    fun entitySummary(world: PacMazeWorldState): String =
        world.entities.joinToString { e ->
            val role = when (e.role) {
                "pac_a", "pac_b" -> "玩家"
                "ghost" -> "幽灵"
                else -> e.role
            }
            "$role(${e.id})@${e.x.format1()},${e.y.format1()}"
        }

    fun ghostReleaseSeconds(ticksLeft: Int): Int =
        (ticksLeft / PacMazeConstants.TICKS_PER_SECOND).coerceAtLeast(0)

    private fun Float.format1(): String = "%.1f".format(this)
}
