package com.example.funlife.ui.screens.pacmaze.cosmetic.trail

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path

/** 沿采样点构建平滑曲线路径（二次贝塞尔）。 */
internal fun buildSmoothTrailPath(points: List<Offset>): Path? {
    if (points.size < 2) return null
    if (points.size == 2) {
        return Path().apply {
            moveTo(points[0].x, points[0].y)
            lineTo(points[1].x, points[1].y)
        }
    }
    return Path().apply {
        moveTo(points[0].x, points[0].y)
        for (i in 1 until points.size) {
            val prev = points[i - 1]
            val curr = points[i]
            val mid = Offset((prev.x + curr.x) * 0.5f, (prev.y + curr.y) * 0.5f)
            if (i == 1) {
                lineTo(mid.x, mid.y)
            } else {
                quadraticBezierTo(prev.x, prev.y, mid.x, mid.y)
            }
        }
        val last = points.last()
        val prev = points[points.size - 2]
        quadraticBezierTo(prev.x, prev.y, last.x, last.y)
    }
}
