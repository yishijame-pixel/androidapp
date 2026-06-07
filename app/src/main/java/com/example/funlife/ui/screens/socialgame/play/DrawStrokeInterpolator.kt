package com.example.funlife.ui.screens.socialgame.play

/**
 * 猜词方远程笔迹插值：在 RTT 较高时向前外推笔尖，提升跟手感（不改变真实同步点）。
 */
object DrawStrokeInterpolator {

    private const val ACTIVE_TAIL_MS = 600L
    private const val EXTRAPOLATE_WINDOW_MS = 120f

    fun renderPoints(
        points: List<Pair<Float, Float>>,
        lastRxMs: Long,
        nowMs: Long = System.currentTimeMillis(),
    ): List<Pair<Float, Float>> {
        if (points.size < 2 || lastRxMs <= 0L) return points
        val age = nowMs - lastRxMs
        if (age > ACTIVE_TAIL_MS) return points
        val (x0, y0) = points[points.size - 2]
        val (x1, y1) = points.last()
        val t = (age.coerceAtMost(ACTIVE_TAIL_MS).toFloat() / EXTRAPOLATE_WINDOW_MS).coerceIn(0f, 1.5f)
        val dx = x1 - x0
        val dy = y1 - y0
        val tipX = (x1 + dx * t * 0.75f).coerceIn(0f, 1f)
        val tipY = (y1 + dy * t * 0.75f).coerceIn(0f, 1f)
        if (kotlin.math.abs(tipX - x1) < 0.00005f && kotlin.math.abs(tipY - y1) < 0.00005f) {
            return points
        }
        return points + (tipX to tipY)
    }
}
