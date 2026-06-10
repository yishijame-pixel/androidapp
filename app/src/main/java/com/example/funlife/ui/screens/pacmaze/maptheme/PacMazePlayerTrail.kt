package com.example.funlife.ui.screens.pacmaze.maptheme

import androidx.compose.ui.geometry.Offset
import kotlin.math.hypot

/** 记录玩家最近位置，用于赛博风运动拖尾。 */
class PacMazePlayerTrail(private val capacity: Int = 10) {
    private val points = ArrayDeque<Offset>()

    fun push(point: Offset) {
        if (points.isNotEmpty()) {
            val last = points.last()
            if (hypot(point.x - last.x, point.y - last.y) < 1.5f) return
        }
        points.addLast(point)
        while (points.size > capacity) points.removeFirst()
    }

    fun reset() {
        points.clear()
    }

    fun snapshot(): List<Offset> = points.toList()
}
