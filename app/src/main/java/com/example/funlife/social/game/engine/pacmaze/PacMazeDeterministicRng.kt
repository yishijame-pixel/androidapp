package com.example.funlife.social.game.engine.pacmaze

/** 可复现 LCG，供 lockstep / 单元测试使用。 */
class PacMazeDeterministicRng(seed: Long) {
    private var state = if (seed == 0L) 1L else seed

    fun nextInt(bound: Int): Int {
        if (bound <= 0) return 0
        state = (state * 6364136223846793005L + 1L) and Long.MAX_VALUE
        return (state % bound).toInt()
    }

    fun nextFloat(): Float = nextInt(10_000) / 10_000f
}
