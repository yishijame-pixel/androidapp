package com.example.funlife.ui.screens.socialgame.play

/**
 * Catmull-Rom 样条平滑：渲染层专用，不改变同步点序列。
 */
object DrawStrokeSmoother {

    private const val MIN_POINTS_FOR_SPLINE = 4
    private const val SEGMENTS_PER_SPAN = 6

    fun smoothForRender(points: List<Pair<Float, Float>>): List<Pair<Float, Float>> {
        if (points.size < 3) return points
        if (points.size < MIN_POINTS_FOR_SPLINE) {
            return quadraticFallback(points)
        }
        val result = ArrayList<Pair<Float, Float>>(points.size * SEGMENTS_PER_SPAN)
        val extended = buildExtended(points)
        for (i in 0 until extended.size - 3) {
            val p0 = extended[i]
            val p1 = extended[i + 1]
            val p2 = extended[i + 2]
            val p3 = extended[i + 3]
            if (i == 0) result.add(p1)
            for (s in 1..SEGMENTS_PER_SPAN) {
                val t = s.toFloat() / SEGMENTS_PER_SPAN
                result.add(catmullRom(p0, p1, p2, p3, t))
            }
        }
        return dedupeAdjacent(result)
    }

    private fun buildExtended(points: List<Pair<Float, Float>>): List<Pair<Float, Float>> {
        val first = points.first()
        val last = points.last()
        return listOf(first) + points + listOf(last)
    }

    private fun catmullRom(
        p0: Pair<Float, Float>,
        p1: Pair<Float, Float>,
        p2: Pair<Float, Float>,
        p3: Pair<Float, Float>,
        t: Float,
    ): Pair<Float, Float> {
        val t2 = t * t
        val t3 = t2 * t
        val x = 0.5f * (
            (2f * p1.first) +
                (-p0.first + p2.first) * t +
                (2f * p0.first - 5f * p1.first + 4f * p2.first - p3.first) * t2 +
                (-p0.first + 3f * p1.first - 3f * p2.first + p3.first) * t3
            )
        val y = 0.5f * (
            (2f * p1.second) +
                (-p0.second + p2.second) * t +
                (2f * p0.second - 5f * p1.second + 4f * p2.second - p3.second) * t2 +
                (-p0.second + 3f * p1.second - 3f * p2.second + p3.second) * t3
            )
        return x.coerceIn(0f, 1f) to y.coerceIn(0f, 1f)
    }

    private fun quadraticFallback(points: List<Pair<Float, Float>>): List<Pair<Float, Float>> {
        if (points.size < 2) return points
        val result = ArrayList<Pair<Float, Float>>(points.size * 3)
        result.add(points.first())
        for (i in 0 until points.size - 1) {
            val p0 = if (i == 0) points[i] else points[i - 1]
            val p1 = points[i]
            val p2 = points[i + 1]
            val mid = (p1.first + p2.first) * 0.5f to (p1.second + p2.second) * 0.5f
            for (s in 1..4) {
                val t = s / 4f
                val u = 1f - t
                val x = u * u * p0.first + 2f * u * t * p1.first + t * t * p2.first
                val y = u * u * p0.second + 2f * u * t * p1.second + t * t * p2.second
                result.add(x.coerceIn(0f, 1f) to y.coerceIn(0f, 1f))
            }
            result.add(mid)
        }
        result.add(points.last())
        return dedupeAdjacent(result)
    }

    private fun dedupeAdjacent(points: List<Pair<Float, Float>>): List<Pair<Float, Float>> {
        if (points.size <= 1) return points
        val out = ArrayList<Pair<Float, Float>>(points.size)
        var last: Pair<Float, Float>? = null
        points.forEach { p ->
            if (last == null || kotlin.math.abs(last!!.first - p.first) > 0.00005f ||
                kotlin.math.abs(last!!.second - p.second) > 0.00005f
            ) {
                out.add(p)
                last = p
            }
        }
        return out
    }
}
